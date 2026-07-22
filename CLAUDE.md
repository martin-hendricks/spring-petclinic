# Plan de implementación en Claude Code
## Experimento de modernización — Spring PetClinic (Equipo 17)

Este plan cubre los cuatro objetivos solicitados, alineado con el pre-experimento (F-03 y F-05) y la cartografía de la Entrega 2.

---

## Índice de fases

| Fase | Objetivo | Tipo | Entregable |
|---|---|---|---|
| **0** | Preparación: contexto, baseline y rama | Habilitador | `CLAUDE.md`, métricas baseline, rama `feature/experimento-modernizacion` |
| **1** | Replatform: Java 17 → 21 LTS | *Otra tarea* (no es requisito) | `pom.xml`, `build.gradle` actualizados |
| **2** | **F-03** — Capa `@Service` (Pets & Visits) | **Requisito** | `PetService`, `VisitService`, controllers delgados |
| **3** | **F-05** — API REST Owners | **Requisito** | `OwnerService`, `OwnerDto`, `OwnerRestController`, OpenAPI |
| **4** | Diagramas de secuencia y clases (to-be) | Documentación | 4 archivos `.puml` + PNG |
| **5** | Contenedores | Infraestructura | `Dockerfile`, `docker-compose.app.yml` |
| **6** | Terraform sobre AWS (sin CI/CD) | Infraestructura | Módulos `network`, `ecr`, `rds`, `ec2`, `observability` |
| **7** | Estimación y esfuerzo real | Medición | Tabla de estimación + bitácora + script de medición |

> **Ojo con el alcance.** El pre-experimento fijó `n = ⌊4/2⌋ = 2` requisitos: **F-03 y F-05**. Java 21 (F-01), Service de Owners (F-02), API de Vets (F-04) y Seguridad (F-06) **no** son requisitos de este experimento. Java 21 se incluye igualmente porque la arquitectura to-be lo exige, pero se reporta como *"otra tarea de modernización"*, exactamente como lo permite la rúbrica. No dejes que Claude Code amplíe el alcance por iniciativa propia — los prompts de abajo lo bloquean explícitamente.

---

## Fase 0 — Preparación

### 0.1 Crear el archivo de contexto persistente

Claude Code lee `CLAUDE.md` en cada sesión. Sin él tendrás que repetir el contexto y el modelo tomará decisiones que se salen del alcance. Crea este archivo **antes** de cualquier prompt:

````markdown
# CLAUDE.md — Experimento de modernización Spring PetClinic

## Contexto académico
Proyecto de la asignatura Modernización de Software (Maestría en Ing. de Software), Equipo 17.
Este repositorio es el **legado** a modernizar. La estrategia es **Replatform + Wrapping (Fase 1)**,
NO descomposición a microservicios.

## Estado as-is (verificado en cartografía Entrega 2)
- Java 17 · Spring Boot 4.0.3 (Spring Framework 7) · Spring MVC + Thymeleaf · Spring Data JPA
- 30 clases · 1.931 LOC · 6 controllers · 3 repositorios · 8 entidades JPA · 12 templates
- **0 clases `@Service`**: los controllers inyectan repositorios directamente
- `Owner` es raíz de agregado: persiste `Pet` y `Visit` en cascada vía `owners.save(owner)`
- Único endpoint REST: `GET /vets`; el resto es HTML server-rendered
- Hotspots CodeScene: `PetController` (183 LOC), `OwnerController`, `VisitController`
- X-Ray: `processCreationForm` y `processUpdateForm` con alerta *Complex Conditional*

## Alcance del experimento — SOLO estos dos requisitos
- **F-03 · Service — Pets & Visits**: extraer lógica de `PetController` y `VisitController`
  a una capa `@Service`. Aceptación: `PetController` < 100 LOC; tests en verde.
- **F-05 · API REST — Owners**: exponer `GET /api/owners` y `GET /api/owners/{id}`.
  Aceptación: endpoints documentados con OpenAPI + tests de API.

