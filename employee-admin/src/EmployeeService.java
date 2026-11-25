import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 员工业务规则与内存存储。
 *
 * <p>V0.1 收尾：在 CRUD 基础上补入职日期（G6）、排序（G4）。
 */
public class EmployeeService {

    /** 工号格式：字母或数字，2—10 位。 */
    private static final String EMPLOYEE_NO_PATTERN = "[A-Za-z0-9]{2,10}";
    /** 姓名长度上限（按字符数，中文姓名足够）。 */
    private static final int NAME_MAX_LENGTH = 20;

    /** 按工号升序。Comparator 是排序规则对象，可复用、可组合。 */
    private static final Comparator<Employee> BY_NO_ASC =
            Comparator.comparing(Employee::getEmployeeNo);
    /** 按入职日期降序：新入职的排在前面。 */
    private static final Comparator<Employee> BY_HIRE_DATE_DESC =
            Comparator.comparing(Employee::getHireDate).reversed();

    private final Map<String, Employee> store = new LinkedHashMap<>();

    /**
     * 新增员工（不指定入职日期时默认今天）。这是方法重载。
     *
     * @throws BusinessException 工号为空/格式非法/重复，或姓名为空/过长
     */
    public Employee create(String employeeNo, String name) {
        return create(employeeNo, name, LocalDate.now());
    }

    /**
     * 新增员工（指定入职日期）。
     *
     * @throws BusinessException 工号为空/格式非法/重复，姓名为空/过长，
     *                           或日期格式不是 yyyy-MM-dd / 日期在未来
     */
    public Employee create(String employeeNo, String name, LocalDate hireDate) {
        String no = requireEmployeeNo(employeeNo);
        String employeeName = requireName(name);
        LocalDate date = requireHireDate(hireDate);

        if (store.containsKey(no)) {
            throw new BusinessException("工号已存在：\"" + no + "\" 已被 " + store.get(no).getName() + " 使用");
        }

        Employee employee = new Employee(no, employeeName, date);
        store.put(no, employee);
        return employee;
    }

    /**
     * 新增员工（日期以字符串传入）。再一个重载，方便上层直接用用户输入。
     *
     * <p>三个同名 create 的存在意义：让调用方按手头有的数据选择，
     * 编译期就能确定走哪个版本，而不是靠传 null 判断。
     */
    public Employee create(String employeeNo, String name, String hireDateText) {
        return create(employeeNo, name, parseDateText(hireDateText));
    }

    /**
     * 修改员工。工号不可变，只能改姓名。
     *
     * <p>刻意不提供"改工号"能力：工号是外部约定的业务标识，
     * 允许改动会让历史引用（将来的审计、关联表）失去意义。
     * 真需要换号，语义上应视为"删除 + 重新建档"（当前无历史关联时的简化做法；一旦接入审批/薪酬等关联数据，换号必须走迁移流程，不能删了重建）。
     *
     * @throws BusinessException 工号或姓名为空、姓名过长、工号不存在
     */
    public Employee updateName(String employeeNo, String newName) {
        Employee existing = requireExisting(employeeNo);
        existing.setName(requireName(newName));
        return existing;
    }

    /**
     * 删除员工。
     *
     * @return 被删除的员工
     * @throws BusinessException 工号为空/格式非法/不存在
     */
    public Employee delete(String employeeNo) {
        Employee existing = requireExisting(employeeNo);
        store.remove(existing.getEmployeeNo());
        return existing;
    }

    /**
     * 取一条必须存在的员工。先校验工号格式，再校验存在性。
     *
     * <p>修改/删除这类「针对已有记录」的操作都走这里，
     * 避免上层先查存在性而绕过格式校验——那样空工号会被报成"不存在"，
     * 语义是错的：空工号根本不是一个合法工号。
     *
     * @throws BusinessException 工号为空/格式非法/不存在
     */
    public Employee requireExisting(String employeeNo) {
        String no = requireEmployeeNo(employeeNo);
        Employee existing = store.get(no);
        if (existing == null) {
            throw new BusinessException("工号不存在：\"" + no + "\"");
        }
        return existing;
    }

    /** 全部员工，按新增顺序。返回副本，外部修改不影响内部存储。 */
    public List<Employee> findAll() {
        return new ArrayList<>(store.values());
    }

    /**
     * 按工号升序列出。
     *
     * <p>排序在 findAll 的**副本**上做，不能改 store 内部顺序——
     * LinkedHashMap 的插入顺序是"列表按新增次序展示"的保证，
     * 一旦被排序打乱，下次打开列表顺序就变了。
     */
    public List<Employee> findAllSortedByNo() {
        List<Employee> copy = findAll();
        copy.sort(BY_NO_ASC);
        return copy;
    }

    /** 按入职日期降序列出（新人在前）。同样只在副本上排序。 */
    public List<Employee> findAllSortedByHireDate() {
        List<Employee> copy = findAll();
        copy.sort(BY_HIRE_DATE_DESC);
        return copy;
    }

    /**
     * 按姓名关键词模糊搜索（不区分大小写）。
     *
     * <p>关键词为空时返回空列表而不是全部——"搜索"语义下，
     * 空条件通常是用户误操作，静默返回全量会掩盖输入问题。
     */
    public List<Employee> searchByName(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        List<Employee> matched = new ArrayList<>();
        for (Employee employee : store.values()) {
            if (employee.getName().toLowerCase(Locale.ROOT).contains(lower)) {
                matched.add(employee);
            }
        }
        return matched;
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

    /**
     * 解析 yyyy-MM-dd 文本。
     *
     * @throws BusinessException 格式不合法
     */
    private LocalDate parseDateText(String text) {
        String value = requireText(text, "入职日期");
        try {
            return LocalDate.parse(value, Employee.DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new BusinessException("日期格式错误：需为 yyyy-MM-dd，当前为 \"" + value + "\"");
        }
    }

    private LocalDate requireHireDate(LocalDate hireDate) {
        if (hireDate == null) {
            throw new BusinessException("入职日期不能为空");
        }
        if (hireDate.isAfter(LocalDate.now())) {
            throw new BusinessException("入职日期不能是未来日期：" + hireDate.format(Employee.DATE_FORMAT));
        }
        return hireDate;
    }

    private String requireEmployeeNo(String employeeNo) {
        String no = requireText(employeeNo, "工号");
        if (!no.matches(EMPLOYEE_NO_PATTERN)) {
            throw new BusinessException("工号格式错误：需为 2—10 位字母或数字，当前为 \"" + no + "\"");
        }
        return no;
    }

    private String requireName(String name) {
        String employeeName = requireText(name, "姓名");
        if (employeeName.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(
                    "姓名过长：最多 " + NAME_MAX_LENGTH + " 个字符，当前 " + employeeName.length() + " 个");
        }
        return employeeName;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(fieldName + "不能为空");
        }
        return trimmed;
    }
}