package depviz.nuget;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты загрузки зависимостей; вместо nuget.org используется локальный сервер.
 */
class NuGetRepositoryTest {

    @Test
    void buildsLowercaseNuspecUri() {
        assertEquals(URI.create("https://repo/newtonsoft.json/13.0.3/newtonsoft.json.nuspec"),
                NuGetRepository.nuspecUri("https://repo", "Newtonsoft.Json", "13.0.3"));
    }

    @Test
    void normalizesVersions() {
        assertEquals("1.0.0", NuGetRepository.normalizeVersion("1.0"));
        assertEquals("1.2.3", NuGetRepository.normalizeVersion("1.02.3.0"));
        assertEquals("1.2.3.4", NuGetRepository.normalizeVersion("1.2.3.4"));
        assertEquals("2.0.0-Beta.1", NuGetRepository.normalizeVersion("2.0-Beta.1"));
    }

    @Test
    void downloadsDirectDependencies() throws Exception {
        try (FakeNuGetServer server = new FakeNuGetServer()
                .add("/demo/1.0.0/demo.nuspec", NuspecParserTest.GROUPED)) {
            List<DependencyGroup> groups = new NuGetRepository(server.baseUrl()).directDependencies("Demo", "1.0");
            assertEquals(2, groups.size());
            assertEquals("Microsoft.CSharp", groups.get(1).dependencies().get(0).id());
        }
    }

    @Test
    void reportsMissingPackage() throws Exception {
        try (FakeNuGetServer server = new FakeNuGetServer()) {
            RepositoryException e = assertThrows(RepositoryException.class,
                    () -> new NuGetRepository(server.baseUrl()).directDependencies("Nope", "1.0.0"));
            assertTrue(e.getMessage().contains("не найден"));
        }
    }

    @Test
    void reportsUnreachableRepository() {
        assertThrows(RepositoryException.class,
                () -> new NuGetRepository("http://127.0.0.1:1").directDependencies("Demo", "1.0.0"));
    }
}
