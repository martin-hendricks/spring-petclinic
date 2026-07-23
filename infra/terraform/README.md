# Infraestructura AWS — Spring PetClinic (Fase 6)

Terraform para desplegar la app modernizada sobre AWS: **ECR + EC2 (instancias gemelas
legado/modernizado) + RDS PostgreSQL + CloudWatch**, sin pipeline de CI/CD (fuera de alcance del
experimento).

## Estructura

```
infra/terraform/
  main.tf  providers.tf  variables.tf  outputs.tf  terraform.tfvars.example
  modules/
    network/        VPC, subredes públicas/privadas, security groups
    ecr/             Repositorio de la imagen de la app
    rds/             PostgreSQL 18.x en subred privada
    ec2/             Instancias gemelas legado/modernizado (reutiliza IAM existente, ver nota abajo)
    observability/   Log group y alarmas CPU/memoria
```

## Prerrequisitos

1. Cuenta AWS con credenciales configuradas (`aws configure` o variables `AWS_ACCESS_KEY_ID` /
   `AWS_SECRET_ACCESS_KEY` / `AWS_SESSION_TOKEN`). Este repositorio **no** incluye credenciales.
2. Terraform >= 1.6 (se validó con 1.14.5) y el proveedor `hashicorp/aws ~> 5.0`.
3. Copiar `terraform.tfvars.example` a `terraform.tfvars` (excluido de git) y completar al menos
   `db_password` — o exportar `TF_VAR_db_password` en el shell, sin escribirlo en ningún archivo.
4. La imagen de la app construida en la Fase 5 (`docker build -t spring-petclinic .` desde la raíz
   del repo) lista para publicarse en el ECR que este módulo crea.

## Orden de aplicación

El grafo de dependencias de Terraform ya resuelve el orden entre recursos, pero **la imagen de la
app debe existir en ECR antes de que las instancias EC2 arranquen**, porque su `user_data` hace
`docker pull` una sola vez al lanzar (no reintenta en un bucle). Por eso se recomienda un apply en
dos pasos:

```bash
cd infra/terraform
terraform init

# 1) Red, ECR y RDS únicamente (module.observability NO va aquí: depende de
#    module.ec2.instance_ids, así que -target lo arrastraría junto con las instancias EC2
#    y perdería el sentido de este primer paso — se aplica junto con ec2 en el paso 3).
terraform plan -target=module.network -target=module.ecr -target=module.rds -out=tfplan-infra
terraform apply tfplan-infra

# 2) Publicar la imagen en el ECR recién creado
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin "$(terraform output -raw ecr_repository_url)"
docker tag spring-petclinic:latest "$(terraform output -raw ecr_repository_url):latest"
docker push "$(terraform output -raw ecr_repository_url):latest"

# 3) Ahora sí, el resto completo (EC2 + observability; su user_data ya encuentra la imagen)
terraform plan -out=tfplan-full
terraform apply tfplan-full
```

Al terminar de medir, liberar los recursos:

```bash
terraform destroy
```

> **Advertencia de costo.** `terraform apply` crea recursos facturables (EC2 ×2 + RDS corriendo
> 24/7 no son gratis). Aplica solo cuando vayan a medir, y ejecuten `terraform destroy` al
> terminar. Este repositorio **no ejecuta `terraform apply` automáticamente** — es una acción
> manual y deliberada del equipo, fuera del alcance de lo que genera este experimento.

## Cómo acceder a la app desplegada

```bash
terraform output ec2_public_ips
```

Con esas IPs, **hay que especificar el esquema y el puerto explícitamente** — no hay balanceador
ni reverse proxy en 80/443, todo se sirve directo en 8080 sobre HTTP plano (sin TLS):

```
http://<ip-instancia-0>:8080/              # UI Thymeleaf (rol "legado")
http://<ip-instancia-1>:8080/api/owners/1  # API REST (rol "modernizado")
http://<ip>:8080/actuator/health           # health check en cualquiera de las dos
```

**Hallazgo real de esta sesión:** al compartir solo la IP desnuda, el primer intento de acceso
falló — el navegador interpretó `https://<ip>` por defecto (sin puerto), y como no hay nada
escuchando en 443/80, la conexión se rechazó. No es un problema de seguridad ni de la instancia:
las IPs y el DNS público que AWS asigna automáticamente
(`ec2-<ip-con-guiones>.compute-1.amazonaws.com`) **no** implican un esquema ni un puerto por
defecto — hay que escribir `http://` y `:8080` siempre. Este repositorio no configura Route 53
ni ningún dominio propio.

Las IPs son efímeras: cambian cada vez que Terraform recrea las instancias (`-replace`, o un
`apply` que fuerce reemplazo por cambios en `user_data`/AMI/etc.). Volver a correr `terraform
output ec2_public_ips` después de cualquier apply para obtener las vigentes.

## Decisiones de diseño relevantes

- **Sin credenciales hardcodeadas.** `db_password` es `sensitive = true` y no tiene default;
  `terraform.tfvars` está en `.gitignore` (solo se versiona el `.example`).
- **RDS en subred privada, sin acceso público**, con `storage_encrypted = true` y backups
  deshabilitados (`backup_retention_period = 0`): decisión documentada como propia de un entorno
  de experimento, no de producción (ver comentario en `modules/rds/main.tf`).
- **Security group de RDS referencia el SG de la app por `security_group_id`**, nunca abre 5432 a
  un CIDR — solo el tráfico que ya pasó por la capa de la aplicación puede llegar a la base.
