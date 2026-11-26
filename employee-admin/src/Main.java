import java.util.Scanner;

/**
 * 员工后台管理系统 —— 程序入口。
 *
 * <p>本类只做一件事：<b>把各个零件组装起来</b>，然后启动菜单。
 * 业务规则在 {@link EmployeeService}，数据存放在 {@link EmployeeStore} 的实现里。
 *
 * <p>这里能直接看到「面向接口编程」的样子：
 * <pre>
 * EmployeeStore store = new InMemoryStore();   // 左边接口，右边实现
 * EmployeeService service = new EmployeeService(store);
 * </pre>
 * V0.2 要换成文件存储时，只需要把 {@code new InMemoryStore()} 改成
 * {@code new FileStore(...)}，后面两行<b>一个字都不用动</b>。
 */
public class Main {

    public static void main(String[] args) {
        // 1. 选定存储实现
        EmployeeStore store = new InMemoryStore();

        // 2. 注入到业务层（业务层只认接口）
        EmployeeService service = new EmployeeService(store);

        // 3. 组装输入输出并启动
        Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8);
        ConsoleMenu menu = new ConsoleMenu(scanner, System.out, service);
        menu.run();
    }
}