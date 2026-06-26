# Entrega 2 — Cartografía y Estrategia de Modernización
## Spring PetClinic

| Campo | Valor |
|-------|-------|
| **Proyecto** | Modernización de Spring PetClinic |
| **Repositorio** | `spring-petclinic` |
| **Stack legado (as-is)** | Java 17 · Spring Boot 4.0.3 · Spring MVC · Thymeleaf · JPA |
| **Estrategia elegida** | Replatform + Rearchitect (Migration) |
| **Herramientas de cartografía** | CodeScene + cartografía manual + PlantUML |

---

## Tabla de contenidos

1. [Actividad 1 — Cartografía](#actividad-1--cartografía)
2. [Actividad 2 — Estrategia y alcance](#actividad-2--estrategia-y-alcance)
3. [Diagramas PlantUML](#diagramas-plantuml)
4. [Uso de IAG](#uso-de-iag)
5. [Anexos CodeScene](#anexos-codescene)

---

# Actividad 1 — Cartografía

## 1.1 Justificación de la herramienta de cartografía

Se adopta una **estrategia híbrida** que combina análisis automático de calidad de código con modelado arquitectónico manual:

| Herramienta | Rol | Justificación |
|-------------|-----|---------------|
| **CodeScene** | Métricas de mantenibilidad: hotspots, acoplamiento, salud del código, duplicación, evolución | Cuantifica deuda técnica con evidencia objetiva. Es la herramienta del curso para evaluar legado. Responde preguntas del eje de **mantenibilidad**. |
| **Cartografía manual del repositorio** | Análisis de `pom.xml`, paquetes Java, perfiles Spring, esquemas SQL, CI/CD y despliegue | CodeScene no modela despliegue, relaciones con BD ni patrones arquitectónicos a nivel de sistema. Responde preguntas del eje de **arquitectura**. |
| **PlantUML** | Diagramas UML en texto plano (`.puml`): componentes, despliegue, dominio y alcance | Versionable en Git, editable como código y exportable a PNG/SVG. Representa la **arquitectura concreta** de la aplicación (no la referencia genérica de Spring). |

**Limitación de documentación existente:** El README y las [slides legacy](https://speakerdeck.com/michaelisvy/spring-petclinic-sample-application) describen una versión **pre–Spring Boot**. No reflejan la arquitectura actual del repositorio.

---

## 1.2 Preguntas de comprensión del legado

### Eje: Arquitectura

| ID | Pregunta |
|----|----------|
| **A1** | ¿Cuáles son los componentes funcionales de la aplicación y cómo se relacionan entre sí? |
| **A2** | ¿Cómo es el despliegue de los componentes (runtime, contenedores, bases de datos)? |
| **A3** | ¿Cómo se relacionan los componentes de la aplicación con las fuentes de datos? |
| **A4** | ¿Qué patrones y tácticas arquitectónicas utiliza la aplicación? |

### Eje: Mantenibilidad

| ID | Pregunta |
|----|----------|
| **M1** | ¿Cuáles son los componentes más grandes en términos de líneas de código? |
| **M2** | ¿Cuáles son los componentes más fuertemente acoplados? |
| **M3** | ¿Existe código muerto, duplicado o lógica de negocio mal ubicada? |
| **M4** | ¿Cuál es la cobertura de pruebas y qué módulos están menos protegidos? |

---

## 1.3 Respuestas — Arquitectura

### A1 — Componentes funcionales y relaciones

Spring PetClinic es un **monolito Spring Boot** (~30 clases Java, ~1.931 LOC) organizado por paquetes de dominio:

| Componente | Paquete | Responsabilidad |
|------------|---------|-----------------|
| Gestión de dueños | `owner/` — `OwnerController` | CRUD, búsqueda paginada, detalle con mascotas |
| Gestión de mascotas | `owner/` — `PetController`, `PetValidator` | Alta/edición de pets |
| Gestión de visitas | `owner/` — `VisitController` | Registro de visitas por mascota |
| Listado de veterinarios | `vet/` — `VetController` | HTML paginado + JSON (`GET /vets`) |
| Infraestructura web | `system/` | Welcome, i18n, caché, demo de errores |
| Modelo compartido | `model/` | `BaseEntity`, `Person`, `NamedEntity` |

**Relaciones clave:**
- No existe capa `@Service`: controladores → repositorios directamente.
- Lógica de negocio en entidades (`Owner.addPet()`, `Owner.addVisit()`).
- `Owner` es agregado raíz con cascada JPA hacia `Pet` y `Visit`.

**Evidencia:** `docs/plantuml/01-componentes-as-is.puml` (ver [Diagramas PlantUML](#diagramas-plantuml)).

---

### A2 — Despliegue de componentes

| Aspecto | Detalle |
|---------|---------|
| Empaquetado | JAR ejecutable Spring Boot (monolito) |
| Build | Maven (primario) / Gradle; **Java 17** |
| Contenedor | `./mvnw spring-boot:build-image` (Cloud Native Buildpacks) |
| Orquestación | `k8s/petclinic.yml` — Deployment + Service NodePort |
| BD desarrollo | H2 embebida (perfil por defecto) |
| BD producción/demo | MySQL 9.6 / PostgreSQL 18.3 vía `docker-compose.yml` |
| Observabilidad | Spring Boot Actuator (`/livez`, `/readyz`) |
| CI/CD | GitHub Actions — Maven, Gradle, despliegue Kind |

**Evidencia:** `docs/plantuml/02-despliegue.puml`; archivos `docker-compose.yml`, `k8s/petclinic.yml`.

---

### A3 — Relación con fuentes de datos

Persistencia: **Spring Data JPA + Hibernate**. Esquema vía scripts SQL (`ddl-auto=none`).

| Repositorio | Entidades | Tablas |
|-------------|-----------|--------|
| `OwnerRepository` | Owner, Pet, Visit (cascada) | `owners`, `pets`, `visits` |
| `PetTypeRepository` | PetType | `types` |
| `VetRepository` | Vet, Specialty | `vets`, `specialties`, `vet_specialties` |

**Modelo relacional:**

```
owners (1) ──< pets (N) ──< visits (N)
                └──> types (N:1)
vets (N) ──< vet_specialties >── specialties (N)
```

**Evidencia:** `src/main/resources/db/h2/schema.sql`; `docs/plantuml/03-modelo-dominio.puml`.

---

### A4 — Patrones y tácticas arquitectónicas

| Patrón / táctica | Implementación |
|------------------|----------------|
| MVC | `@Controller` + Thymeleaf |
| Repository | Spring Data JPA (3 interfaces) |
| Rich Domain Model | Agregado Owner → Pet → Visit |
| Entity inheritance | BaseEntity → Person/NamedEntity → subclases |
| Profile-based config | Perfiles `mysql`, `postgres` |
| Cache-Aside | `@Cacheable("vets")` en VetRepository |
| PRG | Flash attributes en formularios |
| API híbrida | Thymeleaf + JSON parcial en `/vets` |

**Deuda arquitectónica:**
- Sin capa de servicios (acoplamiento presentation ↔ persistence).
- Responsabilidad concentrada en controladores del paquete `owner/`.

**Evidencia:** `docs/plantuml/05-capas-arquitectonicas.puml`.

---

## 1.4 Respuestas — Mantenibilidad

### M1 — Componentes más grandes (LOC)

| LOC | Archivo | Paquete |
|-----|---------|---------|
| 183 | `PetController.java` | owner |
| 176 | `OwnerController.java` | owner |
| 176 | `Owner.java` | owner |
| 114 | `VisitController.java` | owner |
| 85 | `Pet.java` | owner |
| 78 | `VetController.java` | vet |

**Total producción:** ~1.931 LOC en 30 archivos. El paquete `owner/` concentra **~62%** del código.

> Completar con captura CodeScene: *Hotspots / File size ranking*.

---

### M2 — Componentes más acoplados

| Origen | Depende de | Tipo |
|--------|------------|------|
| `PetController` | `OwnerRepository`, `PetTypeRepository`, `PetValidator` | Controller → múltiples repos |
| `OwnerController` | `OwnerRepository` | Controller → repo + paginación |
| `VisitController` | `OwnerRepository` | Navegación agregado Owner→Pet→Visit |
| `VetController` | `VetRepository` | Controller → repo + JSON |
| Entidades `owner/` | `model.Person`, `model.NamedEntity` | Herencia compartida |

**Hotspot principal:** `PetController` (183 LOC).

> Completar con captura CodeScene: *System Map*, *Coupling*, *Code Health*.

---

### M3 — Código muerto, duplicado o mal ubicado

| Hallazgo | Evidencia |
|----------|-----------|
| `CrashController` (`/oups`) | Demo que lanza excepción; no es negocio |
| Paquete `service/` solo en tests | `ClinicServiceTests` — naming confuso |
| Lógica en controladores | Binding, paginación, orquestación mezclados con UI |
| Duplicación potencial | Patrones `@InitBinder` + `setAllowedFields` repetidos |

> Completar con captura CodeScene: *Dead code*, *Duplication*, *Complexity*.

---

### M4 — Cobertura de pruebas

| Métrica | Valor |
|---------|-------|
| Archivos test / producción | 17 / 30 |
| Ratio | 0.57:1 |

Tests: MockMvc (controladores), `@DataJpaTest`, Testcontainers (MySQL/PostgreSQL), i18n sync, JMeter.

> Completar con captura CodeScene si aplica.

---

## 1.5 Atributos de calidad degradados

**Respuesta: Sí**, en los siguientes atributos:

| Atributo | ¿Degradado? | Evidencia |
|----------|-------------|-----------|
| **Mantenibilidad** | **Sí** | Hotspots en `owner/`; controladores >170 LOC; sin capa Service |
| **Modificabilidad** | **Sí (moderado)** | Nuevas reglas requieren tocar controladores |
| **Testabilidad** | **Parcial** | MockMvc existe; difícil test unitario puro de reglas |
| **Seguridad** | **Sí (por omisión)** | Sin autenticación/autorización |
| Rendimiento | No evidenciado | Caché vets; OSIV=false |
| Usabilidad | No degradado | Bootstrap 5, i18n (10 idiomas) |
| Portabilidad | No degradado | Multi-BD, K8s, GraalVM hints |

**Prioridad de modernización:** mantenibilidad y modificabilidad → motivan la introducción de capa de servicios y migración de plataforma.

---

# Actividad 2 — Estrategia y alcance

## 2.1 Motivador de negocio

**Motivador:** Garantizar la **evolución sostenible** de la clínica veterinaria digital para soportar integraciones externas, APIs móviles y despliegues cloud-native **sin incrementar el costo de cambio**.

| Driver | Atributo de calidad | Situación actual |
|--------|---------------------|------------------|
| Reducir time-to-market | Modificabilidad / Mantenibilidad | Degradada — lógica en controladores |
| Habilitar integraciones REST | Interoperabilidad | Solo `/vets` expone JSON |
| Alinear con ecosistema LTS | Portabilidad | Java 17 → migrar a Java 21 |
| Reducir riesgo en producción | Confiabilidad / Seguridad | Sin auth |

---

## 2.2 Estrategia de modernización

**Estrategia: Replatform + Rearchitect (Migration)**

| Estrategia | Qué implica en PetClinic | Justificación |
|------------|--------------------------|---------------|
| **Replatform** | Java 17 → **Java 21 LTS**; dependencias al día | Bajo riesgo; mantiene funcionalidad; habilita virtual threads, records |
| **Rearchitect** | Capa `@Service`, DTOs, API REST ampliada | Responde a mantenibilidad e interoperabilidad (cartografía M1–M2, A4) |
| **Refactor** | Descomponer hotspots; aislar `CrashController` | Evidencia CodeScene + LOC manual |

**Descartadas:**
- **Rewrite/Replace:** codebase pequeño y funcional; costo no justificado.
- **Strangler Fig:** monolito homogéneo; no hay subsistemas heterogéneos.

---

## 2.3 Alcance — Componentes a modernizar

**Evidencia:** `docs/plantuml/04-alcance-modernizacion.puml`.

| Componente | Decisión | Justificación |
|------------|----------|---------------|
| `owner/*` controllers | **Modernizar** | 62% LOC; hotspots M1/M2 |
| Capa `@Service` | **Crear** | Elimina acoplamiento controller→repo |
| `VetController` + REST | **Modernizar** | Base para API completa |
| Repositorios + entidades | **Mantener** | Estables, bien modelados |
| `system/` (welcome, cache, i18n) | **Mantener** | Sin deuda crítica |
| `CrashController` | **Evaluar/remover** | Demo; posible dead code (M3) |

---

## 2.4 Funcionalidades a modernizar

| ID | Funcionalidad | Descripción | Criterios de aceptación |
|----|---------------|-------------|-------------------------|
| **F-01** | Migración Java 21 | Toolchain, CI y runtime | Build Maven/Gradle OK; CI verde; app en Java 21 |
| **F-02** | Service — Owners | Extraer lógica de `OwnerController` | Controller delega; tests unitarios service |
| **F-03** | Service — Pets & Visits | Extraer `PetController`, `VisitController` | `PetController` < 100 LOC; tests OK |
| **F-04** | API REST — Vets | OpenAPI para veterinarios | `GET /api/vets` documentado; tests API |
| **F-05** | API REST — Owners | Consulta owners vía REST | `GET /api/owners`, `GET /api/owners/{id}` |
| **F-06** | Seguridad básica | Spring Security en API | Auth en `/api/**`; UI sin regresión |

**Justificación:** Se prioriza el módulo `owner/` (hotspot cartográfico) y la apertura API (interoperabilidad). F-01 es prerequisito transversal.

---

# Diagramas PlantUML

Los diagramas están en `docs/plantuml/` como archivos `.puml` (texto plano, versionables en Git).

## Diagramas incluidos

| # | Archivo | Propósito | Responde |
|---|---------|-----------|----------|
| 01 | `01-componentes-as-is.puml` | Arquitectura actual: controladores, repos, dominio, BD | A1 |
| 02 | `02-despliegue.puml` | Navegador, JVM, JAR, H2/MySQL/PostgreSQL, K8s | A2 |
| 03 | `03-modelo-dominio.puml` | Clases JPA, herencia, asociaciones | A3, A4 |
| 04 | `04-alcance-modernizacion.puml` | Qué modernizar, crear y mantener | Actividad 2 |
| 05 | `05-capas-arquitectonicas.puml` | Presentación → Persistencia → Dominio (sin Service) | A4 |

## Visualizar y exportar PNG

**VS Code / Cursor:** extensión PlantUML → Preview Current Diagram.

**CLI:**

```bash
brew install plantuml   # si no está instalado
cd docs/plantuml
mkdir -p export
plantuml -o export -tpng *.puml
```

**Online:** [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml)

Insertar los PNG de `docs/plantuml/export/` en el informe Word/PDF final.

---

# Uso de IAG

| Actividad | Uso de IAG | Herramienta |
|-----------|------------|-------------|
| Exploración del repositorio | Análisis de estructura, LOC, patrones | Cursor Agent |
| Generación diagramas PlantUML | Archivos `.puml` de arquitectura | Cursor Agent |
| Redacción del documento | Borrador según rúbrica del curso | Cursor Agent |
| Métricas de mantenibilidad | Validación humana con CodeScene | Equipo |
| Decisiones de alcance | Revisión y aprobación del equipo | Equipo |

**Declaración:** IAG se usa como asistente de análisis y documentación. Las métricas cuantitativas provienen de CodeScene. Las decisiones de modernización son responsabilidad del equipo.

---

# Anexos CodeScene

Insertar capturas en las secciones correspondientes:

- [ ] **System Map** — dependencias entre módulos → M2
- [ ] **Hotspots** — top 10 archivos → M1
- [ ] **Code Health** — `PetController`, `OwnerController`, `Owner.java` → 1.5
- [ ] **Coupling / Change coupling** → M2
- [ ] **Duplicación** → M3
- [ ] **Trend / Evolution** (opcional)

---

## Referencias del repositorio

| Recurso | Ruta |
|---------|------|
| Código fuente | `src/main/java/org/springframework/samples/petclinic/` |
| Esquema BD | `src/main/resources/db/h2/schema.sql` |
| Despliegue K8s | `k8s/petclinic.yml` |
| Docker Compose | `docker-compose.yml` |
| Build / versión Java | `pom.xml` (java.version=17, Spring Boot 4.0.3) |
| Diagramas PlantUML | `docs/plantuml/*.puml` |
