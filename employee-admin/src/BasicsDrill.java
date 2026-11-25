/**
 * 基础语法补漏实验：项目里用到但没展开、或压根没用到的基础。
 *
 * <p>不需要接入主程序，单独运行即可：
 * <pre>
 * javac -encoding UTF-8 -d out src\BasicsDrill.java
 * java -cp out BasicsDrill
 * </pre>
 *
 * <p>每个方法对应知识点清单里的一个缺口，看输出 + 读注释 = 完成一次学习。
 */
public class BasicsDrill {

    public static void main(String[] args) {
        section("1. 基本类型与取值范围");
        basicTypes();

        section("2. 数值转换与整数除法陷阱");
        numericConversion();

        section("3. 算术、比较、逻辑运算");
        operators();

        section("4. && 与 || 的短路行为");
        shortCircuit();

        section("5. 普通 for、break、continue");
        loops();

        section("6. 数组：引用赋值 vs 复制");
        arrays();

        section("7. Java 参数按值传递（重要）");
        passByValue();

        section("8. static 方法与实例方法的区别");
        staticVsInstance();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("===== " + title + " =====");
    }

    // ---------- 1. 基本类型 ----------
    /**
     * Java 有 8 种基本类型，不是"对象"，直接存值。
     *
     * <p>零基础最容易混的是 char：它是**字符**但不是 String，
     * 用单引号 'a'，且可以当数字参与运算。
     */
    private static void basicTypes() {
        byte b = 127;              // 8 位，-128 ~ 127
        short s = 32767;           // 16 位
        int i = 2_147_483_647;     // 32 位（下划线只是可读性，编译时忽略）
        long l = 9_000_000_000L;   // 64 位，字面量必须加 L
        float f = 1.5f;            // 32 位浮点，字面量必须加 f
        double d = 3.14159;        // 64 位浮点，小数默认是 double
        char c = 'A';              // 16 位，单个字符
        boolean flag = true;       // 只有 true / false

        System.out.println("int  最大值 = " + i);
        System.out.println("long 大数   = " + l);
        System.out.println("char 'A'    = " + c + "，当数字用 = " + (int) c);  // 65
        System.out.println("char 加 1   = " + (char) (c + 1));               // B
        System.out.println("float       = " + f + "，double = " + d);
        System.out.println("boolean     = " + flag);

        // ⚠️ int 溢出不会报错，会绕回负数——静默错误
        int overflow = Integer.MAX_VALUE + 1;
        System.out.println("⚠️ int 最大值 + 1 = " + overflow + "（不报错，静默绕回）");

        // ⚠️ 浮点不精确
        System.out.println("⚠️ 0.1 + 0.2 = " + (0.1 + 0.2) + "（不等于 0.3）");
        System.out.println("   正确比较：Math.abs(0.1+0.2-0.3) < 1e-9 → " + (Math.abs(0.1 + 0.2 - 0.3) < 1e-9));
    }

    // ---------- 2. 数值转换 ----------
    /**
     * 两个必须记住的规则：
     * 1. 小范围 → 大范围：自动（int → long）
     * 2. 大范围 → 小范围：必须显式强转，可能丢数据
     */
    private static void numericConversion() {
        int a = 100;
        long widened = a;                 // 自动，int → long 安全
        System.out.println("int 100 → long: " + widened);

        long big = 3_000_000_000L;
        int narrowed = (int) big;         // 强转，超出 int 范围被截断
        System.out.println("⚠️ long 3000000000 → int: " + narrowed + "（数据已损坏）");

        double dd = 9.99;
        int truncate = (int) dd;          // 强转是"截断"，不是四舍五入
        System.out.println("⚠️ (int) 9.99 = " + truncate + "（截断，不四舍五入）");
        System.out.println("   四舍五入要用 Math.round: " + Math.round(dd));

        // ⚠️ 整数除法：两个 int 相除结果还是 int
        int wrongAvg = (1 + 2) / 2;
        double rightAvg = (1 + 2) / 2.0;   // 有一个是 double，结果就是 double
        System.out.println("⚠️ (1+2)/2 = " + wrongAvg + "（丢小数）");
        System.out.println("   (1+2)/2.0 = " + rightAvg);

        // 字符串 → 数字
        int parsed = Integer.parseInt("123");
        System.out.println("Integer.parseInt(\"123\") = " + parsed);
        try {
            Integer.parseInt("12a");      // 非数字会抛异常
        } catch (NumberFormatException e) {
            System.out.println("parseInt(\"12a\") → 抛 NumberFormatException（必须处理）");
        }
    }

