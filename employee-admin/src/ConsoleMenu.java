import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * 控制台菜单：读取选择、分发到对应业务分支、处理非法输入。
 *
 * <p>V0.1 收尾：新增支持入职日期，列表支持按工号/入职日期排序。
 */
public class ConsoleMenu {

    private static final String LINE = "----------------------------------------";
    private static final String DELETE_CONFIRM_WORD = "DELETE";

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
        out.println(" 6. 按姓名搜索");
        out.println(" 7. 按工号排序");
        out.println(" 8. 按入职日期排序");
        out.println(" 0. 退出");
        out.println(LINE);
    }

    private void dispatch(String choice) {
        switch (choice) {
            case "1" -> handleCreate();
            case "2" -> handleList();
            case "3" -> handleFind();
            case "4" -> handleUpdate();
            case "5" -> handleDelete();
            case "6" -> handleSearch();
            case "7" -> handleSortedByNo();
            case "8" -> handleSortedByHireDate();
            case "0" -> running = false;
            case "" -> out.println("输入为空，请输入 0-8 之间的数字。");
            default -> out.println("无效选项：\"" + choice + "\"，请输入 0-8 之间的数字。");
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
        String hireDate = readLine("入职日期 yyyy-MM-dd（直接回车默认今天）: ");
        if (hireDate == null) {
            return;
        }
        try {
            String no = normalize(employeeNo);
            String employeeName = normalize(name);
            String dateText = normalize(hireDate);

            Employee created = dateText.isEmpty()
                    ? service.create(no, employeeName)          // 重载 1：默认今天
                    : service.create(no, employeeName, dateText); // 重载 3：字符串日期
            out.println("新增成功：" + created);
        } catch (BusinessException e) {
            out.println("新增失败：" + e.getMessage());
        }
    }

    private void handleList() {
        printEmployees(service.findAll(), "当前没有员工记录。");
    }

    private void handleSortedByNo() {
        printEmployees(service.findAllSortedByNo(), "当前没有员工记录。");
    }

    private void handleSortedByHireDate() {
        printEmployees(service.findAllSortedByHireDate(), "当前没有员工记录。");
    }

    /** 列表输出统一走这里，避免三处各写一遍 printf。 */
    private void printEmployees(List<Employee> employees, String emptyHint) {
        if (employees.isEmpty()) {
            out.println(emptyHint);
            return;
        }
        out.println("共 " + employees.size() + " 条：");
        int index = 1;
        for (Employee employee : employees) {
            out.printf("%d. %s%n", index++, employee);
        }
    }

    private void handleFind() {
        String no = readLine("要查询的工号: ");
        if (no == null) {
            return;
        }
        String employeeNo = normalize(no);
        Optional<Employee> found = service.findByNo(employeeNo);
        if (found.isPresent()) {
            out.println("查询结果：" + found.get());
        } else {
            out.println("未找到工号为 \"" + employeeNo + "\" 的员工。");
        }
    }

    /**
     * 修改员工：只改姓名，工号作为唯一标识不可变。
     *
     * <p>先展示当前记录再让用户输入新姓名，避免"改错人"。
     * 直接回车保留原姓名，这样只想看看记录的用户不会误清空数据。
     */
    private void handleUpdate() {
        String no = readLine("要修改的工号: ");
        if (no == null) {
            return;
        }
        String employeeNo = normalize(no);

        Employee existing;
        try {
            existing = service.requireExisting(employeeNo);
        } catch (BusinessException e) {
            out.println("修改失败：" + e.getMessage());
            return;
        }
        out.println("当前记录：" + existing);

        String name = readLine("新姓名（直接回车保持不变）: ");
        if (name == null) {
            return;
        }
        String newName = normalize(name);
        if (newName.isEmpty()) {
            out.println("已取消修改，记录保持不变。");
            return;
        }
        try {
            Employee updated = service.updateName(employeeNo, newName);
            out.println("修改成功：" + updated);
        } catch (BusinessException e) {
            out.println("修改失败：" + e.getMessage());
        }
    }

    /**
     * 删除员工。需要二次确认，避免误删；
     * 确认词必须完整输入 DELETE 才算数，其余输入一律视为取消。
     */
    private void handleDelete() {
        String no = readLine("要删除的工号: ");
        if (no == null) {
            return;
        }
        String employeeNo = normalize(no);

        Employee existing;
        try {
            existing = service.requireExisting(employeeNo);
        } catch (BusinessException e) {
            out.println("删除失败：" + e.getMessage());
            return;
        }
        out.println("即将删除：" + existing);

        String confirm = readLine("确认删除请输入 " + DELETE_CONFIRM_WORD + "（其他任意输入取消）: ");
        if (confirm == null) {
            return;
        }
        if (!DELETE_CONFIRM_WORD.equals(normalize(confirm))) {
            out.println("已取消删除，记录保持不变。");
            return;
        }
        try {
            Employee removed = service.delete(employeeNo);
            out.println("删除成功：" + removed);
        } catch (BusinessException e) {
            out.println("删除失败：" + e.getMessage());
        }
    }

    /**
     * 按姓名关键词搜索（独立扩展任务）。
     *
     * <p>与"查询"区分：查询按工号精确定位单条，搜索按姓名模糊匹配多条。
     */
    private void handleSearch() {
        String keyword = readLine("姓名关键词: ");
        if (keyword == null) {
            return;
        }
        String trimmed = normalize(keyword);
        if (trimmed.isEmpty()) {
            out.println("搜索关键词不能为空。");
            return;
        }
        List<Employee> matched = service.searchByName(trimmed);
        if (matched.isEmpty()) {
            out.println("没有姓名包含 \"" + trimmed + "\" 的员工。");
            return;
        }
        out.println("匹配 " + matched.size() + " 条：");
        int index = 1;
        for (Employee employee : matched) {
            out.printf("%d. %s%n", index++, employee);
        }
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