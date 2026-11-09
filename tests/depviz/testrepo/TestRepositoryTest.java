package depviz.testrepo;

import depviz.graph.PackageRef;
import depviz.nuget.RepositoryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты чтения файла тестового репозитория.
 */
class TestRepositoryTest {

    @TempDir
    Path tempDir;

    private TestRepository load(String content) throws IOException, RepositoryException {
        return TestRepository.load(Files.writeString(tempDir.resolve("repo.txt"), content));
    }

    private static List<String> deps(TestRepository repository, String name) {
        return repository.dependencies(new PackageRef(name, "")).stream().map(PackageRef::id).toList();
    }

    @Test
    void readsPackagesAndSkipsComments() throws Exception {
        TestRepository repository = load("# комментарий\nA: B  C # хвост\n\nB:\n");
        assertEquals(List.of("B", "C"), deps(repository, "A"));
        assertTrue(deps(repository, "B").isEmpty());
        assertTrue(repository.contains("A"));
    }

    @Test
    void undescribedPackageHasNoDependencies() throws Exception {
        assertTrue(deps(load("A: B\n"), "B").isEmpty());
    }

    @Test
    void rejectsLineWithoutColon() {
        assertThrows(RepositoryException.class, () -> load("A B\n"));
    }

    @Test
    void rejectsLowercaseName() {
        RepositoryException e = assertThrows(RepositoryException.class, () -> load("A: b\n"));
        assertTrue(e.getMessage().contains("строка 1"));
    }

    @Test
    void rejectsDuplicatePackage() {
        assertThrows(RepositoryException.class, () -> load("A: B\nA: C\n"));
    }
}
