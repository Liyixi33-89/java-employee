# Java 零基础导读：怎么读懂这个项目

> 目标：不是背语法，而是能完整讲出「一次新增员工」从键盘输入到数据落地的全过程。

## 1. 先建立最小直觉：Java 程序是什么

你写的 `.java` 文件是**纯文本**，JVM 不认识。要两步：

```
src/Main.java  ──javac 编译──>  out/Main.class  ──java 运行──>  JVM 执行
   （人写的）                      （字节码）                  （真正跑起来）
```

`build.ps1` 就是这两行：

```powershell
javac -encoding UTF-8 -d out src\*.java   # 编译：把 src 下所有 .java 翻成 out 下的 .class
java  -Dstdout.encoding=UTF-8 -cp out Main  # 运行：告诉 JVM 去 out 目录找 Main 类
```

- `-encoding UTF-8`：源码里有中文，不加会乱码甚至编译失败。
- `-d out`：编译产物放哪。
- `-cp out`：classpath，告诉 JVM「去哪个目录找类」。
- `Main`：要启动的类名（**不带 .class 后缀**）。

**关键点**：`out/` 里的东西全是自动生成的，不要手改；改 `src/` 后必须重新编译。

## 2. 程序的入口：只有这一个地方是"起点"

打开 `Main.java`，全部内容就 4 行有效代码：

```java
public static void main(String[] args) {
    Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
    EmployeeService service = new EmployeeService();
    ConsoleMenu menu = new ConsoleMenu(scanner, System.out, service);
    menu.run();
}
```

JVM 启动后**只做一件事**：找到 `Main` 类里签名长这样的方法，从第一行开始执行。

```java
public static void main(String[] args)
```

拆开看：

| 部分 | 含义 |
|---|---|
| `public` | 公开的，JVM 才能调用到 |
| `static` | 静态的，不需要 `new Main()` 就能调用 |
| `void` | 不返回任何值 |
| `main` | 名字是规定死的，不能改 |
| `String[] args` | 命令行参数的字符串数组，本项目用不到 |

> 你可以把 `main` 想成「总开关」。整个程序只有一个总开关。

三行 `new` 分别是什么？

```java
Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);  // 造一个「读键盘」的工具
EmployeeService service = new EmployeeService();                   // 造一个「管数据」的对象
ConsoleMenu menu = new ConsoleMenu(scanner, System.out, service);  // 造一个「管菜单」的对象
```

- `new Xxx(...)` = **创建一个对象**。
- `System.in` = 键盘输入流；`System.out` = 屏幕输出流。
- 后面两个对象需要前者，所以通过构造参数**传进去**（这叫依赖注入，后面 2.6 讲为什么）。

## 3. 追一条主线：用户敲 `1` 新增一名员工

这是理解整个项目的**唯一必要路径**。建议你对照代码走一遍：

```
① 用户在菜单敲 1
        ↓
② ConsoleMenu.run() 里的 readLine("请选择操作: ") 读到 "1"
        ↓
③ dispatch("1")   ← switch 判断是哪个功能
        ↓
④ handleCreate()  ← 处理"新增"
        ↓
⑤ 依次 readLine("工号: ") / readLine("姓名: ")
        ↓
⑥ normalize(raw)  ← 清洗输入（去空格、去不可见字符）
        ↓
⑦ service.create(工号, 姓名)
        ↓
⑧ requireEmployeeNo() → 校验非空 + 格式 + 是否重复
   requireName()       → 校验非空 + 长度
        ↓
⑨ new Employee(工号, 姓名)  →  store.put(工号, 员工)  ← 存进内存的 Map
        ↓
⑩ 回到 run() 的 while 循环开头，重新打印菜单
```

退出时：读到 `"0"` → `running = false` → `while` 条件不成立 → 打印「已退出，再见。」

> `while (running) { ... }` 就是「只要 running 还是 true 就一直转圈」。这是菜单能反复操作的原理。

## 4. 五个文件为什么不能写成一个大文件

零基础最常见的想法：功能都能跑，为什么要拆？

| 文件 | 只负责一件事 | 如果合并会怎样 |
|---|---|---|
| `Main` | 组装零件、点火启动 | 启动逻辑和业务混在一起，无处下手 |
| `ConsoleMenu` | 和用户对话（读输入、打印） | 换 Web 接口时要重写业务代码 |
| `EmployeeService` | 业务规则（能不能新增、能不能删） | 规则散落在 print 语句之间，测不了 |
| `Employee` | 数据长什么样 | 到处传两个裸字符串，容易传错顺序 |
| `BusinessException` | 「用户输入不合法」这类预期错误 | 和程序 bug 混为一谈，无法区分对待 |

这个分法叫**按职责分层**。好处在下一版直接体现：

