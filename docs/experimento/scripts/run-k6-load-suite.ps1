<#
.SYNOPSIS
  Corre la matriz de carga (10/100/250/500 VUs x N repeticiones) contra legado o
  modernizado, con calentamiento y enfriamiento entre rondas, y exporta un JSON de
  resumen por corrida para poder calcular media/desviación entre repeticiones.

.EXAMPLE
  # Legado en localhost (siguiente iteración, cuando el legado esté arriba)
  ./run-k6-load-suite.ps1 -BaseUrl "http://localhost:8080" -Mode legado

.EXAMPLE
  # Modernizado en AWS, mismos niveles, para comparar
  ./run-k6-load-suite.ps1 -BaseUrl "http://18.233.169.133:8080" -Mode modernizado
#>
param(
	[string]$BaseUrl = "http://localhost:8080",
	[ValidateSet("legado", "modernizado")]
	[string]$Mode = "legado",
	[int[]]$VuLevels = @(10, 100, 250, 500),
	[int]$Repetitions = 3,
	[string]$Ramp = "30s",
	[string]$Steady = "3m",
	[int]$CooldownSeconds = 60
)

$scriptDir = $PSScriptRoot
$resultsDir = Join-Path $scriptDir "k6-results"
New-Item -ItemType Directory -Force -Path $resultsDir | Out-Null

$totalRuns = $VuLevels.Count * $Repetitions
$runIndex = 0

foreach ($vus in $VuLevels) {
	for ($rep = 1; $rep -le $Repetitions; $rep++) {
		$runIndex++
		$env:BASE_URL = $BaseUrl
		$env:MODE = $Mode
		$env:TARGET_VUS = "$vus"
		$env:RAMP = $Ramp
		$env:STEADY = $Steady
		$outFile = Join-Path $resultsDir "$Mode-vus$vus-run$rep.json"

		Write-Host "[$runIndex/$totalRuns] $Mode | VUs=$vus | repeticion $rep/$Repetitions -> $outFile"
		& k6 run (Join-Path $scriptDir "k6-load-test.js") --summary-export=$outFile

		$isLast = ($vus -eq $VuLevels[-1]) -and ($rep -eq $Repetitions)
		if (-not $isLast) {
			Write-Host "Enfriando $CooldownSeconds s antes de la siguiente corrida..."
			Start-Sleep -Seconds $CooldownSeconds
		}
	}
}

Write-Host "Listo. Resultados en $resultsDir"
