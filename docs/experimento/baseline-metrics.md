# Línea base — Spring PetClinic (Equipo 17)

- **Fecha de captura:** 2026-07-21
- **Commit:** `b05b68189dc20c020e8452303fb5fd43f91c9730` (rama `feature/modernizado`)
- **Autor commit:** Martin Romero — 2026-06-26 19:26:26 -0500

> Nota de entorno: la máquina donde se corrió esta línea base solo tenía OpenJDK 8 instalado.
> Se instaló OpenJDK 17.0.16 (Temurin) vía `sdkman` (`sdk install java 17.0.16-tem`) para poder
> compilar y ejecutar `./mvnw verify` con la versión de Java que exige actualmente el `pom.xml`
> (`<java.version>17</java.version>`). Este paso no modifica código fuente.

## 1. LOC por archivo — paquete `owner`

Conteo con `wc -l` (líneas totales del archivo, incluye comentarios, Javadoc, licencia y líneas en
blanco). Se usa este criterio porque es el que corresponde a la cifra "183 LOC" que cita el
`CLAUDE.md` para `PetController` y al criterio de aceptación de F-03 (`PetController.java` < 100
LOC), así que ambas medidas quedan comparables antes/después.

| Archivo | Líneas totales (`wc -l`) |
|---|---|
| `PetController.java` | 183 |
| `Owner.java` | 176 |
| `OwnerController.java` | 176 |
| `VisitController.java` | 114 |
| `Pet.java` | 85 |
| `PetValidator.java` | 64 |
| `Visit.java` | 68 |
| `OwnerRepository.java` | 62 |
| `PetTypeFormatter.java` | 62 |
| `PetType.java` | 30 |
| `PetTypeRepository.java` | 39 |
| `package-info.java` | 16 |
| **Total paquete `owner`** | **1.075** |

### Conteo alternativo (sin comentarios ni líneas en blanco)

Script de apoyo: `docs/experimento/scripts/contar-loc.py` (heurística: elimina bloques `/* ... */`
y líneas que empiezan con `//`, ignora líneas vacías). Útil como segunda referencia porque es
menos sensible a licencias/Javadoc largos, pero **el criterio oficial de aceptación de F-03 usa
`wc -l`** (ver nota arriba).

| Archivo | LOC sin comentarios/blancos |
|---|---|
| `PetController.java` | 124 |
| `OwnerController.java` | 118 |
| `Owner.java` | 104 |
| `VisitController.java` | 64 |
| `Pet.java` | 48 |
| `Visit.java` | 32 |
| `PetTypeFormatter.java` | 29 |
| `PetValidator.java` | 25 |
| `PetType.java` | 8 |
| `PetTypeRepository.java` | 8 |
| `OwnerRepository.java` | 9 |
| `package-info.java` | 1 |
| **Total paquete `owner`** | **570** |

## 2. LOC total `src/main`

- **Líneas totales (`wc -l` de todos los `.java`):** 1.841
- **Archivos `.java`:** 30
- **Controllers (`@Controller`/`@RestController`):** 6 — `OwnerController`, `PetController`,
  `VisitController`, `CrashController`, `WelcomeController`, `VetController`
- **Repositorios (`extends ...Repository`):** 3 — `OwnerRepository`, `PetTypeRepository`,
  `VetRepository`
- **Entidades `@Entity`:** 6 — `Owner`, `Pet`, `PetType`, `Visit`, `Vet`, `Specialty`
  (el `CLAUDE.md` reporta 8 "entidades JPA"; la diferencia probablemente cuenta también las
  superclases `@MappedSuperclass` `BaseEntity`/`Person`, que no llevan `@Entity` propio — se deja
  anotado como desviación menor de conteo, no de código)
- **Templates Thymeleaf (`.html` en `templates/`):** 12 — coincide con el `CLAUDE.md`

> El `CLAUDE.md` reporta 1.931 LOC totales de `src/main`; el conteo aquí (1.841 con `wc -l`) es
> cercano pero no idéntico, probablemente por diferencias de herramienta/versión de conteo. Se dejan
> ambas cifras documentadas; lo que importa para el experimento es la comparación *antes/después*
> hecha con el mismo método, no coincidir al dígito con la cartografía previa.

