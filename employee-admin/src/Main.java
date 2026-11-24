import java.util.Scanner;

/**
 * 员工后台管理系统 —— V0.1 第二周版本。
 *
 * <p>已实现：员工对象、集合管理、新增/列表/按工号查询，含工号与姓名校验。
 * 第三周实现修改、删除与更完整的边界处理。
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
        EmployeeService service = new EmployeeService();
        ConsoleMenu menu = new ConsoleMenu(scanner, System.out, service);
        menu.run();
    }
}