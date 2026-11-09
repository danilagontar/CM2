package depviz.nuget;

/**
 * Ошибка при получении данных о пакете: нет сети, пакет не найден, некорректный .nuspec.
 */
public class RepositoryException extends Exception {

    private static final long serialVersionUID = 1L;

    public RepositoryException(String message) {
        super(message);
    }
}