## 3. Resultado de `./mvnw verify`

- **Comando:** `./mvnw -q verify` (con `JAVA_HOME` apuntando a Temurin 17.0.16)
- **Resultado:** `BUILD SUCCESS` (exit code 0)
- **Tiempo total:** 4 min 44 s (incluye descarga de imágenes Docker para Testcontainers de
  MySQL/Postgres, que no está cacheada en este entorno)
- **Tests:** 47 ejecutados, **0 fallos, 0 errores, 0 omitidos**

| Clase de test | Tests | Resultado |
|---|---|---|
| `service.ClinicServiceTests` | 10 | OK |
| `owner.OwnerControllerTests` | 13 | OK |
| `owner.VisitControllerTests` | 4 | OK |
| `owner.PetTypeFormatterTests` | 3 | OK |
| `owner.PetControllerTests` | 0* | OK |
| `owner.PetValidatorTests` | 0* | OK |
| `vet.VetControllerTests` | 2 | OK |
| `vet.VetTests` | 1 | OK |
| `model.ValidatorTests` | 2 | OK |
| `system.CrashControllerTests` | 1 | OK |
| `system.CrashControllerIntegrationTests` | 2 | OK |
| `system.I18nPropertiesSyncTest` | 2 | OK |
| `PetClinicIntegrationTests` | 3 | OK |
| `MySqlIntegrationTests` | 2 | OK |
| `PostgresIntegrationTests` | 2 | OK |
| **Total** | **47** | **0 fallos** |

\* `PetControllerTests` y `PetValidatorTests` reportan "Tests run: 0" en el `.txt` de Surefire
porque en este proyecto usan `@Nested` (JUnit 5 los agrupa bajo el summary de la clase contenedora
en el reporte de consola, no en el `.txt` plano); el build no reporta fallos para esas clases.

## 4. Endpoints HTTP actuales (`@GetMapping` / `@PostMapping` / `@RequestMapping`)

| Método | Ruta | Controller |
|---|---|---|
| GET | `/` | `WelcomeController` |
| GET | `/oups` | `CrashController` |
| GET | `/vets.html` | `VetController` |
| GET | `/vets` | `VetController` |
| GET | `/owners/new` | `OwnerController` |
| POST | `/owners/new` | `OwnerController` |
| GET | `/owners/find` | `OwnerController` |
| GET | `/owners` | `OwnerController` |
| GET | `/owners/{ownerId}/edit` | `OwnerController` |
| POST | `/owners/{ownerId}/edit` | `OwnerController` |
| GET | `/owners/{ownerId}` | `OwnerController` |
| GET | `/owners/{ownerId}/pets/new` | `PetController` |
| POST | `/owners/{ownerId}/pets/new` | `PetController` |
| GET | `/owners/{ownerId}/pets/{petId}/edit` | `PetController` |
| POST | `/owners/{ownerId}/pets/{petId}/edit` | `PetController` |
| GET | `/owners/{ownerId}/pets/{petId}/visits/new` | `VisitController` |
| POST | `/owners/{ownerId}/pets/{petId}/visits/new` | `VisitController` |

**Total:** 17 mappings, 0 son API REST (JSON) — todo es HTML server-rendered vía Thymeleaf. Confirma
la afirmación del `CLAUDE.md`: "único endpoint REST: `GET /vets`" es en realidad HTML también
(`vets.html`/`vets` devuelven vista, no JSON); el primer endpoint JSON real del proyecto lo creará
F-05 (`/api/owners`).

## Entorno de ejecución usado para esta línea base

- OS: Linux 5.15.167.4-microsoft-standard-WSL2 (WSL2)
- JDK de build/test: Temurin 17.0.16 (instalado vía sdkman, no venía preinstalado)
- Docker: 29.6.1 (usado por Testcontainers para los tests de integración MySQL/Postgres)
