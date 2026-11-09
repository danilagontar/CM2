package depviz.graph;

import depviz.nuget.RepositoryException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Строит граф зависимостей с учётом транзитивности рекурсивным обходом в глубину (DFS).
 */
public final class GraphBuilder {

    private final PackageSource source;
    private final int maxDepth;
    private final String filter;

    /**
     * Пакеты, имя которых содержит {@code filter} (без учёта регистра), в граф не попадают;
     * пустой фильтр ничего не исключает. Корневой пакет не фильтруется.
     */
    public GraphBuilder(PackageSource source, int maxDepth, String filter) {
        this.source = source;
        this.maxDepth = maxDepth;
        this.filter = filter.toLowerCase(Locale.ROOT);
    }

    public DependencyGraph build(PackageRef root) throws RepositoryException {
        DependencyGraph graph = new DependencyGraph(root);
        visit(root, 0, graph, new HashSet<>());
        return graph;
    }

    /**
     * Зависимости пакета читаются, только если его глубина меньше максимальной.
     * Пакет, уже раскрытый на такой же или меньшей глубине, повторно не обходится. Если же он найден
     * ближе к корню, чем раньше, обходится заново – иначе потерялись бы зависимости, обрезанные глубиной.
     * {@code path} – пакеты на текущем пути от корня: ребро в такой пакет замыкает цикл, по нему не идём.
     */
    private void visit(PackageRef pkg, int depth, DependencyGraph graph, Set<String> path)
            throws RepositoryException {
        Integer seenAt = graph.expandedDepth(pkg.id());
        if (depth >= maxDepth || seenAt != null && seenAt <= depth) {
            return;
        }
        graph.markExpanded(pkg.id(), depth);
        path.add(pkg.id());
        for (PackageRef dependency : source.dependencies(pkg)) {
            if (isExcluded(dependency)) {
                continue;
            }
            PackageRef node = graph.addEdge(pkg.id(), dependency);
            if (path.contains(node.id())) {
                graph.addCycleEdge(pkg.id(), node.id());
            } else {
                visit(node, depth + 1, graph, path);
            }
        }
        path.remove(pkg.id());
    }

    private boolean isExcluded(PackageRef pkg) {
        return !filter.isEmpty() && pkg.id().toLowerCase(Locale.ROOT).contains(filter);
    }
}