- **IAM de EC2 — desviación registrada.** El diseño original creaba un `aws_iam_role` propio de
  mínimo privilegio (solo `ecr:GetAuthorizationToken`/`BatchGetImage`/etc. y
  `logs:*`/`cloudwatch:PutMetricData` acotado). La cuenta usada para el despliegue real (AWS
  Academy Learner Lab) deniega `iam:CreateRole` por política de la cuenta (verificado con un
  `create-role` de prueba: `AccessDenied`), así que el módulo `ec2` ahora recibe
  `instance_profile_name` como variable y reutiliza el `LabInstanceProfile` pre-aprovisionado
  (default `"LabInstanceProfile"`), que ya trae adjunta `AmazonEC2ContainerRegistryReadOnly` — cubre
  el pull de ECR, aunque con permisos más amplios de lo que el mínimo privilegio original hubiera
  elegido. En una cuenta AWS sin esa restricción, se puede reintroducir el rol propio y pasar su
  nombre por la misma variable.
- **`instance_count = 2` por defecto**: el pre-experimento pide instancias gemelas (legado y
  modernizado) para poder comparar métricas bajo las mismas condiciones de infraestructura.
- **Alarma de memoria depende del agente de CloudWatch** instalado vía `user_data`
  (`modules/ec2/user_data.sh.tftpl`): EC2 no publica memoria de forma nativa. **Verificado en
  un despliegue real**: el paquete `amazon-cloudwatch-agent` sí está disponible en los repos de
  Amazon Linux 2023 y se instala sin problema — el riesgo que se había anotado en el checkpoint
  6.2 no se materializó. Las alarmas de memoria sí quedan en `INSUFFICIENT_DATA` los primeros
  ~10 minutos (`evaluation_periods = 2` × `period = 300s`), es esperado, no un error.
- **`root_block_device` con tamaño explícito (`root_volume_size`, default 20 GB) — desviación
  descubierta en el primer despliegue real.** La AMI base de Amazon Linux 2023 usada en este
  Learner Lab trae solo **2 GB** de disco raíz (`aws ec2 describe-images` lo confirmó),
  insuficiente para `docker` + `amazon-cloudwatch-agent` (~517 MB instalados). Sin este bloque,
  `dnf install` falla a mitad de camino ("needs 49MB more space") y, como `user_data` usa `set
  -euxo pipefail`, el resto del script (arranque de Docker, pull, `docker run`) nunca se ejecuta
  — la instancia queda arriba pero sin la app. Diagnosticado con `aws ec2 get-console-output`.
- **`SPRING_PROFILES_ACTIVE=` (vacío) forzado en el `docker run` de `user_data` — segunda
  desviación descubierta en el mismo despliegue.** El `Dockerfile` (Fase 5) fija
  `SPRING_PROFILES_ACTIVE=postgres` como default de la imagen, pensado para
  `docker-compose.app.yml`, donde sí hay un sidecar de PostgreSQL con las credenciales
  inyectadas por variables de entorno. En EC2, `user_data` deliberadamente **no** pasa las
  credenciales de RDS al contenedor (para no exponerlas en texto plano dentro de `user_data`,
  legible por cualquiera con permiso `DescribeInstanceAttribute`) — sin el override, el
  contenedor intentaba conectarse a Postgres en `localhost`, fallaba, y `--restart
  unless-stopped` lo reiniciaba en bucle indefinidamente (visible en `get-console-output` como
  interfaces `veth*` de Docker creándose y destruyéndose cada 10-60s). **Consecuencia real:** las
  instancias EC2 de este despliegue sirven la app contra **H2 embebida, no contra la RDS que
  Terraform aprovisionó** — el RDS existe, está cifrado y accesible desde el security group de
  la app, pero nada lo usa todavía. Conectarlas de verdad requeriría un mecanismo de secretos
  (SSM Parameter Store / Secrets Manager) en vez de variables de entorno en `user_data`; queda
  fuera del alcance resuelto en esta sesión.

## Estimación de costo mensual aproximado

Precios de lista on-demand en `us-east-1`, orientativos (no se consultó la AWS Pricing API en
vivo desde este entorno — verificar con la [calculadora oficial de AWS](https://calculator.aws)
antes de comprometer presupuesto):

| Recurso | Cantidad | Costo aprox./mes |
|---|---|---|
| EC2 `t3.medium` (24/7) | 2 (legado + modernizado) | ~ USD 60 |
| RDS `db.t3.micro` PostgreSQL (24/7) | 1 | ~ USD 13 |
| Almacenamiento RDS (20 GB gp2) | 1 | ~ USD 2 |
| ECR (almacenamiento de imágenes, ≤10) | 1 repo | < USD 1 |
| CloudWatch (log group 7 días + 4 alarmas) | — | ~ USD 2-3 |
| Transferencia de datos (estimado, uso bajo) | — | ~ USD 1-5 |
| **Total aproximado corriendo 24/7** | | **~ USD 80-85/mes** |

Si solo se enciende durante las sesiones de medición (p. ej. unas pocas horas totales), el costo
real es una fracción pequeña de esa cifra — es lo que motiva la recomendación de `terraform
destroy` entre sesiones. Considerar `db.t3.micro`/`t3.small` (ya son los defaults más económicos
razonables) si el presupuesto del equipo es una restricción dura.
