# Java 零基础导读：怎么读懂这个项目

> 目标：不是背语法，而是能完整讲出「一次新增员工」从键盘输入到数据落地的全过程。

## 本文档的定位（重要）

本文档是**纵切**视角：沿着"用户敲 1 → 数据落库"这一条主线走，只讲这条线经过的东西。
它**不是**知识点全集，会漏掉很多"项目用到但主线没经过"的语法。

**知识点全集**请看 [`V0.1-知识点清单.md`](V0.1-知识点清单.md)：
按路线 §5.1 的六组知识顺序逐条盘点（A JDK/编译 7 项 · B 语法 18 项 · C 对象 16 项 · D 集合 11 项 · E 异常/调试 10 项 · F Git 7 项），
共 69 项，每项标注代码位置与覆盖状态；另有 36 道自测题、§7 掌握度自评表。

⚠️ 清单里的 ✅ 只代表"代码里有实现"，**不代表已经掌握**。掌握度看 §7 自评表。

| 想解决什么 | 读哪份 |
|---|---|
| 这程序怎么跑起来？一次新增走了哪些代码？ | 本文档（纵切） |
| V0.1 到底涉及哪些知识点？我漏了什么？ | `V0.1-知识点清单.md`（横切） |
| 这阶段做完了吗？怎么验收？ | `V0.1-验收记录.md` |

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
| `main` | 名字和签名都是 JVM 规定死的，不能改（改了 JVM 就找不到入口） |
| `String[] args` | 命令行参数的字符串数组，本项目用不到 |

> 把 `main` 想成「启动开关」：JVM 启动时只认这一个方法签名。
>
> 但**一个项目可以有多个 main**——本项目就有两个：`Main`（主程序）和 `ErrorDrill`（报错练习）。
> 运行 `java -cp out ErrorDrill` 时，JVM 只执行 `ErrorDrill.main`，`Main.main` 根本不会被加载。
>
> 所以准确说法是：**每次启动只有一个 main 被调用，但项目可以有多个 main。**

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
⑤ 依次 readLine("工号: ") / readLine("姓名: ") /
   readLine("入职日期 yyyy-MM-dd（直接回车默认今天）: ")
        ↓
⑥ normalize(raw)  ← 清洗输入（去空格、去不可见字符）
        ↓
⑦ 日期为空 → service.create(工号, 姓名)           ← 重载1，默认今天
   日期非空 → service.create(工号, 姓名, 日期文本)  ← 重载3，先解析
        ↓
⑧ requireEmployeeNo() → 校验非空 + 格式
   requireName()       → 校验非空 + 长度
   parseDateText()     → 解析 uuuu-MM-dd（严格模式）
   requireHireDate()   → 校验非空 + 不是未来日期
   然后 store.containsKey() 判断是否重复
        ↓
⑨ new Employee(工号, 姓名, 日期)  →  store.put(工号, 员工)  ← 存进内存的 Map
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

`final` 字段**只能被赋值一次**，但赋值的位置有三种：声明处、初始化块、构造方法。本项目用的是构造方法。

```java
private final String employeeNo;              // 在构造方法里赋值
private static final String LINE = "----";    // 在声明处赋值
```

还要分清两件事：

| 说法 | 对不对 |
|---|---|
| `final` 字段不能再指向别的对象 | ✅ 对 |
| `final` 对象的内容也不能改 | ❌ 错 |

```java
private final List<String> list = new ArrayList<>();
list.add("x");        // ✅ 允许：改的是对象内容
list = new ArrayList<>();  // ❌ 编译错误：改的是引用
```

本项目 `employeeNo` 是 `String`（不可变类），所以"引用不可变"顺带等于"值不可变"——这是 `String` 的特性，不是 `final` 保证的。

所以 `Employee` 创建后工号固定、没有 `setEmployeeNo`，是**故意的设计**。理由见验收记录 §6：工号是外部业务标识，改了历史记录就对不上。

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
    case ""  -> out.println("输入为空，请输入 0-8 之间的数字。");
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
- `Exception`（受检）：编译器要求你**必须做点什么**——要么 `try/catch` 处理，要么在方法签名上 `throws` 声明"往上层抛"。两种都算处理，不是只有 catch。适合「外部环境意外」，比如文件读不到。

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

