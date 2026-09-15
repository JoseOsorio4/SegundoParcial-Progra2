param([switch]$SoloComprobar)
$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path -Parent $PSScriptRoot
$taskConfigPath = Join-Path $taskProjectRoot 'db-local.properties'
if (-not (Test-Path -LiteralPath $taskConfigPath)) {
    throw 'Copia db-example.properties como db-local.properties y configura tu conexión.'
}
$taskConfig = @{}
Get-Content -LiteralPath $taskConfigPath -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^\s*db\.[^=]+=') {
        $taskPair = $_ -split '=', 2
        $taskConfig[$taskPair[0].Trim()] = $taskPair[1]
    }
}
$taskMysql = Get-Command mysql.exe -ErrorAction Stop
$taskToolsPath = Join-Path $taskProjectRoot '.tools'
New-Item -ItemType Directory -Path $taskToolsPath -Force | Out-Null
$taskClientPath = Join-Path $taskToolsPath ('mysql-' + [guid]::NewGuid().ToString('N') + '.cnf')
function ConvertTo-MySqlOption([string]$Value) {
    if ($Value.Contains("`r") -or $Value.Contains("`n")) { throw 'Opción de conexión inválida.' }
    return '"' + $Value.Replace('\', '\\').Replace('"', '\"') + '"'
}
$taskOptions = @('[client]', 'protocol=TCP')
foreach ($taskKey in @('host', 'port', 'user', 'password')) {
    $taskOptions += $taskKey + '=' + (ConvertTo-MySqlOption $taskConfig['db.' + $taskKey])
}
try {
    [System.IO.File]::WriteAllLines($taskClientPath, $taskOptions, [System.Text.UTF8Encoding]::new($false))
    if ($SoloComprobar) {
        & $taskMysql.Source "--defaults-file=$taskClientPath" --batch --execute='SELECT VERSION() AS version, CURRENT_USER() AS usuario;'
    } else {
        $taskSchema = Join-Path $taskProjectRoot 'agenda-citas-ui\sql\schema.sql'
        $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
        Get-Content -LiteralPath $taskSchema -Raw -Encoding UTF8 |
            & $taskMysql.Source "--defaults-file=$taskClientPath" --default-character-set=utf8mb4 --batch
    }
    if ($LASTEXITCODE -ne 0) { throw 'MySQL rechazó la operación. Revisa la conexión local y el mensaje anterior.' }
    if (-not $SoloComprobar) { Write-Output 'Base segundo_parcial_progra2 y tabla citas preparadas.' }
} finally {
    if (Test-Path -LiteralPath $taskClientPath) { Remove-Item -LiteralPath $taskClientPath }
}
