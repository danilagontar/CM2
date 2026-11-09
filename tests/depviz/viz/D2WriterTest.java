package depviz.viz;

import depviz.nuget.RepositoryException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты описания графа на языке D2.
 */
class D2WriterTest {

    private static final int NO_LIMIT = 100;

    @Test
    void writesNodesWithVersionsAndEdges() throws RepositoryException {
        String d2 = D2Writer.toD2(VizTestGraphs.build(Map.of("A", List.of("B", "C"), "B", List.of("C")), NO_LIMIT));
        assertTrue(d2.startsWith("direction: down"));
        assertTrue(d2.contains("\"B\": \"B 1.0.0\""));
        assertTrue(d2.contains("\"A\" -> \"B\""));
        assertTrue(d2.contains("\"B\" -> \"C\""));
        assertTrue(d2.contains("\"A\": \"A\" {style.fill"));
    }

    @Test
    void marksCyclesAndNotExpandedNodes() throws RepositoryException {
        String cycle = D2Writer.toD2(VizTestGraphs.build(Map.of("A", List.of("B"), "B", List.of("A")), NO_LIMIT));
        assertTrue(cycle.contains("\"B\" -> \"A\": цикл"));
        String deep = D2Writer.toD2(VizTestGraphs.build(Map.of("A", List.of("B"), "B", List.of("C")), 1));
        assertTrue(deep.contains("\"B\": \"B 1.0.0\" {style.stroke-dash"));
    }
}
