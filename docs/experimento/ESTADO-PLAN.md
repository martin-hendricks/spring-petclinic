# Estado de ejecución del plan — `CLAUDE.md`

Seguimiento vivo de las 8 fases del plan de modernización (Equipo 17). Se actualiza al cerrar cada
fase o punto de control. Rama de trabajo: `feature/modernizado`.

Leyenda: ⬜ Pendiente · 🟨 En progreso · ✅ Completa · 🛑 Bloqueada / esperando aprobación

| Fase | Objetivo | Estado | Última actualización |
|---|---|---|---|
| 0 | Preparación: contexto, baseline y rama | ✅ Completa | 2026-07-21 |
| 1 | Replatform Java 17 → 21 | ⬜ Pendiente | — |
| 2 | F-03 — Capa `@Service` (Pets & Visits) | ⬜ Pendiente | — |
| 3 | F-05 — API REST Owners | ⬜ Pendiente | — |
| 4 | Diagramas secuencia/clases to-be | ⬜ Pendiente | — |
| 5 | Contenedores | ⬜ Pendiente | — |
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

- [ ] `pom.xml`: `<java.version>` 17 → 21
- [ ] `build.gradle`: toolchain → 21
- [ ] Revisar `maven-enforcer-plugin` (`requireJavaVersion`)
- [ ] `./mvnw verify` en verde con Java 21
- [ ] Instalar JDK 21 en el entorno (no está instalado; se instalará vía `sdkman` al ejecutar esta fase)

## Fase 2 — F-03 (Service Pets & Visits)

- [ ] Punto de control: propuesta de manejo de errores de validación (excepciones de dominio vs.
      objeto de errores) — **pendiente de aprobación del usuario antes de escribir código**
- [ ] `PetService` (`createPet`, `updatePet`, validaciones)
- [ ] `VisitService` (`createVisit`, validación de fecha futura)
- [ ] Refactor `PetController` / `VisitController` a orquestación pura
- [ ] `PetController.java` < 100 LOC (hoy: 183)
- [ ] `PetServiceTests`, `VisitServiceTests` con Mockito
- [ ] `./mvnw verify` en verde sin tocar aserciones existentes

## Fase 3 — F-05 (API REST Owners)

- [ ] Punto de control: versión de `springdoc-openapi` compatible con Spring Boot 4.0.3 / Spring
      Framework 7 — **pendiente de verificación y confirmación antes de fijar versión**
- [ ] `OwnerDto` / `PetDto` (records)
- [ ] `OwnerMapper`
- [ ] `OwnerService` (`findById`, `findByLastName` paginado)
- [ ] `OwnerRestController` (`GET /api/owners`, `GET /api/owners/{id}`) + `@RestControllerAdvice`
- [ ] Documentación OpenAPI (`@Operation`, `@ApiResponse`)
- [ ] `OwnerRestControllerTests` (`@WebMvcTest`)
- [ ] Confirmar que `OwnerController` (MVC) sigue intacto y las vistas Thymeleaf no tienen regresión

## Fase 4 — Diagramas to-be

- [ ] `08-secuencia-crear-pet-tobe.puml`
- [ ] `09-secuencia-api-owners-tobe.puml`
- [ ] `10-clases-capa-service-tobe.puml`
- [ ] `11-despliegue-tobe.puml`
- [ ] `docs/plantuml/README.md` actualizado + PNGs exportados

## Fase 5 — Contenedores

- [ ] `Dockerfile` multi-stage (build JDK 21 + runtime JRE 21 slim, usuario no root, `HEALTHCHECK`)
- [ ] `.dockerignore`
- [ ] `docker-compose.app.yml` (nuevo, no se toca `docker-compose.yml`)
- [ ] `docs/experimento/contenedores.md`
- [ ] Verificación manual: imagen construida, compose levantado, `GET /api/owners/1` y `GET /owners/1`
      responden, tamaño de imagen reportado

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
