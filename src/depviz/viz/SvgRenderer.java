package depviz.viz;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Сохраняет описание графа в файл .d2 и строит по нему SVG утилитой d2.
 */
public final class SvgRenderer {

    /** Команда утилиты d2 по умолчанию. */
    public static final String D2_COMMAND = "d2";

    private static final String SVG_EXTENSION = ".svg";
    private static final String D2_EXTENSION = ".d2";
    private static final int EXIT_OK = 0;

    private final String command;

    /**
     * В тестах вместо настоящей утилиты можно передать другую команду.
     */
    public SvgRenderer(String command) {
        this.command = command;
    }

    /**
     * Пишет D2 рядом с SVG ({@code graph.svg -> graph.d2}), запускает {@code d2 graph.d2 graph.svg}
     * и возвращает путь к файлу .d2.
     */
    public Path render(String d2, Path svg) throws RenderException {
        Path d2File = d2PathFor(svg);
        try {
            Files.writeString(d2File, d2);
        } catch (IOException e) {
            throw new RenderException("Не удалось сохранить описание графа в " + d2File + ": " + e.getMessage());
        }
        runD2(d2File, svg);
        return d2File;
    }

    static Path d2PathFor(Path svg) {
        String name = svg.getFileName().toString();
        boolean hasSvgExtension = name.toLowerCase(Locale.ROOT).endsWith(SVG_EXTENSION);
        String base = hasSvgExtension ? name.substring(0, name.length() - SVG_EXTENSION.length()) : name;
        return svg.resolveSibling(base + D2_EXTENSION);
    }

    private void runD2(Path d2File, Path svg) throws RenderException {
        Process process;
        try {
            process = new ProcessBuilder(command, d2File.toString(), svg.toString())
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new RenderException("Не удалось запустить утилиту '" + command
                    + "'. Установите D2 (https://d2lang.com, на macOS: brew install d2). Описание графа сохранено в "
                    + d2File);
        }
        try {
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.waitFor() != EXIT_OK) {
                throw new RenderException("Утилита d2 завершилась с ошибкой: " + output.trim());
            }
        } catch (IOException e) {
            throw new RenderException("Ошибка при работе утилиты d2: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RenderException("Построение изображения прервано");
        }
    }
}
