package depviz.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Порядок загрузки пакетов: каждый пакет идёт после всех своих зависимостей.
 */
public final class LoadOrder {

    private LoadOrder() {
    }

    /**
     * Топологическая сортировка обходом в глубину: пакет добавляется в список, когда обойдены
     * все его зависимости, поэтому корневой пакет оказывается последним.
     * Ребро, замыкающее цикл, ведёт в уже посещённый пакет и пропускается – иначе порядка не существует,
     * поэтому для пакетов из цикла порядок условный.
     */
    public static List<PackageRef> of(DependencyGraph graph) {
        List<PackageRef> order = new ArrayList<>();
        visit(graph, graph.root().id(), new HashSet<>(), order);
        return order;
    }

    private static void visit(DependencyGraph graph, String id, Set<String> visited, List<PackageRef> order) {
        visited.add(id);
        for (String dependency : graph.dependencies(id)) {
            if (!visited.contains(dependency)) {
                visit(graph, dependency, visited, order);
            }
        }
        order.add(graph.node(id));
    }
}
