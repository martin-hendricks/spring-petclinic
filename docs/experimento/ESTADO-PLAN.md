# Estado de ejecución del plan — `CLAUDE.md`

Seguimiento vivo de las 8 fases del plan de modernización (Equipo 17). Se actualiza al cerrar cada
fase o punto de control. Rama de trabajo: `feature/modernizado`.

Leyenda: ⬜ Pendiente · 🟨 En progreso · ✅ Completa · 🛑 Bloqueada / esperando aprobación

| Fase | Objetivo | Estado | Última actualización |
|---|---|---|---|
| 0 | Preparación: contexto, baseline y rama | ✅ Completa | 2026-07-21 |
| 1 | Replatform Java 17 → 21 | ✅ Completa | 2026-07-21 |
| 2 | F-03 — Capa `@Service` (Pets & Visits) | ✅ Completa | 2026-07-21 |
| 3 | F-05 — API REST Owners | ✅ Completa | 2026-07-21 |
| 4 | Diagramas secuencia/clases to-be | ✅ Completa | 2026-07-22 |
| 5 | Contenedores | ✅ Completa | 2026-07-22 |
| 6 | Terraform sobre AWS | ✅ Completa | 2026-07-22 |
| 7 | Estimación y esfuerzo real | ✅ Completa | 2026-07-22 |

---

## Fase 0 — Preparación

- [x] `CLAUDE.md` ya existe en la raíz del repo (estaba sin trackear en git al iniciar esta sesión).
- [x] Rama de trabajo: el repo ya estaba en `feature/modernizado` (no `feature/experimento-modernizacion`
      como sugiere el plan). **Desviación registrada**, se mantiene esa rama en vez de crear una nueva
      para no duplicar el trabajo ya commiteado ahí.
- [x] `docs/experimento/baseline-metrics.md` generado: LOC por archivo (paquete `owner` y total
      `src/main`), resultado de `./mvnw verify` (47 tests, 0 fallos, BUILD SUCCESS) y listado de los
      17 endpoints HTTP actuales.
- [x] Deviación de entorno registrada: la máquina solo tenía JDK 8; se instaló Temurin 17.0.16 vía
      `sdkman` para poder ejecutar `./mvnw verify`. No se modificó código fuente.
- [x] Commit de cierre de Fase 0 (`CLAUDE.md` + `docs/experimento/`).

## Fase 1 — Java 21

- [x] `pom.xml`: `<java.version>` 17 → 21
- [x] `build.gradle`: toolchain → 21
- [x] Revisado `maven-enforcer-plugin` (`requireJavaVersion`): usa `${java.version}`, no necesitó
      cambio propio, solo heredó el nuevo valor de la propiedad.
- [x] `./mvnw verify` en verde con Java 21 (Temurin 21.0.11, instalado vía `sdkman`): 47 tests,
      0 fallos, `BUILD SUCCESS` en 1m19s — mismo resultado que la línea base en Java 17.
- [x] No se tocó código fuente ni se usaron features de Java 21 (records/pattern matching/virtual
      threads), tal como pide el alcance estricto de esta fase.
- [ ] Nota: `.github/workflows/gradle-build.yml` y `.github/workflows/maven-build.yml` siguen
      referenciando Java 17. **No se tocaron** porque cualquier pipeline de CI/CD está fuera de
      alcance del experimento (ver `CLAUDE.md`, sección "FUERA de alcance"). Se deja registrado
      como desviación conocida, no como pendiente a resolver dentro de este plan.
- [x] Commit de cierre de Fase 1.

## Fase 2 — F-03 (Service Pets & Visits)

- [x] Punto de control resuelto: propuse dos opciones (excepción de dominio con lista de
      violaciones vs. objeto de resultado). El usuario aprobó la **Opción A** (excepción de
      dominio `ValidationException` + `FieldViolation`, con el controller comprobando
      `result.hasErrors()` de Bean Validation antes de persistir).
- [x] `PetService` (`createPet`, `updatePet`, `validateDuplicateName`, `validateBirthDate`,
      todos `@Transactional` donde corresponde) — reproduce exactamente la semántica original de
      `Owner.getPet(name, ignoreNew)` usando `pet.isNew()` como flag `ignoreNew` (unifica los dos
      chequeos de duplicado de create/update en un solo método).
