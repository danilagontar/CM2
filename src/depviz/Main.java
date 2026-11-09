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
import depviz.viz.AsciiTree;
import depviz.viz.D2Writer;
import depviz.viz.RenderException;
import depviz.viz.SvgRenderer;

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
    static final int EXIT_RENDER_ERROR = 4;

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err, SvgRenderer.D2_COMMAND));
    }

    /**
     * Выполняет приложение и возвращает код завершения. Потоки вывода и команда утилиты d2
     * передаются явно, чтобы в тестах их можно было подменить.
     */
    static int run(String[] args, PrintStream out, PrintStream err, String d2Command) {
        try {
            Map<Option, String> options = new ArgsParser().parse(args);
            if (options.containsKey(Option.HELP)) {
                out.print(Usage.text());
                return EXIT_OK;
            }
            execute(new ConfigFactory().create(options), out, d2Command);
            return EXIT_OK;
        } catch (ConfigException e) {
            err.println("Ошибка: " + e.getMessage());
            err.println("Список параметров: ./run.sh --help");
            return EXIT_CONFIG_ERROR;
        } catch (RepositoryException e) {
            err.println("Ошибка: " + e.getMessage());
            return EXIT_REPOSITORY_ERROR;
        } catch (RenderException e) {
            err.println("Ошибка: " + e.getMessage());
            return EXIT_RENDER_ERROR;
        }
    }

    private static void execute(Config config, PrintStream out, String d2Command)
            throws RepositoryException, RenderException {
        DependencyGraph graph = buildGraph(config);
        out.print(config.output().asciiTree() ? AsciiTree.render(graph) : GraphFormatter.adjacencyList(graph));
        Path svg = Path.of(config.output().imageFile());
        Path d2 = new SvgRenderer(d2Command).render(D2Writer.toD2(graph), svg);
        out.println();
        out.println("Описание графа на D2: " + d2);
        out.println("Изображение графа: " + svg);
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
