$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Path out | Out-Null

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host '[1/2] compiling...' -ForegroundColor Cyan
# 递归收集 src 下所有 .java（含子目录），支持 package 分层
$sources = @(Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName })
if ($sources.Count -eq 0) { throw 'src 目录下没有找到任何 .java 文件' }
Write-Host "      共 $($sources.Count) 个源文件" -ForegroundColor DarkGray

javac -encoding UTF-8 -d out $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Write-Host '[2/2] running...' -ForegroundColor Cyan
java '-Dstdout.encoding=UTF-8' -cp out Main