package pkgdemo.model;

/**
 * 演示用：一个<b>有包声明</b>的类。
 *
 * <p>文件位置必须是 {@code src/pkgdemo/model/Student.java}——
 * <b>包的层级必须和目录层级一致</b>，否则编译报错。
 *
 * <p>对比：本项目其它类（{@code Employee}、{@code EmployeeService} 等）
 * 都在<b>默认包</b>（没有 {@code package} 行），所以它们之间互相引用不需要 import。
 */
public class Student {

    private final String name;
    private final int score;

    public Student(String name, int score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    /** 内部用的辅助方法：{@code private} 表示同包也不能调，只有本类能调。 */
    private String grade() {
        if (score >= 90) {
            return "A";
        }
        if (score >= 80) {
            return "B";
        }
        return "C";
    }

    /** {@code public} 方法，跨包可调。 */
    public String describe() {
        return name + " " + score + "分 等级" + grade();
    }
}