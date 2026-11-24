$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Path out | Out-Null

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host '[1/2] compiling...' -ForegroundColor Cyan
javac -encoding UTF-8 -d out (Get-ChildItem src\*.java | ForEach-Object { $_.FullName })
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Write-Host '[2/2] running...' -ForegroundColor Cyan
java '-Dstdout.encoding=UTF-8' -cp out Main