### 5.11 `while` 循环：菜单为什么能一直转

整个程序能反复操作，全靠 `ConsoleMenu.run()` 里这一个循环：

```java
while (running) {
    printMenu();                        // 打印菜单
    String input = readLine("请选择操作: ");
    if (input == null) break;           // EOF 退出
    dispatch(normalize(input));         // 分发处理
}
out.println("已退出，再见。");
```

`while` 的规则：**括号里是 `boolean`，为 `true` 就重复执行循环体，为 `false` 就往下走。**

这个循环能停，靠两处：

| 退出方式 | 触发 | 代码 |
|---|---|---|
| 用户选 0 | `dispatch("0")` 把 `running` 改成 `false` | `case "0" -> running = false;` |
| 输入流结束 | `readLine` 返回 `null` | `if (input == null) break;` |

**为什么 `running` 是成员变量而不是局部变量？**

```java
private boolean running = true;      // 成员变量：dispatch() 能改到
```

如果写成 `run()` 里的局部变量，`dispatch()` 是另一个方法，**看不到也改不了它**——循环永远停不下来。这是"为什么需要成员变量"最直接的一个例子。

**`while` vs `for` 怎么选**：

| 场景 | 用哪个 | 理由 |
|---|---|---|
| 次数已知（遍历数组） | `for` | 循环变量（下标）写在头部，一目了然 |
| 次数未知、靠条件停 | `while` | 停止条件就是业务条件本身 |
| 至少执行一次 | `do-while` | 菜单其实更适合 `do-while`，本项目用 `while` 是为了显式判断 EOF |

> **自己改一次**：把 `while (running)` 改成 `while (true)`，再把 `case "0"` 改成 `running = false; return;`。
> 猜猜会怎样？（答案：也能跑，但 `while (true)` 隐藏了退出条件，读代码的人必须翻遍整个方法才知道怎么退出——这是可读性倒退。）

### 5.12 方法：定义、参数、返回值、`void`

```java
//  private  static  void      printEmployees  (List<Employee> employees, String emptyHint)
//    ①       ②     ③            ④              ⑤                                 ⑤
```

| 位置 | 名称 | 含义 |
|---|---|---|
| ① | 访问修饰符 | `private` 只有本类能调；`public` 外部能调 |
| ② | `static` | 有 → 属于类；无 → 属于对象（见 5.13） |
| ③ | 返回类型 | `void` 表示不返回；否则必须 `return` 一个该类型的值 |
| ④ | 方法名 | 驼峰命名，动词开头 |
| ⑤ | 参数列表 | 可以没有，可以多个，用逗号分隔 |

**`return` 的两个作用**（容易只记住第一个）：

```java
private String requireText(String value, String fieldName) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.isEmpty()) {
        throw new BusinessException(fieldName + "不能为空");   // 抛异常也是"提前结束"
    }
    return trimmed;                                          // ① 结束方法 ② 带回一个值
}
```

**`void` 方法里的 `return;`** 只表示"提前结束"，不带值：

```java
private void handleCreate() {
    String employeeNo = readLine("工号: ");
    if (employeeNo == null) {
        return;              // 输入结束了，直接退出方法，不往下走
    }
    // ...
}
```

> **不看代码回答**：`handleCreate()` 里为什么 `employeeNo == null` 要 `return`？
> （答案：`null` 表示输入流已结束，继续读姓名会读到 `null` 或者死循环，所以要提前退出。）

**参数是"按值传递"**（这是最重要的一个概念，`BasicsDrill.passByValue()` 有完整实验）：

```java
int num = 10;
changeInt(num);          // 传的是 10 这个值的副本
// num 仍然是 10，因为方法里改的是副本

StringBuilder sb = new StringBuilder("原始");
changeContent(sb);       // 传的是"地址"的副本，但地址指向同一个对象
// sb 变成"原始被修改"，因为通过地址改到了对象内容
```

一句话：**基本类型传值，对象传地址的副本。所以「改内容」外部能看到，「改指向」外部看不到。**

### 5.13 `static` 方法 vs 实例方法

```java
// 实例方法：需要 this，能访问实例字段
String describe() {
    return "边长 " + this.side;
}

// 静态方法：没有 this，只能访问参数和静态字段
static int area(int side) {
    return side * side;
}
```

