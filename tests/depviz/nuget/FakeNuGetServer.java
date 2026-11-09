package depviz.nuget;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Локальный HTTP-сервер, который отдаёт заранее заданные файлы .nuspec вместо настоящего nuget.org.
 */
public final class FakeNuGetServer implements AutoCloseable {

    private static final int HTTP_OK = 200;
    private static final int HTTP_NOT_FOUND = 404;
    private static final int NO_BODY = -1;

    private final HttpServer server;
    private final Map<String, String> files = new HashMap<>();

    public FakeNuGetServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public FakeNuGetServer add(String path, String content) {
        files.put(path, content);
        return this;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = files.get(exchange.getRequestURI().getPath());
        if (body == null) {
            exchange.sendResponseHeaders(HTTP_NOT_FOUND, NO_BODY);
        } else {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(HTTP_OK, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
