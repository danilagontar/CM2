package depviz.viz;

import depviz.graph.DependencyGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Вывод графа зависимостей в виде ASCII-дерева.
 */
public final class AsciiTree {

    private static final String NL = System.lineSeparator();
    private static final String BRANCH = "├── ";
    private static final String LAST_BRANCH = "└── ";
    private static final String PIPE = "│   ";
    private static final String SPACE = "    ";
    private static final String REPEATED = " (*)";
    private static final String CYCLE = " (цикл)";
    private static final String NOT_EXPANDED = " (...)";
    private static final String LEGEND = "(*) - уже показан выше, (цикл) - циклическая зависимость, "
            + "(...) - не раскрыт из-за ограничения глубины";

    private AsciiTree() {
    }

    /** Состояние обхода: что уже показано и какие пакеты лежат на пути от корня. */
    private record Walk(DependencyGraph graph, Set<String> shown, Set<String> path, StringBuilder out) {
    }

    /**
     * Граф – не дерево: один пакет может быть нужен нескольким. Поэтому поддерево пакета
     * раскрывается только при первом появлении, а повторы, циклы и пакеты на границе глубины
     * помечаются, и под деревом печатается расшифровка пометок.
     */
    public static String render(DependencyGraph graph) {
        String rootId = graph.root().id();
        Walk walk = new Walk(graph, new HashSet<>(Set.of(rootId)), new HashSet<>(Set.of(rootId)),
                new StringBuilder(graph.root().label()).append(NL));
        appendChildren(walk, rootId, "");
        String tree = walk.out().toString();
        boolean hasMarks = tree.contains(REPEATED) || tree.contains(CYCLE) || tree.contains(NOT_EXPANDED);
        return hasMarks ? tree + LEGEND + NL : tree;
    }

    private static void appendChildren(Walk walk, String id, String prefix) {
        List<String> children = new ArrayList<>(walk.graph().dependencies(id));
        for (int i = 0; i < children.size(); i++) {
            boolean last = i == children.size() - 1;
            String child = children.get(i);
            String mark = markOf(walk, child);
            walk.out().append(prefix).append(last ? LAST_BRANCH : BRANCH)
                    .append(walk.graph().node(child).label()).append(mark).append(NL);
            if (mark.isEmpty()) {
                walk.shown().add(child);
                walk.path().add(child);
                appendChildren(walk, child, prefix + (last ? SPACE : PIPE));
                walk.path().remove(child);
            }
        }
    }

    private static String markOf(Walk walk, String id) {
        if (walk.path().contains(id)) {
            return CYCLE;
        }
        if (walk.shown().contains(id)) {
            return REPEATED;
        }
        return walk.graph().isExpanded(id) ? "" : NOT_EXPANDED;
    }
}
