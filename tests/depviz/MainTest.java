package depviz;

import depviz.nuget.FakeNuGetServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты запуска приложения целиком.
 */
class MainTest {

    private static final String NUSPEC = """
            <package><metadata><dependencies>
              <group targetFramework="net8.0">
                <dependency id="Serilog" version="4.0.0" />
              </group>
              <group targetFramework="net462" />
            </dependencies></metadata></package>
            """;

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    private int run(String... args) {
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        return Main.run(args, out, err);
    }

    @Test
    void printsDirectDependencies() throws Exception {
        try (FakeNuGetServer server = new FakeNuGetServer().add("/demo/2.1.0/demo.nuspec", NUSPEC)) {
            int code = run("--package", "Demo", "--package-version", "2.1.0", "--repo", server.baseUrl());
            String out = outBytes.toString(StandardCharsets.UTF_8);
            assertEquals(Main.EXIT_OK, code);
            assertTrue(out.contains("[net8.0]"));
            assertTrue(out.contains("    Serilog 4.0.0"));
            assertTrue(out.contains("[net462] нет зависимостей"));
        }
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
        assertTrue(outBytes.toString(StandardCharsets.UTF_8).contains("--max-depth"));
    }

    @Test
    void reportsConfigErrorWithExitCode() {
        assertEquals(Main.EXIT_CONFIG_ERROR, run("--package", "X", "--max-depth", "-1"));
        assertTrue(errBytes.toString(StandardCharsets.UTF_8).startsWith("Ошибка:"));
    }
}
