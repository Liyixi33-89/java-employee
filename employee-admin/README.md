# 员工后台管理系统

Java 学习主线（《01-初级后端工程师（0-1.5年）学习路线》）的贯穿项目。
当前处于 **V0.1 第二周**：员工对象、集合管理、新增/列表/查询。

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
│   ├── Employee.java          员工模型（工号不可变，作唯一标识）
│   ├── EmployeeService.java   业务规则 + 内存存储
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
| R1 | 工号必填、2—10 位字母或数字、全局唯一 | `EmployeeService.create` |
| R1 | 姓名必填、最多 20 字符 | `EmployeeService.create` |
| — | 工号创建后不可变 | `Employee.employeeNo` 为 `final` |

## 当前功能

- 1 新增员工：输入工号 + 姓名，校验后写入内存集合。
- 2 员工列表：按新增顺序列出全部记录；无记录时给出提示。
- 3 查询员工：按工号精确查询，未命中给出明确提示。
- 输入容错：字母、空输入、超范围数字、未知选项均提示后返回菜单。

## 当前不支持（阶段边界）

- 4 修改、5 删除为占位提示，第三周实现。
- **数据只在内存，重启后全部丢失**。
- 无持久化、无数据库、无自动化测试、无日志。

## 验证方式

```powershell
powershell -File build.ps1        # 编译并交互运行
powershell -File run-samples.ps1  # 批量验证边界场景
```

`run-samples.ps1` 覆盖：新增成功、工号重复、空工号、空姓名、工号格式非法、
查询不存在、空列表、非法菜单输入。

> 脚本用 `cmd` 重定向文件输入，而不是 PowerShell 管道——PS 5.1 管道会按
> 控制台代码页转码，中文姓名会变成 `??`。

## 下一周

修改、删除；修改时保持唯一性与格式规则有效；更完整的边界处理与调试复盘。