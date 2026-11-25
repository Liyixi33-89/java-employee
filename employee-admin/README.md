# 员工后台管理系统

Java 学习主线（《01-初级后端工程师（0-1.5年）学习路线》）的贯穿项目。

> 远程仓库：https://github.com/Liyixi33-89/java-employee
> ⚠️ **状态：V0.1 功能实现已完成，学习验收未闭环。**
>
> - ✅ 代码实现完成：CRUD + 日期 + 排序 + 搜索，9 个场景断言全部通过
> - ❌ 学习验收待完成：断点走查、知识自测（§3 的 29 道题）、Git 亲手操作、"独立完成搜索功能"的过程需自证
>
> 详见 [docs/V0.1-验收记录.md](docs/V0.1-验收记录.md) 与 [docs/V0.1-知识点清单.md](docs/V0.1-知识点清单.md) §7。

## 学习文档（按阅读顺序）

| 文档 | 作用 | 什么时候读 |
|---|---|---|
| [docs/Java零基础导读.md](docs/Java零基础导读.md) | 单个项目的**纵切**：程序怎么跑起来、一次新增的完整代码路径、语法点的作用 | 第一次接触本项目 |
| [docs/V0.1-知识点清单.md](docs/V0.1-知识点清单.md) | V0.1 阶段的**横切**：69 个知识点（按组：A7/B18/C16/D11/E10/F7），含 29 道自测题与掌握度自评表 | 想确认"这阶段到底该会什么、我会了多少" |
| [docs/V0.1-验收记录.md](docs/V0.1-验收记录.md) | 验收矩阵、断点走查步骤、设计取舍复盘 | 阶段结束时 |

> 每个版本都要产出对应的「知识点清单」，不能只有一个通用导读。

## 环境要求

| 项 | 版本 |
|---|---|
| JDK | 21（Temurin 21.0.x LTS） |
| 构建 | 现阶段 `javac` + `java`，V0.2 引入 Maven |
| Git | 任意近期版本 |

## 目录结构

```
employee-admin/
├── src/
│   ├── Main.java              程序入口，组装依赖
│   ├── ConsoleMenu.java       菜单循环、输入容错、分支分发
│   ├── Employee.java          员工模型（工号与入职日期不可变）
│   ├── EmployeeService.java   业务规则 + 内存存储 + 排序
│   ├── ErrorDrill.java        G1 练习：编译错误 / NPE 堆栈阅读
│   └── BusinessException.java 预期业务错误（区别于程序缺陷）
├── build.ps1                  编译并运行
├── run-samples.ps1            批量跑边界场景
└── README.md
```

## 编译与运行

```powershell
cd employee-admin
powershell -File build.ps1
```

等价的命令行：

```powershell
javac -encoding UTF-8 -d out src\*.java
java -Dstdout.encoding=UTF-8 -cp out Main
```

Windows 控制台若中文乱码，先执行 `chcp 65001`。

## 业务规则（已实现）

| 编号 | 规则 | 实现位置 |
|---|---|---|
| R1 | 工号必填、2—10 位字母或数字、全局唯一 | `EmployeeService.requireEmployeeNo` |
| R1 | 姓名必填、最多 20 字符 | `EmployeeService.requireName` |
| — | 工号创建后不可变 | `Employee.employeeNo` 为 `final` |
| — | 修改/删除先校验格式再校验存在性 | `EmployeeService.requireExisting` |
| R2 | 入职日期必填、`yyyy-MM-dd`、拒绝未来日期 | `EmployeeService.requireHireDate` |
| R3 | 工号与入职日期创建后不可变 | `Employee` 对应字段为 `final` |

## 当前功能

| 菜单 | 功能 | 说明 |
|---|---|---|
| 1 | 新增员工 | 工号 + 姓名，校验通过写入内存 |
| 2 | 员工列表 | 按新增顺序展示；空集合有明确提示 |
| 3 | 查询员工 | 按工号精确查询 |
| 4 | 修改员工 | 先展示当前记录，只改姓名；直接回车取消 |
| 5 | 删除员工 | 先展示待删记录，需输入 `DELETE` 二次确认 |
| 6 | 按姓名搜索 | 姓名关键词模糊匹配，不区分大小写（独立扩展任务） |
| 7 | 按工号排序 | 工号升序显示，不改内部存储顺序 |
| 8 | 按入职日期排序 | 日期降序（新人在前） |
| 0 | 退出 | 或输入流结束（EOF）也会干净退出 |

输入容错：字母、空输入、超范围数字、未知选项、粘贴内容带 BOM 或零宽字符，
均不会崩溃且能继续接受下一次输入。

## 当前不支持（阶段边界）

- **数据只在内存，重启后全部丢失**（V0.2 引入文件持久化）。
- 无部门、无状态、无权限（V0.3 之后逐步引入）。
- 无持久化、无数据库、无自动化测试、无日志。

## 验证方式

```powershell
powershell -File build.ps1        # 编译并交互运行
powershell -File run-samples.ps1  # 批量验证边界场景
```

`run-samples.ps1` 覆盖：完整 CRUD + 搜索流程、工号重复、空工号、空姓名、工号格式非法、
姓名过长、修改时回车取消、删除确认词错误、修改/删除不存在工号、搜索空关键词与大小写、
非法菜单输入。

> 脚本用 `cmd` 重定向文件输入，而不是 PowerShell 管道——PS 5.1 管道会按
> 控制台代码页转码，中文姓名会变成 `??`。

## 下一版本 V0.2

按路线第 5—6 周：引入 Maven 与 JUnit 5、拆出存储接口、加文件持久化。