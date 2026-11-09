package depviz.graph;

import depviz.nuget.RepositoryException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты порядка загрузки.
 */
class LoadOrderTest {

    private static final int NO_LIMIT = 100;

    private static List<String> order(Map<String, List<String>> packages) throws RepositoryException {
        PackageSource source = pkg -> packages.getOrDefault(pkg.id(), List.of()).stream()
                .map(name -> new PackageRef(name, ""))
                .toList();
        DependencyGraph graph = new GraphBuilder(source, NO_LIMIT, "").build(new PackageRef("A", ""));
        return LoadOrder.of(graph).stream().map(PackageRef::id).toList();
    }

    @Test
    void dependenciesComeFirst() throws RepositoryException {
        assertEquals(List.of("D", "B", "C", "A"),
                order(Map.of("A", List.of("B", "C"), "B", List.of("D"), "C", List.of("D"))));
    }

    @Test
    void chainIsReversed() throws RepositoryException {
        assertEquals(List.of("C", "B", "A"), order(Map.of("A", List.of("B"), "B", List.of("C"))));
    }

    @Test
    void cycleStillGivesEveryPackageOnce() throws RepositoryException {
        List<String> order = order(Map.of("A", List.of("B"), "B", List.of("C"), "C", List.of("A", "D")));
        assertEquals(List.of("D", "C", "B", "A"), order);
    }

    @Test
    void formatterWarnsAboutCycles() throws RepositoryException {
        PackageSource source = pkg -> pkg.id().equals("A") ? List.of(new PackageRef("A", "")) : List.of();
        DependencyGraph graph = new GraphBuilder(source, NO_LIMIT, "").build(new PackageRef("A", ""));
        assertTrue(GraphFormatter.loadOrder(graph).contains("циклы"));
    }
}