    // ---------- 3. 运算符 ----------
    private static void operators() {
        int x = 7, y = 2;

        System.out.println("算术：7+2=" + (x + y) + " 7-2=" + (x - y)
                + " 7*2=" + (x * y) + " 7/2=" + (x / y) + " 7%2=" + (x % y));
        System.out.println("注意 7/2=" + (x / y) + " 而不是 3.5（整数除法）");

        System.out.println("比较：7>2=" + (x > y) + " 7==2=" + (x == y) + " 7!=2=" + (x != y));

        // ⚠️ 字符串比较必须用 equals，== 比的是引用地址
        String s1 = new String("abc");
        String s2 = new String("abc");
        System.out.println("⚠️ new String(\"abc\") == new String(\"abc\") → " + (s1 == s2) + "（引用不同）");
        System.out.println("   s1.equals(s2) → " + s1.equals(s2) + "（内容相同）");

        System.out.println("逻辑：true&&false=" + (true && false)
                + " true||false=" + (true || false) + " !true=" + (!true));
    }

    // ---------- 4. 短路行为 ----------
    /**
     * && 和 || 会「短路」：左边已经能决定结果时，右边**根本不执行**。
     *
     * <p>这不是优化细节，而是安全写法的基础：
     * {@code if (obj != null && obj.getName() != null)} 之所以不炸，
     * 就是因为左边为 false 时右边不求值。
     */
    private static void shortCircuit() {
        System.out.println("--- && 短路 ---");
        // 左边 false，右边不执行 → 不会 NPE
        String s = null;
        boolean result = (s != null) && s.length() > 3;
        System.out.println("(s != null) && s.length()>3 → " + result + "（右边没执行，所以不炸）");

        // 顺序反过来就会 NPE
        try {
            boolean bad = s.length() > 3 && s != null;
            System.out.println("不会到这里：" + bad);
        } catch (NullPointerException e) {
            System.out.println("⚠️ 写成 s.length()>3 && s!=null 就会 NPE（顺序很重要）");
        }

        System.out.println("--- || 短路 ---");
        // 左边 true，右边不执行
        boolean ok = (s == null) || s.isEmpty();
        System.out.println("(s == null) || s.isEmpty() → " + ok + "（右边没执行）");

        // 用副作用验证"右边到底执行了没有"
        System.out.println("--- 用副作用验证 ---");
        System.out.println("false && sideEffect() → " + (false && sideEffect()));
        System.out.println("true  || sideEffect() → " + (true || sideEffect()));
        System.out.println("（上面两行都没有打印 'sideEffect 执行了'，证明右边被跳过）");

        System.out.println("--- 对比 & 和 |（不短路）---");
        System.out.println("false & sideEffect() → " + (false & sideEffect()) + "（右边执行了）");
    }

    private static boolean sideEffect() {
        System.out.println("   sideEffect 执行了");
        return true;
    }