| | 静态方法 | 实例方法 |
|---|---|---|
| 调用方式 | `Square.area(3)` | `sq.describe()` |
| 需要 `new` 吗 | 不需要 | 需要 |
| 能用 `this` 吗 | **不能** | 能 |
| 能读实例字段吗 | **不能** | 能 |
| 典型例子 | `Math.abs()`、`Integer.parseInt()`、`main()` | `employee.getName()` |

**为什么 `main` 必须是 `static`？**

JVM 启动时**还没有任何对象**。它要做的第一件事就是调用 `main`，
此时没有 `Main` 的实例，所以只能用 `Main.main(...)` 这种"属于类"的调用方式 →
`main` 必须是 `static`。

**同理，为什么 `ConsoleMenu.printMenu()` 不是 `static`？**

因为它要访问 `this.out`（每个菜单对象自己的输出流）。如果写成 `static`，
编译器会报「无法从静态上下文引用非静态变量 out」。

> **判断口诀**：方法里用到了 `this.xxx` 或实例字段 → 不能 `static`；
> 只依赖参数 → 可以 `static`（且通常应该加，因为更容易测）。

### 5.14 `print` / `println` / `printf` / `flush`

```java
out.print("请选择操作: ");                    // 不换行
out.println("=== 员工后台管理系统 ===");       // 换行
out.printf("%d. %s%n", index++, employee);    // 格式化 + 换行
out.flush();                                  // 把缓冲区推出去
```

**`print` vs `println`**：区别只有一个——末尾加不加换行符。
菜单提示「请选择操作: 」用 `print`，因为光标要停在同一行等用户输入。

**`printf` 的占位符**：

| 占位符 | 用于 | 例子 |
|---|---|---|
| `%d` | 整数 | `%d` → `1` |
| `%s` | 任何对象（自动调 `toString()`） | `%s` → `E001  张三  入职:2024-03-01` |
| `%f` | 浮点 | `%.2f` → `3.14` |
| `%n` | 平台相关的换行符 | 等价于 `println` 的那个换行 |

`printf` **不自动换行**，所以本项目每处都写了 `%n`。忘了加 → 所有记录连成一行。

**`flush()` 到底解决什么？**

先说清楚 `System.out` 的性质：它是 `PrintStream`，**自动刷新的**。
它的 `println` 会自动 flush，`print` 则在遇到换行符或缓冲区满时刷新。

所以在本项目的实际场景里（输出到控制台、有大量 `println`），
**去掉 `out.flush()` 后行为完全一样**——我实际做过这个实验：
临时删掉 `flush()` 重新编译，`run-samples.ps1` 的 9 个场景全部照旧 PASS。

那为什么还留着？因为它防的是另一种情况：

| 场景 | 不加 `flush()` 的后果 |
|---|---|
| 输出重定向到文件、且用 `print` 且长时间不换行 | 内容可能滞留在缓冲区，进程被强杀时丢失 |
| 输出流被包装成不带自动刷新的缓冲流 | 提示可能不显示 |
| 期望"打印即生效"的调试场景 | 日志顺序错乱 |

本项目里它是**防御性写法**，不是必需。诚实的说法是：

> `flush()` 在这里的作用是「显式保证提示先于输入显示」，
> 在控制台场景下 `PrintStream` 已自动处理，删掉不影响结果。
> 真正需要它的是重定向输出、或用 `BufferedWriter` 这类需要手动刷新的流。

（顺带一句：`printf` 不会自动刷新，这也是某些场景下加 `flush()` 的理由。）

> **自己验一次**：把 `readLine()` 里的 `out.flush();` 删掉，跑 `run-samples.ps1`，
> 你会发现 9 个场景仍然全过。**这个"实验没有变化"的结论本身比"它很重要"更有价值**——
> 说明不能凭直觉判断某个 API 的作用，要实测。（改完记得加回来。）

### 5.15 重写 `toString`：为什么 `println(employee)` 能直接打印

```java
Employee employee = new Employee("E001", "张三", LocalDate.now());
System.out.println(employee);
// 输出：E001  张三  入职:2026-11-26
```

你没写 `.getName()`，它怎么知道打什么？因为 `println` 内部对参数调用了 `toString()`。

