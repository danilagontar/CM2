package depviz.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты проверки значений параметров.
 */
class ConfigFactoryTest {

    private final ConfigFactory factory = new ConfigFactory();

    @TempDir
    Path tempDir;

    private static Map<Option, String> nugetOptions() {
        Map<Option, String> options = new EnumMap<>(Option.class);
        options.put(Option.PACKAGE, "Newtonsoft.Json");
        options.put(Option.PACKAGE_VERSION, "13.0.3");
        return options;
    }

    private Map<Option, String> testModeOptions() throws IOException {
        Path repo = Files.writeString(tempDir.resolve("repo.txt"), "A: B\n");
        Map<Option, String> options = new EnumMap<>(Option.class);
        options.put(Option.PACKAGE, "A");
        options.put(Option.TEST_MODE, ArgsParser.FLAG_VALUE);
        options.put(Option.REPO, repo.toString());
        return options;
    }

    @Test
    void appliesDefaults() throws ConfigException {
        Config config = factory.create(nugetOptions());
        assertEquals(ConfigFactory.DEFAULT_REPOSITORY, config.source().location());
        assertFalse(config.source().testMode());
        assertEquals(ConfigFactory.DEFAULT_OUTPUT, config.output().imageFile());
        assertFalse(config.output().asciiTree());
        assertEquals(ConfigFactory.DEFAULT_MAX_DEPTH, config.analysis().maxDepth());
        assertEquals("", config.analysis().filter());
    }

    @Test
    void acceptsAllParameters() throws ConfigException {
        Map<Option, String> options = nugetOptions();
        options.put(Option.REPO, "https://example.org/nuget/");
        options.put(Option.OUTPUT, tempDir.resolve("out.svg").toString());
        options.put(Option.ASCII_TREE, ArgsParser.FLAG_VALUE);
        options.put(Option.MAX_DEPTH, "4");
        options.put(Option.FILTER, "Test");
        Config config = factory.create(options);
        assertEquals("https://example.org/nuget", config.source().location());
        assertTrue(config.output().asciiTree());
        assertEquals(4, config.analysis().maxDepth());
        assertEquals("Test", config.analysis().filter());
    }

    @Test
    void acceptsTestModeWithoutVersion() throws Exception {
        Config config = factory.create(testModeOptions());
        assertTrue(config.source().testMode());
        assertEquals("", config.version());
    }

    @Test
    void requiresPackage() {
        Map<Option, String> options = nugetOptions();
        options.remove(Option.PACKAGE);
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsInvalidPackageName() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.PACKAGE, "bad name!");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void testModeRequiresUppercaseName() throws Exception {
        Map<Option, String> options = testModeOptions();
        options.put(Option.PACKAGE, "Abc");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void requiresVersionOutsideTestMode() {
        Map<Option, String> options = nugetOptions();
        options.remove(Option.PACKAGE_VERSION);
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsInvalidVersion() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.PACKAGE_VERSION, "latest");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsNonHttpUrl() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.REPO, "ftp://example.org/nuget");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsMalformedUrl() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.REPO, "http://exa mple.org");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void testModeRequiresExistingFile() throws Exception {
        Map<Option, String> options = testModeOptions();
        options.put(Option.REPO, tempDir.resolve("missing.txt").toString());
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void testModeRequiresRepo() throws Exception {
        Map<Option, String> options = testModeOptions();
        options.remove(Option.REPO);
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsNonSvgOutput() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.OUTPUT, "graph.png");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsOutputInMissingDirectory() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.OUTPUT, tempDir.resolve("no/such/dir/graph.svg").toString());
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsNonNumericDepth() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.MAX_DEPTH, "deep");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsZeroDepth() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.MAX_DEPTH, "0");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }

    @Test
    void rejectsBlankFilter() {
        Map<Option, String> options = nugetOptions();
        options.put(Option.FILTER, " ");
        assertThrows(ConfigException.class, () -> factory.create(options));
    }
}
