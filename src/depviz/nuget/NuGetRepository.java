package depviz.nuget;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Получает описания пакетов из репозитория NuGet по протоколу «плоского контейнера» API v3.
 */
public final class NuGetRepository {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int MIN_VERSION_PARTS = 3;
    private static final int MAX_VERSION_PARTS = 4;
    private static final String ZERO = "0";

    private final String baseUrl;
    private final HttpClient client;
    private final NuspecParser parser = new NuspecParser();

    /**
     * Адрес передаётся без завершающего «/», например {@code https://api.nuget.org/v3-flatcontainer}.
     */
    public NuGetRepository(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Загружает .nuspec пакета заданной версии и возвращает его прямые зависимости по фреймворкам.
     */
    public List<DependencyGroup> directDependencies(String id, String version) throws RepositoryException {
        return parser.parse(download(nuspecUri(baseUrl, id, version), id, version));
    }

    /**
     * Адрес .nuspec в плоском контейнере: {@code {base}/{id}/{версия}/{id}.nuspec}, имя и версия в нижнем регистре.
     */
    static URI nuspecUri(String baseUrl, String id, String version) {
        String lowerId = id.toLowerCase(Locale.ROOT);
        String lowerVersion = normalizeVersion(version).toLowerCase(Locale.ROOT);
        return URI.create(baseUrl + "/" + lowerId + "/" + lowerVersion + "/" + lowerId + ".nuspec");
    }

    /**
     * NuGet хранит пакеты под нормализованной версией: без ведущих нулей, минимум три числа,
     * четвёртое число убирается, если оно равно нулю, метаданные сборки после «+» отбрасываются.
     * Например, {@code 1.0 -> 1.0.0}, {@code 1.02.3.0 -> 1.2.3}.
     */
    static String normalizeVersion(String fullVersion) {
        int plus = fullVersion.indexOf('+');
        String version = plus < 0 ? fullVersion : fullVersion.substring(0, plus);
        int dash = version.indexOf('-');
        String release = dash < 0 ? version : version.substring(0, dash);
        String suffix = dash < 0 ? "" : version.substring(dash);
        List<String> parts = new ArrayList<>();
        for (String part : release.split("\\.")) {
            parts.add(part.replaceFirst("^0+(?=\\d)", ""));
        }
        while (parts.size() < MIN_VERSION_PARTS) {
            parts.add(ZERO);
        }
        if (parts.size() == MAX_VERSION_PARTS && ZERO.equals(parts.get(MAX_VERSION_PARTS - 1))) {
            parts.remove(MAX_VERSION_PARTS - 1);
        }
        return String.join(".", parts) + suffix;
    }

    private byte[] download(URI uri, String id, String version) throws RepositoryException {
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
        HttpResponse<byte[]> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new RepositoryException("Не удалось подключиться к репозиторию " + baseUrl + ": " + describe(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositoryException("Загрузка данных о пакете прервана");
        }
        if (response.statusCode() == HTTP_NOT_FOUND) {
            throw new RepositoryException("Пакет " + id + " версии " + version + " не найден в репозитории " + baseUrl);
        }
        if (response.statusCode() != HTTP_OK) {
            throw new RepositoryException("Репозиторий вернул код " + response.statusCode() + " для " + uri);
        }
        return response.body();
    }

    private static String describe(IOException e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
