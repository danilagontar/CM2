package depviz.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Проверяет значения параметров, подставляет значения по умолчанию и создаёт {@link Config}.
 */
public final class ConfigFactory {

    /** Репозиторий NuGet по умолчанию (плоский контейнер API v3). */
    public static final String DEFAULT_REPOSITORY = "https://api.nuget.org/v3-flatcontainer";
    public static final String DEFAULT_OUTPUT = "graph.svg";
    public static final int DEFAULT_MAX_DEPTH = 10;

    private static final int MIN_DEPTH = 1;
    private static final String SVG_EXTENSION = ".svg";
    private static final String SLASH = "/";
    private static final Set<String> URL_SCHEMES = Set.of("http", "https");
    private static final Pattern NUGET_ID = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]*");
    private static final Pattern TEST_ID = Pattern.compile("[A-Z]+");
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+){1,3}(-[0-9A-Za-z.-]+)?");

    /**
     * Проверяет все параметры и собирает из них конфигурацию.
     */
    public Config create(Map<Option, String> options) throws ConfigException {
        boolean testMode = options.containsKey(Option.TEST_MODE);
        String packageName = validatePackage(options.get(Option.PACKAGE), testMode);
        String version = validateVersion(options.get(Option.PACKAGE_VERSION), testMode);
        String repository = validateRepository(options.get(Option.REPO), testMode);
        String output = validateOutput(options.getOrDefault(Option.OUTPUT, DEFAULT_OUTPUT));
        int maxDepth = validateDepth(options.get(Option.MAX_DEPTH));
        String filter = validateFilter(options.get(Option.FILTER));
        return new Config(packageName, version,
                new Config.RepositorySource(repository, testMode),
                new Config.OutputSettings(output, options.containsKey(Option.ASCII_TREE)),
                new Config.AnalysisSettings(maxDepth, filter));
    }

    /**
     * В тестовом режиме пакеты называются заглавными латинскими буквами, в обычном – по правилам NuGet.
     */
    private String validatePackage(String value, boolean testMode) throws ConfigException {
        if (value == null || value.isBlank()) {
            throw new ConfigException("Не задано имя пакета: укажите --package <имя>");
        }
        if (testMode && !TEST_ID.matcher(value).matches()) {
            throw new ConfigException("В тестовом режиме имя пакета должно состоять из заглавных латинских букв: '"
                    + value + "'");
        }
        if (!NUGET_ID.matcher(value).matches()) {
            throw new ConfigException("Некорректное имя пакета NuGet: '" + value
                    + "' (допустимы латинские буквы, цифры, '.', '-', '_')");
        }
        return value;
    }

    /**
     * Версия обязательна только при работе с настоящим репозиторием.
     */
    private String validateVersion(String value, boolean testMode) throws ConfigException {
        if (value == null) {
            if (testMode) {
                return "";
            }
            throw new ConfigException("Не задана версия пакета: укажите --package-version <версия>, например 13.0.3");
        }
        if (!VERSION.matcher(value).matches()) {
            throw new ConfigException("Некорректная версия пакета: '" + value
                    + "' (ожидается формат 1.2, 1.2.3, 1.2.3.4 или 1.2.3-beta)");
        }
        return value;
    }

    private String validateRepository(String value, boolean testMode) throws ConfigException {
        if (testMode) {
            return validateTestFile(value);
        }
        return value == null ? DEFAULT_REPOSITORY : validateUrl(value);
    }

    private String validateTestFile(String value) throws ConfigException {
        if (value == null || value.isBlank()) {
            throw new ConfigException("В тестовом режиме нужно указать путь к файлу репозитория: --repo <путь>");
        }
        Path path;
        try {
            path = Path.of(value);
        } catch (InvalidPathException e) {
            throw new ConfigException("Некорректный путь к файлу тестового репозитория: '" + value + "'");
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new ConfigException("Файл тестового репозитория не найден или недоступен для чтения: '"
                    + value + "'");
        }
        return value;
    }

    /**
     * Принимает только http/https с хостом; завершающий «/» убирается, чтобы потом склеивать пути.
     */
    private String validateUrl(String value) throws ConfigException {
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new ConfigException("Некорректный URL репозитория: '" + value + "'");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!URL_SCHEMES.contains(scheme) || uri.getHost() == null) {
            throw new ConfigException("URL репозитория должен начинаться с http:// или https:// и содержать хост: '"
                    + value + "'. Для локального файла используйте --test-mode");
        }
        return value.endsWith(SLASH) ? value.substring(0, value.length() - 1) : value;
    }

    private String validateOutput(String value) throws ConfigException {
        if (value.isBlank()) {
            throw new ConfigException("Имя файла изображения не может быть пустым");
        }
        if (!value.toLowerCase(Locale.ROOT).endsWith(SVG_EXTENSION)) {
            throw new ConfigException("Файл изображения должен иметь расширение .svg: '" + value + "'");
        }
        Path parent;
        try {
            parent = Path.of(value).toAbsolutePath().getParent();
        } catch (InvalidPathException e) {
            throw new ConfigException("Некорректное имя файла изображения: '" + value + "'");
        }
        if (parent == null || !Files.isDirectory(parent)) {
            throw new ConfigException("Каталог для файла изображения не существует: '" + value + "'");
        }
        return value;
    }

    private int validateDepth(String value) throws ConfigException {
        if (value == null) {
            return DEFAULT_MAX_DEPTH;
        }
        int depth;
        try {
            depth = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigException("Максимальная глубина должна быть целым числом: '" + value + "'");
        }
        if (depth < MIN_DEPTH) {
            throw new ConfigException("Максимальная глубина должна быть не меньше " + MIN_DEPTH + ": " + depth);
        }
        return depth;
    }

    private String validateFilter(String value) throws ConfigException {
        if (value == null) {
            return "";
        }
        if (value.isBlank()) {
            throw new ConfigException("Подстрока фильтра не может быть пустой");
        }
        return value;
    }
}
