import java.util.ArrayList;
import java.util.List;

/**
 * 接口与多态的最小例子（本阶段只要求"看懂"，V0.2 才真正用于存储替换）。
 *
 * <pre>
 * javac -encoding UTF-8 -d out src\OopDrill.java
 * java -cp out OopDrill
 * </pre>
 *
 * <p>要看懂的核心只有一句：<b>调用方写的是接口类型，运行时才知道跑哪个实现。</b>
 */
public class OopDrill {

    public static void main(String[] args) {
        System.out.println("===== 1. 不写接口会怎样 =====");
        withoutInterface();

        System.out.println();
        System.out.println("===== 2. 引入接口后 =====");
        withInterface();

        System.out.println();
        System.out.println("===== 3. 多态的关键：编译期看类型，运行期看对象 =====");
        polymorphism();

        System.out.println();
        System.out.println("===== 4. 多态的实际价值：加新实现不改调用方 =====");
        extensibility();
    }

    /**
     * 反面例子：调用方直接依赖具体实现。
     *
     * <p>问题：将来要换成"写文件"，这两个方法都得改。
     */
    private static void withoutInterface() {
        MemoryOnly store = new MemoryOnly();
        store.save("张三");
        store.save("李四");
        System.out.println("  存在内存里的：" + store.all());

        // ⚠️ 如果现在有了 FileOnly，就要再写一套 saveToFile / allFromFile 方法
        // 每加一种存储，调用方代码就翻一倍
    }

    /**
     * 正面例子：调用方只依赖接口。
     *
     * <p>注意 {@link #printAll} 的签名——参数是接口类型 {@code Store}，
     * 传内存实现还是文件实现都能编译通过。
     */
    private static void withInterface() {
        Store memory = new MemoryStore();
        Store fileish = new FakeFileStore();     // 假装是文件实现

        memory.save("张三");
        memory.save("李四");
        fileish.save("王五");

        System.out.println("  内存实现：");
        printAll(memory);
        System.out.println("  文件实现（演示用假实现）：");
        printAll(fileish);
    }

    /** 这个方法只认识接口，不认识任何具体实现。 */
    private static void printAll(Store store) {
        for (String name : store.all()) {
            System.out.println("    - " + name + "（共 " + store.count() + " 条）");
        }
    }

    /**
     * 多态：同一个变量类型，不同对象，行为不同。
     *
     * <p>{@code Store s = new MemoryStore()} 和 {@code Store s = new FakeFileStore()}，
     * 变量类型都是 {@code Store}，但 {@code s.save(...)} 实际执行的是各自的实现。
     */
    private static void polymorphism() {
        Store[] stores = {new MemoryStore(), new FakeFileStore()};

        for (Store s : stores) {
            // 编译期：只知道 s 是 Store，能调 save/all/count
            // 运行期：才决定调 MemoryStore 的还是 FakeFileStore 的
            s.save("甲");
            System.out.println("  " + s.name() + " → save 后 count = " + s.count());
        }
    }

    /**
     * 扩展性：想加"数据库实现"，只需新写一个类，不动 printAll 一行。
     *
     * <p>这就是 V0.2 要做的事：抽 {@code EmployeeRepository} 接口，
     * 内存实现 + 文件实现并存，服务层只依赖接口。
     */
    private static void extensibility() {
        List<Store> allStores = new ArrayList<>();
        allStores.add(new MemoryStore());
        allStores.add(new FakeFileStore());

        // 假设将来加了 DatabaseStore，这里只需要 add 一行
        // allStores.add(new DatabaseStore());

        for (Store s : allStores) {
            s.save("同一个调用代码");
        }
        System.out.println("  两种实现用的是同一句 s.save(...)，调用方无需知道区别");
    }

    // ==================== 接口与实现 ====================

    /**
     * 接口：只声明"能做什么"，不写"怎么做"。
     *
     * <p>接口里的方法默认是 {@code public abstract}，可以不写这两个词。
     */
    private interface Store {
        void save(String name);

        List<String> all();

        int count();

        /** 默认方法：接口也可以给默认实现，实现类不必重写。 */
        default String name() {
            return getClass().getSimpleName();
        }
    }

    /** 实现 1：内存。 */
    private static class MemoryStore implements Store {
        private final List<String> data = new ArrayList<>();

        @Override
        public void save(String name) {
            data.add(name);
        }

        @Override
        public List<String> all() {
            return new ArrayList<>(data);
        }

        @Override
        public int count() {
            return data.size();
        }
    }

    /** 实现 2：假文件（真实现等 V0.2 学文件 IO 后写）。 */
    private static class FakeFileStore implements Store {
        private final List<String> lines = new ArrayList<>();

        @Override
        public void save(String name) {
            lines.add("line: " + name);     // 假装写进了文件
        }

        @Override
        public List<String> all() {
            return new ArrayList<>(lines);
        }

        @Override
        public int count() {
            return lines.size();
        }
    }

    /** 反面例子用的具体类（没有接口）。 */
    private static class MemoryOnly {
        private final List<String> data = new ArrayList<>();

        void save(String name) {
            data.add(name);
        }

        List<String> all() {
            return new ArrayList<>(data);
        }
    }
}