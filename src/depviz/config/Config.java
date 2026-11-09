package depviz.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Проверенная конфигурация приложения.
 * Пустая строка в {@code version} или {@code filter} означает, что значение не задано.
 */
public record Config(String packageName, String version, RepositorySource source,
                     OutputSettings output, AnalysisSettings analysis) {

    private static final String NOT_SET = "(не задано)";

    /** Источник данных: URL репозитория или путь к файлу тестового репозитория. */
    public record RepositorySource(String location, boolean testMode) {
    }

    public record OutputSettings(String imageFile, boolean asciiTree) {
    }

    public record AnalysisSettings(int maxDepth, String filter) {
    }

    /**
     * Представляет все настраиваемые параметры в виде пар «ключ – значение».
     *
     * @return пары, ключи совпадают с именами параметров без «--»
     */
    public Map<String, String> toKeyValue() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("package", packageName);
        map.put("package-version", orNotSet(version));
        map.put("repo", source.location());
        map.put("test-mode", String.valueOf(source.testMode()));
        map.put("output", output.imageFile());
        map.put("ascii-tree", String.valueOf(output.asciiTree()));
        map.put("max-depth", String.valueOf(analysis.maxDepth()));
        map.put("filter", orNotSet(analysis.filter()));
        return map;
    }

    private static String orNotSet(String value) {
        return value.isEmpty() ? NOT_SET : value;
    }
}
