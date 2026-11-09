package depviz;

import depviz.config.ArgsParser;
import depviz.config.Config;
import depviz.config.ConfigException;
import depviz.config.ConfigFactory;
import depviz.config.Option;

import java.io.PrintStream;
import java.util.Map;

/**
 * Точка входа CLI-приложения depviz.
 */
public final class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_CONFIG_ERROR = 2;

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Выполняет приложение; потоки вывода передаются явно, чтобы их можно было перехватить в тестах.
     *
     * @param args аргументы командной строки
     * @param out  поток для обычного вывода
     * @param err  поток для сообщений об ошибках
     * @return код завершения
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Map<Option, String> options = new ArgsParser().parse(args);
            if (options.containsKey(Option.HELP)) {
                out.print(Usage.text());
                return EXIT_OK;
            }
            Config config = new ConfigFactory().create(options);
            printConfig(config, out);
            return EXIT_OK;
        } catch (ConfigException e) {
            err.println("Ошибка: " + e.getMessage());
            err.println("Список параметров: ./run.sh --help");
            return EXIT_CONFIG_ERROR;
        }
    }

    private static void printConfig(Config config, PrintStream out) {
        out.println("Параметры запуска:");
        config.toKeyValue().forEach((key, value) -> out.println("  " + key + " = " + value));
    }
}
