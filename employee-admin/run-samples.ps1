# 用样例输入批量验证 V0.1 第二周边界场景（不依赖手工敲键盘）
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

Invoke-Scenario '新增成功 + 列表 + 查询命中' @(
    '1', 'E001', '张三',
    '1', 'E002', '李四',
    '2',
    '3', 'E002',
    '0'
)

Invoke-Scenario '工号重复被拒绝' @(
    '1', 'E001', '张三',
    '1', 'E001', '王五',
    '2',
    '0'
)

Invoke-Scenario '空工号/空姓名/工号格式非法' @(
    '1', '', '张三',
    '1', 'E001', '   ',
    '1', 'A', '张三',
    '1', 'E001-', '张三',
    '0'
)

Invoke-Scenario '查询不存在 + 空列表后新增' @(
    '2',
    '3', 'NOPE',
    '1', 'E100', '赵六',
    '2',
    '0'
)

Invoke-Scenario '非法菜单输入仍可用' @(
    'abc', '', '9', '-1',
    '1', 'E001', '张三',
    '0'
)

Write-Host "`n全部场景执行完毕。" -ForegroundColor Cyan