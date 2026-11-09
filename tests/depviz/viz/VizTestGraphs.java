package depviz.viz;

import depviz.graph.DependencyGraph;
import depviz.graph.GraphBuilder;
import depviz.graph.PackageRef;
import depviz.graph.PackageSource;
import depviz.nuget.RepositoryException;

import java.util.List;
import java.util.Map;

/**
 * Построение небольших графов для тестов визуализации.
 */
final class VizTestGraphs {

    private VizTestGraphs() {
    }

    static DependencyGraph build(Map<String, List<String>> packages, int maxDepth) throws RepositoryException {
        PackageSource source = pkg -> packages.getOrDefault(pkg.id(), List.of()).stream()
                .map(name -> new PackageRef(name, name.equals("B") ? "1.0.0" : ""))
                .toList();
        return new GraphBuilder(source, maxDepth, "").build(new PackageRef("A", ""));
    }
}