**如果没重写 `toString`，输出是什么？**

```
Employee@1b6d3586
```

`类名@十六进制哈希码`——完全看不出内容。这就是为什么必须重写。

**重写的本质**：`toString()` 定义在 `Object` 里（所有类的老祖宗），
默认实现就是打印那个哈希码。重写 = 用你自己的版本覆盖它。

```java
@Override
public String toString() {
    return employeeNo + "  " + name + "  入职:" + hireDate.format(DATE_FORMAT);
}
```

**这也顺带解释了字符串拼接**（知识点 `B12`）：

```java
out.println("新增成功：" + created);
```

`"新增成功：" + created` 里，`created` 不是 `String`，Java 会自动调用它的 `toString()`
拼到后面。**所以「对象能直接拼字符串」和「println 能直接打印对象」是同一个机制。**

**`@Override` 为什么必须有**（知识点 `C10`）：

```java
@Override
public String toString() { ... }        // 拼错成 toSting() → 编译报错 ✅

public String toSting() { ... }         // 没有 @Override → 编译通过，但方法永远不被调用 ❌
```

`@Override` 让编译器帮你核对"这个方法真的覆盖了父类的同名方法吗"。不加的话，
拼错方法名编译器不会报错，你会得到一个**永远不会被调用的死方法**，然后花几小时找为什么打印不对。

> **这是最值得养成习惯的一个注解**：凡是覆盖父类方法，一律加 `@Override`。

`equals` 和 `hashCode` 同理，本项目 `Employee` 里三处都加了：

```java
@Override public boolean equals(Object o) { ... }
@Override public int hashCode() { ... }
@Override public String toString() { ... }
```

### 5.16 三元表达式：一行写完 if-else

```java
String trimmed = value == null ? "" : value.trim();
//                              ↑条件   ↑真   ↑假
```

等价于：

```java
String trimmed;
if (value == null) {
    trimmed = "";
} else {
    trimmed = value.trim();
}
```

**读法**：`条件 ? 值为真时 : 值为假时`。问号前是条件，冒号分隔两个结果。

**什么时候用**：只在「二选一、且两边都只是一表达式」时用。本项目 `requireText` 和
`searchByName` 两处都用它处理 `null`：

```java
String trimmed = keyword == null ? "" : keyword.trim();
```

**什么时候别用**：嵌套三元。下面这种可读性极差，不如写 `if`：

```java
// ❌ 别这么写
String level = score > 90 ? "A" : score > 80 ? "B" : score > 60 ? "C" : "D";
```

**为什么这里要特判 `null`？**

```java
value.trim()      // value 是 null → NullPointerException
```

`null` 上不能调任何方法。`null == x` 只能靠 `==` 判断，不能 `value.equals(null)`
（因为 `value` 本身就是 `null`，调 `equals` 会先崩）。

> **注意顺序**：`value == null ? "" : value.trim()` 里，如果 `value` 为 `null`，
> 冒号后的 `value.trim()` **不会执行**——三元表达式也是"短路"的。

### 5.17 `printf` 格式化输出

```java
out.printf("%d. %s%n", index++, employee);
```

| 部分 | 含义 |
|---|---|
| `"%d. %s%n"` | 格式模板 |
| `index++` | 第一个参数，填 `%d` |
| `employee` | 第二个参数，填 `%s` |
| `%n` | 换行（平台相关） |

**参数按顺序填坑**，不是按名字。所以占位符个数必须和参数个数一致，否则运行时抛
`MissingFormatArgumentException`。

| 占位符 | 类型 | 示例输出 |
|---|---|---|
| `%d` | 整数（`int`/`long`） | `1` |
| `%s` | 任意对象（自动 `toString()`） | `E001  张三  入职:2024-03-01` |
| `%f` | 浮点，`%.2f` 保留两位 | `3.14` |
| `%n` | 换行 | — |
| `%%` | 一个百分号 | `%` |

**`printf` 不自动换行**。本项目每处都写了 `%n`，忘了加 → 所有记录连成一行。

**为什么不直接拼字符串？**

```java
out.println(index++ + ". " + employee);                    // 可以
out.printf("%d. %s%n", index++, employee);                 // 更清楚
```