## FUERA de alcance (no implementar salvo instrucción explícita)
- F-02 (Service de Owners para la UI MVC), F-04 (API REST de Vets), F-06 (Spring Security)
- Descomposición a microservicios, Strangler Fig, AWS Lambda (son Fases 3 y 4 del roadmap)
- Migraciones Flyway/Liquibase
- Cualquier pipeline de CI/CD
- Eliminar `CrashController` (está marcado como "evaluar", no como "remover")

## Reglas invariantes
1. **Equivalencia funcional**: la suite de tests existente debe quedar en verde SIN modificar
   las aserciones. Si un test falla, el código nuevo está mal, no el test.
2. **No tocar** entidades JPA (`Owner`, `Pet`, `Visit`, `Vet`...), repositorios ni templates
   Thymeleaf. Se conservan según el alcance de la Entrega 2.
3. Las vistas Thymeleaf deben seguir funcionando en paralelo a la API REST (esto es Wrapping,
   no reemplazo).
4. Respetar el formato del proyecto: tabulaciones, checkstyle y `spring-javaformat`.
   Ejecutar `./mvnw spring-javaformat:apply` antes de cada commit.
5. Los controllers son **package-private** (`class PetController`, no `public class`).
   Mantener esa visibilidad salvo que sea imprescindible cambiarla.

## Comandos
- Build + tests: `./mvnw verify`
- Solo tests: `./mvnw test`
- Formato: `./mvnw spring-javaformat:apply`
- Arrancar: `./mvnw spring-boot:run`
````

### 0.2 Prompt — capturar la línea base

> Antes de modificar nada, necesito una foto del estado actual para poder comparar después.
>
> 1. Cuenta las LOC (sin comentarios ni líneas en blanco) de cada archivo en `src/main/java/org/springframework/samples/petclinic/owner/` y del total de `src/main`.
> 2. Ejecuta `./mvnw verify` y registra: número de tests, cuántos pasan y el tiempo total del build.
> 3. Lista los endpoints HTTP actuales recorriendo las anotaciones `@GetMapping` / `@PostMapping` / `@RequestMapping`.
>
> Guarda todo en `docs/experimento/baseline-metrics.md` con la fecha y el hash del commit actual. No modifiques código fuente en este paso.

**Criterio de aceptación:** existe `docs/experimento/baseline-metrics.md` con LOC por archivo y conteo de tests. Este archivo es la evidencia "antes" para la métrica de mantenibilidad.

---

## Fase 1 — Replatform a Java 21 (*otra tarea*)

### Prompt

> Migra el proyecto de Java 17 a Java 21 LTS. Alcance estricto:
>
> - En `pom.xml`, cambia la propiedad `<java.version>` de 17 a 21.
> - En `build.gradle`, actualiza la toolchain de Java a 21.
> - Revisa si el plugin `maven-enforcer-plugin` (regla `requireJavaVersion`) necesita ajuste.
>
> **No** refactorices código para usar features de Java 21 (virtual threads, records, pattern matching). Esta fase es solo de runtime: el código debe compilar igual.
>
> Después ejecuta `./mvnw verify` y reporta el resultado. Si algo falla, dime exactamente qué antes de intentar arreglarlo.

**Criterio de aceptación:** `./mvnw verify` en verde con Java 21, sin cambios en `src/`.

> **Riesgo a vigilar:** Spring Boot 4.0.3 es reciente; si alguna dependencia transitiva no soporta Java 21, no fuerces la actualización de esa dependencia — repórtalo como desviación en el post-experimento (la rúbrica pide justamente reportar desviaciones).

---

## Fase 2 — F-03: Capa `@Service` para Pets & Visits

Esta es la fase que ataca directamente los hallazgos M1–M3 de la cartografía.

### Lógica de negocio a extraer (verificada en el código)

| Origen | Regla de negocio | Destino |
|---|---|---|
| `PetController.processCreationForm` L110-112 | Nombre de mascota duplicado (`pet.isNew()`) | `PetService.validateDuplicateName(...)` |
| `PetController.processUpdateForm` L141-146 | Nombre duplicado en edición (compara IDs) | `PetService.validateDuplicateName(...)` |
| `PetController` L114-117 y L148-151 | `birthDate` no puede ser futura | `PetService.validateBirthDate(...)` |
| `PetController.updatePetDetails` L167-181 | Actualizar o agregar mascota + `owners.save()` | `PetService.updatePet(...)` |
| `VisitController.processNewVisitForm` | Fecha de visita debe ser futura | `VisitService.validateVisitDate(...)` |
| `VisitController.processNewVisitForm` | `owner.addVisit(petId, visit)` + `owners.save()` | `VisitService.createVisit(...)` |

