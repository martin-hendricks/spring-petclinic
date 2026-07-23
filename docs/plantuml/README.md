# Diagramas PlantUML — Spring PetClinic

## Archivos (Iteración 1 — validados contra código)

| Archivo | Tipo | Uso |
|---------|------|-----|
| `01-componentes-as-is.puml` | Componentes | A1 — arquitectura actual |
| `02-despliegue.puml` | Despliegue | A2 — runtime y BD |
| `03-modelo-dominio.puml` | Clases | A3/A4 — entidades JPA |
| `04-alcance-modernizacion.puml` | Componentes | Actividad 2 (borrador) |
| `05-capas-arquitectonicas.puml` | Capas | A4 — deuda sin Service |
| `06-secuencia-buscar-owner.puml` | Secuencia | A1 — flujo verificado |
| `07-secuencia-agregar-pet.puml` | Secuencia | A1 — flujo verificado |

## Archivos (Iteración 2 — diagramas to-be, Fase 4 del experimento)

Generados a partir del código realmente implementado en las Fases 2-3 (F-03 y F-05), no de un
diseño idealizado. Cada uno referencia su contraparte as-is cuando existe.

| Archivo | Tipo | Uso |
|---------|------|-----|
| `08-secuencia-crear-pet-tobe.puml` | Secuencia | F-03 — `POST /owners/{ownerId}/pets/new` con `PetService`; compara contra `07-secuencia-agregar-pet.puml` |
| `09-secuencia-api-owners-tobe.puml` | Secuencia | F-05 — `GET /api/owners/{id}`, incluye camino alterno 404 vía `@RestControllerAdvice` |
| `10-clases-capa-service-tobe.puml` | Clases | Capa `@Service` completa: `PetService`, `VisitService`, `OwnerService`, DTOs, mapper, controllers y `OwnerRepository`, con firmas reales |
| `11-despliegue-tobe.puml` | Despliegue | Adapta `02-despliegue.puml`: Java 21, contenedor Docker (Fase 5), EC2 + RDS PostgreSQL (Fase 6); distingue componentes legados conservados de componentes nuevos |

## Exportar PNG

```bash
brew install plantuml
cd docs/plantuml
mkdir -p export
plantuml -o export -tpng *.puml
```

O usar extensión **PlantUML** en Cursor/VS Code → Preview.

> **Nota (Fase 4):** el entorno de esta sesión de Claude Code no tiene `plantuml`, `graphviz`
> (`dot`) ni Docker instalados localmente, por lo que los PNG de los diagramas 08-11 **no se
> generaron** en esta iteración — solo se crearon los `.puml`. Ejecuta el comando de arriba (o la
> extensión de IDE) para exportarlos cuando tengas alguna de esas herramientas disponible. Queda
> registrado como desviación en `docs/experimento/ESTADO-PLAN.md`.

## Iteraciones

- **Iteración 1:** diagramas 01, 02, 03, 05, 06, 07 validados contra código fuente.
- **Iteración 2:** diagramas 08, 09, 10, 11 (to-be) generados a partir del código de F-03/F-05;
  PNGs pendientes de exportar por falta de tooling local (ver nota arriba).
- **Iteración 4:** actualizar `04-alcance-modernizacion.puml` cuando se acote alcance.
