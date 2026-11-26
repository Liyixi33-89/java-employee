package pkgdemo.util;

/**
 * 演示子包：{@code pkgdemo.util} 比 {@code pkgdemo} 深一层。
 *
 * <p>目录必须是 {@code src/pkgdemo/util/Formats.java}。
 */
public class Formats {

    /** 工具类通常不实例化：私有构造阻止 {@code new Formats()}。 */
    private Formats() {
    }

    public static String percent(int score) {
        return score + "%";
    }

    public static String pad(String text, int width) {
        StringBuilder sb = new StringBuilder(text);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
}