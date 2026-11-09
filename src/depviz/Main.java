package depviz;

import depviz.config.ArgsParser;
import depviz.config.Config;
import depviz.config.ConfigException;
import depviz.config.ConfigFactory;
import depviz.config.Option;
import depviz.graph.DependencyGraph;
import depviz.graph.GraphBuilder;
import depviz.graph.GraphFormatter;
import depviz.graph.PackageRef;
import depviz.graph.PackageSource;
import depviz.nuget.NuGetRepository;
import depviz.nuget.NuGetSource;
import depviz.nuget.RepositoryException;
import depviz.testrepo.TestRepository;

import java.io.PrintStream;
import java.nio.file.Path;
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
            DependencyGraph graph = buildGraph(config);
            out.print(GraphFormatter.adjacencyList(graph));
            out.println();
            out.print(GraphFormatter.loadOrder(graph));
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

    private static DependencyGraph buildGraph(Config config) throws RepositoryException {
        GraphBuilder builder = new GraphBuilder(createSource(config),
                config.analysis().maxDepth(), config.analysis().filter());
        return builder.build(new PackageRef(config.packageName(), config.version()));
    }

    private static PackageSource createSource(Config config) throws RepositoryException {
        String location = config.source().location();
        if (!config.source().testMode()) {
            return new NuGetSource(new NuGetRepository(location));
        }
        TestRepository repository = TestRepository.load(Path.of(location));
        if (!repository.contains(config.packageName())) {
            throw new RepositoryException("Пакет " + config.packageName() + " не описан в тестовом репозитории "
                    + location);
        }
        return repository;
    }
}
