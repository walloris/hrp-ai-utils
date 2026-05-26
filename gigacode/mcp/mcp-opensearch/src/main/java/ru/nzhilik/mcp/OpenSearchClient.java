package ru.nzhilik.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class OpenSearchClient {

    private static final String KIBANA_URL = "https://hr-log.sberbank.ru/techlogs/internal/search/opensearch-with-long-numerals";

    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;
    private final BasicCookieStore cookieStore;
    private final String xHrpPersonId;

    public OpenSearchClient() {
        this.objectMapper = new ObjectMapper();
        this.cookieStore = new BasicCookieStore();
        this.httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();

        // Load required environment variables
        this.xHrpPersonId = loadXHrpPersonId();
    }

    private String loadXHrpPersonId() {
        String xHrpPersonId = System.getenv("X_HRP_PERSON_ID");
        log.debug("Loaded X-HPR-Persone-id: {}", xHrpPersonId);
        if (xHrpPersonId == null || xHrpPersonId.isEmpty()) {
            throw new IllegalStateException("X_HRP_PERSON_ID environment variable is required but not set");
        }
        return xHrpPersonId;
    }

    public void updateCookie(String cookie) {
        // Parse and add cookie to cookieStore
        String[] parts = cookie.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (!part.contains("=")) {
                continue;
            }
            
            String name = part.substring(0, part.indexOf("=")).trim();
            String value = part.substring(part.indexOf("=") + 1).trim();

            // Skip cookie flags
            String lowerName = name.toLowerCase();
            if (lowerName.equals("path") || lowerName.equals("domain") || 
                lowerName.equals("secure") || lowerName.equals("httponly") || 
                lowerName.equals("expires") || lowerName.equals("max-age")) {
                continue;
            }

            BasicClientCookie basicCookie = new BasicClientCookie(name, value);
            basicCookie.setDomain("hr-log.sberbank.ru");
            basicCookie.setPath("/techlogs");
            basicCookie.setSecure(true);
            cookieStore.addCookie(basicCookie);
        }
    }

    public AtomicReference<String> getCookieRef() {
        return new AtomicReference<>(null);
    }

    public JsonNode search(String index, JsonNode queryBody) throws IOException, ParseException {
        ObjectNode requestParams = objectMapper.createObjectNode();
        requestParams.put("index", index);
        requestParams.set("body", queryBody);

        ObjectNode request = objectMapper.createObjectNode();
        request.set("params", requestParams);
        request.put("preference", System.currentTimeMillis());

        return executeRequest(request.toString());
    }

    public JsonNode executeRequest(String payload) throws IOException, ParseException {
        HttpPost post = new HttpPost(KIBANA_URL);

        // Log request details
        log.debug("Using cookie store with {} cookies", cookieStore.getCookies().size());
        log.debug("Request payload for curl: {}", payload);

        post.setHeader("Cache-Control", "no-cache");
        post.setHeader("Connection", "keep-alive");
        post.setHeader("Content-Type", "application/json");
        post.setHeader("osd-version", "1.6.4");
        post.setHeader("osd-xsrf", "osd-fetch");
        post.setHeader("x-hrp-person-id", xHrpPersonId);

        // Add request body
        post.setEntity(new StringEntity(payload, StandardCharsets.UTF_8));

        try (var response = httpClient.execute(post)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            log.debug("Response payload for curl: {}", responseBody);

            if (statusCode != 200) {
                log.error("OpenSearch request failed with status {}: {}", statusCode, responseBody);
                // Parse error response and return it as structured error
                try {
                    JsonNode errorNode = objectMapper.readTree(responseBody);
                    throw new IOException("OpenSearch request failed with status " + statusCode + ": " + errorNode.toPrettyString());
                } catch (Exception e) {
                    // Fallback to plain text if parsing fails
                    throw new IOException("OpenSearch request failed with status " + statusCode + ": " + responseBody);
                }
            }

            return objectMapper.readTree(responseBody);
        }
    }

    public void close() throws IOException {
        httpClient.close();
    }
}
