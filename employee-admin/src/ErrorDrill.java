/**
 * G1 练习：编译错误 vs 运行错误 vs 调用栈阅读。
 *
 * <p>用法：按注释里的「任务」逐条改代码、重新编译运行、记录现象。
 * 每条都对应 V0.1 知识点清单 §4-G1。
 *
 * <p>编译运行命令：
 * <pre>
 * javac -encoding UTF-8 -d out src\ErrorDrill.java
 * java -cp out ErrorDrill
 * </pre>
 */
public class ErrorDrill {

    public static void main(String[] args) {
        System.out.println("=== 任务 1：编译错误 ===");
        task1CompileError();

        System.out.println("=== 任务 2：运行错误（NPE）+ 读堆栈 ===");
        task2NullPointer();
    }

    /**
     * 任务 1：把下面一行末尾的分号删掉再编译。
     *
     * <p>现象：编译失败，输出形如
     * <pre>
     * src\ErrorDrill.java:34: 错误: 需要';'
     * </pre>
     * 要点：<b>编译错误根本不生成 .class，程序一行都没跑。</b>
     * 错误信息第一行就给了「文件:行号」，直接去那一行看，不用往下翻。
     */
    private static void task1CompileError() {
        int level = 1;
        System.out.println("任务 1 正常执行，level = " + level);
    }

    /**
     * 任务 2：触发 NullPointerException 并读堆栈。
     *
     * <p>本项目实测输出（JDK 21）：
     * <pre>
     * Exception in thread "main" java.lang.NullPointerException:
     *     Cannot invoke "String.length()" because "&lt;parameter1&gt;" is null
     *     at ErrorDrill.validate(ErrorDrill.java:68)
     *     at ErrorDrill.task2NullPointer(ErrorDrill.java:63)
     *     at ErrorDrill.main(ErrorDrill.java:20)
     * </pre>
     *
     * <p><b>正确的读法：从上往下读。第一行是「异常类型 + 原因」，
     * 第一个 `at 自己的类.方法(自己文件:行号)` 就是出错现场。</b>
     * 上面这段现场是 `validate` 的第 68 行，不是 `main` 的第 20 行。
     *
     * <p>堆栈里越往下越是「更早的调用者」，用来判断是谁触发了它；
     * 但定位 bug 只需看<b>第一个属于自己代码的帧</b>。
     * 夹在中间的 `java.base/...` 帧是 JDK 内部实现，通常不用看。
     *
     * <p>注意 JDK 14+ 的 NPE 消息会指明「哪个表达式、哪个变量是 null」，
     * 这比老版本只有一行 `NullPointerException` 好用得多。
     */
    private static void task2NullPointer() {
        String input = null;              // 故意给 null
        String result = validate(input);
        System.out.println("不会执行到这里，因为上一行抛异常了：" + result);
    }

    private static String validate(String raw) {
        if (raw.length() > 10) {          // ← 现场：raw 为 null，调 length() 就炸
            return "too long";
        }
        return normalize(raw);
    }

    private static String normalize(String input) {
        return input.trim();
    }
}