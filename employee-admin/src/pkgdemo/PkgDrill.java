package pkgdemo;

import pkgdemo.model.Student;
import pkgdemo.util.Formats;

/**
 * 演示 {@code package} 与 {@code import} 的工作方式。
 *
 * <p>运行方式（注意主类要写<b>全限定名</b>）：
 * <pre>
 * javac -encoding UTF-8 -d out src/pkgdemo/PkgDrill.java src/pkgdemo/model/Student.java src/pkgdemo/util/Formats.java
 * java -cp out pkgdemo.PkgDrill
 * </pre>
 *
 * <p>⚠️ 踩坑记录：注释里写 {@code src\pkgdemo} 会编译失败，因为 Java 把
 * <b>反斜杠 + u</b> 当成 Unicode 转义的开头（{@code \\uXXXX} 是字符字面量），
 * 报错「非法 Unicode 转义」。写路径一律用正斜杠，或在文档里避开反斜杠。
 *
 * <p><b>为什么主类名变了？</b>在默认包里写 {@code java -cp out Main}；
 * 有了包之后必须写 {@code java -cp out pkgdemo.PkgDrill}——
 * 包名 + 点 + 类名，这叫<b>全限定名</b>。
 */
public class PkgDrill {

    public static void main(String[] args) {
        System.out.println("===== 1. 跨包使用类必须 import =====");
        Student s = new Student("张三", 92);
        System.out.println("  " + s.describe());
        System.out.println("  ↑ 用的 import pkgdemo.model.Student;（不写会编译报错「找不到符号」）");
        System.out.println();

        System.out.println("===== 2. 子包也算不同包，仍然要 import =====");
        System.out.println("  Formats 在 pkgdemo.util，本类在 pkgdemo —— 是两个不同的包");
        System.out.println("  所以文件头必须写 import pkgdemo.util.Formats;");
        System.out.println("  Formats.percent(92) = " + Formats.percent(92));
        System.out.println("  ⚠️ Java 没有「父子包自动可见」这回事，pkgdemo 看不到 pkgdemo.util 的类");
        System.out.println();

        System.out.println("===== 3. 全限定名：不用 import 也能访问 =====");
        // 不 import 时，写完整路径也行（一般只在类名冲突时用）
        pkgdemo.model.Student s2 = new pkgdemo.model.Student("李四", 75);
        System.out.println("  " + s2.describe());
        System.out.println();

        System.out.println("===== 4. 包的层级 = 目录层级 =====");
        System.out.println("  package pkgdemo.model;  →  src/pkgdemo/model/Student.java");
        System.out.println("  package pkgdemo;        →  src/pkgdemo/PkgDrill.java");
        System.out.println("  不一致会报错：类 Student 是公共的，应在名为 pkgdemo.model.Student.java 的文件中声明");
        System.out.println();

        System.out.println("===== 5. 编译产物也会按包分目录 =====");
        System.out.println("  javac -d out 后生成 out/pkgdemo/model/Student.class");
        System.out.println("  所以 -cp out 时 JVM 能顺着包名找到类");
        System.out.println();

        System.out.println("===== 6. 默认包的限制（本项目现状）=====");
        System.out.println("  默认包里的类（如 Employee）不能被任何有包名的类 import。");
        System.out.println("  也就是说：pkgdemo.PkgDrill 无法使用 Employee。");
        System.out.println("  这是 V0.2 必须把全部类加上 package 的原因。");
    }
}