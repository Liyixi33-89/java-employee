# 批量验证 V0.1 场景：带断言、失败即退出非零。
#
# 设计要点（针对"只打印不断言"的缺陷）：
#   1. 每个场景声明期望输出片段，缺任何一条即判定失败
#   2. 编译产物时间戳必须晚于源文件，否则拒绝运行旧 class
#   3. 任一场景失败，脚本最终以退出码 1 结束
#
# 注意：不要用 PowerShell 管道喂输入给 java，PS 5.1 会按控制台代码页转码，
# 中文姓名会变成 ??。这里统一落到 UTF-8 文件，再用 cmd 重定向输入。
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# ---------- 前置换算：确保跑的是最新编译产物 ----------
if (-not (Test-Path out)) {
    throw 'out 目录不存在，请先运行 build.ps1 编译'
}
$newestSource = Get-ChildItem src\*.java | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$classFiles = @(Get-ChildItem out\*.class -ErrorAction SilentlyContinue)
if ($classFiles.Count -eq 0) {
    throw 'out 目录里没有任何 .class，请先运行 build.ps1 编译'
}
$oldestClass = $classFiles | Sort-Object LastWriteTime | Select-Object -First 1
if ($oldestClass.LastWriteTime -lt $newestSource.LastWriteTime) {
    throw "编译产物比源码旧（$($oldestClass.Name) 早于 $($newestSource.Name)），请先重新运行 build.ps1"
}
Write-Host "编译产物校验通过：$($oldestClass.Name) $($oldestClass.LastWriteTime) >= $($newestSource.Name) $($newestSource.LastWriteTime)" -ForegroundColor DarkGray

$Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$script:Failures = @()
$script:Passed = 0

function Invoke-Scenario {
    param(
        [string]$Name,
        [string[]]$Lines,
        [string[]]$Expect,       # 必须出现的片段
        [string[]]$Reject = @()  # 必须不出现的片段
    )

    $id = [guid]::NewGuid().ToString('N')
    $inputFile = Join-Path $env:TEMP ("scenario_$id.txt")
    $outputFile = Join-Path $env:TEMP ("scenario_$id.out")
    [IO.File]::WriteAllText($inputFile, ($Lines -join "`r`n") + "`r`n", $Utf8NoBom)

    cmd /c "java -Dstdout.encoding=UTF-8 -cp out Main < `"$inputFile`" > `"$outputFile`" 2>&1"
    $exitCode = $LASTEXITCODE
    $output = if (Test-Path $outputFile) { [IO.File]::ReadAllText($outputFile, [Text.Encoding]::UTF8) } else { '' }

    $problems = @()
    if ($exitCode -ne 0) {
        $problems += "进程退出码 $exitCode（期望 0）"
    }
    foreach ($p in $Expect) {
        if ($output -notlike "*$p*") {
            $problems += "缺少期望输出：$p"
        }
    }
    foreach ($p in $Reject) {
        if ($output -like "*$p*") {
            $problems += "出现了不该有的输出：$p"
        }
    }

    if ($problems.Count -eq 0) {
        Write-Host "[PASS] $Name" -ForegroundColor Green
        $script:Passed++
    } else {
        Write-Host "[FAIL] $Name" -ForegroundColor Red
        $problems | ForEach-Object { Write-Host "       - $_" -ForegroundColor Red }
        Write-Host "       ---- 实际输出 ----" -ForegroundColor DarkGray
        $output -split "`n" | Where-Object { $_.Trim() } | ForEach-Object { Write-Host "       $_" -ForegroundColor DarkGray }
        $script:Failures += $Name
    }

    Remove-Item $inputFile, $outputFile -Force -ErrorAction SilentlyContinue
}

# ============ 场景 1：默认日期 + 指定日期 + 三种排序对比 ============
Invoke-Scenario -Name '新增：默认今天 / 指定日期，三个列表顺序各不相同' -Lines @(
    '1', 'E003', '王五', '2024-03-01',
    '1', 'E001', '张三', '',
    '1', 'E002', '李四', '2023-07-15',
    '2', '7', '8', '0'
) -Expect @(
    '新增成功：E003  王五  入职:2024-03-01',
    '新增成功：E002  李四  入职:2023-07-15',
    # 列表默认顺序 = 插入顺序 E003, E001, E002
    "1. E003  王五",
    # 按工号升序 = E001, E002, E003
    "1. E001  张三",
    # 按入职日期降序 = 2024-03-01(E003), 2023-07-15(E002)
    "1. E003  王五  入职:2024-03-01"
)

