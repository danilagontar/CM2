package depviz.config;

import java.util.Arrays;
import java.util.Optional;

/**
 * Параметры командной строки, которые поддерживает приложение.
 */
public enum Option {

    PACKAGE("--package", true, "<имя>", "имя анализируемого пакета (обязательный)"),
    REPO("--repo", true, "<url|путь>", "URL репозитория NuGet или путь к файлу тестового репозитория"),
    TEST_MODE("--test-mode", false, "", "режим работы с тестовым репозиторием (файлом)"),
    PACKAGE_VERSION("--package-version", true, "<версия>", "версия пакета (обязательна вне тестового режима)"),
    OUTPUT("--output", true, "<файл.svg>", "имя файла с изображением графа (по умолчанию graph.svg)"),
    ASCII_TREE("--ascii-tree", false, "", "вывести зависимости в виде ASCII-дерева"),
    MAX_DEPTH("--max-depth", true, "<n>", "максимальная глубина анализа, целое >= 1 (по умолчанию 10)"),
    FILTER("--filter", true, "<подстрока>", "не учитывать пакеты, имя которых содержит подстроку"),
    HELP("--help", false, "", "показать справку и выйти");

    private final String cliName;
    private final boolean takesValue;
    private final String valueHint;
    private final String description;

    Option(String cliName, boolean takesValue, String valueHint, String description) {
        this.cliName = cliName;
        this.takesValue = takesValue;
        this.valueHint = valueHint;
        this.description = description;
    }

    public static Optional<Option> byName(String name) {
        return Arrays.stream(values()).filter(o -> o.cliName.equals(name)).findFirst();
    }

    public String cliName() {
        return cliName;
    }

    public boolean takesValue() {
        return takesValue;
    }

    public String valueHint() {
        return valueHint;
    }

    public String description() {
        return description;
    }
}
