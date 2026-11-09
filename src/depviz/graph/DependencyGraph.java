package depviz.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Граф зависимостей: пакеты (по одной версии на имя) и рёбра «пакет – его зависимость».
 */
public final class DependencyGraph {

    /** Ребро графа, которое замыкает цикл. */
    public record Edge(String from, String to) {
    }

    private final PackageRef root;
    private final Map<String, PackageRef> nodes = new LinkedHashMap<>();
    private final Map<String, Set<String>> edges = new LinkedHashMap<>();
    private final Set<Edge> cycleEdges = new LinkedHashSet<>();
    private final Map<String, Integer> expandedAt = new HashMap<>();

    public DependencyGraph(PackageRef root) {
        this.root = root;
        addNode(root);
    }

    public PackageRef root() {
        return root;
    }

    public Collection<PackageRef> nodes() {
        return nodes.values();
    }

    public Set<String> dependencies(String id) {
        return edges.get(id);
    }

    public Set<Edge> cycleEdges() {
        return cycleEdges;
    }

    /**
     * Были ли прочитаны зависимости пакета. Нераскрытым пакет остаётся, если до него дошли на максимальной глубине.
     */
    public boolean isExpanded(String id) {
        return expandedAt.containsKey(id);
    }

    /**
     * Добавляет ребро и возвращает пакет, уже лежащий в графе под этим именем (если он там есть),
     * чтобы у каждого имени была одна версия – та, что встретилась первой.
     */
    PackageRef addEdge(String from, PackageRef to) {
        PackageRef node = addNode(to);
        edges.get(from).add(node.id());
        return node;
    }

    void addCycleEdge(String from, String to) {
        cycleEdges.add(new Edge(from, to));
    }

    Integer expandedDepth(String id) {
        return expandedAt.get(id);
    }

    void markExpanded(String id, int depth) {
        expandedAt.put(id, depth);
    }

    private PackageRef addNode(PackageRef pkg) {
        edges.computeIfAbsent(pkg.id(), k -> new LinkedHashSet<>());
        return nodes.computeIfAbsent(pkg.id(), k -> pkg);
    }
}
