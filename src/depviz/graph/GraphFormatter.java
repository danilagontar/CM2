package depviz.graph;

import java.util.Set;

/**
 * Текстовое представление графа зависимостей для вывода в консоль.
 */
public final class GraphFormatter {

    private static final String NL = System.lineSeparator();

    private GraphFormatter() {
    }

    /**
     * Список смежности: для каждого пакета – его прямые зависимости в графе, затем найденные циклы.
     */
    public static String adjacencyList(DependencyGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("Граф зависимостей пакета ").append(graph.root().label())
                .append(" (пакетов: ").append(graph.nodes().size()).append("):").append(NL);
        for (PackageRef node : graph.nodes()) {
            sb.append("  ").append(node.label()).append(" -> ").append(describe(graph, node.id())).append(NL);
        }
        if (!graph.cycleEdges().isEmpty()) {
            sb.append("Циклические зависимости:").append(NL);
            for (DependencyGraph.Edge edge : graph.cycleEdges()) {
                sb.append("  ").append(edge.from()).append(" -> ").append(edge.to()).append(NL);
            }
        }
        return sb.toString();
    }

    /**
     * Нумерованный порядок загрузки; при циклах добавляется предупреждение, что порядок для них условный.
     */
    public static String loadOrder(DependencyGraph graph) {
        StringBuilder sb = new StringBuilder("Порядок загрузки (сначала зависимости, затем зависящие от них):")
                .append(NL);
        int number = 1;
        for (PackageRef pkg : LoadOrder.of(graph)) {
            sb.append(String.format("  %3d. %s%n", number++, pkg.label()));
        }
        if (!graph.cycleEdges().isEmpty()) {
            sb.append("Внимание: в графе есть циклы, для пакетов из них правильного порядка не существует.").append(NL);
        }
        return sb.toString();
    }

    private static String describe(DependencyGraph graph, String id) {
        if (!graph.isExpanded(id)) {
            return "(не раскрыт: достигнута максимальная глубина)";
        }
        Set<String> dependencies = graph.dependencies(id);
        return dependencies.isEmpty() ? "нет зависимостей" : String.join(", ", dependencies);
    }
}