### Prompt

> Implementa el requisito **F-03: extraer la lógica de negocio de `PetController` y `VisitController` a una capa `@Service`**.
>
> **Crea** en el paquete `org.springframework.samples.petclinic.owner`:
>
> 1. `PetService` anotado con `@Service`, que reciba `OwnerRepository` por constructor y exponga:
>    - `Pet createPet(Owner owner, Pet pet)` anotado `@Transactional`
>    - `Pet updatePet(Owner owner, Pet pet)` anotado `@Transactional`
>    - métodos de validación reutilizables por ambos: uno para nombre duplicado y otro para fecha de nacimiento futura
> 2. `VisitService` anotado con `@Service`, que reciba `OwnerRepository` y exponga:
>    - `Visit createVisit(Owner owner, int petId, Visit visit)` anotado `@Transactional`
>    - validación de fecha de visita futura
>
> **Punto crítico de diseño — cómo manejar los errores de validación:**
> Hoy los controllers llaman a `result.rejectValue(...)` sobre el `BindingResult` de Spring MVC. Un `@Service` no debe conocer `BindingResult` (eso lo acoplaría a la capa web y rompería el objetivo de reutilizarlo desde el `@RestController` de F-05).
>
> Propón **primero** cómo resolverlo y espera mi aprobación antes de escribir código. Evalúa al menos: (a) que el service lance excepciones de dominio que el controller traduzca a `rejectValue`, y (b) que el service devuelva un objeto con los errores de validación. Indica ventajas y desventajas de cada una para nuestra arquitectura to-be y recomienda una.
>
> **Después**, cuando yo apruebe el enfoque:
> - Refactoriza `PetController` y `VisitController` para que **solo** orquesten: resolver el modelo, delegar en el service, manejar el resultado y devolver la vista o el redirect.
> - Conserva intactos los `@ModelAttribute`, `@InitBinder`, los nombres de vista y los mensajes flash: cualquier cambio ahí rompe los tests y las vistas Thymeleaf.
> - **No modifiques** `Owner.java`, `Pet.java`, `Visit.java`, `PetValidator.java`, `OwnerRepository.java` ni ningún template.
>
> **Restricción de aceptación:** `PetController.java` debe quedar por debajo de **100 líneas** (hoy tiene 183). Verifícalo y repórtalo.
>
> Finalmente ejecuta `./mvnw verify`. La suite existente (`PetControllerTests`, `VisitControllerTests`, `ClinicServiceTests`) debe pasar **sin que modifiques sus aserciones**.

### Prompt de seguimiento — tests unitarios del service

> Ahora agrega tests unitarios para la capa nueva: `PetServiceTests` y `VisitServiceTests` en `src/test/java/.../owner/`.
>
> Usa Mockito para simular `OwnerRepository` (sin levantar el contexto de Spring, para que sean rápidos). Cubre como mínimo:
> - Nombre duplicado al crear → rechaza
> - Nombre duplicado al editar, pero es la misma mascota (mismo ID) → acepta
> - `birthDate` futura → rechaza
> - Caso feliz de creación → invoca `owners.save(owner)` exactamente una vez
> - Fecha de visita en el pasado o de hoy → rechaza (la regla es *estrictamente* futura)
>
> Sigue el estilo de los tests existentes del repositorio. No modifiques tests ya existentes.

**Criterios de aceptación F-03:**
- [ ] `PetController.java` < 100 LOC
- [ ] Existen `PetService` y `VisitService` con `@Transactional`
- [ ] Los controllers ya no invocan `owners.save(...)` directamente
- [ ] `./mvnw verify` en verde sin editar aserciones preexistentes
- [ ] Tests nuevos del service pasan

