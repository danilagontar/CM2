package depviz;

import depviz.config.ArgsParser;
import depviz.config.Config;
import depviz.config.ConfigException;
import depviz.config.ConfigFactory;
import depviz.config.Option;
import depviz.nuget.Dependency;
import depviz.nuget.DependencyGroup;
import depviz.nuget.NuGetRepository;
import depviz.nuget.RepositoryException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Точка входа CLI-приложения depviz.
 */
public final class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_CONFIG_ERROR = 2;
    static final int EXIT_REPOSITORY_ERROR = 3;

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Выполняет приложение и возвращает код завершения.
     * Потоки вывода передаются явно, чтобы их можно было перехватить в тестах.
     */
    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Map<Option, String> options = new ArgsParser().parse(args);
            if (options.containsKey(Option.HELP)) {
                out.print(Usage.text());
                return EXIT_OK;
            }
            Config config = new ConfigFactory().create(options);
            printDirectDependencies(config, out);
            return EXIT_OK;
        } catch (ConfigException e) {
            err.println("Ошибка: " + e.getMessage());
            err.println("Список параметров: ./run.sh --help");
            return EXIT_CONFIG_ERROR;
        } catch (RepositoryException e) {
            err.println("Ошибка: " + e.getMessage());
            return EXIT_REPOSITORY_ERROR;
        }
    }

    private static void printDirectDependencies(Config config, PrintStream out) throws RepositoryException {
        if (config.source().testMode()) {
            throw new RepositoryException("работа с тестовым репозиторием пока не реализована");
        }
        NuGetRepository repository = new NuGetRepository(config.source().location());
        List<DependencyGroup> groups = repository.directDependencies(config.packageName(), config.version());
        out.println("Прямые зависимости пакета " + config.packageName() + " " + config.version() + ":");
        if (groups.isEmpty()) {
            out.println("  нет зависимостей");
        }
        for (DependencyGroup group : groups) {
            printGroup(group, out);
        }
    }

    private static void printGroup(DependencyGroup group, PrintStream out) {
        String framework = group.targetFramework().isEmpty() ? "любой фреймворк" : group.targetFramework();
        out.println("  [" + framework + "]" + (group.dependencies().isEmpty() ? " нет зависимостей" : ""));
        for (Dependency dependency : group.dependencies()) {
            out.println("    " + dependency.id() + " " + dependency.versionRange());
        }
    }
}
