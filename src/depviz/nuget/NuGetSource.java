package depviz.nuget;

import depviz.graph.PackageRef;
import depviz.graph.PackageSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Источник зависимостей из настоящего репозитория NuGet.
 */
public final class NuGetSource implements PackageSource {

    private final NuGetRepository repository;
    private final Map<PackageRef, List<PackageRef>> cache = new HashMap<>();

    public NuGetSource(NuGetRepository repository) {
        this.repository = repository;
    }

    /**
     * Объединяет зависимости всех целевых фреймворков (без повторов) и для каждой берёт
     * нижнюю границу диапазона версий. Пакет без конкретной версии не раскрывается.
     */
    @Override
    public List<PackageRef> dependencies(PackageRef pkg) throws RepositoryException {
        if (pkg.version().isEmpty()) {
            return List.of();
        }
        List<PackageRef> result = cache.get(pkg);
        if (result == null) {
            result = load(pkg);
            cache.put(pkg, result);
        }
        return result;
    }

    private List<PackageRef> load(PackageRef pkg) throws RepositoryException {
        Map<String, PackageRef> unique = new LinkedHashMap<>();
        for (DependencyGroup group : repository.directDependencies(pkg.id(), pkg.version())) {
            for (Dependency dependency : group.dependencies()) {
                unique.putIfAbsent(dependency.id().toLowerCase(Locale.ROOT),
                        new PackageRef(dependency.id(), VersionRange.lowest(dependency.versionRange())));
            }
        }
        return List.copyOf(unique.values());
    }
}
