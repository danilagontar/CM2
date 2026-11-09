package depviz;

import depviz.nuget.FakeNuGetServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    private int run(String... args) {
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        return Main.run(args, out, err);
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
        assertTrue(out().contains("Порядок загрузки"));
        assertTrue(out().contains("3. A"));
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
