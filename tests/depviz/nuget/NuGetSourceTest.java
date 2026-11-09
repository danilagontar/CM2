package depviz.nuget;

import depviz.graph.PackageRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты источника зависимостей NuGet на локальном сервере.
 */
class NuGetSourceTest {

    @Test
    void mergesFrameworksAndTakesLowestVersion() throws Exception {
        try (FakeNuGetServer server = new FakeNuGetServer()
                .add("/demo/1.0.0/demo.nuspec", NuspecParserTest.GROUPED)) {
            NuGetSource source = new NuGetSource(new NuGetRepository(server.baseUrl()));
            assertEquals(List.of(new PackageRef("Microsoft.CSharp", "4.3.0"),
                            new PackageRef("NETStandard.Library", "1.6.1")),
                    source.dependencies(new PackageRef("Demo", "1.0.0")));
        }
    }

    @Test
    void packageWithoutVersionIsNotExpanded() throws Exception {
        NuGetSource source = new NuGetSource(new NuGetRepository("http://127.0.0.1:1"));
        assertTrue(source.dependencies(new PackageRef("Demo", "")).isEmpty());
    }
}
