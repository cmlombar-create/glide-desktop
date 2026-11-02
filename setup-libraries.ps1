# Run once in PowerShell from the project directory (or via its full path).
$ErrorActionPreference = 'Stop'
$lib = Join-Path $PSScriptRoot 'lib'
New-Item -ItemType Directory -Force -Path $lib | Out-Null
foreach ($name in @('slf4j-api', 'slf4j-nop')) {
    $file = "$name-2.0.17.jar"
    $url = "https://repo.maven.apache.org/maven2/org/slf4j/$name/2.0.17/$file"
    Invoke-WebRequest -Uri $url -OutFile (Join-Path $lib $file)
    Write-Host "Downloaded $file"
}
Write-Host 'Dependencies ready. Clean and Build in NetBeans.'
