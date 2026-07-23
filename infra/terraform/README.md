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
    ec2/             Instancias gemelas legado/modernizado + IAM mínimo
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

# 1) Todo menos las instancias EC2 (necesitamos el repo ECR creado primero)
terraform plan  -target=module.network -target=module.ecr -target=module.rds \
  -target=module.observability -out=tfplan-infra
terraform apply tfplan-infra

# 2) Publicar la imagen en el ECR recién creado
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin "$(terraform output -raw ecr_repository_url)"
docker tag spring-petclinic:latest "$(terraform output -raw ecr_repository_url):latest"
docker push "$(terraform output -raw ecr_repository_url):latest"

# 3) Ahora sí, las instancias EC2 (su user_data ya encuentra la imagen)
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

## Decisiones de diseño relevantes

- **Sin credenciales hardcodeadas.** `db_password` es `sensitive = true` y no tiene default;
  `terraform.tfvars` está en `.gitignore` (solo se versiona el `.example`).
- **RDS en subred privada, sin acceso público**, con `storage_encrypted = true` y backups
  deshabilitados (`backup_retention_period = 0`): decisión documentada como propia de un entorno
  de experimento, no de producción (ver comentario en `modules/rds/main.tf`).
- **Security group de RDS referencia el SG de la app por `security_group_id`**, nunca abre 5432 a
  un CIDR — solo el tráfico que ya pasó por la capa de la aplicación puede llegar a la base.
- **IAM de mínimo privilegio en EC2**: solo `ecr:GetAuthorizationToken`/`BatchGetImage`/etc. para
  el pull de la imagen, y `logs:*`/`cloudwatch:PutMetricData` acotado al log group del proyecto.
  Nada de policies gestionadas amplias ni `AdministratorAccess`.
- **`instance_count = 2` por defecto**: el pre-experimento pide instancias gemelas (legado y
  modernizado) para poder comparar métricas bajo las mismas condiciones de infraestructura.
- **Alarma de memoria depende del agente de CloudWatch** instalado vía `user_data`
  (`modules/ec2/user_data.sh.tftpl`): EC2 no publica memoria de forma nativa. No se pudo verificar
  contra una cuenta AWS real que el paquete `amazon-cloudwatch-agent` esté disponible tal cual en
  los repos de Amazon Linux 2023 al momento de aplicar — queda anotado como riesgo a validar por
  el equipo antes de un `apply` real.

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
