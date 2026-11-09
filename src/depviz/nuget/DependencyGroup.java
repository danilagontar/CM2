package depviz.nuget;

import java.util.List;

/**
 * Зависимости пакета для одного целевого фреймворка.
 * Пустой {@code targetFramework} означает, что группа подходит для любого фреймворка.
 */
public record DependencyGroup(String targetFramework, List<Dependency> dependencies) {
}