---

## Fase 3 — F-05: API REST de Owners

### Prompt 3.1 — dependencia OpenAPI

> Agrega `springdoc-openapi-starter-webmvc-ui` al `pom.xml` y al `build.gradle`.
>
> **Antes de fijar la versión, verifícala**: el proyecto usa Spring Boot 4.0.3 / Spring Framework 7, y springdoc requiere una versión específica compatible con esa línea. Consulta la documentación o el repositorio de springdoc y dime qué versión vas a usar y por qué antes de escribirla.
>
> Si no existe una versión de springdoc compatible con Spring Boot 4, **detente y avísame** — no improvises un downgrade de Spring Boot ni escribas el contrato OpenAPI a mano sin consultarme. En ese caso evaluaremos alternativas y lo reportaremos como desviación del experimento.
>
> Verifica con `./mvnw verify` que la app sigue arrancando y que `/v3/api-docs` responde.

### Prompt 3.2 — service, DTO y controller REST

> Implementa el requisito **F-05: exponer la consulta de owners como API REST**.
>
> **Crea** en `org.springframework.samples.petclinic.owner`:
>
> 1. `OwnerDto` — un **record** de Java que exponga: `id`, `firstName`, `lastName`, `address`, `city`, `telephone` y la lista de mascotas como `PetDto` (`id`, `name`, `birthDate`, `type`). Propósito: desacoplar el contrato JSON de la entidad JPA (evita exponer el modelo de persistencia y romper clientes cuando cambie la entidad).
> 2. `OwnerMapper` — conversión `Owner` → `OwnerDto`. Solo en esa dirección: la API de esta fase es de **lectura**.
> 3. `OwnerService` anotado `@Service`, con `OwnerRepository` por constructor:
>    - `OwnerDto findById(int id)` — lanza una excepción de dominio si no existe
>    - `Page<OwnerDto> findByLastName(String lastName, Pageable pageable)` — reutiliza `owners.findByLastNameStartingWith(...)`; si `lastName` es null, usa cadena vacía (búsqueda amplia), igual que hace hoy `OwnerController.processFindForm`
>    - anotado `@Transactional(readOnly = true)`
> 4. `OwnerRestController` — `@RestController` con `@RequestMapping("/api/owners")`:
>    - `GET /api/owners` con paginación (`page`, `size`) y filtro opcional `lastName`
>    - `GET /api/owners/{id}`
>    - Devuelve `ResponseEntity<...>` con códigos HTTP correctos: **200** cuando hay resultado, **404** cuando el owner no existe. Usa `@RestControllerAdvice` para el manejo de errores, no bloques try/catch en el controller.
>    - Documenta ambos endpoints con anotaciones OpenAPI (`@Operation`, `@ApiResponse`).
>
> **Restricciones:**
> - **No modifiques** `OwnerController` (el MVC de Thymeleaf). Debe seguir sirviendo las vistas HTML en paralelo — eso es precisamente el patrón Wrapping de nuestra estrategia.
> - **No toques** la entidad `Owner` ni `OwnerRepository`.
> - Mantén el tamaño de página por defecto en 5, igual que el legado (`OwnerController.findPaginatedForOwnersLastName`), para que la API sea consistente con la UI.

### Prompt 3.3 — tests de contrato

> Agrega `OwnerRestControllerTests` usando `@WebMvcTest` y MockMvc. Cubre:
> - `GET /api/owners/{id}` de un owner existente → 200 y el JSON contiene los campos del `OwnerDto`
> - `GET /api/owners/{id}` inexistente → 404
> - `GET /api/owners?lastName=Franklin` → 200 con estructura paginada
> - `GET /api/owners` sin parámetros → 200, devuelve todos paginados
> - Verifica explícitamente que la respuesta **no** expone campos internos de la entidad JPA
>
> Sigue el estilo de `OwnerControllerTests`. Ejecuta `./mvnw verify` al final.

**Criterios de aceptación F-05:**
- [ ] `GET /api/owners` y `GET /api/owners/{id}` responden JSON
- [ ] La UI Thymeleaf sigue funcionando sin regresión
- [ ] `/v3/api-docs` y `/swagger-ui.html` exponen el contrato
- [ ] Tests de API en verde

