package depviz.graph;

import depviz.nuget.RepositoryException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты построения графа на репозиториях, заданных прямо в коде.
 */
class GraphBuilderTest {

    private static final int NO_LIMIT = 100;

    private static PackageSource source(Map<String, List<String>> packages) {
        return pkg -> packages.getOrDefault(pkg.id(), List.of()).stream()
                .map(name -> new PackageRef(name, ""))
                .toList();
    }

    private static DependencyGraph build(Map<String, List<String>> packages, int depth, String filter)
            throws RepositoryException {
        return new GraphBuilder(source(packages), depth, filter).build(new PackageRef("A", ""));
    }

    private static List<String> ids(DependencyGraph graph) {
        return graph.nodes().stream().map(PackageRef::id).toList();
    }

    @Test
    void collectsTransitiveDependenciesOnce() throws RepositoryException {
        DependencyGraph graph = build(Map.of("A", List.of("B", "C"), "B", List.of("D"), "C", List.of("D")),
                NO_LIMIT, "");
        assertEquals(List.of("A", "B", "D", "C"), ids(graph));
        assertEquals(Set.of("D"), graph.dependencies("C"));
        assertTrue(graph.cycleEdges().isEmpty());
    }

    @Test
    void stopsAtMaxDepth() throws RepositoryException {
        DependencyGraph graph = build(Map.of("A", List.of("B"), "B", List.of("C"), "C", List.of("D")), 2, "");
        assertEquals(List.of("A", "B", "C"), ids(graph));
        assertTrue(graph.isExpanded("B"));
        assertFalse(graph.isExpanded("C"));
    }

    @Test
    void reexpandsPackageFoundCloserToRoot() throws RepositoryException {
        Map<String, List<String>> packages = Map.of(
                "A", List.of("B", "C"), "B", List.of("C"), "C", List.of("D"), "D", List.of("E"));
        DependencyGraph graph = build(packages, 3, "");
        assertTrue(graph.isExpanded("D"));
        assertTrue(ids(graph).contains("E"));
    }

    @Test
    void skipsFilteredPackagesAndTheirSubtrees() throws RepositoryException {
        DependencyGraph graph = build(Map.of("A", List.of("TESTKIT", "WEB"), "TESTKIT", List.of("MOCK")),
                NO_LIMIT, "test");
        assertEquals(List.of("A", "WEB"), ids(graph));
    }

    @Test
    void detectsCycles() throws RepositoryException {
        DependencyGraph graph = build(Map.of("A", List.of("B"), "B", List.of("C"), "C", List.of("A", "C")),
                NO_LIMIT, "");
        assertEquals(List.of("A", "B", "C"), ids(graph));
        assertEquals(Set.of(new DependencyGraph.Edge("C", "A"), new DependencyGraph.Edge("C", "C")),
                graph.cycleEdges());
    }
}
