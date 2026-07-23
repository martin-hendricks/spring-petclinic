#!/usr/bin/env bash
# Extrae, por cada commit de la rama del experimento, un proxy de esfuerzo a partir del
# historial de git: hash, fecha/hora, autor, mensaje, líneas añadidas/eliminadas, y horas
# transcurridas desde el commit anterior. Exporta a CSV para cruzarlo con la bitácora
# manual (bitacora-esfuerzo.csv) — ver la nota metodológica en estimacion.md: el tiempo
# entre commits es SOLO un proxy, no la fuente autoritativa de esfuerzo.
#
# Uso: ./medir-esfuerzo.sh [rama] [commit_base]
#   rama:         por defecto feature/modernizado
#   commit_base:  primer commit A EXCLUIR (se listan los commits posteriores a este). Por
#                 defecto se toma el hash documentado en baseline-metrics.md (el estado
#                 justo antes de empezar la Fase 0); si ese archivo no existe, se usa el
#                 merge-base con main. Sin este límite, el log incluiría también el
#                 historial completo del PetClinic original (miles de commits ajenos al
#                 experimento), no solo el trabajo de las Fases 0-7.
set -euo pipefail

BRANCH="${1:-feature/modernizado}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT_FILE="${SCRIPT_DIR}/../metricas-commits.csv"
BASELINE_FILE="${SCRIPT_DIR}/../baseline-metrics.md"

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

if [[ -n "${2:-}" ]]; then
	BASE_COMMIT="$2"
elif [[ -f "$BASELINE_FILE" ]] && grep -qE '[0-9a-f]{40}' "$BASELINE_FILE"; then
	BASE_COMMIT=$(grep -oE '[0-9a-f]{40}' "$BASELINE_FILE" | head -1)
else
	BASE_COMMIT=$(git merge-base main "$BRANCH")
fi

echo "Rango: ${BASE_COMMIT}..${BRANCH}" >&2

echo "hash,fecha_hora,autor,mensaje,lineas_agregadas,lineas_eliminadas,horas_desde_commit_anterior" > "$OUT_FILE"

{ git log --reverse --pretty=format:'%H|%at|%aI|%an|%s' "${BASE_COMMIT}..${BRANCH}"; echo; } | {
	prev_epoch=""
	while IFS='|' read -r hash epoch fecha_iso autor mensaje; do
		[[ -z "$hash" ]] && continue
		stats=$(git show --shortstat --format='' "$hash")
		agregadas=$(grep -oE '[0-9]+ insertion' <<<"$stats" | grep -oE '[0-9]+' || echo 0)
		eliminadas=$(grep -oE '[0-9]+ deletion' <<<"$stats" | grep -oE '[0-9]+' || echo 0)
		mensaje_csv=${mensaje//\"/\"\"}

		if [[ -n "$prev_epoch" ]]; then
			horas=$(awk -v a="$prev_epoch" -v b="$epoch" 'BEGIN { printf "%.2f", (b - a) / 3600 }')
		else
			horas=""
		fi

		echo "${hash},${fecha_iso},${autor},\"${mensaje_csv}\",${agregadas},${eliminadas},${horas}"
		prev_epoch="$epoch"
	done
} >> "$OUT_FILE"

echo "Escrito: $OUT_FILE"