---

## Fase 4 — Diagramas de secuencia y de clases (to-be)

Estos son los **2 diagramas de diseño detallado** que exige la rúbrica (distintos a componentes y despliegue). Reutiliza el pipeline PlantUML que ya existe en `docs/plantuml/`.

### Prompt

> Genera los diagramas de diseño de la arquitectura **to-be**, a partir del código que acabas de implementar (no de la teoría). Crea en `docs/plantuml/`:
>
> 1. **`08-secuencia-crear-pet-tobe.puml`** — diagrama de secuencia del flujo `POST /owners/{ownerId}/pets/new` en la versión modernizada. Participantes: Usuario, `PetController`, `PetService`, `OwnerRepository`, agregado `Owner`, BD. Marca explícitamente:
>    - dónde actúa `@Transactional` (usa una nota o un bloque `group`)
>    - dónde ocurren las validaciones que antes estaban en el controller
>    - Añade una nota comparativa contra `07-secuencia-agregar-pet.puml` (el as-is) señalando qué se movió de capa.
>
> 2. **`09-secuencia-api-owners-tobe.puml`** — secuencia de `GET /api/owners/{id}`. Participantes: Cliente REST, `DispatcherServlet`, `OwnerRestController`, `OwnerService`, `OwnerMapper`, `OwnerRepository`, BD. Incluye el camino alternativo de owner no encontrado (404 vía `@RestControllerAdvice`) usando un bloque `alt`.
>
> 3. **`10-clases-capa-service-tobe.puml`** — diagrama de clases de la capa nueva: `PetService`, `VisitService`, `OwnerService`, `OwnerDto`, `PetDto`, `OwnerMapper`, los controllers (MVC y REST) y `OwnerRepository`. Muestra las firmas reales de los métodos públicos, las dependencias por constructor y los estereotipos (`<<Service>>`, `<<RestController>>`, `<<DTO>>`, `<<Repository>>`).
>
> 4. **`11-despliegue-tobe.puml`** — diagrama de despliegue to-be, adaptando `02-despliegue.puml` (que es el as-is). Debe mostrar nodos y artefactos reales: entorno de ejecución con **Java 21**, el JAR con los tres tipos de componente (MVC + REST + Service), el contenedor Docker, la instancia EC2 y Amazon RDS PostgreSQL. Marca visualmente qué componentes son **legados que se conservan** (repositorios, entidades, templates Thymeleaf) y cuáles son **nuevos**.
>
> **Importante:** las firmas de métodos y las clases deben corresponder al código real que implementaste, no a un diseño idealizado. Léelo antes de dibujar.
>
> Al final, actualiza `docs/plantuml/README.md` con la tabla de los diagramas nuevos y exporta los PNG.

**Nota:** el diagrama 4 cierra el hallazgo "EN PROGRESO" del ítem 1.1 de la rúbrica (el diagrama actual es de capas, no de despliegue formal).

---

## Fase 5 — Contenedores

Hoy el repositorio **no tiene `Dockerfile`** en la raíz (solo uno en `.devcontainer/`, que es para el entorno de desarrollo), y `docker-compose.yml` únicamente levanta bases de datos.

### Prompt

