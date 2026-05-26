package ru.nzhilik;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.nzhilik.infra.CookieServer;
import ru.nzhilik.mcp.OpenSearchClient;
import ru.nzhilik.mcp.tools.GetMessagesTool;
import ru.nzhilik.mcp.tools.SearchLogsTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
public class Main {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static OpenSearchClient client;
    private static CookieServer cookieServer;

    public static void main(String[] args) {

        int port = getCookieServerPort();

        try {
            client = new OpenSearchClient();
            cookieServer = new CookieServer(client, port);
            cookieServer.start();

            logger.info("Cookie server started on port {}", port);

            // Daemon mode - read from stdin
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                // Check for Content-Length header
                if (line.startsWith("Content-Length:")) {
                    continue;
                }

                try {
                    JsonNode request = objectMapper.readTree(line);
                    boolean isNotification = !request.has("id") || request.path("id").isNull();
                    String method = request.path("method").asText();
                    JsonNode params = request.path("params");

                    if (isNotification) {
                        handleNotification(method, params);
                    } else {
                        int id = request.path("id").asInt();
                        ObjectNode result = handleRequest(method, params);
                        sendResponse(id, result);
                    }

                } catch (Exception e) {
                    sendErrorResponse(0, -32603, "Internal error: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Execution failed", e);
            // Silent exit on startup errors
        } finally {
            if (cookieServer != null) {
                cookieServer.stop();
            }
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    // Silent
                }
            }
        }
    }

    private static int getCookieServerPort() {
        // Read port from environment variable
        String portStr = System.getenv("COOKIE_SERVER_PORT");
        if (portStr != null && !portStr.isEmpty()) {
            try {
                return Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                // Use default port if parsing fails
            }
        }
        return 8080;
    }

    private static ObjectNode handleRequest(String method, JsonNode params) throws IOException {
        ObjectNode result = objectMapper.createObjectNode();

        switch (method) {
            case "initialize":
                ObjectNode capabilities = objectMapper.createObjectNode();
                capabilities.put("tools", objectMapper.createObjectNode());

                ObjectNode serverInfo = objectMapper.createObjectNode();
                serverInfo.put("name", "opensearch");
                serverInfo.put("version", "1.0.0");

                ObjectNode initResult = objectMapper.createObjectNode();
                initResult.put("protocolVersion", "2024-11-05");
                initResult.set("capabilities", capabilities);
                initResult.set("serverInfo", serverInfo);

                result = initResult;
                break;

            case "notifications/initialized":
                // Ignore - client has acknowledged connection
                result = objectMapper.createObjectNode();
                break;

            case "tools/list":
                ArrayNode tools = objectMapper.createArrayNode();
                tools.add(SearchLogsTool.getToolDefinition());
                tools.add(GetMessagesTool.getToolDefinition());
                result.set("tools", tools);
                break;

            case "tools/call":
                String toolName = params.path("name").asText();
                JsonNode toolParams = params.path("arguments");

                // Check if index is empty
                String index = toolParams.path("index").asText();
                if (index == null || index.isEmpty()) {
                    throw new IllegalArgumentException("index parameter is required but not provided");
                }

                String startTimeStr = toolParams.has("startTime") ? toolParams.path("startTime").asText() : null;
                String endTimeStr = toolParams.has("endTime") ? toolParams.path("endTime").asText() : null;
                int limit = toolParams.has("limit") ? toolParams.path("limit").asInt() : 5;
                String dql = toolParams.has("dql") ? toolParams.path("dql").asText() : null;
                JsonNode filters = toolParams.has("filters") ? toolParams.path("filters") : null;
                String bucketSize = toolParams.has("bucketSize") ? toolParams.path("bucketSize").asText() : null;
                boolean withoutAgg = toolParams.has("withoutAgg") ? toolParams.path("withoutAgg").asBoolean() : false;

                ObjectMapper mapper = new ObjectMapper();
                ArrayNode contentArray;
                switch (toolName) {
                    case GetMessagesTool.TOOL_NAME:
                        logger.debug("MCP: Received index='{}'", index);
                        try {
                            contentArray = (ArrayNode) new GetMessagesTool(client).execute(index, startTimeStr, endTimeStr, limit, dql, filters);
                        } catch (ParseException e) {
                            throw new IOException(e);
                        } catch (IOException e) {
                            // Return error as MCP-compatible array
                            contentArray = mapper.createArrayNode();
                            ObjectNode errorContent = mapper.createObjectNode();
                            errorContent.put("type", "text");
                            errorContent.put("text", "Error: " + e.getMessage());
                            contentArray.add(errorContent);
                        }
                        result.set("content", contentArray);
                        break;

                    case SearchLogsTool.TOOL_NAME:
                        try {
                            contentArray = (ArrayNode) new SearchLogsTool(client).execute(index, startTimeStr, endTimeStr, limit, dql, filters, bucketSize, withoutAgg);
                        } catch (ParseException e) {
                            throw new IOException(e);
                        } catch (IOException e) {
                            // Return error as MCP-compatible array
                            contentArray = mapper.createArrayNode();
                            ObjectNode errorContent = mapper.createObjectNode();
                            errorContent.put("type", "text");
                            errorContent.put("text", "Error: " + e.getMessage());
                            contentArray.add(errorContent);
                        }
                        result.set("content", contentArray);
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown tool: " + toolName);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown method: " + method);
        }

        return result;
    }

    private static ObjectNode getCapabilities() {
        ObjectNode caps = objectMapper.createObjectNode();
        caps.put("tools", objectMapper.createObjectNode());
        return caps;
    }

    private static void sendResponse(int id, JsonNode data) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.set("result", data);

        String json = objectMapper.writeValueAsString(response);
        System.out.println(json);
        System.out.flush();
    }

    private static void sendErrorResponse(int id, int code, String message) throws IOException {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.put("id", id);

        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);

        String json = objectMapper.writeValueAsString(response);
        System.out.println(json);
        System.out.flush();
    }

    private static void handleNotification(String method, JsonNode params) throws IOException {
        switch (method) {
            case "notifications/initialized":
                // Client has acknowledged connection - nothing to do
                break;
            default:
                // Ignore unknown notifications
                break;
        }
    }
}