- [x] `VisitService` (`createVisit`, `validateVisitDate`)
- [x] `FieldViolation` (record) y `ValidationException` (excepción compartida, package-private)
- [x] Refactor `PetController` / `VisitController` a orquestación pura (ya no llaman
      `owners.save(...)` directamente)
- [x] `PetController.java`: **147 líneas crudas** (`wc -l`) / **98 líneas** con la metodología
      "sin comentarios ni blancos" del §0.2 del plan. El original tenía 183/124 respectivamente
      con esas mismas dos métricas — el criterio "<100 LOC" del plan solo es alcanzable con la
      segunda metodología (ver desviación #4 abajo); con esa métrica **sí se cumple** (98 < 100).
- [x] `PetServiceTests` (4 tests) y `VisitServiceTests` (3 tests) con Mockito puro
      (`@ExtendWith(MockitoExtension.class)`, sin contexto Spring), cubriendo: nombre duplicado al
      crear (rechaza), nombre duplicado al editar la misma mascota por id (acepta), `birthDate`
      futura (rechaza), caso feliz con `verify(owners, times(1)).save(...)`; y para visitas: fecha
      pasada (rechaza), fecha de hoy (rechaza, la regla es estrictamente futura), caso feliz.
- [x] `./mvnw verify` en verde: **54 tests, 0 fallos** (47 originales + 7 nuevos), sin modificar
      ninguna aserción existente.
- [x] Corrección durante la implementación: `PetControllerTests`/`VisitControllerTests` fallaban
      al cargar el contexto porque `@WebMvcTest` no registra beans `@Service` y el constructor de
      los controllers ahora exige `PetService`/`VisitService`. Se resolvió con `@Import(PetService
      .class)` / `@Import(VisitService.class)` (trae el service **real**, no un mock, usando el
      `OwnerRepository` ya mockeado) — cero cambios de aserciones, solo una anotación añadida.
- [x] Corrección de diseño durante la implementación: la primera versión cortaba en
      `result.hasErrors()` **antes** de invocar al service, lo que rompía dos tests que combinan
      un error de Bean Validation (`type` faltante) con una regla de negocio (nombre duplicado /
      fecha futura) en la misma petición — el código original evaluaba siempre ambas reglas y
      solo cortaba al final. Se corrigió: el controller ahora invoca los métodos de validación del
      service **siempre** (antes de decidir persistir), y solo llama a `createPet`/`updatePet` si
      el resultado combinado no tiene errores; `ValidationException` queda como respaldo defensivo
      para llamadores futuros que invoquen el service directamente sin pasar por este pre-chequeo.
- [x] Commit de cierre de Fase 2.

## Fase 3 — F-05 (API REST Owners)

- [x] Punto de control resuelto: la búsqueda web inicial devolvió versiones/fechas fabricadas
      (mencionaba Spring Boot 4 en 2024, imposible). Se descartó esa fuente y se verificó
      directamente contra Maven Central (`repo1.maven.org/.../maven-metadata.xml` + los `.pom` de
      cada candidata). Resultado: `springdoc-openapi-starter-webmvc-ui:3.0.2` tiene como parent
      `spring-boot-starter-parent:4.0.3` — match exacto con este proyecto (`3.0.3` usa `4.0.5`).
      Usuario aprobó `3.0.2`.
- [x] Dependencia añadida a `pom.xml` y `build.gradle`. `./mvnw verify` en verde (54 tests, 0
      fallos). App levantada manualmente: `GET /v3/api-docs` → 200 con contrato OpenAPI válido, y
      `GET /owners` / `GET /vets` (Thymeleaf) siguen respondiendo 200 sin regresión.
- [x] `OwnerDto` / `PetDto` (records, con anotaciones `@Schema` de OpenAPI)
- [x] `OwnerMapper` (solo lectura: `Owner`→`OwnerDto`, `Pet`→`PetDto`; `type` se aplana al nombre
      del `PetType` como `String`)
- [x] `OwnerService` (`findById`, `findByLastName` paginado, `@Transactional(readOnly = true)`;
      `findById` lanza `OwnerNotFoundException` si no existe; `findByLastName` reproduce la regla
      de `OwnerController.processFindForm` de tratar `lastName == null` como búsqueda amplia)
- [x] `OwnerRestController` (`GET /api/owners` con `page`/`size`/`lastName` opcional, `GET
      /api/owners/{id}`) + `OwnerRestExceptionHandler` (`@RestControllerAdvice(assignableTypes =
      OwnerRestController.class)`, acotado a este controller para no interferir con nada más)
      traduce `OwnerNotFoundException` a 404 vía `ProblemDetail` — sin try/catch en el controller
- [x] Documentación OpenAPI (`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema`)
- [x] `OwnerRestControllerTests` (`@WebMvcTest` + `@Import(OwnerService.class)`, 5 tests): 200 con
      campos de `OwnerDto`, 404 en owner inexistente, filtro por `lastName`, listado paginado sin
      filtro, y verificación explícita de que la respuesta **no** expone campos internos de la
      entidad JPA (`new` de `BaseEntity`, `visits` de `Pet`)
- [x] Verificado en caliente (app levantada con `spring-boot:run`): `GET /api/owners/1` → 200 JSON;
      `GET /api/owners/9999` → 404; `GET /api/owners?lastName=Franklin` → 200 filtrado; `GET
      /api/owners` → 200 paginado (`totalElements=10, size=5`); `GET /swagger-ui.html` → 200;
      `GET /owners/1` y `GET /owners` (Thymeleaf) → 200 sin regresión.
- [x] `OwnerController` (MVC), `Owner`/`Pet`/`Visit`/`PetType`, `OwnerRepository` y templates:
      **sin tocar**.
- [x] `./mvnw verify` en verde: **59 tests, 0 fallos** (54 previos + 5 nuevos).
- [x] Commit de cierre de Fase 3.

## Fase 4 — Diagramas to-be

- [x] `08-secuencia-crear-pet-tobe.puml` — `POST /owners/{ownerId}/pets/new` con `PetService`;
      marca el bloque `@Transactional` (`group`), dónde se movieron las validaciones (antes en
      `PetController` L110-117, ahora en `PetService`) y compara explícitamente contra el as-is
      `07-secuencia-agregar-pet.puml` en una nota final.
- [x] `09-secuencia-api-owners-tobe.puml` — `GET /api/owners/{id}` con `OwnerService` +
      `OwnerMapper`; camino alterno 404 (`alt`) vía `OwnerNotFoundException` →
      `OwnerRestExceptionHandler` → `ProblemDetail`.
- [x] `10-clases-capa-service-tobe.puml` — `PetService`, `VisitService`, `OwnerService`,
      `OwnerDto`/`PetDto`, `OwnerMapper`, ambos controllers (MVC y REST), `OwnerRepository`,
      `ValidationException`/`FieldViolation`, `OwnerNotFoundException`; firmas de métodos y
      estereotipos (`<<Service>>`, `<<RestController>>`, `<<DTO>>`, `<<Repository>>`) tomados
      directamente del código de las Fases 2-3, no de un diseño idealizado.
- [x] `11-despliegue-tobe.puml` — adapta `02-despliegue.puml`: nodo EC2 (t3.medium) con contenedor
      Docker corriendo el JAR en Java 21, distinguiendo visualmente componentes legados
      conservados (MVC, entidades, templates — color amarillo) de los nuevos (capa `@Service` y
      API REST — color azul); RDS PostgreSQL en subred privada; ECR + CloudWatch. Anticipa las
      Fases 5-6 (aún no implementadas) tal como pide el criterio de la Fase 4.
- [x] `docs/plantuml/README.md` actualizado con la tabla "Iteración 2" (diagramas 08-11) y el
      comando de exportación vía Docker.
- [x] **PNGs exportados** — `plantuml`/`graphviz` no están instalados como binarios nativos, pero
      **Docker sí está disponible** en el entorno (verificación inicial con `which docker` fue
      errónea: se leyó la salida de un comando en background antes de que terminara de escribirla;
      corregido tras el aviso del usuario). Se generaron los 11 PNG —incluyendo los 7 as-is que
      tampoco estaban exportados— con `docker run --rm -v "$(pwd)":/data plantuml/plantuml -tpng
      -o /data/export /data/*.puml`, y se verificaron visualmente los 4 nuevos (08-11) contra el
      código real.

## Fase 5 — Contenedores

- [x] `Dockerfile` multi-stage: build con `eclipse-temurin:21-jdk` + `./mvnw` (cache de
      dependencias en capa separada vía `dependency:go-offline`), runtime con
      `eclipse-temurin:21-jre-alpine`, usuario no root `spring:spring`, `HEALTHCHECK` sobre
      `/actuator/health`, `SPRING_PROFILES_ACTIVE` parametrizable (default `postgres`).
- [x] `.dockerignore` (`target/`, `build/`, `.git/`, `docs/`, `*.md`, IDE, `.devcontainer/`).
- [x] `docker-compose.app.yml` — nuevo y separado; **no se tocó** el `docker-compose.yml`
      existente. `depends_on: condition: service_healthy` sobre un `HEALTHCHECK` de `pg_isready`
      en el servicio `postgres`.
- [x] `docs/experimento/contenedores.md` con las decisiones de diseño y el log de verificación.
- [x] **Verificado en caliente** (Docker sí está disponible en este entorno, ver desviación #5):
      `docker compose -f docker-compose.app.yml up -d --build` → `postgres` y `app` healthy;
      `GET /api/owners/1` → 200 JSON; `GET /owners/1` → 200 HTML (sin regresión Thymeleaf);
      `GET /actuator/health` → `{"status":"UP"}`; `docker compose exec app whoami` → `spring`
      (confirma usuario no root). Contenedores bajados con `down -v` al terminar la verificación.
      **Tamaño final de la imagen: 357 MB.**

## Fase 6 — Terraform AWS

- [x] Estructura `infra/terraform/` (`main.tf`, `providers.tf`, `variables.tf`, `outputs.tf`,
      `terraform.tfvars.example`, `modules/{network,ecr,rds,ec2,observability}/`).
- [x] Provider AWS fijado (`~> 5.0`, resolvió `5.100.0`), `required_version >= 1.6.0`,
      `default_tags` con `Project=spring-petclinic-modernizacion`, `Equipo=17`,
      `Ambiente=experimento`, `ManagedBy=terraform` en todos los recursos.
- [x] Módulo `network`: VPC (CIDR parametrizable), 2 subredes públicas + 2 privadas en AZs
      distintas (`data.aws_availability_zones`), Internet Gateway + route table pública,
      security group `app` (entrada al `app_port` desde internet) y security group `rds`
      (entrada 5432 **solo** desde `aws_security_group.app.id`, sin CIDR abierto).
- [x] `terraform.tfvars.example` con los valores por defecto; `terraform.tfvars` real excluido
      vía `.gitignore` (patrón `*.tfvars` con excepción `!*.tfvars.example`); sin credenciales
      hardcodeadas en ningún `.tf`.
- [x] **Checkpoint 6.1 resuelto:** `terraform init` (descargó `hashicorp/aws 5.100.0`, generó
      `.terraform.lock.hcl`), `terraform fmt -recursive` (corrigió alineación en `main.tf`) y
      `terraform validate` → `Success! The configuration is valid.`. Se intentó también
      `terraform plan` como verificación adicional (no pedida por el checkpoint): falló con
      `ExpiredToken` en STS porque no hay credenciales AWS configuradas en este entorno — es el
      comportamiento esperado sin credenciales, no un error de la configuración.
- [x] **Aprobado por el usuario, prompt 6.2 completo:**
  - `modules/ecr`: repositorio con `scan_on_push = true` y lifecycle policy que conserva las
    últimas 10 imágenes (`max_images` parametrizable).
  - `modules/rds`: PostgreSQL `18.3` (misma major version que `docker-compose.yml` del legado),
    `instance_class` parametrizable (default `db.t3.micro`), subredes **privadas**,
    `publicly_accessible = false`, `storage_encrypted = true`, backups deshabilitados con
    justificación documentada en un comentario (entorno de experimento, no producción).
  - `modules/ec2`: `t3.medium`, `instance_count` parametrizable con **default 2** (instancias
    gemelas `legado`/`modernizado`, etiquetadas por `Rol`), `user_data.sh.tftpl` instala Docker +
    agente de CloudWatch, se autentica contra ECR y arranca el contenedor; rol IAM +
    instance profile con permisos mínimos (`ecr:GetAuthorizationToken`/`BatchGetImage`/etc. y
    `logs:*`/`cloudwatch:PutMetricData` acotado al log group del proyecto) — nada de
    `AdministratorAccess`.
  - `modules/observability`: log group con retención de 7 días, alarmas de CPU (`AWS/EC2`) y de
    memoria (`CWAgent`, requiere el agente instalado en `user_data` — riesgo documentado, no se
    pudo verificar contra una cuenta AWS real que el paquete esté disponible tal cual en los
    repos de Amazon Linux 2023).
  - `outputs.tf` raíz extendido: `ecr_repository_url`, `ec2_public_ips`, `rds_endpoint`
    (`sensitive = true`).
  - `infra/terraform/README.md`: orden de aplicación en dos pasos (red/ECR/RDS/observability
    primero, publicar la imagen, luego EC2 — porque el `user_data` hace `docker pull` una sola
    vez al arrancar), prerrequisitos, estimación de costo mensual aproximado
    (~USD 80-85/mes corriendo 24/7, precios de lista orientativos no verificados contra la AWS
    Pricing API en vivo desde este entorno).
  - `terraform init`, `terraform fmt -recursive` (corrigió alineación en `main.tf`) y
    `terraform validate` → `Success! The configuration is valid.` con los 5 módulos conectados.
    **No se ejecutó `terraform plan` ni `terraform apply`** con recursos reales, tal como pide
    el plan explícitamente por control de costos.

### Despliegue real ejecutado (post-aprobación explícita del usuario, 2026-07-23)

El usuario pidió explícitamente levantar la infraestructura descrita, con credenciales AWS ya
configuradas. Se verificó identidad (`aws sts get-caller-identity`) antes de tocar nada: cuenta
`538120053579`, rol `voclabs` — un **AWS Academy Learner Lab**, no una cuenta de producción.

- **Desviación descubierta y corregida:** la cuenta deniega `iam:CreateRole` (verificado con un
  `create-role` de prueba descartable → `AccessDenied`). El módulo `ec2` original creaba su
  propio `aws_iam_role`/`aws_iam_instance_profile` de mínimo privilegio; se reemplazó por una
  variable `instance_profile_name` (default `"LabInstanceProfile"`) que reutiliza el instance
  profile pre-aprovisionado del laboratorio — ya trae adjunta
  `AmazonEC2ContainerRegistryReadOnly`, cubre el pull de ECR. Detalle completo en
  `infra/terraform/README.md` → "Decisiones de diseño relevantes".
- **Bug corregido en el propio `README.md`:** el orden de aplicación en dos pasos documentado
  originalmente incluía `-target=module.observability` en el primer paso, pero ese módulo
  depende de `module.ec2.instance_ids` — con `-target`, Terraform habría arrastrado también las
  instancias EC2 al primer paso, anulando el propósito de diferirlas hasta después del push de
  la imagen. Corregido: paso 1 = `network`+`ecr`+`rds` únicamente; paso 3 = todo lo demás.
  Se determinó al construir el plan real, no en el diseño original (no se había probado en
  caliente hasta ahora).
- **Paso 1** (`terraform apply` con `-target=network,ecr,rds`): 15 recursos creados sin errores.
  PostgreSQL `18.3` fue aceptado por la API de RDS (la versión que en el checkpoint 6.2 se había
  marcado como "no verificada contra una cuenta real" — queda confirmada).
- **Paso 2:** imagen de la Fase 5 (`spring-petclinic-app:latest`, 357 MB, ya construida
  localmente) etiquetada y publicada en el ECR recién creado.
- **Paso 3** (`terraform apply` completo — EC2 + observability): 7 recursos creados (2 instancias
  + 1 log group + 4 alarmas) sin error de Terraform, pero las instancias no respondieron en el
  puerto 8080 tras ~10 minutos. Diagnóstico con `aws ec2 get-console-output`: **segunda
  desviación descubierta** — la AMI base de este Learner Lab trae solo **2 GB de disco raíz**
  (`aws ec2 describe-images` lo confirmó), insuficiente para `dnf install docker
  amazon-cloudwatch-agent` (~517 MB instalados). El `user_data` usa `set -euxo pipefail`, así que
  la instalación falló ("needs 49MB more space on the / filesystem") y todo lo posterior
  (arranque de Docker, login ECR, `docker run`) nunca se ejecutó — la instancia quedó arriba pero
  sin la app corriendo. Corregido en `modules/ec2/main.tf`: se añadió un bloque
  `root_block_device` con tamaño parametrizable (`root_volume_size`, default 20 GB, gp3) y se
  recreó ambas instancias con `terraform apply -replace=...` (6 recursos reemplazados: 2 EC2 + 4
  alarmas, cuyos nombres incluyen el ID de instancia).
- **Tercera desviación descubierta al recrear las instancias:** con el disco corregido, `dnf
  install` sí completó, pero las instancias seguían sin responder. El log de consola mostró
  interfaces de red `veth*` de Docker creándose y destruyéndose cada 10-60s — un contenedor
  crasheando y reiniciándose en bucle (`--restart unless-stopped`). Causa: el `Dockerfile`
  fija `SPRING_PROFILES_ACTIVE=postgres` como default de imagen (para el escenario de
  `docker-compose.app.yml`, que sí inyecta credenciales de un sidecar Postgres); en EC2,
  `user_data` deliberadamente no pasa credenciales de RDS al contenedor (para no exponerlas en
  texto plano, legible por cualquiera con `DescribeInstanceAttribute`), así que el contenedor
  intentaba conectarse a Postgres en `localhost`, fallaba, y se reiniciaba indefinidamente.
  Corregido: `user_data.sh.tftpl` ahora pasa `-e SPRING_PROFILES_ACTIVE=` (vacío) al `docker
  run`, forzando la BD H2 embebida. **Consecuencia real, documentada también en
  `infra/terraform/README.md`:** estas instancias EC2 sirven la app contra H2, no contra la RDS
  que Terraform aprovisionó — el RDS existe y está correctamente asegurado, pero nada lo usa
  todavía; conectarlas de verdad requeriría un mecanismo de secretos (SSM Parameter Store /
  Secrets Manager), fuera del alcance resuelto en esta sesión. Se recreó de nuevo con
  `-replace=...` (otros 6 recursos).
- **Resultado final verificado (2026-07-23):** ambas instancias `RUNNING` y respondiendo. IP
  `44.193.211.76` (rol `legado`): `GET /actuator/health` → `{"status":"UP"}`, `GET /` → 200
  HTML. IP `3.95.208.52` (rol `modernizado`): `GET /actuator/health` → `{"status":"UP"}`, `GET
  /api/owners/1` → 200 JSON (`George Franklin`, mascota `Leo`). Alarmas CloudWatch: las 2 de CPU
  en `OK` (con datos); las 2 de memoria en `INSUFFICIENT_DATA` (esperado, necesitan
  `evaluation_periods=2 × period=300s` ≈ 10 min de datos acumulados — no es un error). Recursos
  totales aplicados: 22 (15 red/ECR/RDS + 2 EC2 + 1 log group + 4 alarmas).
- **Cuarto hallazgo, esta vez de acceso (no de infraestructura):** al compartir las IPs
  desnudas con el usuario, el primer intento de acceso reportó "no logro llegar a ninguna de
  esas direcciones". Diagnóstico: no fue un problema de red ni de la instancia (`curl` desde
  esta sesión ya devolvía 200 en ese momento) — el navegador del usuario interpretaba
  `https://<ip>` por defecto (sin puerto), y como no hay balanceador ni TLS en 80/443 (todo se
  sirve HTTP plano en 8080), la conexión fallaba. Se corrigió indicando explícitamente el
  formato `http://<ip>:8080/`. Documentado en `infra/terraform/README.md` → "Cómo acceder a la
  app desplegada", para que no se repita el mismo diagnóstico manual la próxima vez.

**Resumen de las 3 desviaciones reales de infraestructura encontradas al desplegar** (ninguna
estaba en el diseño original de la Fase 6, las tres surgieron al aplicar contra una cuenta AWS
real): (1) IAM Learner Lab no permite `iam:CreateRole` → se reutiliza `LabInstanceProfile`; (2)
AMI base con solo 2 GB de disco → `root_block_device` explícito de 20 GB; (3) `Dockerfile` con
`SPRING_PROFILES_ACTIVE=postgres` por default + `user_data` sin credenciales de RDS por diseño
→ conflicto que causaba un crash-loop, resuelto forzando el perfil vacío (H2). Las tres son el
tipo de hallazgo que la rúbrica del experimento pide reportar como desviación, no como fallo:
ningún `./mvnw verify` ni test unitario las habría detectado — solo aparecieron al aplicar la
infraestructura contra una cuenta real, que es exactamente el valor de haber hecho este
despliegue en vez de dejarlo solo en `terraform validate`.

## Fase 7 — Estimación y esfuerzo real (paralelo desde Fase 0)

- [x] `docs/experimento/estimacion.md`: tabla de 29 subtareas (F03-1..7, F05-1..7, J21-1,
      DOC-1..3, TF-1..6, DIAG-1..5) en escala Fibonacci, **puntos en blanco a propósito** —
      los asigna el equipo, no la IA. Incluye sección "a completar por el equipo" para la
      unidad de estimación y las dos preguntas orientadoras de `CLAUDE.md` §7.1 (sin
      respuestas precargadas), la nota metodológica de que el tiempo entre commits es un
      proxy (no la fuente autoritativa — la bitácora manual sí lo es), y la nota de
      honestidad sobre uso de IAG de §7.3.
- [x] `docs/experimento/bitacora-esfuerzo.csv` con los encabezados exactos pedidos
      (`fecha,tarea_id,responsable,hora_inicio,hora_fin,horas,tipo_esfuerzo,observaciones`),
      sin filas — la completa el equipo.
- [x] `docs/experimento/scripts/medir-esfuerzo.sh`: recorre el git log y exporta a
      `metricas-commits.csv` (hash, fecha/hora, autor, mensaje, líneas añadidas/eliminadas,
      horas desde el commit anterior). Acota el rango al commit base documentado en
      `baseline-metrics.md` (`b05b681...`), no a toda la rama — de lo contrario habría
      arrastrado el historial completo del PetClinic original (miles de commits ajenos al
      experimento, desde 2009). Corrección durante la prueba: el primer intento perdía
      silenciosamente el último commit (`git log --pretty=format` no añade salto de línea
      final, y un `while read` sin esa línea final no ejecuta el cuerpo del bucle para el
      último registro); se corrigió añadiendo un `echo` final al pipe y filtrando líneas
      vacías. Probado en caliente contra `feature/modernizado`: exportó correctamente los
      10 commits de las Fases 0-6.2. `metricas-commits.csv` es un artefacto generado, no se
      versiona (añadido a `.gitignore` junto con `docs/plantuml/export/`).

---

## Desviaciones registradas hasta ahora

1. **Entorno sin JDK 17/21 preinstalado.** Se instalaron vía `sdkman` (JDK 17 ya instalado; JDK 21
   se instalará al iniciar la Fase 1). No es un cambio de alcance del código, solo del entorno de
   ejecución de esta sesión.
2. **Rama existente `feature/modernizado`** se usa en vez de crear `feature/experimento-modernizacion`
   como sugiere el §0.1 del plan, para no fragmentar el historial ya commiteado en esta rama.
3. **CI/CD sin actualizar a Java 21.** `.github/workflows/*.yml` quedan en Java 17 porque tocar
   pipelines de CI/CD está explícitamente fuera de alcance (`CLAUDE.md`). Si se ejecutan, fallarán
   al compilar con `java.version=21`; es un efecto esperado de la decisión de alcance, no un bug.
4. **Metodología de conteo de LOC para el criterio "<100 líneas" de F-03.** El plan cita "183" como
   el tamaño actual de `PetController` y pide dejarlo bajo 100, pero en la Fase 0 especifica contar
   LOC "sin comentarios ni líneas en blanco" — con esa metodología el original mide 124, no 183
   (183 es el conteo crudo con `wc -l`, que incluye la cabecera de licencia de 15 líneas presente
   en todos los archivos del proyecto). Cumplir "<100" con el conteo crudo es matemáticamente
   inviable sin romper convenciones del proyecto (quitar la cabecera de licencia) o eliminar
   métodos `@ModelAttribute`/`@InitBinder` requeridos. Se adoptó la metodología "sin comentarios ni
   blancos" del propio §0.2 del plan como la consistente: `PetController` quedó en 98 líneas por
   esa métrica (147 en crudo). Ambas cifras están documentadas en `baseline-metrics.md` y en la
   Fase 2 de este archivo para que se pueda auditar la decisión.
5. **Falso negativo sobre disponibilidad de Docker (corregido).** Un chequeo inicial (`which
   docker`) concluyó erróneamente que Docker no estaba disponible en el entorno: se leyó la salida
   de un comando en background antes de que terminara de escribirse, cortando la línea de
   `docker`. El usuario señaló la inconsistencia; al re-verificar, Docker (CLI, daemon y
   `compose`) sí funciona con normalidad. `plantuml` y `graphviz` (`dot`) sí están genuinamente
   ausentes como binarios nativos (confirmado con una verificación limpia), pero con Docker
   disponible se generaron los 11 PNG vía `plantuml/plantuml` sin necesidad de instalar nada más.
   Lección para próximas fases (5 y 6, que dependen de Docker/Terraform): re-verificar
   disponibilidad de herramientas con un comando síncrono antes de asumir su ausencia por una
   lectura parcial de un proceso en background.
