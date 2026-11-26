import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集合进阶实验：遍历时修改集合（知识点 D11），以及集合选型的实测对比。
 *
 * <pre>
 * javac -encoding UTF-8 -d out src\CollectionDrill.java
 * java -cp out CollectionDrill
 * </pre>
 *
 * <p><b>核心结论先说</b>：fail-fast 只是"尽力检测"，不是保证。
 * 本项目实测证明：同样是遍历中删除，{@code ArrayList} 会抛异常，
 * 而 {@code HashMap} 删唯一键时<b>不抛</b>。所以不能靠"它会不会抛"来判断代码对不对。
 */
public class CollectionDrill {

    public static void main(String[] args) {
        section("1. ArrayList：遍历中删除 → 抛异常");
        listRemoveInLoop();

        section("2. HashMap：删唯一键 → 竟然不抛（实测）");
        mapRemoveLastKey();

        section("3. HashMap：删多个键 → 抛异常");
        mapRemoveManyKeys();

        section("4. ConcurrentHashMap：遍历中删除 → 不抛，但不保证看到新数据");
        concurrentMapRemove();

        section("5. 正确做法一：Iterator.remove()");
        iteratorRemove();

        section("6. 正确做法二：removeIf()（推荐）");
        removeIfDemo();

        section("7. 正确做法三：先收集再批量删");
        collectThenRemove();

        section("8. 为什么 HashMap 顺序看起来是乱的");
        mapOrdering();
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("===== " + title + " =====");
    }

    /**
     * ⚠️ 最反直觉的一组实测：<b>删哪个元素，决定抛不抛</b>。
     *
     * <p>下面 6 组是本机实跑的真实结果（不是猜的）：
     * <pre>
     * [a,b,c]   删 b(倒数第二) → 未抛，结果 [a, c]
     * [a,b,c]   删 a(首)       → 抛了
     * [a,b,c]   删 c(尾)       → 抛了
     * [a,b]     删 b(尾)       → 抛了
     * [a]       删 a(唯一)     → 抛了
     * [a,b,c,d] 删 b           → 抛了
     * </pre>
     *
     * <p><b>为什么这么不规律？</b>看 {@code ArrayList.Itr} 的判断顺序：
     * <pre>
     * hasNext():  return cursor != size;        // ① 先比进度和长度
     * next():     checkForComodification();     // ② 再查 modCount
     * </pre>
     * 删元素后 {@code size} 变小。只有恰好 {@code cursor == size} 时，
     * ① 才返回 {@code false} 直接结束循环，<b>永远走不到 ②</b>，所以不抛异常。
     *
     * <p>{@code [a,b,c]} 删 {@code b} 时：cursor 走到 2，删完 size 也变 2 → 相等 → 不抛。
     * 删 {@code c} 时：cursor 是 3，删完 size 变 2 → 不等 → 进 next() → 抛。
     *
     * <p><b>结论：抛不抛取决于「删完之后 cursor 正好等于 size 吗」，
     * 这是 ArrayList 内部实现细节，不是语言规范。</b>
     * 换个 JDK 版本、换个集合实现，行为都可能变。
     *
     * <p>所以：<b>绝不能靠"有没有抛异常"判断代码对不对。
     * 不抛 ≠ 做对了，只代表这次检查没被触发。</b>
     */
    private static void listRemoveInLoop() {
        // 6 组对照，同一份逻辑，只在删除位置不同
        String[][] cases = {
                {"a,b,c", "b"},
                {"a,b,c", "a"},
                {"a,b,c", "c"},
                {"a,b", "b"},
                {"a", "a"},
                {"a,b,c,d", "b"},
        };

        for (String[] cs : cases) {
            List<String> list = new ArrayList<>(List.of(cs[0].split(",")));
            String target = cs[1];
            boolean thrown = false;
            try {
                for (String s : list) {
                    if (target.equals(s)) {
                        list.remove(s);        // ❌ 每次都是错的做法
                    }
                }
            } catch (ConcurrentModificationException e) {
                thrown = true;
            }
            System.out.printf("  [%-7s] 删 %s → %s  %s%n",
                    cs[0], target,
                    thrown ? "抛了 CME  " : "未抛异常",
                    thrown ? "" : "← 运气，不是正确");
        }
    }

