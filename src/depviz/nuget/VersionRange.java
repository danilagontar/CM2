package depviz.nuget;

import java.util.regex.Pattern;

/**
 * Выбор версии зависимости по диапазону из .nuspec.
 */
final class VersionRange {

    private static final int MIN_BRACKETED_LENGTH = 2;
    private static final char COMMA = ',';
    private static final Pattern VERSION = Pattern.compile("\\d+(\\.\\d+)*(-[0-9A-Za-z.-]+)?(\\+[0-9A-Za-z.-]+)?");

    private VersionRange() {
    }

    /**
     * NuGet по умолчанию берёт наименьшую подходящую версию, то есть нижнюю границу диапазона:
     * {@code 4.3.0 -> 4.3.0}, {@code [1.6.1, ) -> 1.6.1}, {@code [1.0] -> 1.0}.
     * Если нижней границы нет ({@code (, 2.0]}) или версия записана непонятно, возвращается пустая строка.
     */
    static String lowest(String range) {
        String trimmed = range.trim();
        String bound = trimmed;
        if (trimmed.startsWith("[") || trimmed.startsWith("(")) {
            if (trimmed.length() < MIN_BRACKETED_LENGTH) {
                return "";
            }
            String inner = trimmed.substring(1, trimmed.length() - 1);
            int comma = inner.indexOf(COMMA);
            bound = (comma < 0 ? inner : inner.substring(0, comma)).trim();
        }
        return VERSION.matcher(bound).matches() ? bound : "";
    }
}
