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

## Exportar PNG

```bash
brew install plantuml
cd docs/plantuml
mkdir -p export
plantuml -o export -tpng *.puml
```

O usar extensión **PlantUML** en Cursor/VS Code → Preview.

## Iteraciones

- **Iteración 1:** diagramas 01, 02, 03, 05, 06, 07 validados contra código fuente.
- **Iteración 2:** pendiente integración CodeScene en documento.
- **Iteración 4:** actualizar `04-alcance-modernizacion.puml` cuando se acote alcance.
