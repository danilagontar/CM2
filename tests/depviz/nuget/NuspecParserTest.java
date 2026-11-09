package depviz.nuget;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты разбора файлов .nuspec.
 */
class NuspecParserTest {

    static final String GROUPED = """
            <?xml version="1.0" encoding="utf-8"?>
            <package xmlns="http://schemas.microsoft.com/packaging/2013/05/nuspec.xsd">
              <metadata>
                <id>Demo</id>
                <version>1.0.0</version>
                <dependencies>
                  <group targetFramework="net6.0" />
                  <group targetFramework=".NETStandard1.0">
                    <dependency id="Microsoft.CSharp" version="4.3.0" exclude="Build,Analyzers" />
                    <dependency id="NETStandard.Library" version="[1.6.1, )" />
                  </group>
                </dependencies>
              </metadata>
            </package>
            """;

    private final NuspecParser parser = new NuspecParser();

    private List<DependencyGroup> parse(String xml) throws RepositoryException {
        return parser.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readsGroupsByFramework() throws RepositoryException {
        List<DependencyGroup> groups = parse(GROUPED);
        assertEquals(2, groups.size());
        assertEquals("net6.0", groups.get(0).targetFramework());
        assertTrue(groups.get(0).dependencies().isEmpty());
        assertEquals(List.of(new Dependency("Microsoft.CSharp", "4.3.0"),
                new Dependency("NETStandard.Library", "[1.6.1, )")), groups.get(1).dependencies());
    }

    @Test
    void readsOldFormatWithoutGroups() throws RepositoryException {
        List<DependencyGroup> groups = parse("""
                <package><metadata><dependencies>
                  <dependency id="Castle.Core" version="3.3.3" />
                </dependencies></metadata></package>
                """);
        assertEquals(List.of(new DependencyGroup("", List.of(new Dependency("Castle.Core", "3.3.3")))), groups);
    }

    @Test
    void packageWithoutDependencies() throws RepositoryException {
        assertTrue(parse("<package><metadata><id>Solo</id></metadata></package>").isEmpty());
    }

    @Test
    void rejectsInvalidXml() {
        assertThrows(RepositoryException.class, () -> parse("<package><metadata>"));
    }

    @Test
    void rejectsDoctype() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE p [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<package>&x;</package>";
        assertThrows(RepositoryException.class, () -> parse(xxe));
    }
}
