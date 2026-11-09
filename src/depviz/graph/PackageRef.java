package depviz.graph;

/**
 * Пакет и его версия. В тестовом репозитории версий нет, поэтому {@code version} там пустая.
 */
public record PackageRef(String id, String version) {

    public String label() {
        return version.isEmpty() ? id : id + " " + version;
    }
}
