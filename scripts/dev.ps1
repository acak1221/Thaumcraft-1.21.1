param(
    [ValidateSet('build','test','compile','validate','gametest','client-smoke','server','tasks')]
    [string]$Task = 'validate',
    [string]$JavaHome = $env:JAVA_HOME,
    [switch]$RerunTasks
)
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $taskPortableRoot = Join-Path (Split-Path -Parent $taskRoot) 'work\jdk21'
    $taskPortableJdk = Get-ChildItem -LiteralPath $taskPortableRoot -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($taskPortableJdk) { $JavaHome = $taskPortableJdk.FullName }
}
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) {
    throw 'Java 21 JDK not found. Pass -JavaHome or set JAVA_HOME to a JDK 21 installation.'
}
$taskPreviousJavaHome = $env:JAVA_HOME
$taskPreviousTemp = $env:TEMP
$taskPreviousTmp = $env:TMP
$taskPreviousPathExt = $env:PATHEXT
[string[]]$taskArguments = switch ($Task) {
    'build' { @(':fabric:build') }
    'test' { @(':common:test') }
    'compile' { @(':common:compileJava', ':fabric:compileJava') }
    'validate' { @(':common:test', ':fabric:clientTestClasses', ':fabric:build', ':fabric:runGameTest') }
    'gametest' { @(':fabric:runGameTest') }
    'client-smoke' { @(':fabric:runClientSmoke') }
    'server' { @(':fabric:runServer', '--args=--nogui') }
    'tasks' { @('tasks') }
}
if ($RerunTasks) { $taskArguments += '--rerun-tasks' }
try {
    $env:JAVA_HOME = $JavaHome
    if (($env:PATHEXT -split ';') -notcontains '.EXE') {
        $env:PATHEXT = "$($env:PATHEXT);.EXE"
    }
    $taskTemp = Join-Path $taskRoot '.tmp'
    New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
    $env:TEMP = $taskTemp
    $env:TMP = $taskTemp
    Push-Location $taskRoot
    try {
        # Launch the wrapper directly: this also works when .bat file associations are restricted.
        & (Join-Path $JavaHome 'bin\java.exe') -classpath (Join-Path $taskRoot 'gradle\wrapper\gradle-wrapper.jar') org.gradle.wrapper.GradleWrapperMain --no-daemon --console=plain --gradle-user-home (Join-Path $taskRoot '.gradle21') @taskArguments
        if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $taskPreviousJavaHome
    $env:TEMP = $taskPreviousTemp
    $env:TMP = $taskPreviousTmp
    $env:PATHEXT = $taskPreviousPathExt
}
