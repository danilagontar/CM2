package depviz.nuget;

/**
 * Прямая зависимость пакета. {@code versionRange} записан в синтаксисе NuGet: {@code 4.3.0} или {@code [1.0, 2.0)}.
 */
public record Dependency(String id, String versionRange) {
}
