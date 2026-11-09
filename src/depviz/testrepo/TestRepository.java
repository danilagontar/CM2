package depviz.testrepo;

import depviz.graph.PackageRef;
import depviz.graph.PackageSource;
import depviz.nuget.RepositoryException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Тестовый репозиторий из текстового файла. Каждая строка – «ИМЯ: ЗАВИСИМОСТЬ ЗАВИСИМОСТЬ ...»,
 * имена из заглавных латинских букв, всё после {@code #} – комментарий.
 * Пакет, который упомянут только как зависимость, считается пакетом без зависимостей.
 */
public final class TestRepository implements PackageSource {

    private static final Pattern NAME = Pattern.compile("[A-Z]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final char COMMENT = '#';
    private static final char COLON = ':';
    private static final int NOT_FOUND = -1;

    private final Map<String, List<String>> packages;

    private TestRepository(Map<String, List<String>> packages) {
        this.packages = packages;
    }

    public static TestRepository load(Path file) throws RepositoryException {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            throw new RepositoryException("Не удалось прочитать тестовый репозиторий " + file + ": " + e.getMessage());
        }
        Map<String, List<String>> packages = new LinkedHashMap<>();
        for (int i = 0; i < lines.size(); i++) {
            parseLine(lines.get(i), i + 1, packages);
        }
        return new TestRepository(packages);
    }

    public boolean contains(String name) {
        return packages.containsKey(name);
    }

    @Override
    public List<PackageRef> dependencies(PackageRef pkg) {
        return packages.getOrDefault(pkg.id(), List.of()).stream()
                .map(name -> new PackageRef(name, ""))
                .toList();
    }

    private static void parseLine(String raw, int lineNumber, Map<String, List<String>> packages)
            throws RepositoryException {
        int commentPos = raw.indexOf(COMMENT);
        String line = (commentPos == NOT_FOUND ? raw : raw.substring(0, commentPos)).trim();
        if (line.isEmpty()) {
            return;
        }
        int colonPos = line.indexOf(COLON);
        if (colonPos == NOT_FOUND) {
            throw lineError(lineNumber, "ожидается формат «ИМЯ: ЗАВИСИМОСТИ»");
        }
        String name = checkName(line.substring(0, colonPos).trim(), lineNumber);
        String rest = line.substring(colonPos + 1).trim();
        List<String> dependencies = rest.isEmpty() ? List.of() : Arrays.asList(SPACES.split(rest));
        for (String dependency : dependencies) {
            checkName(dependency, lineNumber);
        }
        if (packages.putIfAbsent(name, dependencies) != null) {
            throw lineError(lineNumber, "пакет " + name + " описан повторно");
        }
    }

    private static String checkName(String name, int lineNumber) throws RepositoryException {
        if (!NAME.matcher(name).matches()) {
            throw lineError(lineNumber, "имя пакета '" + name + "' должно состоять из заглавных латинских букв");
        }
        return name;
    }

    private static RepositoryException lineError(int lineNumber, String message) {
        return new RepositoryException("Тестовый репозиторий, строка " + lineNumber + ": " + message);
    }
}
