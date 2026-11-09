package depviz.graph;

import depviz.nuget.RepositoryException;

import java.util.List;

/**
 * Откуда берутся прямые зависимости пакета: настоящий репозиторий NuGet или тестовый файл.
 */
@FunctionalInterface
public interface PackageSource {

    List<PackageRef> dependencies(PackageRef pkg) throws RepositoryException;
}