> Crea los artefactos para ejecutar la aplicación modernizada en contenedores.
>
> 1. **`Dockerfile`** en la raíz, con multi-stage build:
>    - Stage 1 (build): imagen con JDK **21** y Maven; aprovecha la caché de dependencias copiando primero `pom.xml`/`.mvn` y ejecutando `dependency:go-offline` antes de copiar `src/`.
>    - Stage 2 (runtime): imagen **JRE 21 slim**, no JDK. Ejecuta con un usuario **no root**. Expón el 8080.
>    - Añade un `HEALTHCHECK` que consulte `/actuator/health`.
>    - Define `SPRING_PROFILES_ACTIVE` como variable de entorno parametrizable (por defecto `postgres`).
>
> 2. **`.dockerignore`** que excluya `target/`, `build/`, `.git/`, `docs/`, `*.md` y archivos de IDE.
>
> 3. **`docker-compose.app.yml`** — archivo **nuevo y separado**, no modifiques el `docker-compose.yml` existente (la cartografía A2 documenta que ese solo provisiona BD, y romperlo invalidaría esa evidencia). Debe levantar la app + PostgreSQL con `depends_on` usando `condition: service_healthy`, y variables de entorno para la conexión JDBC.
>
> 4. **`docs/experimento/contenedores.md`** — documenta cómo construir y ejecutar, y qué decisiones tomaste (por qué multi-stage, por qué usuario no root, por qué JRE y no JDK en runtime).
>
> **Verifica que funciona** antes de darlo por terminado: construye la imagen, levanta el compose, y comprueba que responden tanto `GET /api/owners/1` (JSON) como `GET /owners/1` (HTML). Reporta el tamaño final de la imagen.
>
> **No** crees workflows de GitHub Actions ni ningún pipeline de CI/CD: está fuera del alcance.

---

## Fase 6 — Terraform sobre AWS

Infraestructura según la propuesta del pre-experimento: **ECR + EC2 + RDS PostgreSQL + CloudWatch**, sin CI/CD.

### Prompt 6.1 — estructura y red

> Crea la infraestructura como código en `infra/terraform/`, siguiendo la arquitectura de nube del pre-experimento. Empieza por la estructura y la capa de red.
>
> **Estructura:**
> ```
> infra/terraform/
>   main.tf  providers.tf  variables.tf  outputs.tf  terraform.tfvars.example
>   modules/
>     network/  ecr/  rds/  ec2/  observability/
> ```
>
> **Requisitos generales:**
> - Provider AWS con versión fijada (`required_providers` con constraint `~>`), y `required_version` de Terraform.
> - Región parametrizable, por defecto `us-east-1`.
> - Etiquetas comunes en todos los recursos vía `default_tags`: `Project=spring-petclinic-modernizacion`, `Equipo=17`, `Ambiente=experimento`, `ManagedBy=terraform`.
> - **Nunca** credenciales hardcodeadas. La contraseña de la BD debe venir de una variable marcada `sensitive = true`, y `terraform.tfvars` debe estar en `.gitignore` (deja solo el `.example`).
>
> **Módulo `network`:**
> - VPC con CIDR parametrizable
> - 2 subredes públicas y 2 privadas en AZs distintas
> - Internet Gateway + route table para las públicas
> - Security group para la app: entrada 8080 desde internet
> - Security group para RDS: entrada 5432 **únicamente** desde el security group de la app (referencia por `security_group_id`, nunca un CIDR abierto)
>
> Genera solo la estructura y este módulo. Ejecuta `terraform init` y `terraform validate` y muéstrame el resultado antes de continuar.

### Prompt 6.2 — cómputo, datos y observabilidad

> Ahora completa los módulos restantes.
>
> **`ecr`**: un repositorio para la imagen de la app, con escaneo de imágenes al hacer push y una lifecycle policy que conserve las últimas 10 imágenes.
>
> **`rds`**: instancia PostgreSQL 18.x (la misma major version que usa el `docker-compose.yml` del legado, para que el experimento sea comparable), clase parametrizable con default `db.t3.micro`, en las subredes **privadas**, sin acceso público, con `storage_encrypted = true` y backups deshabilitados o mínimos (es un entorno de experimento, no producción — documenta esa decisión en un comentario).
>
> **`ec2`**: instancias `t3.medium` en subred pública.
> - Parametriza el número de instancias con una variable `instance_count` **con valor por defecto 2**: el pre-experimento especifica desplegar el legado y la versión modernizada en *instancias gemelas* para poder comparar métricas. Etiqueta cada una según su rol (`legado` / `modernizado`).
> - `user_data` que instale Docker, se autentique contra ECR y arranque el contenedor.
> - IAM role + instance profile con los permisos **mínimos**: pull de ECR y escritura de logs/métricas en CloudWatch. Nada de `AdministratorAccess`.
>
> **`observability`**: log group de CloudWatch con retención de 7 días, y alarmas de CPU y de memoria para las instancias.
>
> **Salidas (`outputs.tf`)**: URL del repositorio ECR, IPs públicas de las instancias, endpoint de RDS (marcado `sensitive`).
>
> Finalmente crea `infra/terraform/README.md` explicando el orden de aplicación, los prerrequisitos y **una estimación del costo mensual aproximado** de estos recursos.
>
> Ejecuta `terraform validate` y `terraform fmt -check`. **No ejecutes `terraform apply`** — yo lo haré manualmente cuando revise el plan.

