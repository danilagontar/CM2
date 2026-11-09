package depviz;

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

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    private int run(String... args) {
        PrintStream out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(errBytes, true, StandardCharsets.UTF_8);
        return Main.run(args, out, err);
    }

    @Test
    void printsAllParametersAsKeyValue() {
        int code = run("--package", "Newtonsoft.Json", "--package-version", "13.0.3", "--filter", "Test");
        String out = outBytes.toString(StandardCharsets.UTF_8);
        assertEquals(Main.EXIT_OK, code);
        for (String key : new String[] {"package", "package-version", "repo", "test-mode",
            "output", "ascii-tree", "max-depth", "filter"}) {
            assertTrue(out.contains("  " + key + " = "), "нет ключа " + key);
        }
        assertTrue(out.contains("filter = Test"));
    }

    @Test
    void printsHelp() {
        assertEquals(Main.EXIT_OK, run("--help"));
        assertTrue(outBytes.toString(StandardCharsets.UTF_8).contains("--max-depth"));
    }

    @Test
    void reportsErrorWithExitCode() {
        assertEquals(Main.EXIT_CONFIG_ERROR, run("--package", "X", "--max-depth", "-1"));
        assertTrue(errBytes.toString(StandardCharsets.UTF_8).startsWith("Ошибка:"));
    }
}
