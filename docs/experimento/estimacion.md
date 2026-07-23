# Estimación de esfuerzo — Fase 7

Cierra los ítems **2.5 (estimación de esfuerzo)** y sirve de insumo para **3.2 (esfuerzo real)**
de la rúbrica. La tabla de puntos está desagregada por subtarea, siguiendo el mismo criterio que
llevó el resto del experimento: desagregación + juicio de expertos (ver más abajo).

## Tabla de estimación (puntos de historia, escala Fibonacci: 1, 2, 3, 5, 8, 13)

**Los puntos quedan en blanco a propósito.** Los asigna el equipo en su propia sesión de
estimación (p. ej. Planning Poker), no la IA que redactó este documento — así lo pide
`CLAUDE.md` §7.1: la estimación es una decisión del equipo.

| ID | Tarea | Puntos | Justificación |
|---|---|---|---|
| F03-1 | Diseñar el manejo de errores de validación (excepción de dominio vs. objeto de resultado) y obtener aprobación | | Decisión arquitectónica con punto de control explícito antes de escribir código |
| F03-2 | Implementar `PetService` (`createPet`, `updatePet`, validaciones de nombre duplicado y fecha) | | Debe reproducir exactamente la semántica original de `Owner.getPet(name, ignoreNew)` |
| F03-3 | Implementar `VisitService` (`createVisit`, validación de fecha futura) | | Más simple que `PetService`: una sola regla de negocio |
| F03-4 | Refactorizar `PetController` a orquestación pura, preservando `@ModelAttribute`/`@InitBinder`/nombres de vista | | Riesgo de romper el comportamiento combinado de errores de Bean Validation + reglas de negocio (ocurrió durante la implementación) |
| F03-5 | Refactorizar `VisitController` a orquestación pura | | Sin el caso combinado de errores que sí tuvo `PetController` |
| F03-6 | Tests unitarios `PetServiceTests`/`VisitServiceTests` con Mockito puro | | Sin contexto Spring, cobertura de los 5 casos de negocio (duplicado crear/editar, fecha futura, caso feliz) |
| F03-7 | Verificar `PetControllerTests`/`VisitControllerTests`/`ClinicServiceTests` en verde sin modificar aserciones | | Regla invariante de equivalencia funcional del experimento |
| F05-1 | Verificar versión compatible de `springdoc-openapi` con Spring Boot 4.0.3 y obtener aprobación | | Punto de control explícito; la búsqueda web inicial dio resultados fabricados, hubo que verificar contra Maven Central |
| F05-2 | Añadir dependencia `springdoc-openapi-starter-webmvc-ui` a `pom.xml`/`build.gradle` | | Cambio mecánico una vez resuelto F05-1 |
| F05-3 | Crear `OwnerDto`/`PetDto` (records) + `OwnerMapper` (solo lectura) | | Desacopla el contrato JSON de las entidades JPA |
| F05-4 | Implementar `OwnerService` (`findById`, `findByLastName` paginado, `@Transactional(readOnly = true)`) | | Debe reproducir la regla de `OwnerController#processFindForm` para `lastName == null` |
| F05-5 | Implementar `OwnerRestController` + `OwnerRestExceptionHandler` (404 vía `ProblemDetail`) | | Sin try/catch en el controller; advice acotado solo a este controller |
| F05-6 | Documentación OpenAPI (`@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema`) | | Requisito explícito de aceptación de F-05 |
| F05-7 | Tests de contrato `OwnerRestControllerTests` (`@WebMvcTest`) | | Incluye verificación explícita de que no se filtran campos internos de la entidad JPA |
| J21-1 | Migrar `pom.xml`/`build.gradle` de Java 17 a 21 (solo runtime, sin refactor de código) | | Alcance estrictamente de toolchain, "otra tarea" no requisito del experimento |
| DOC-1 | `Dockerfile` multi-stage (build JDK 21 + runtime JRE 21 Alpine, usuario no root, `HEALTHCHECK`) | | Incluye decisiones de seguridad (usuario no root, capas de cache de dependencias) |
| DOC-2 | `docker-compose.app.yml` nuevo (app + PostgreSQL, `depends_on: condition: service_healthy`) | | No se toca el `docker-compose.yml` existente |
| DOC-3 | Verificación en caliente (build, `compose up`, `GET /api/owners/1` y `GET /owners/1`, tamaño de imagen) + `contenedores.md` | | Exige un entorno con Docker funcional para poder cerrarse |
| TF-1 | Estructura `infra/terraform/` + módulo `network` (checkpoint `init`/`validate`) | | Punto de control explícito antes de continuar con el resto de módulos |
| TF-2 | Módulo `ecr` (scan-on-push, lifecycle de 10 imágenes) | | Módulo pequeño y autocontenido |
| TF-3 | Módulo `rds` (PostgreSQL 18.x, subred privada, cifrado, backups mínimos documentados) | | Requiere justificar por qué se deshabilitan backups (entorno de experimento) |
| TF-4 | Módulo `ec2` (`instance_count = 2`, IAM de mínimo privilegio, `user_data`) | | El más complejo: IAM, plantilla de `user_data`, agente de CloudWatch para métricas de memoria |
| TF-5 | Módulo `observability` (log group 7 días, alarmas CPU/memoria) | | La alarma de memoria depende de que TF-4 instale el agente correctamente |
| TF-6 | `outputs.tf` extendido + `infra/terraform/README.md` (orden de aplicación, prerrequisitos, costo estimado) | | Necesario para que el equipo pueda aplicar sin adivinar el orden correcto |
| DIAG-1 | `08-secuencia-crear-pet-tobe.puml` | | Debe reflejar el código real de F03, no un diseño idealizado |
| DIAG-2 | `09-secuencia-api-owners-tobe.puml` (incluye camino alterno 404) | | Debe reflejar el código real de F05 |
| DIAG-3 | `10-clases-capa-service-tobe.puml` (firmas reales) | | El más grande de los 4: cubre toda la capa `@Service` + DTOs |
| DIAG-4 | `11-despliegue-tobe.puml` (Java 21 + Docker + EC2/RDS) | | Anticipa las Fases 5-6, que en el momento de dibujarlo aún no existían |
| DIAG-5 | Exportar los 11 PNG (7 as-is + 4 to-be) y actualizar `docs/plantuml/README.md` | | Depende de tener `plantuml`/`graphviz` o Docker disponible |

