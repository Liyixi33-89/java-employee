import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 内存实现：用 {@link LinkedHashMap} 保存。
 *
 * <p>选 {@code LinkedHashMap} 而不是 {@code HashMap}，是因为列表要按
 * <b>新增顺序</b>展示。{@code HashMap} 的顺序由哈希值决定，看起来是乱的。
 */
public class InMemoryStore implements EmployeeStore {

    private final Map<String, Employee> data = new LinkedHashMap<>();

    @Override
    public void put(String employeeNo, Employee employee) {
        data.put(employeeNo, employee);
    }

    @Override
    public Employee get(String employeeNo) {
        return data.get(employeeNo);
    }

    @Override
    public boolean containsKey(String employeeNo) {
        return data.containsKey(employeeNo);
    }

    @Override
    public Employee remove(String employeeNo) {
        return data.remove(employeeNo);
    }

    /**
     * 返回副本，不是内部集合。
     *
     * <p>如果直接 {@code return data.values()}，调用方一句 {@code clear()}
     * 就能把内部数据清空。{@code new ArrayList<>(...)} 复制一份，外部随便改。
     *
     * <p>注意这是<b>浅拷贝</b>：元素还是同一个 {@code Employee} 对象，
     * 调用方改字段仍会影响存储内容——本项目的 {@code updateName} 正是依赖这一点。
     */
    @Override
    public List<Employee> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public int size() {
        return data.size();
    }
}