差异在多列对齐时体现：`printf` 支持 `%-10s`（左对齐补空格到 10 位）这类格式控制，
拼字符串做不到。本项目的 `printEmployees` 选 `printf`，就是为了以后加列对齐时不用重写。

### 5.18 `ArrayList` 与 `List` 接口：左边为什么写接口

```java
public List<Employee> findAll() {
    return new ArrayList<>(store.values());     // 左边 List，右边 ArrayList
}
```

**`List` 是接口，`ArrayList` 是实现。** 这一行同时有两个信息：

| 位置 | 写了什么 | 含义 |
|---|---|---|
| 返回类型 | `List` | 「我给你一个**有序集合**」，不承诺底层是数组还是链表 |
| 实际对象 | `ArrayList` | 「内部我用数组实现」，调用方不该关心 |

**为什么返回类型不直接写 `ArrayList`？**

```java
// 假设写成 ArrayList
public ArrayList<Employee> findAll() { ... }
```

后果：调用方代码里可能会写 `ArrayList<Employee> list = service.findAll();`。
哪天内部改用 `LinkedList`（比如频繁中间插入），返回类型一改，
**所有调用方全部编译失败**。写接口类型就不会——`LinkedList` 也是 `List`。

这就是「**面向接口编程**」最朴素的一次应用。V0.2 抽 `EmployeeRepository` 是同一个道理，
只是从「集合接口」升级到「自己的业务接口」。

**`new ArrayList<>(store.values())` 的构造参数是什么？**

`ArrayList` 有个构造方法接收任何 `Collection`，把它里面的元素复制进来：

```java
new ArrayList<>()               // 空列表
new ArrayList<>(store.values()) // 把 Map 的所有值复制进来
new ArrayList<>(anotherList)    // 复制另一个 List
```

**为什么必须复制**（知识点 `D10` 防御性拷贝）：

```java
// 如果不复制，直接暴露内部集合
public List<Employee> findAll() {
    return (List<Employee>) store.values();   // ❌
}
```

调用方一句 `list.clear()` 就把**内部数据清空了**。`new ArrayList<>(...)` 产生新列表，
外部怎么改都不影响 `store`。

> **但要注意这是浅拷贝**：复制的是"引用的列表"，元素还是同一个 `Employee` 对象。
> 调用方 `employee.setName("新名")` 仍然会改到存储里的对象。
> 真要隔离，得连对象一起复制（深拷贝）——本阶段不需要，`updateName` 正是靠对象引用工作的。

### 5.19 两种查找：`findByNo` vs `searchByName`

项目里有两个"找员工"的方法，用途完全不同：

| | `findByNo` | `searchByName` |
|---|---|---|
| 匹配方式 | 精确（工号完全相等） | 模糊（姓名包含关键词） |
| 返回 | `Optional<Employee>` 单条 | `List<Employee>` 多条 |
| 底层 | `store.get(key)` 直接定位 | 遍历全部逐个比较 |
| 复杂度 | O(1) | O(n) |
| 菜单 | 3 查询员工 | 6 按姓名搜索 |

**为什么 `findByNo` 用 `Map.get` 而不是遍历？**

因为 `Map` 的键就是工号，`get` 走哈希，一步到位。遍历 10000 条记录找一条，
和直接 `get` 一次，差距是数量级的。

**这就是"选对数据结构"的意义**：同样叫"查找"，放进 `Map` 当键，和放在 `List` 里遍历，
代码看起来差不多，性能差很多。

**为什么 `searchByName` 只能遍历？**

因为 `Map` 的键是工号，不是姓名。名字不是唯一键（可能有重名），无法当 `Map` 的键。
要按姓名查，只能扫一遍。

> **如果姓名搜索变成高频操作怎么办？**
> 常见做法是再建一个 `Map<String, List<Employee>>` 作索引（姓名 → 同名的人）。
> 这是空间换时间，V0.3 引入数据库后由索引解决，现在不必过度设计。
> 但要**知道有这个问题**——这是"为什么需要索引"的起点。

**为什么返回类型不同？**

- `findByNo` 最多一条 → `Optional<Employee>`（可能没有）
- `searchByName` 可能 0 条、1 条、多条 → `List<Employee>`（空列表表示没有）

`Optional` 表达"单个可能不存在"，`List` 表达"集合可能为空"——**类型本身就说明了语义**。

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