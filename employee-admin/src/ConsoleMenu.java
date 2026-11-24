import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * 控制台菜单：读取选择、分发到对应业务分支、处理非法输入。
 *
 * <p>第二周起「新增/列表/查询」接真实业务；「修改/删除」仍是受控占位，
 * 第三周实现。未实现项明确提示，不冒充成功。
 */
public class ConsoleMenu {

    private static final String LINE = "----------------------------------------";

    private final Scanner scanner;
    private final PrintStream out;
    private final EmployeeService service;

    private boolean running = true;

    public ConsoleMenu(Scanner scanner, PrintStream out, EmployeeService service) {
        this.scanner = scanner;
        this.out = out;
        this.service = service;
    }

    /** 菜单主循环：直到用户选择退出，或输入流结束（EOF）。 */
    public void run() {
        out.println("=== 员工后台管理系统 ===");
        while (running) {
            printMenu();
            String input = readLine("请选择操作: ");
            if (input == null) {
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
            case "1" -> handleCreate();
            case "2" -> handleList();
            case "3" -> handleFind();
            case "4" -> todo("修改员工");
            case "5" -> todo("删除员工");
            case "0" -> running = false;
            case "" -> out.println("输入为空，请输入 0-5 之间的数字。");
            default -> out.println("无效选项：\"" + choice + "\"，请输入 0-5 之间的数字。");
        }
    }

    private void handleCreate() {
        String employeeNo = readLine("工号: ");
        if (employeeNo == null) {
            return;
        }
        String name = readLine("姓名: ");
        if (name == null) {
            return;
        }
        try {
            Employee created = service.create(normalize(employeeNo), normalize(name));
            out.println("新增成功：" + created);
        } catch (BusinessException e) {
            out.println("新增失败：" + e.getMessage());
        }
    }

    private void handleList() {
        List<Employee> employees = service.findAll();
        if (employees.isEmpty()) {
            out.println("当前没有员工记录。");
            return;
        }
        out.println("共 " + employees.size() + " 条：");
        int index = 1;
        for (Employee employee : employees) {
            out.printf("%d. %s%n", index++, employee);
        }
    }

    private void handleFind() {
        String employeeNo = readLine("要查询的工号: ");
        if (employeeNo == null) {
            return;
        }
        Optional<Employee> found = service.findByNo(normalize(employeeNo));
        if (found.isPresent()) {
            out.println("查询结果：" + found.get());
        } else {
            out.println("未找到工号为 \"" + normalize(employeeNo) + "\" 的员工。");
        }
    }

    private void todo(String action) {
        out.println("[尚未实现] " + action + "：该功能将在第三周实现。");
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

    /** 读取一行输入。用户直接回车视为空串；流结束返回 null。 */
    private String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine();
    }
}