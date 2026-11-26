import java.util.List;

/**
 * 员工存储契约。
 *
 * <p><b>只声明"能做什么"，不关心"怎么存"。</b>目前只有内存实现
 * {@link InMemoryStore}，V0.2 会加文件实现，届时业务层一行都不用改。
 *
 * <p>为什么现在只有一种实现就要抽接口？两个理由：
 * <ol>
 *   <li>路线 §5.2 明确要求「业务层可以切换内存/文件存储，不重写菜单与全部规则」，
 *       这是为它预先留好的接缝。</li>
 *   <li>接口同时是"约束"：新增实现时必须提供这 6 个方法，少一个就编译不过，
 *       不会出现"文件版忘了实现 findAll"这种运行时才炸的问题。</li>
 * </ol>
 *
 * <p>⚠️ 注意与"不要提前抽象"的关系：如果项目永远只有内存一种存储，
 * 这个接口就是多余的一层。是否值得抽象，取决于
 * <b>是否真的存在第二种实现的可能</b>——这里存在（V0.2 的文件存储）。
 */
public interface EmployeeStore {

    /**
     * 存入/覆盖一条记录。
     *
     * <p>唯一性由上层 {@code EmployeeService} 负责校验——
     * 存储层只管存，不重复做业务判断。这样换成数据库后，
     * 唯一约束由数据库承担，业务层的错误文案不用变。
     */
    void put(String employeeNo, Employee employee);

    /** 按工号取，没有返回 {@code null}。 */
    Employee get(String employeeNo);

    /** 判断工号是否存在。 */
    boolean containsKey(String employeeNo);

    /** 删除并返回，没有返回 {@code null}。 */
    Employee remove(String employeeNo);

    /** 全部记录，按实现约定顺序返回副本。 */
    List<Employee> findAll();

    /** 当前条数。 */
    int size();
}