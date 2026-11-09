package depviz.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты разбора аргументов командной строки.
 */
class ArgsParserTest {

    private final ArgsParser parser = new ArgsParser();

    @Test
    void parsesValuesAndFlags() throws ConfigException {
        Map<Option, String> options = parser.parse(new String[] {
            "--package", "Newtonsoft.Json", "--max-depth=3", "--ascii-tree"});
        assertEquals("Newtonsoft.Json", options.get(Option.PACKAGE));
        assertEquals("3", options.get(Option.MAX_DEPTH));
        assertEquals(ArgsParser.FLAG_VALUE, options.get(Option.ASCII_TREE));
    }

    @Test
    void emptyArgsGiveEmptyMap() throws ConfigException {
        assertTrue(parser.parse(new String[0]).isEmpty());
    }

    @Test
    void rejectsUnknownOption() {
        ConfigException e = assertThrows(ConfigException.class,
                () -> parser.parse(new String[] {"--colour", "red"}));
        assertTrue(e.getMessage().contains("--colour"));
    }

    @Test
    void rejectsPositionalArgument() {
        assertThrows(ConfigException.class, () -> parser.parse(new String[] {"Newtonsoft.Json"}));
    }

    @Test
    void rejectsMissingValueAtEnd() {
        assertThrows(ConfigException.class, () -> parser.parse(new String[] {"--package"}));
    }

    @Test
    void rejectsMissingValueBeforeNextOption() {
        assertThrows(ConfigException.class,
                () -> parser.parse(new String[] {"--package", "--ascii-tree"}));
    }

    @Test
    void rejectsDuplicateOption() {
        assertThrows(ConfigException.class,
                () -> parser.parse(new String[] {"--filter", "a", "--filter", "b"}));
    }

    @Test
    void rejectsValueForFlag() {
        assertThrows(ConfigException.class, () -> parser.parse(new String[] {"--test-mode=yes"}));
    }
}
