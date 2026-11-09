package depviz.config;

/**
 * Проверенная конфигурация приложения.
 * Пустая строка в {@code version} или {@code filter} означает, что значение не задано.
 */
public record Config(String packageName, String version, RepositorySource source,
                     OutputSettings output, AnalysisSettings analysis) {

    /** Источник данных: URL репозитория или путь к файлу тестового репозитория. */
    public record RepositorySource(String location, boolean testMode) {
    }

    public record OutputSettings(String imageFile, boolean asciiTree) {
    }

    public record AnalysisSettings(int maxDepth, String filter) {
    }
}
