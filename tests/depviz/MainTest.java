package depviz;

import depviz.nuget.FakeNuGetServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты запуска приложения целиком.
 */
class MainTest {

    private static final String DEMO_NUSPEC = """
            <package><metadata><dependencies>
              <group targetFramework="net8.0">
                <dependency id="Serilog" version="[4.0.0, )" />
              </group>
            </dependencies></metadata></package>
            """;
    private static final String SERILOG_NUSPEC = "<package><metadata></metadata></package>";

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    @TempDir
    Path tempDir;

    /**
     * Запускает приложение с картинкой во временном каталоге; вместо d2 вызывается команда {@code true},
     * чтобы тесты не зависели от установленной утилиты.
     */
    private int runWith(String d2Command, String... args) {
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        String[] all = Arrays.copyOf(args, args.length + 2);
        all[args.length] = "--output";
        all[args.length + 1] = tempDir.resolve("graph.svg").toString();
        return Main.run(all, out, err, d2Command);
    }

    private int run(String... args) {
        return runWith("true", args);
    }

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void printsTransitiveGraphFromNuGet() throws Exception {
        try (FakeNuGetServer server = new FakeNuGetServer()
                .add("/demo/2.1.0/demo.nuspec", DEMO_NUSPEC)
                .add("/serilog/4.0.0/serilog.nuspec", SERILOG_NUSPEC)) {
            int code = run("--package", "Demo", "--package-version", "2.1.0", "--repo", server.baseUrl());
            assertEquals(Main.EXIT_OK, code);
            assertTrue(out().contains("Demo 2.1.0 -> Serilog"));
            assertTrue(out().contains("Serilog 4.0.0 -> нет зависимостей"));
        }
    }

    @Test
    void printsGraphFromTestRepository() throws Exception {
        Path repo = Files.writeString(tempDir.resolve("repo.txt"), "A: B\nB: A C\n");
        assertEquals(Main.EXIT_OK, run("--test-mode", "--repo", repo.toString(), "--package", "A"));
        assertTrue(out().contains("B -> A, C"));
        assertTrue(out().contains("Циклические зависимости:"));
        assertTrue(Files.readString(tempDir.resolve("graph.d2")).contains("\"B\" -> \"A\": цикл"));
    }

    @Test
    void printsAsciiTreeWhenRequested() throws Exception {
        Path repo = Files.writeString(tempDir.resolve("repo.txt"), "A: B C\nB: C\n");
        assertEquals(Main.EXIT_OK, run("--test-mode", "--repo", repo.toString(), "--package", "A", "--ascii-tree"));
        assertTrue(out().contains("├── B"));
        assertTrue(out().contains("└── C (*)"));
    }

    @Test
    void reportsMissingD2() throws Exception {
        Path repo = Files.writeString(tempDir.resolve("repo.txt"), "A: B\n");
        int code = runWith("no-such-d2-command", "--test-mode", "--repo", repo.toString(), "--package", "A");
        assertEquals(Main.EXIT_RENDER_ERROR, code);
        assertTrue(errBytes.toString(StandardCharsets.UTF_8).contains("d2"));
        assertTrue(Files.exists(tempDir.resolve("graph.d2")));
    }

    @Test
    void reportsPackageMissingInTestRepository() throws Exception {
        Path repo = Files.writeString(tempDir.resolve("repo.txt"), "A: B\n");
        assertEquals(Main.EXIT_REPOSITORY_ERROR, run("--test-mode", "--repo", repo.toString(), "--package", "Z"));
    }

    @Test
    void reportsRepositoryError() throws Exception {
        try (FakeNuGetServer server = new FakeNuGetServer()) {
            int code = run("--package", "Demo", "--package-version", "9.9.9", "--repo", server.baseUrl());
            assertEquals(Main.EXIT_REPOSITORY_ERROR, code);
            assertTrue(errBytes.toString(StandardCharsets.UTF_8).contains("не найден"));
        }
    }

    @Test
    void printsHelp() {
        assertEquals(Main.EXIT_OK, run("--help"));
        assertTrue(out().contains("--max-depth"));
    }

    @Test
    void reportsConfigErrorWithExitCode() {
        assertEquals(Main.EXIT_CONFIG_ERROR, run("--package", "X", "--max-depth", "-1"));
        assertTrue(errBytes.toString(StandardCharsets.UTF_8).startsWith("Ошибка:"));
    }
}