    // ---------- 5. 循环 ----------
    /**
     * for-each 只能"看"，改不了下标、不能中途跳着走。
     * 需要下标或 break/continue 时用普通 for。
     */
    private static void loops() {
        System.out.println("--- 普通 for：能拿到下标 ---");
        String[] names = {"甲", "乙", "丙", "丁"};
        for (int i = 0; i < names.length; i++) {
            System.out.println("  下标 " + i + " = " + names[i]);
        }

        System.out.println("--- for-each：更简洁，但没有下标 ---");
        for (String n : names) {
            System.out.print(n + " ");
        }
        System.out.println();

        System.out.println("--- break：整个循环结束 ---");
        for (int i = 0; i < 10; i++) {
            if (i == 3) {
                System.out.println("  i==3 时 break，后面的 4..9 都不执行");
                break;
            }
            System.out.print("  " + i);
        }
        System.out.println();

        System.out.println("--- continue：跳过本次，继续下一轮 ---");
        for (int i = 0; i < 6; i++) {
            if (i % 2 == 0) {
                continue;      // 偶数跳过
            }
            System.out.print("  " + i);
        }
        System.out.println("  ← 只打印了奇数");

        System.out.println("--- 双层循环里的 break 只跳内层 ---");
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                if (j == 1) {
                    break;     // 只结束内层，外层继续
                }
                System.out.println("  i=" + i + " j=" + j);
            }
        }
    }

    // ---------- 6. 数组 ----------
    /**
     * 数组是**引用类型**。赋值 arr2 = arr1 只是复制了地址，
     * 两个变量指向同一个数组——改一个另一个也变。
     */
    private static void arrays() {
        System.out.println("--- 引用赋值（危险）---");
        int[] a = {1, 2, 3};
        int[] b = a;              // 不是复制数组，是复制"地址"
        b[0] = 999;
        System.out.println("  a[0] = " + a[0] + "（被 b 改掉了！）");

        System.out.println("--- 真正复制 ---");
        int[] c = {1, 2, 3};
        int[] d = c.clone();      // 或 Arrays.copyOf(c, c.length)
        d[0] = 999;
        System.out.println("  c[0] = " + c[0] + "（未被影响）");

        System.out.println("--- 数组长度固定，越界直接抛异常 ---");
        try {
            int[] e = new int[2];
            System.out.println(e[5]);
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.println("  ⚠️ 访问 e[5] 抛 ArrayIndexOutOfBoundsException");
        }
    }

    // ---------- 7. 参数传递 ----------
    /**
     * Java **只有值传递**，没有引用传递。但"值"是引用时会让人误以为是引用传递。
     *
     * <p>关键区分：
     * <ul>
     *   <li>改**对象内容** → 外部能看到（因为传的是同一个对象的地址）</li>
     *   <li>改**引用本身**（让它指向新对象）→ 外部看不到（改的是副本）</li>
     * </ul>
     */
    private static void passByValue() {
        System.out.println("--- 基本类型：副本，外部不变 ---");
        int num = 10;
        changeInt(num);
        System.out.println("  调用 changeInt(10) 后，num 仍是 " + num);

        System.out.println("--- 传对象：能改内容 ---");
        StringBuilder sb = new StringBuilder("原始");
        changeContent(sb);
        System.out.println("  调用 changeContent 后 = " + sb + "（内容被改了）");

        System.out.println("--- 传对象：改引用不影响外部 ---");
        StringBuilder sb2 = new StringBuilder("原始");
        changeReference(sb2);
        System.out.println("  调用 changeReference 后 = " + sb2 + "（引用没变）");
    }

    private static void changeInt(int x) {
        x = 999;                   // 只改了副本
    }

    private static void changeContent(StringBuilder s) {
        s.append("被修改");         // 通过地址改对象内容 → 外部可见
    }

    private static void changeReference(StringBuilder s) {
        s = new StringBuilder("新对象");   // 只改了副本的指向 → 外部不可见
    }

    // ---------- 8. static vs 实例 ----------
    /**
     * static 方法属于**类**，没有 this，不能访问实例字段。
     * 实例方法属于**对象**，能访问 this 和实例字段。
     */
    private static void staticVsInstance() {
        // static 方法直接通过类名调用，不需要 new
        System.out.println("静态方法调用：" + Square.area(3));

        Square sq = new Square(4);
        System.out.println("实例方法调用：" + sq.describe());
        System.out.println("实例字段：" + sq.side);

        // 每个实例的实例字段互不影响，但共享同一个 static 字段
        Square s1 = new Square(1);
        Square s2 = new Square(2);
        System.out.println("s1.side=" + s1.side + " s2.side=" + s2.side + "（各自独立）");
        System.out.println("共享的 static 计数 = " + Square.createdCount + "（两个实例共用一份）");
    }

    /** 演示用的小类：静态方法与实例方法的对比。 */
    private static class Square {
        /** static 字段：所有实例共享一份。 */
        static int createdCount = 0;

        /** 实例字段：每个对象一份。 */
        final int side;

        Square(int side) {
            this.side = side;
            createdCount++;         // 每 new 一个就 +1
        }

        /** static 方法：只依赖参数，不依赖任何对象状态。 */
        static int area(int side) {
            return side * side;
        }

        /** 实例方法：要用到 this.side，所以不能是 static。 */
        String describe() {
            return "边长 " + this.side + " 的正方形，面积 " + area(this.side);
        }
    }
}