## Unidad de estimación y preguntas orientadoras — a completar por el equipo

`CLAUDE.md` §7.1 deja material de referencia para esta discusión (no es una decisión de la IA):

- **Unidad recomendada:** puntos de historia, porque F-03 no cambia funcionalidad visible
  (puntos de función darían ~0 para el trabajo más difícil del experimento).
- **Pregunta 1** (¿analogía, desagregación o juicio de expertos?): argumento sugerido —
  desagregación + juicio de expertos, descartando analogía por falta de histórico propio.
- **Pregunta 2** (clasificación en "datos" y "transacciones" de IFPUG): aplicada a PetClinic,
  con ventajas/desventajas ya esbozadas en `CLAUDE.md`.

El equipo debe registrar aquí sus propias conclusiones tras la sesión de estimación:

### Unidad elegida y justificación
_(a completar por el equipo)_

### Respuesta a la pregunta 1
_(a completar por el equipo)_

### Respuesta a la pregunta 2
_(a completar por el equipo)_

## El tiempo entre commits es un proxy, no la fuente autoritativa

`scripts/medir-esfuerzo.sh` recorre el historial de git de la rama del experimento y exporta a
`metricas-commits.csv`: hash, fecha/hora, autor, mensaje, líneas añadidas/eliminadas, y horas
transcurridas desde el commit anterior.

Ese tiempo entre commits **no equivale al esfuerzo real**, por dos razones concretas:

1. **Incluye pausas.** El reloj entre dos commits sigue corriendo aunque nadie esté trabajando:
   interrupciones, descansos, cambios de contexto a otra tarea.
2. **Excluye el trabajo previo al primer commit de cada fase.** La discusión y aprobación del
   diseño de manejo de errores de F-03, o la verificación de la versión de `springdoc` en F-05,
   fueron esfuerzo real que no deja huella en git hasta que se commitea el resultado.

La **fuente autoritativa** de esfuerzo es la bitácora manual (`bitacora-esfuerzo.csv`), donde
cada responsable registra sus horas reales por tipo de esfuerzo. `metricas-commits.csv` sirve
para **contrastar** ambas fuentes (p. ej. detectar sesiones donde el tiempo de reloj fue mucho
mayor o mucho menor que lo registrado a mano), no para reemplazar la bitácora.

## Nota de honestidad metodológica (uso de IA generativa)

Ver `CLAUDE.md` §7.3. Al completar la bitácora, el equipo debe distinguir explícitamente:

- **Horas de reloj con asistencia de IAG** — lo que efectivamente tomó ejecutar cada fase con
  Claude Code.
- **Esfuerzo estimado equivalente sin asistencia** — la columna de puntos de historia de arriba,
  pensada como si el equipo lo implementara a mano.
- **Esfuerzo de revisión y corrección del código generado** — es esfuerzo real del experimento
  (leer el diff, verificar que no se saliera del alcance, correr `./mvnw verify`) y suele ser la
  parte más informativa para la sección de "uso de IAG" de la rúbrica.

La brecha entre las dos primeras columnas, si se documenta con estas dos fuentes cruzadas
(bitácora + `metricas-commits.csv`), es evidencia cuantificada y no una declaración genérica.
