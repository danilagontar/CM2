package depviz.viz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Тесты построения SVG; тест с настоящей утилитой d2 пропускается, если она не установлена.
 */
class SvgRendererTest {

    @TempDir
    Path tempDir;

    private static boolean d2Installed() {
        try {
            return new ProcessBuilder(SvgRenderer.D2_COMMAND, "--version").start().waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Test
    void d2FileLiesNextToSvg() {
        assertEquals(Path.of("out", "deps.d2"), SvgRenderer.d2PathFor(Path.of("out", "deps.svg")));
        assertEquals(Path.of("deps.d2"), SvgRenderer.d2PathFor(Path.of("deps.SVG")));
    }

    @Test
    void reportsMissingUtility() {
        assertThrows(RenderException.class,
                () -> new SvgRenderer("no-such-d2-command").render("a -> b", tempDir.resolve("g.svg")));
    }

    @Test
    void reportsD2SyntaxError() {
        assumeTrue(d2Installed(), "утилита d2 не установлена");
        assertThrows(RenderException.class,
                () -> new SvgRenderer(SvgRenderer.D2_COMMAND).render("a -> {", tempDir.resolve("g.svg")));
    }

    @Test
    void createsSvgWithRealD2() throws Exception {
        assumeTrue(d2Installed(), "утилита d2 не установлена");
        Path svg = tempDir.resolve("g.svg");
        new SvgRenderer(SvgRenderer.D2_COMMAND).render("\"A\" -> \"B.C\"", svg);
        assertTrue(Files.readString(svg).contains("<svg"));
    }
}
