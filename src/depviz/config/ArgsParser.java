package depviz.config;

import java.util.EnumMap;
import java.util.Map;

/**
 * Разбирает аргументы командной строки в набор «параметр – значение» без проверки смысла значений.
 */
public final class ArgsParser {

    /** Значение, которое записывается для флагов (параметров без значения). */
    public static final String FLAG_VALUE = "true";

    private static final String PREFIX = "--";
    private static final char ASSIGN = '=';
    private static final int NOT_FOUND = -1;

    /**
     * Разбирает аргументы вида {@code --имя значение} и {@code --имя=значение}.
     *
     * @param args аргументы, переданные в {@code main}
     * @return значения параметров
     * @throws ConfigException если параметр неизвестен, повторяется или у него нет значения
     */
    public Map<Option, String> parse(String[] args) throws ConfigException {
        Map<Option, String> result = new EnumMap<>(Option.class);
        int index = 0;
        while (index < args.length) {
            index = parseOne(args, index, result);
        }
        return result;
    }

    /**
     * Разбирает один параметр и возвращает позицию следующего неразобранного аргумента.
     */
    private int parseOne(String[] args, int index, Map<Option, String> result) throws ConfigException {
        String token = args[index];
        if (!token.startsWith(PREFIX)) {
            throw new ConfigException("Неожиданный аргумент '" + token
                    + "': параметры задаются в виде --имя значение");
        }
        int assignPos = token.indexOf(ASSIGN);
        String name = assignPos == NOT_FOUND ? token : token.substring(0, assignPos);
        Option option = Option.byName(name).orElseThrow(
                () -> new ConfigException("Неизвестный параметр '" + name + "'"));
        if (result.containsKey(option)) {
            throw new ConfigException("Параметр " + name + " указан несколько раз");
        }
        if (assignPos != NOT_FOUND) {
            result.put(option, inlineValue(option, token.substring(assignPos + 1)));
            return index + 1;
        }
        if (!option.takesValue()) {
            result.put(option, FLAG_VALUE);
            return index + 1;
        }
        result.put(option, separateValue(args, index + 1, option));
        return index + 2;
    }

    private String inlineValue(Option option, String value) throws ConfigException {
        if (!option.takesValue()) {
            throw new ConfigException("Параметр " + option.cliName() + " не принимает значения");
        }
        return value;
    }

    private String separateValue(String[] args, int index, Option option) throws ConfigException {
        if (index >= args.length || args[index].startsWith(PREFIX)) {
            throw new ConfigException("Для параметра " + option.cliName() + " не указано значение "
                    + option.valueHint());
        }
        return args[index];
    }
}
