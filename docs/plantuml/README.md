# Diagramas PlantUML — Spring PetClinic

Diagramas UML de cartografía y modernización en formato [PlantUML](https://plantuml.com/).

## Archivos

| Archivo | Diagrama | Uso en entrega |
|---------|----------|----------------|
| `01-componentes-as-is.puml` | Componentes As-Is | Actividad 1 — A1 |
| `02-despliegue.puml` | Despliegue | Actividad 1 — A2 |
| `03-modelo-dominio.puml` | Modelo de dominio | Actividad 1 — A3, A4 |
| `04-alcance-modernizacion.puml` | Alcance modernización | Actividad 2 |
| `05-capas-arquitectonicas.puml` | Capas arquitectónicas | Actividad 1 — A4 |

## Visualizar y exportar

### Opción 1 — VS Code / Cursor

Instalar extensión **PlantUML** (jebbs.plantuml). Abrir cualquier `.puml` y usar **Preview Current Diagram**.

### Opción 2 — CLI (Java)

```bash
# Instalar PlantUML (macOS)
brew install plantuml

# Generar PNG de todos los diagramas
cd docs/plantuml
plantuml -tpng *.puml

# O SVG
plantuml -tsvg *.puml
```

Los PNG se guardan en `docs/plantuml/export/` si usas:

```bash
mkdir -p export
plantuml -o export -tpng *.puml
```

### Opción 3 — Online

Copiar el contenido de un `.puml` en [plantuml.com/plantuml](https://www.plantuml.com/plantuml/uml) y exportar PNG/SVG.

## Editar

Los archivos `.puml` son texto plano. Modificar y regenerar las imágenes para el informe final.