- V0.2 加文件存储 → 只动 `EmployeeService` 的存储部分，菜单一行不改。
- V0.4 换成 HTTP 接口 → `ConsoleMenu` 整个换掉，`EmployeeService` 一行不改。

**这就是为什么现在要拆，而不是以后重构。**

## 5. 逐个拆解你可能不熟的语法

### 5.1 类、对象、构造方法

```java
public class Employee {          // 类 = 图纸
    private final String employeeNo;   // 字段：这个对象有哪些数据
    private String name;

    public Employee(String employeeNo, String name) {   // 构造方法：new 的时候执行
        this.employeeNo = employeeNo;
        this.name = name;
    }
}
```

- **类**是图纸，**对象**是按图纸造出来的实物：`new Employee("E001", "张三")`。
- **构造方法**名字必须和类名一样，没有返回值类型（连 `void` 都不写）。
- `this.employeeNo = employeeNo`：左边是对象的字段，右边是参数。重名时必须用 `this` 区分。
- `private` 表示只有这个类内部能碰；外部要用 `getName()` 这样的**方法**访问。

### 5.2 `final` 是什么意思

```java
private final String employeeNo;   // 赋值一次后不能再改
private String name;               // 可以改
```

`final` 字段只能在**构造方法里赋一次值**。所以 `Employee` 创建后工号就固定了，没有任何 `setEmployeeNo` 方法——这不是忘了写，是**故意的设计**。理由见验收记录 §6：工号是外部业务标识，改了历史记录就对不上了。

### 5.3 `Map` 为什么能保证工号唯一

```java
private final Map<String, Employee> store = new LinkedHashMap<>();
```

`Map` 是「键 → 值」的字典。这里键是工号，值是员工对象。

| 操作 | 含义 |
|---|---|
| `store.put("E001", emp)` | 放入 / 覆盖 |
| `store.get("E001")` | 取值，没有返回 `null` |
| `store.containsKey("E001")` | 判断键在不在 |
| `store.remove("E001")` | 删除 |
| `store.values()` | 所有值的集合 |

`Map` 的键天然不能重复——`put` 同一个键会**覆盖**而不是新增。但我们仍然显式写了：

```java
if (store.containsKey(no)) {
    throw new BusinessException("工号已存在 ...");
}
```

为什么多此一举？因为不写的话，重复新增会**静默覆盖**前一个员工，用户看到「新增成功」但数据其实被顶掉了。这是最阴险的一类 bug：不报错、数据错了。

> 为什么用 `LinkedHashMap` 而不是 `HashMap`：前者保持插入顺序，列表按新增次序展示。`HashMap` 顺序是乱的。

### 5.4 泛型 `<String, Employee>` 是什么

`Map<String, Employee>` 里的尖括号就是告诉编译器：**键只能是 String，值只能是 Employee**。

好处：`store.get("E001")` 直接返回 `Employee`，不用强转；存错类型编译期就报错。零基础阶段理解成「给容器贴标签」就够。

### 5.5 `switch` 的新写法

```java
switch (choice) {
    case "1" -> handleCreate();
    case "5" -> handleDelete();
    case "0" -> running = false;
    case ""  -> out.println("输入为空，请输入 0-6 之间的数字。");
    default  -> out.println("无效选项：\"" + choice + "\"");
}
```

`case "1" ->` 是 Java 14+ 的**箭头写法**，比老的 `case "1": break;` 少写 `break`（不会漏写导致穿透）。JDK 21 完全支持。

`switch` 的 `default` 相当于 `if/else` 里的 `else`——所有 case 都没匹配上时走这里。**这是"未知选项不崩溃"的实现位置。**

### 5.6 异常：为什么要有 `BusinessException`

```java
public class BusinessException extends RuntimeException {   // 自定义异常
    public BusinessException(String message) {
        super(message);
    }
}
```

抛出和使用：

```java
// 服务层：发现问题就"扔出去"
if (trimmed.isEmpty()) {
    throw new BusinessException("工号不能为空");
}

// 菜单层：接住它，转成给用户看的一句话
try {
    Employee created = service.create(no, name);
    out.println("新增成功：" + created);
} catch (BusinessException e) {
    out.println("新增失败：" + e.getMessage());
}
```

三个关键概念：

| 概念 | 白话 |
|---|---|
| `throw new` | 出问题了，扔一个信号出去，本方法**立刻中断** |
| `throws`（不是 `throw`） | 方法签名上的**声明**：「我可能扔这类异常，调用方自己处理」 |
| `try / catch` | 接住信号，程序继续往下走，不崩 |

**为什么不用返回值表示错误？**

比如 `create` 返回 `true/false`，那错误原因（"重复"还是"为空"）就没法带回来；如果返回字符串，调用方还得判断是不是错误。异常把「正常流程」和「出错流程」分开，代码主线不被错误处理淹没。

