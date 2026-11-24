import java.util.Objects;

/**
 * 员工模型（V0.1 第二周最小字段：工号 + 姓名）。
 *
 * <p>工号创建后不可变，作为唯一标识；姓名允许后续修改（第三周）。
 */
public final class Employee {

    private final String employeeNo;
    private String name;

    public Employee(String employeeNo, String name) {
        this.employeeNo = employeeNo;
        this.name = name;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Employee other)) {
            return false;
        }
        return employeeNo.equals(other.employeeNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeNo);
    }

    @Override
    public String toString() {
        return employeeNo + "  " + name;
    }
}