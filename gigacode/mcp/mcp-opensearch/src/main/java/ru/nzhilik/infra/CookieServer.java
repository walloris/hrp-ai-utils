package ru.nzhilik.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.RequiredArgsConstructor;
import ru.nzhilik.mcp.OpenSearchClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@RequiredArgsConstructor
public class CookieServer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OpenSearchClient client;
    private final int port;

    private HttpServer server;

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", this::handleHealth);
        server.createContext("/update_cookie", this::handleUpdateCookie);
        server.createContext("/", this::handleHealth);
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        String response = "{\"status\":\"ok\"}";
        sendResponse(exchange, response, 200);
    }

    private void handleUpdateCookie(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, "{\"error\":\"Method not allowed\"}", 405);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());

        try {
            JsonNode request = objectMapper.readTree(body);
            String newCookie = request.path("cookie").asText();

            if (newCookie == null || newCookie.isEmpty()) {
                sendResponse(exchange, "{\"error\":\"Cookie is required\"}", 400);
                return;
            }

            client.updateCookie(newCookie);
            sendResponse(exchange, "{\"message\":\"Cookie updated successfully\"}", 200);

        } catch (Exception e) {
            sendResponse(exchange, "{\"error\":\"Invalid JSON\"}", 400);
        }
    }

    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
