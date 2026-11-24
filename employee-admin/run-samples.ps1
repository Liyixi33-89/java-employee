# 批量验证 V0.1 验收场景（第四周）
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

Invoke-Scenario '完整 CRUD + 搜索' @(
    '1', 'E001', '张三',
    '1', 'E002', '李四',
    '1', 'E003', '张小三',
    '6', '张',
    '4', 'E001', '张三丰',
    '5', 'E002', 'DELETE',
    '2',
    '0'
)

Invoke-Scenario '搜索：空关键词 / 无匹配 / 英文大小写' @(
    '1', 'E001', 'Alice',
    '1', 'E002', 'BOB',
    '6', '',
    '6', 'zzz',
    '6', 'alice',
    '6', 'bo',
    '0'
)

Invoke-Scenario '修改：回车保持不变 / 工号不存在 / 姓名过长' @(
    '1', 'E001', '张三',
    '4', 'E001', '',
    '4', 'NOPE', '李四',
    '4', 'E001', '这个名字实在是太长了吧超过二十个字符的限制了啊啊啊',
    '2',
    '0'
)

Invoke-Scenario '删除：确认词错误取消 / 工号不存在' @(
    '1', 'E001', '张三',
    '5', 'E001', 'delete',
    '2',
    '5', 'NOPE', 'DELETE',
    '2',
    '0'
)

Invoke-Scenario '工号规则在各路径一致生效' @(
    '1', '', '张三',
    '1', 'A', '张三',
    '1', 'E001-', '张三',
    '1', 'E001', '   ',
    '4', '', '张三',
    '5', 'A', 'DELETE',
    '0'
)

Invoke-Scenario '非法菜单输入与 EOF 退出' @(
    'abc', '', '9', '-1', '7',
    '2',
    '1', 'E001', '张三'
)

Write-Host "`n全部场景执行完毕。" -ForegroundColor Cyan