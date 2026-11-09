package depviz.viz;

import depviz.nuget.RepositoryException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты вывода ASCII-дерева.
 */
class AsciiTreeTest {

    private static final int NO_LIMIT = 100;
    private static final String NL = System.lineSeparator();

    @Test
    void drawsTreeAndMarksRepeats() throws RepositoryException {
        String tree = AsciiTree.render(VizTestGraphs.build(
                Map.of("A", List.of("B", "C"), "B", List.of("D"), "C", List.of("D")), NO_LIMIT));
        String expected = String.join(NL,
                "A",
                "├── B 1.0.0",
                "│   └── D",
                "└── C",
                "    └── D (*)",
                "(*) - уже показан выше, (цикл) - циклическая зависимость, "
                        + "(...) - не раскрыт из-за ограничения глубины") + NL;
        assertEquals(expected, tree);
    }

    @Test
    void marksCyclesAndDepthLimit() throws RepositoryException {
        String cycle = AsciiTree.render(VizTestGraphs.build(Map.of("A", List.of("B"), "B", List.of("A")), NO_LIMIT));
        assertTrue(cycle.contains("└── A (цикл)"));
        String deep = AsciiTree.render(VizTestGraphs.build(Map.of("A", List.of("B"), "B", List.of("C")), 1));
        assertTrue(deep.contains("└── B 1.0.0 (...)"));
    }

    @Test
    void noLegendForPlainTree() throws RepositoryException {
        assertEquals("A" + NL + "└── B 1.0.0" + NL,
                AsciiTree.render(VizTestGraphs.build(Map.of("A", List.of("B")), NO_LIMIT)));
    }
}