> **Advertencia sobre costos.** `terraform apply` crea recursos que se facturan (EC2 t3.medium ×2 + RDS corriendo 24/7 no son gratis). Aplica solo cuando vayan a medir, y ejecuten `terraform destroy` al terminar. Consideren usar `db.t3.micro` y `t3.small` si el presupuesto es una restricción.

---

## Fase 7 — Estimación y esfuerzo real

Esta fase cierra los ítems **2.5** (estimación) y **3.2** (esfuerzo real) de la rúbrica.

### 7.1 Estimación — decisiones que debe tomar el equipo

La rúbrica pide justificar la unidad y responder dos preguntas orientadoras. Estas son **decisiones del equipo**, no algo que deba generar la IA; abajo va material para que discutan y decidan.

**Unidad recomendada: puntos de historia.** Razón: el trabajo es de refactorización sobre código existente, donde el esfuerzo lo domina la complejidad del cambio (dónde está enredada la lógica, cuánto riesgo de regresión) y no el volumen de funcionalidad nueva. Los puntos de función miden tamaño funcional entregado — y aquí, por diseño, la funcionalidad de F-03 **no cambia**: sale exactamente igual, solo reubicada. Un método que mide "cuánta función nueva hay" daría casi cero para el trabajo más difícil del experimento.

**Pregunta 1 — ¿analogía, desagregación o juicio de expertos?** Argumento defendible: **desagregación + juicio de expertos**. Desagregación porque F-03 y F-05 se parten naturalmente en subtareas verificables (extraer validaciones, refactorizar controller, escribir tests, exponer endpoint). Juicio de expertos porque el equipo ya hizo la cartografía y conoce dónde están los hotspots. Se descarta la analogía como técnica principal: es la primera modernización del equipo sobre este código, no hay histórico propio con qué comparar.

**Pregunta 2 — clasificación en "datos" y "transacciones".** Es la base del análisis de puntos de función (IFPUG): se cuentan ficheros lógicos internos y externos (*datos*) y entradas, salidas y consultas externas (*transacciones*). Aplicado a PetClinic: los datos serían `Owner`+`Pet`+`Visit` (el agregado) y `Vet`+`Specialty`; las transacciones serían el alta de mascota, la consulta de owner, etc.
- *Ventajas*: independiente de la tecnología y del lenguaje, permite comparar sistemas heterogéneos y auditar la estimación con un estándar externo.
- *Desventajas*: requiere entrenamiento formal para contar consistentemente, es costoso, y —el punto clave aquí— **es ciego a la refactorización**: como F-03 no altera ninguna función visible al usuario, el conteo de puntos de función antes y después es idéntico, aunque el esfuerzo real sea alto.

Esa última observación es un buen argumento para el documento: justifica la elección de puntos de historia con evidencia del propio caso.

### 7.2 Prompt — bitácora e instrumentación

> Crea el sistema de registro de esfuerzo del experimento en `docs/experimento/`.
>
> 1. **`estimacion.md`** con una tabla de estimación en puntos de historia (escala Fibonacci: 1, 2, 3, 5, 8, 13) desagregada por subtarea, con columnas: ID, tarea, puntos, y una justificación de una línea. Cubre: F-03 (subdividido), F-05 (subdividido), migración a Java 21, contenedores, Terraform y diagramas. **Deja los puntos en blanco** — los asigna el equipo en sesión de estimación, no la IA.
>
> 2. **`bitacora-esfuerzo.csv`** con encabezados: `fecha,tarea_id,responsable,hora_inicio,hora_fin,horas,tipo_esfuerzo,observaciones`. Donde `tipo_esfuerzo` distingue `disenio`, `implementacion`, `pruebas`, `depuracion`, `documentacion`.
>
> 3. **`scripts/medir-esfuerzo.sh`** que, a partir de `git log` de la rama del experimento, extraiga por commit: hash, fecha/hora, autor, mensaje y líneas añadidas/eliminadas; y calcule el tiempo transcurrido entre commits consecutivos. Que exporte a CSV para cruzarlo con la bitácora manual.
>
> Documenta en `estimacion.md` que el tiempo entre commits es solo un **proxy** del esfuerzo (incluye pausas y excluye el trabajo de diseño previo al primer commit), y que la fuente autoritativa es la bitácora manual.

