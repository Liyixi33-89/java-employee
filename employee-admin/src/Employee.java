import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Objects;

/**
 * 员工模型（V0.1 收尾：工号 + 姓名 + 入职日期）。
 *
 * <p>工号和入职日期创建后不可变；姓名允许修改。
 */
public final class Employee {

    /**
     * 展示与解析统一用这个格式，避免各处 pattern 不一致。
     *
     * <p>两个细节缺一不可：
     * <ul>
     *   <li>{@code ResolverStyle.STRICT}：默认的 SMART 会把不存在的日期"修正"成月末，
     *       例如用户输入 {@code 2024-02-30} 会被静默改成 {@code 2024-02-29}——
     *       看着成功，数据其实错了。严格模式下这种输入直接抛异常。</li>
     *   <li>年份用 {@code uuuu} 而不是 {@code yyyy}：STRICT 模式下 {@code yyyy} 表示
     *       "纪元年"（year-of-era），缺少纪元信息会解析失败。{@code uuuu} 是纯年份。</li>
     * </ul>
     */
    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final String employeeNo;
    private String name;
    private final LocalDate hireDate;

    /** 便捷构造：不传日期时默认今天（这是构造方法重载，见 G2）。 */
    public Employee(String employeeNo, String name) {
        this(employeeNo, name, LocalDate.now());
    }

    public Employee(String employeeNo, String name, LocalDate hireDate) {
        this.employeeNo = employeeNo;
        this.name = name;
        this.hireDate = hireDate;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    /**
     * 只按工号判断相等。
     *
     * <p>刻意不把姓名、入职日期算进来：工号是这条记录的业务身份。
     * 如果加入可变字段（姓名），同一个员工改完名字就不再 equals 自己，
     * 放进 HashSet/Map 后会「找不到」——这是重写 equals 最典型的坑。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Employee other)) {
            return false;
        }
        return Objects.equals(employeeNo, other.employeeNo);
    }

    /** 必须与 equals 用同一组字段，否则违反 equals/hashCode 契约。 */
    @Override
    public int hashCode() {
        return Objects.hash(employeeNo);
    }

    @Override
    public String toString() {
        return employeeNo + "  " + name + "  入职:" + hireDate.format(DATE_FORMAT);
    }
}