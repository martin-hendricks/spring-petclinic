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
| 6 | Terraform sobre AWS | ⬜ Pendiente | — |
| 7 | Estimación y esfuerzo real | ⬜ Pendiente (arranca en paralelo desde Fase 0) | — |

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

- [ ] Estructura `infra/terraform/` + módulo `network` (checkpoint: `terraform init`/`validate`)
- [ ] Módulos `ecr`, `rds`, `ec2` (con `instance_count = 2`), `observability`
- [ ] `outputs.tf`, `infra/terraform/README.md` con estimación de costo
- [ ] `terraform validate` + `terraform fmt -check` (sin `terraform apply`)

## Fase 7 — Estimación y esfuerzo real (paralelo desde Fase 0)

- [ ] `docs/experimento/estimacion.md` (tabla de puntos de historia, sin asignar — la llena el equipo)
- [ ] `docs/experimento/bitacora-esfuerzo.csv`
- [ ] `docs/experimento/scripts/medir-esfuerzo.sh`

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
