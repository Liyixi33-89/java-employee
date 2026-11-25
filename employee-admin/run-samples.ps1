# 批量验证 V0.1 收尾场景：日期输入 + 两个排序
#
# 注意：不要用 PowerShell 管道喂输入给 java，PS 5.1 会按控制台代码页转码，
# 中文姓名会变成 ??。这里统一落到 UTF-8 文件，再用 cmd 重定向输入。
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

if (-not (Test-Path out)) {
    throw 'out 目录不存在，请先运行 build.ps1 编译'
}

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Invoke-Scenario {
    param([string]$Name, [string[]]$Lines)

    Write-Host "`n===== $Name =====" -ForegroundColor Yellow

    $id = [guid]::NewGuid().ToString('N')
    $inputFile = Join-Path $env:TEMP ("scenario_$id.txt")
    $outputFile = Join-Path $env:TEMP ("scenario_$id.out")
    [IO.File]::WriteAllText($inputFile, ($Lines -join "`r`n") + "`r`n", $Utf8NoBom)

    cmd /c "java -Dstdout.encoding=UTF-8 -cp out Main < `"$inputFile`" > `"$outputFile`" 2>&1"
    $exitCode = $LASTEXITCODE
    Get-Content -Path $outputFile -Encoding UTF8
    Write-Host "退出码: $exitCode" -ForegroundColor Green

    Remove-Item $inputFile, $outputFile -Force -ErrorAction SilentlyContinue
}

Invoke-Scenario '日期：默认今天 / 指定日期 / 排序对比' @(
    '1', 'E003', '王五', '2024-03-01',
    '1', 'E001', '张三', '',
    '1', 'E002', '李四', '2023-07-15',
    '2',
    '7',
    '8',
    '0'
)

Invoke-Scenario '日期校验：格式错误 / 不存在日期 / 未来日期' @(
    '1', 'E001', '张三', '2024/01/01',
    '1', 'E001', '张三', '2024-02-30',
    '1', 'E001', '张三', '2999-01-01',
    '1', 'E001', '张三', 'abc',
    '2',
    '0'
)

Invoke-Scenario '排序在副本上做，不影响列表原顺序' @(
    '1', 'E009', '甲', '2024-01-01',
    '1', 'E001', '乙', '2024-02-01',
    '7',
    '2',
    '0'
)

Invoke-Scenario '空数据时两个排序入口' @(
    '7',
    '8',
    '0'
)

Invoke-Scenario '完整 CRUD + 日期 + 搜索' @(
    '1', 'E001', '张三', '2022-05-01',
    '1', 'E002', '张小三', '2023-05-01',
    '4', 'E001', '张三丰',
    '6', '张',
    '3', 'E002',
    '5', 'E002', 'DELETE',
    '8',
    '0'
)

Write-Host "`n全部场景执行完毕。" -ForegroundColor Cyan