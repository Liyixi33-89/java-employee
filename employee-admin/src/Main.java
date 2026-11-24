import java.util.Scanner;

/**
 * 员工后台管理系统 —— 第一周版本（V0.1 起步）。
 *
 * <p>当前只实现菜单循环与输入容错，业务功能（新增/列表/查询/修改/删除）
 * 从第二周开始实现。未实现的功能会明确提示"尚未实现"，不冒充成功。
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConsoleMenu menu = new ConsoleMenu(scanner, System.out);
        menu.run();
    }
}