import java.io.PrintStream;
import java.util.Scanner;

/**
 * 控制台菜单：负责读取选择、分发到对应分支、处理非法输入。
 *
 * <p>第一周只要求菜单能稳定循环、非法输入不崩溃、退出干净。
 * 业务分支用 {@link #notImplemented(String)} 占位，并在 V0.1 验收前替换为真实业务。
 */
public class ConsoleMenu {

    private static final String LINE = "----------------------------------------";

    private final Scanner scanner;
    private final PrintStream out;
    private boolean running = true;

    public ConsoleMenu(Scanner scanner, PrintStream out) {
        this.scanner = scanner;
        this.out = out;
    }

    /** 菜单主循环：直到用户选择退出，或输入流结束（EOF）。 */
    public void run() {
        out.println("=== 员工后台管理系统 ===");
        while (running) {
            printMenu();
            String input = readLine("请选择操作: ");
            if (input == null) {
                // 输入流结束（如 Ctrl+Z / 重定向文件读完），属于正常退出路径
                out.println();
                out.println("输入流已结束，程序退出。");
                break;
            }
            dispatch(normalize(input));
        }
        out.println("已退出，再见。");
    }

    private void printMenu() {
        out.println(LINE);
        out.println(" 1. 新增员工");
        out.println(" 2. 员工列表");
        out.println(" 3. 查询员工");
        out.println(" 4. 修改员工");
        out.println(" 5. 删除员工");
        out.println(" 0. 退出");
        out.println(LINE);
    }

    private void dispatch(String choice) {
        switch (choice) {
            case "1" -> notImplemented("新增员工");
            case "2" -> notImplemented("员工列表");
            case "3" -> notImplemented("查询员工");
            case "4" -> notImplemented("修改员工");
            case "5" -> notImplemented("删除员工");
            case "0" -> running = false;
            case "" -> out.println("输入为空，请输入 0-5 之间的数字。");
            default -> out.println("无效选项：\"" + choice + "\"，请输入 0-5 之间的数字。");
        }
    }

    private void notImplemented(String action) {
        out.println("[尚未实现] " + action + "：该功能将在第二周实现。");
    }

    /**
     * 归一化输入：去掉首尾空白，并剔除可能被粘贴进来的 BOM / 零宽字符。
     *
     * <p>真实场景里从文件或网页复制内容时，首字符可能是 U+FEFF，
     * 会让人看着"输入了 1"却被告知非法。这里统一清理而不是散落各处判断。
     */
    private String normalize(String raw) {
        return raw.replace("\uFEFF", "").replace("\u200B", "").trim();
    }

    /**
     * 读取一行输入。用户直接回车视为空串；流结束返回 null。
     */
    private String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine();
    }
}