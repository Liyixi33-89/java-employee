import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 员工业务规则与内存存储（V0.1 第二周）。
 *
 * <p>用 {@link LinkedHashMap} 保持插入顺序，工号作键天然保证唯一；
 * 但唯一性校验仍然显式做，因为将来换成数据库时约束会由数据库承担，
 * 业务层要保持同一套错误语义。
 *
 * <p>数据只在内存中，进程结束即消失——这是本阶段的边界。
 */
public class EmployeeService {

    /** 工号格式：字母或数字，2—10 位。 */
    private static final String EMPLOYEE_NO_PATTERN = "[A-Za-z0-9]{2,10}";
    /** 姓名长度上限（按字符数，中文姓名足够）。 */
    private static final int NAME_MAX_LENGTH = 20;

    private final Map<String, Employee> store = new LinkedHashMap<>();

    /**
     * 新增员工。
     *
     * @throws BusinessException 工号为空/格式非法/重复，或姓名为空/过长
     */
    public Employee create(String employeeNo, String name) {
        String no = requireText(employeeNo, "工号");
        String employeeName = requireText(name, "姓名");

        if (!no.matches(EMPLOYEE_NO_PATTERN)) {
            throw new BusinessException("工号格式错误：需为 2—10 位字母或数字，当前为 \"" + no + "\"");
        }
        if (employeeName.length() > NAME_MAX_LENGTH) {
            throw new BusinessException("姓名过长：最多 " + NAME_MAX_LENGTH + " 个字符，当前 " + employeeName.length() + " 个");
        }
        if (store.containsKey(no)) {
            throw new BusinessException("工号已存在：\"" + no + "\" 已被 " + store.get(no).getName() + " 使用");
        }

        Employee employee = new Employee(no, employeeName);
        store.put(no, employee);
        return employee;
    }

    /** 全部员工，按新增顺序。返回副本，外部修改不影响内部存储。 */
    public List<Employee> findAll() {
        return new ArrayList<>(store.values());
    }

    /** 按工号查询，找不到返回空。 */
    public Optional<Employee> findByNo(String employeeNo) {
        if (employeeNo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(employeeNo.trim()));
    }

    public int count() {
        return store.size();
    }

    private String requireText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return trimmed;
    }
}