**为什么 `extends RuntimeException` 而不是 `Exception`？**

- `RuntimeException`（非受检）：不强制 `try/catch`，调用方想处理就处理。业务规则错误属于这一类。
- `Exception`（受检）：编译器强制你必须处理，适合「必须处理的意外」，比如文件读不到。

### 5.7 `Optional` 是什么

```java
public Optional<Employee> findByNo(String employeeNo) {
    if (employeeNo == null) {
        return Optional.empty();          // 明确的"没有"
    }
    return Optional.ofNullable(store.get(employeeNo.trim()));
}

// 调用方
Optional<Employee> found = service.findByNo(no);
if (found.isPresent()) {
    out.println("查询结果：" + found.get());
} else {
    out.println("未找到 ...");
}
```

它解决的问题：**`null` 是隐形的**。`findByNo` 如果直接返回 `Employee`，返回 `null` 时调用方很容易忘记判空 → `NullPointerException`。

`Optional<T>` 在**类型上**就写着「我可能没有值」，逼调用方显式处理。

- `Optional.empty()` = 没有
- `Optional.ofNullable(x)` = 有就装，`null` 就空
- `.isPresent()` / `.isEmpty()` = 有没有
- `.get()` = 取出来（**没值时调用会抛异常**，所以必须先判）

### 5.8 正则 `matches`

```java
private static final String EMPLOYEE_NO_PATTERN = "[A-Za-z0-9]{2,10}";
...
if (!no.matches(EMPLOYEE_NO_PATTERN)) {
    throw new BusinessException("工号格式错误 ...");
}
```

| 片段 | 含义 |
|---|---|
| `[A-Za-z0-9]` | 允许的字符：大小写字母或数字 |
| `{2,10}` | 重复 2 到 10 次 |
| `matches` | **整个字符串**完全符合才算 `true`（不是"包含"） |

所以 `E001` 通过，`A`（只有 1 位）、`E001-`（含 `-`）、`` 空串都不通过。

### 5.9 `String` 的几个坑

```java
String trimmed = value == null ? "" : value.trim();   // 三元表达式：条件 ? 真 : 假
```

- `== null` 判断"是不是没给"；`"".equals(x)` 判断"是不是空串"。**不要用 `==` 比较字符串内容**（`==` 比的是引用地址）。
- `.trim()` 去掉首尾空白（空格、Tab）。用户输入 `"  张三  "` 必须清洗，否则 `"张三" != " 张三"` 会让查重失效。
- `.isEmpty()` / `.length()` 判断空与长度。
- `.toLowerCase(Locale.ROOT)` 小写化。为什么要 `Locale.ROOT`：土耳其语环境下 `"I".toLowerCase()` 会变成 `"ı"`（无点），导致 `Alice` 搜不到。这是**真实存在的国际坑**。

### 5.10 接口与实现（V0.2 会用到）

现在 `EmployeeService` 直接持有 `Map`。下一版要换成文件存储时，正确做法是抽出接口：

```java
public interface EmployeeRepository {          // 只定契约，不写实现
    void save(Employee e);
    Optional<Employee> findByNo(String no);
    List<Employee> findAll();
}

public class InMemoryRepository implements EmployeeRepository { ... }   // 内存版
public class FileRepository implements EmployeeRepository { ... }       // 文件版
```

`implements` 表示"我承诺实现这个接口的全部方法"。

服务层只依赖 `EmployeeRepository` 接口，运行时传入哪个实现就用哪个——**换存储不用改业务代码**。这就是路线 §5.2 的验收项「业务层可以切换内存/文件存储，不重写菜单与全部规则」。

## 6. 现在就该动手做的三件事

### 6.1 亲手跑一遍，并故意弄错

```powershell
cd employee-admin
powershell -File build.ps1
```

然后按顺序输入，每步**预期**如下：

| 你输入 | 预期输出 | 观察什么 |
|---|---|---|
| `abc` | `无效选项："abc"` | 程序没崩，菜单重新出现 |
| 直接回车 | `输入为空` | 空输入被单独识别 |
| `1` → `E001` → `张三` | `新增成功：E001  张三` | 数据从输入变成对象 |
| `1` → `E001` → `李四` | `新增失败：工号已存在` | 唯一性校验生效 |
| `1` → `A` → `王五` | `工号格式错误` | 正则在起作用 |
| `2` | `共 1 条：1. E001  张三` | 注意李四**没有**进去 |
| `6` → `张` | `匹配 1 条` | 模糊搜索 |
| `5` → `E001` → `abc` | `已取消删除` | 确认词不是 DELETE 就不删 |
| `0` | `已退出，再见。` | 循环靠 running 退出 |