# ============ 场景 2：日期校验（含静默修正陷阱）============
Invoke-Scenario -Name '日期校验：格式错/不存在日期/未来日期全部拒绝' -Lines @(
    '1', 'E001', '张三', '2024/01/01',
    '1', 'E001', '张三', '2024-02-30',
    '1', 'E001', '张三', '2999-01-01',
    '1', 'E001', '张三', 'abc',
    '2', '0'
) -Expect @(
    '新增失败：日期格式错误：需为 yyyy-MM-dd，当前为 "2024/01/01"',
    '新增失败：日期格式错误：需为 yyyy-MM-dd，当前为 "2024-02-30"',
    '新增失败：入职日期不能是未来日期：2999-01-01',
    '新增失败：日期格式错误：需为 yyyy-MM-dd，当前为 "abc"',
    '当前没有员工记录。'
) -Reject @('新增成功')

# ============ 场景 3：闰年合法日期必须通过（防过度拒绝）============
Invoke-Scenario -Name '闰年 2024-02-29 合法，必须成功' -Lines @(
    '1', 'E001', '张三', '2024-02-29',
    '2', '0'
) -Expect @(
    '新增成功：E001  张三  入职:2024-02-29'
) -Reject @('新增失败')

# ============ 场景 4：排序只在副本上，不破坏插入顺序 ============
Invoke-Scenario -Name '排序不改变默认列表顺序' -Lines @(
    '1', 'E009', '甲', '2024-01-01',
    '1', 'E001', '乙', '2024-02-01',
    '7',    # 按工号排序
    '2',    # 回到默认列表，应仍是 E009 在前
    '0'
) -Expect @(
    # 排序后第一次列表：E001 在前
    "1. E001  乙",
    # 再查默认列表：必须回到插入顺序 E009 在前
    "1. E009  甲"
)

# ============ 场景 5：空数据时两个排序入口 ============
Invoke-Scenario -Name '空数据排序入口给出提示而非异常' -Lines @('7', '8', '0') -Expect @(
    '当前没有员工记录。',
    '当前没有员工记录。'
)

# ============ 场景 6：工号与姓名规则 ============
Invoke-Scenario -Name '工号重复/空值/格式非法/姓名空/姓名过长' -Lines @(
    '1', 'E001', '张三', '',
    '1', 'E001', '王五', '',
    '1', '', '张三', '',
    '1', 'A', '张三', '',
    '1', 'E002', '   ', '',
    '1', 'E003', '这个名字实在是太长了吧超过二十个字符的限制了啊啊啊', '',
    '2', '0'
) -Expect @(
    '新增成功：E001  张三',
    '新增失败：工号已存在："E001" 已被 张三 使用',
    '新增失败：工号不能为空',
    '新增失败：工号格式错误：需为 2—10 位字母或数字',
    '新增失败：姓名不能为空',
    '新增失败：姓名过长',
    '共 1 条'
)

# ============ 场景 7：修改与删除 ============
Invoke-Scenario -Name '修改：回车取消/不存在；删除：确认词错误取消' -Lines @(
    '1', 'E001', '张三', '',
    '4', 'E001', '',
    '4', 'NOPE', 'x',
    '5', 'E001', 'delete',
    '2',
    '5', 'E001', 'DELETE',
    '2',
    '0'
) -Expect @(
    '已取消修改，记录保持不变。',
    '修改失败：工号不存在："NOPE"',
    '已取消删除，记录保持不变。',
    '删除成功：E001  张三',
    '当前没有员工记录。'
) -Reject @('修改成功')

# ============ 场景 8：搜索 ============
Invoke-Scenario -Name '搜索：空关键词/无匹配/大小写不敏感' -Lines @(
    '1', 'E001', 'Alice', '',
    '1', 'E002', 'BOB', '',
    '6', '',
    '6', 'zzz',
    '6', 'alice',
    '6', 'bo',
    '0'
) -Expect @(
    '搜索关键词不能为空。',
    '没有姓名包含 "zzz" 的员工。',
    '匹配 1 条',
    '匹配 1 条'
)

# ============ 场景 9：非法菜单输入与 EOF ============
Invoke-Scenario -Name '非法菜单输入不中断；EOF 干净退出' -Lines @(
    'abc', '', '9', '-1',
    '1', 'E001', '张三', ''
) -Expect @(
    '无效选项："abc"',
    '输入为空，请输入 0-8 之间的数字。',
    '无效选项："9"',
    '无效选项："-1"',
    '新增成功：E001  张三',
    '输入流已结束，程序退出。'
)

# ============ 汇总 ============
Write-Host ''
if ($script:Failures.Count -eq 0) {
    Write-Host "全部 $($script:Passed) 个场景通过。" -ForegroundColor Green
    exit 0
} else {
    Write-Host "通过 $($script:Passed) 个，失败 $($script:Failures.Count) 个：" -ForegroundColor Red
    $script:Failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}