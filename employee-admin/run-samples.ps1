# 用样例输入批量验证第一周边界场景（不依赖手工敲键盘）
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not (Test-Path out)) {
    throw 'out 目录不存在，请先运行 build.ps1 编译'
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$JAVA_ARGS = @('-Dstdout.encoding=UTF-8', '-Dstdin.encoding=UTF-8', '-cp', 'out', 'Main')

function Invoke-Scenario {
    param([string]$Name, [string[]]$Lines)

    Write-Host "`n===== $Name =====" -ForegroundColor Yellow

    # 明确以 UTF-8 无 BOM 落地输入文件，避免 BOM 混进第一行输入
    $tempFile = Join-Path $env:TEMP ("scenario_" + [guid]::NewGuid().ToString('N') + ".txt")
    [IO.File]::WriteAllText($tempFile, ($Lines -join "`r`n") + "`r`n", $Utf8NoBom)

    Get-Content -Path $tempFile -Encoding UTF8 | & java @JAVA_ARGS
    Write-Host "退出码: $LASTEXITCODE" -ForegroundColor Green

    Remove-Item $tempFile -Force -ErrorAction SilentlyContinue
}

Invoke-Scenario '非法输入容错（字母/空/越界/负数/合法/退出）' @('abc', '', '9', '-1', '1', '0')
Invoke-Scenario '连续操作三次后退出' @('2', '3', '5', '4', '0')
Invoke-Scenario 'EOF 退出（无退出指令）' @('1', '2')

Write-Host "`n全部场景执行完毕。" -ForegroundColor Cyan