### 6.2 用断点看「一次新增」的真实数据流

这是路线 §5.1 验收里唯一需要手工完成的一项，也是最有效的学习方式。

1. 用 IDEA 打开 `employee-admin` 目录（`File → Open`，选 `employee-admin` 文件夹）。
2. 在 `ConsoleMenu.java` 第 78 行 `Employee created = service.create(...)` 左侧点一下，出现红点 = 断点。
3. 右键 `Main.java` → `Debug 'Main.main()'`。
4. 控制台输入 `1`、`E001`、`张三`，程序会在断点处**停住**。
5. 底部 `Variables` 面板看 `employeeNo`、`name` 的值——这时候它们还是原始输入。
6. 按 `F7`（Step Into）进入 `create`，再看 `requireEmployeeNo` 里的 `no`。
7. 继续 `F7` 步入 `requireText`，看 `trimmed`。

**每一步都要能说出"这个变量的值是什么、为什么会走这条分支"**。说不出来的地方就是你的知识缺口。

### 6.3 改一处，看变化

选一个改（改完重新编译运行）：

- 把 `NAME_MAX_LENGTH` 从 `20` 改成 `2`，再新增一个三字姓名 → 观察报错。
- 把 `EMPLOYEE_NO_PATTERN` 改成 `[A-Za-z0-9]{1,10}`，再试 `A` → 这次应该成功。**改完记得改回去。**
- 把 `printMenu` 里的 `" 6. 按姓名搜索"` 删掉，但保留 `case "6"` → 猜猜会发生什么？（答案：菜单看不到但输入 6 仍能用。这说明**菜单显示和功能接线是两件事**，是很多 bug 的来源。）

## 7. 这份代码里"故意没做"的东西

看懂"为什么不做"比看懂"怎么写"更重要：

| 没做的 | 为什么 | 什么时候做 |
|---|---|---|
| Maven / JUnit | 先把语言本身跑通，不想让构建工具盖过语法 | V0.2 |
| 文件 / 数据库 | 内存版能验证业务规则，存储是独立问题 | V0.2 / V0.3 |
| 并发控制 | 单进程单用户，没有真实竞态 | V1.1 |
| 部门 / 状态 | 一次只加一条规则，否则一次要改太多 | V0.3 |
| 日志框架 | `System.out` 足够看结果 | V0.4 起 |
| 部门 / 权限 | 需要先有 Web 层才有意义 | V0.5 |
| 分页 / 索引 | 内存里 `List` 遍历就是全部，没有性能问题 | V0.3 |

## 8. 学习节奏建议（对照路线 §7.1）

每周 10 小时的分配，用在当前这个项目上：

| 时段 | 做什么 |
|---|---|
| 2 小时 | 读本文档 + JDK 官方文档对应章节，写小实验 |
| 5 小时 | 实现当周功能（不看逐行教程） |
| 2 小时 | 故意弄错、看报错、用断点定位 |
| 1 小时 | 写复盘：今天卡在哪、根因是什么、怎么解决 |

**三条硬规则**（来自路线 §1.3）：

1. 遇到报错，先读**第一行**（真正的错误），不要直接搜最后一行堆栈。
2. 不看逐行教程写一遍——写完再对照，有差异才学得进去。
3. 不要靠吞异常、关校验让程序"看起来跑通"。宁可报错，也不要静默错。

## 9. 常见报错对照表

| 报错 | 含义 | 怎么改 |
|---|---|---|
| `找不到或无法加载主类` | JVM 找不到要启动的类 | 确认 `-cp out` 路径对、先编译过 |
| `错误: 需要 class, interface 或 enum` | 大括号不匹配 / 代码写在类外面 | 检查 `{}` 配对 |
| `错误: 找不到符号` | 拼错名字 / 少了 import / 方法参数不对 | 看报错行号，检查拼写和 import |
| `NullPointerException` | 对 `null` 调方法 | 加判空，或用 `Optional` |
| `ClassCastException` | 类型强转错了 | 检查泛型标签是否用对 |
| `java.lang.NumberFormatException` | 字符串转数字失败 | 先校验格式再转 |
| 中文乱码 | 编码不一致 | `-encoding UTF-8` 编译、`-Dstdout.encoding=UTF-8` 运行、终端 `chcp 65001` |
| `NoSuchElementException` | `Scanner` 没数据了还在读 | 本项目用 `hasNextLine()` 已规避；自己写时要判断 |

## 10. 一句话总结

> 这个项目现在做的，是用**最简单的方式**把「输入 → 校验 → 存储 → 输出」这条完整链路跑通。
> 后面加数据库、加框架，都只是把这四步里的某一步换成更专业的实现，链路本身不变。
> 所以现在把这条链路讲清楚，比记住任何语法都重要。