### 7.3 Nota de honestidad metodológica

Si el equipo implementa con Claude Code, el tiempo de reloj será mucho menor que el esfuerzo humano equivalente. **Repórtenlo así, explícitamente**, y usen esa brecha como material para dos secciones de la rúbrica:

- En **esfuerzo real**: distingan "horas de reloj con asistencia de IAG" de "esfuerzo estimado sin asistencia". La rúbrica pide explicar cuando el esfuerzo real no corresponde al tamaño en puntos — esta es exactamente esa explicación, y es un hallazgo legítimo, no una excusa.
- En **uso de IAG**: es evidencia concreta y cuantificada del impacto de la herramienta, mucho más sólida que una declaración genérica.

Registren también el esfuerzo de *revisión y corrección* del código generado. Es esfuerzo real del experimento y suele ser la parte más informativa.

---

## Orden de ejecución y puntos de control

```
Fase 0 ──► Fase 1 ──► Fase 2 (F-03) ──► Fase 3 (F-05) ──► Fase 4 (diagramas)
                            │                  │
                            └──────────────────┴──► Fase 5 (contenedores) ──► Fase 6 (Terraform)

Fase 7 (bitácora) corre en paralelo desde la Fase 0.
```

**Reglas de trabajo con Claude Code:**

1. **Un commit por fase, como mínimo.** Sin commits intermedios no hay forma de medir esfuerzo por tarea ni de revertir limpiamente.
2. **Ejecuta `./mvnw verify` antes de cerrar cada fase.** No avances con tests en rojo.
3. **Revisa el diff, no solo el resumen.** El modelo puede tocar archivos fuera del alcance; `git diff --stat` antes de cada commit lo detecta rápido.
4. **Las fases 2 y 3 tienen un punto de aprobación explícito** (el diseño del manejo de errores de validación, y la versión de springdoc). No las dejes correr de largo: son las dos decisiones de diseño que realmente importan para la calidad de la arquitectura to-be.
5. **Sesiones separadas por fase.** Contextos largos degradan la precisión; `CLAUDE.md` recupera el contexto entre sesiones.

---

## Trazabilidad con la rúbrica

| Fase de este plan | Ítem de la rúbrica que cierra |
|---|---|
| 2 y 3 | Ejemplos de código legado/modernizado (2.3.3) — pasa de "planeado" a implementado |
| 4 (diagramas 1-3) | **2.4 Diseño detallado** — los 2+ diagramas adicionales exigidos |
| 4 (diagrama 4) | **1.1 Diagrama de despliegue to-be** — cierra el ítem que estaba "EN PROGRESO" |
| 5 y 6 | 2.3.4 Infraestructura computacional — pasa de diagrama a IaC ejecutable |
| 7.1 | **2.5 Estimación de esfuerzo** + las dos preguntas orientadoras |
| 7.2 | **3.2 Esfuerzo real** |
| Todas | **3.3 Repositorio** — el código modernizado que hoy no existe en `src/main` |
| Todas | **3.4 Video** — al haber código ejecutable, se puede grabar legado vs. modernizado |

**Sigue pendiente y no lo cubre este plan:** la pregunta sobre **Apigee** (ítem 1.4) y la **justificación de patrones y tácticas** (ítem 1.3). Son reflexiones del equipo que deben redactarse a mano — aunque después de la Fase 2 tendrán mucho mejor material para argumentarlas, porque habrán visto en la práctica qué implicó aplicar Service Layer y DTO.
