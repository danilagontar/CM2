package depviz;

import depviz.config.Option;

/**
 * Текст справки по параметрам командной строки.
 */
final class Usage {

    private static final String FORMAT = "  %-32s %s%n";

    private Usage() {
    }

    static String text() {
        StringBuilder sb = new StringBuilder();
        sb.append("depviz – визуализатор графа зависимостей пакетов NuGet").append(System.lineSeparator());
        sb.append("Использование: ./run.sh --package <имя> --package-version <версия> [параметры]")
                .append(System.lineSeparator());
        sb.append("Параметры:").append(System.lineSeparator());
        for (Option option : Option.values()) {
            String name = (option.cliName() + " " + option.valueHint()).trim();
            sb.append(String.format(FORMAT, name, option.description()));
        }
        return sb.toString();
    }
}
