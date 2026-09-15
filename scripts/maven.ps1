param([Parameter(ValueFromRemainingArguments = $true)][string[]]$MavenArgs)
$ErrorActionPreference = 'Stop'
$taskProjectRoot = Split-Path -Parent $PSScriptRoot
if (-not $MavenArgs -or $MavenArgs.Count -eq 0) { $MavenArgs = @('install') }
Push-Location -LiteralPath $taskProjectRoot
try {
    $taskMvn = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($taskMvn) {
        & $taskMvn.Source @MavenArgs
        exit $LASTEXITCODE
    }

    # Eclipse m2e incluye Maven aunque mvn no esté en PATH.
    $taskPool = Join-Path $env:USERPROFILE '.p2\pool\plugins'
    $taskRuntime = Get-ChildItem -LiteralPath $taskPool -Directory -ErrorAction SilentlyContinue |
        Where-Object Name -Like 'org.eclipse.m2e.maven.runtime_*' |
        Sort-Object Name -Descending | Select-Object -First 1
    if (-not $taskRuntime) {
        throw 'No se encontró Maven. Usa Run As > Maven build en Eclipse o instala Maven y agrégalo al PATH.'
    }
    $taskExtras = @(
        'org.apache.commons.cli_*.jar', 'org.apache.commons.lang3_*.jar',
        'org.apache.commons.commons-codec_*.jar', 'com.google.guava_*.jar',
        'com.google.guava.failureaccess_*.jar', 'slf4j.api_*.jar',
        'jakarta.annotation-api_1.*.jar', 'jakarta.inject.jakarta.inject-api_1.*.jar'
    ) | ForEach-Object {
        Get-ChildItem -LiteralPath $taskPool -Filter $_ -File |
            Sort-Object Name -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
    $taskClasspath = (@($taskRuntime.FullName, (Join-Path $taskRuntime.FullName 'jars\*')) + @($taskExtras)) -join ';'
    $taskJava = (Get-Command java.exe -ErrorAction Stop).Source
    $taskLocalRepo = Join-Path $taskProjectRoot '.maven-repository'
    & $taskJava "-Dmaven.multiModuleProjectDirectory=$taskProjectRoot" "-Dmaven.home=$($taskRuntime.FullName)" `
        -cp $taskClasspath org.apache.maven.cli.MavenCli "-Dmaven.repo.local=$taskLocalRepo" @MavenArgs
    exit $LASTEXITCODE
} finally { Pop-Location }
