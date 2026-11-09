package depviz.viz;

import depviz.graph.DependencyGraph;
import depviz.graph.PackageRef;

/**
 * Текстовое описание графа зависимостей на языке диаграмм D2 (https://d2lang.com).
 */
public final class D2Writer {

    private static final String NL = System.lineSeparator();
    private static final String ROOT_STYLE = " {style.fill: \"#dbeafe\"; style.bold: true}";
    private static final String NOT_EXPANDED_STYLE = " {style.stroke-dash: 4}";
    private static final String CYCLE_STYLE = ": цикл {style.stroke: \"#dc2626\"; style.stroke-dash: 3}";

    private D2Writer() {
    }

    /**
     * Узел – пакет с версией в подписи, ребро – зависимость. Корень выделен цветом, пакеты,
     * не раскрытые из-за глубины, нарисованы пунктиром, рёбра циклов – красным пунктиром.
     * Имена берутся в кавычки, потому что точка в D2 без кавычек означает вложенность.
     */
    public static String toD2(DependencyGraph graph) {
        StringBuilder sb = new StringBuilder("direction: down").append(NL).append(NL);
        for (PackageRef node : graph.nodes()) {
            sb.append(quote(node.id())).append(": ").append(quote(node.label()))
                    .append(nodeStyle(graph, node)).append(NL);
        }
        sb.append(NL);
        for (PackageRef node : graph.nodes()) {
            for (String dependency : graph.dependencies(node.id())) {
                boolean cycle = graph.cycleEdges().contains(new DependencyGraph.Edge(node.id(), dependency));
                sb.append(quote(node.id())).append(" -> ").append(quote(dependency))
                        .append(cycle ? CYCLE_STYLE : "").append(NL);
            }
        }
        return sb.toString();
    }

    private static String nodeStyle(DependencyGraph graph, PackageRef node) {
        if (node.equals(graph.root())) {
            return ROOT_STYLE;
        }
        return graph.isExpanded(node.id()) ? "" : NOT_EXPANDED_STYLE;
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