    /**
     * ⚠️ 反直觉的实测结果：HashMap 遍历中删掉<b>唯一</b>的键，不抛异常。
     *
     * <p>原因：迭代器的 {@code hasNext()} 判断依赖 {@code next != null}（或索引），
     * 删掉最后一个元素后再调 {@code hasNext()} 就直接返回 false，
     * <b>根本没机会检查 modCount</b>。
     *
     * <p>这说明「遍历中改集合」不一定抛异常——它只是<b>可能</b>抛。
     * 不抛的时候更危险，因为数据可能已经错了，而你毫无察觉。
     */
    private static void mapRemoveLastKey() {
        Map<String, String> map = new HashMap<>();
        map.put("onlyKey", "value");

        try {
            for (String key : map.keySet()) {
                map.remove(key);           // 遍历中删除唯一键
            }
            System.out.println("  ⚠️ 竟然没抛异常！剩余=" + map + "（size=" + map.size() + "）");
            System.out.println("  这就是 fail-fast 不可靠的证据：它只是「尽力检测」");
        } catch (ConcurrentModificationException e) {
            System.out.println("  抛了 ConcurrentModificationException");
        }
    }

    /** 删多个键时，中途会触发 modCount 检查，所以抛异常。 */
    private static void mapRemoveManyKeys() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        try {
            for (String key : map.keySet()) {
                map.remove(key);           // 第一次删完，下一次 hasNext 触发检查
            }
            System.out.println("  未抛异常，剩余=" + map);
        } catch (ConcurrentModificationException e) {
            System.out.println("  ✓ 多键删除时抛了异常（因为还有下一个元素要取）");
        }
    }

    /**
     * {@link ConcurrentHashMap} 的迭代器是<b>弱一致</b>的：
     * 不抛异常，但也不保证你能看到修改后的数据。
     *
     * <p>它解决的是并发场景的可见性问题，不是"让你随便在遍历中改"。
     */
    private static void concurrentMapRemove() {
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        try {
            for (String key : map.keySet()) {
                map.remove(key);
            }
            System.out.println("  ✓ 未抛异常，剩余=" + map + "（弱一致迭代器）");
            System.out.println("  注意：不抛 ≠ 结果正确，弱一致只保证「不会崩」");
        } catch (ConcurrentModificationException e) {
            System.out.println("  抛了（不应该发生）");
        }
    }

    /**
     * 正确做法一：用 {@link Iterator#remove()}。
     *
     * <p>迭代器自己知道"是我删的"，所以会同步更新 {@code modCount}，不报错。
     * <b>这是唯一允许的「遍历中删除」方式</b>（for-each 里拿不到迭代器，必须手写 while）。
     */
    private static void iteratorRemove() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c", "d"));
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if ("b".equals(s) || "d".equals(s)) {
                it.remove();               // ✅ 安全
            }
        }
        System.out.println("  ✅ Iterator.remove 结果=" + list + "（b、d 已删）");
    }

    /**
     * 正确做法二：{@code removeIf}（Java 8+，最推荐）。
     *
     * <p>内部就是用迭代器实现的，但写法更短、意图更清楚：「删掉满足条件的元素」。
     * {@code Map} 通过 {@code entrySet().removeIf(...)} 同样可用。
     */
    private static void removeIfDemo() {
        List<String> list = new ArrayList<>(List.of("a", "b", "c", "d"));
        list.removeIf(s -> "b".equals(s) || "d".equals(s));
        System.out.println("  ✅ List.removeIf 结果=" + list);

        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.entrySet().removeIf(e -> e.getValue() == 2);
        System.out.println("  ✅ Map.entrySet().removeIf 结果=" + map);
    }

    /**
     * 正确做法三：先收集待删的键，循环结束后批量删。
     *
     * <p>好处是逻辑分离：遍历只负责"判断"，修改只负责"执行"。
     * 坏处是多一个临时集合。数据量大时 removeIf 更好。
     */
    private static void collectThenRemove() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 2);

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            if (e.getValue() == 2) {
                toRemove.add(e.getKey());  // 只收集，不修改
            }
        }
        toRemove.forEach(map::remove);     // 循环结束后统一删除
        System.out.println("  ✅ 先收集后删 结果=" + map);
    }

    /** {@code HashMap} 的顺序由哈希值决定，看起来是乱的；{@code LinkedHashMap} 保持插入顺序。 */
    private static void mapOrdering() {
        String[] keys = {"E003", "E001", "E005", "E002", "E004"};

        Map<String, Integer> hash = new HashMap<>();
        Map<String, Integer> linked = new LinkedHashMap<>();
        for (int i = 0; i < keys.length; i++) {
            hash.put(keys[i], i);
            linked.put(keys[i], i);
        }

        System.out.println("  插入顺序:    " + java.util.Arrays.toString(keys));
        System.out.println("  HashMap:     " + hash.keySet() + "   ← 顺序不可预期");
        System.out.println("  LinkedHashMap: " + linked.keySet() + "   ← 保持插入顺序");
        System.out.println();
        System.out.println("  本项目的员工列表要按新增顺序展示，所以必须用 LinkedHashMap。");
    }
}