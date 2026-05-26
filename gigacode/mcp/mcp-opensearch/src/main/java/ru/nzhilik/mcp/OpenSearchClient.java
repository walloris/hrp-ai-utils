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

    private static final String KIBANA_URL = "https://hr-log.sberbank.ru/kibana/internal/search/opensearch";
    private static final String COOKIE_LOG_PATH = "/home/work/21599655@sigma.sbrf.ru/IdeaProjects/mcp/mcp-opensearch/build/mcp-cookie.log";

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

        // Initialize with provided cookie
        String initialCookie = loadCookie();
        if (initialCookie != null && !initialCookie.isEmpty()) {
            updateCookie(initialCookie);
        }
    }

    private String loadXHrpPersonId() {
        String xHrpPersonId = System.getenv("X_HRP_PERSON_ID");
        log.debug("Loaded X-HPR-Persone-id: {}", xHrpPersonId);
        if (xHrpPersonId == null || xHrpPersonId.isEmpty()) {
            throw new IllegalStateException("X_HRP_PERSON_ID environment variable is required but not set");
        }
        return xHrpPersonId;
    }

    private void setInitialCookie(String cookie) {
        // Parse cookie and add to cookie store
        String[] parts = cookie.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.contains("=")) {
                String name = part.substring(0, part.indexOf("="));
                String value = part.substring(part.indexOf("=") + 1);
                
                BasicClientCookie basicCookie = new BasicClientCookie(name, value);
                basicCookie.setDomain("hr-log.sberbank.ru");
                basicCookie.setPath("/kibana");
                basicCookie.setSecure(true);
                cookieStore.addCookie(basicCookie);
            }
        }
    }

    private static String loadCookie() {
        // 1. First check Java system property (-DOPENSEARCH_COOKIE)
        String cookie = System.getProperty("OPENSEARCH_COOKIE");
        if (cookie != null && !cookie.isEmpty()) {
            return parseCookieValue(cookie);
        }

        // 2. Then check environment variable
        cookie = System.getenv("OPENSEARCH_COOKIE");
        if (cookie != null && !cookie.isEmpty()) {
            return parseCookieValue(cookie);
        }
        return cookie;
    }

    private static String parseCookieValue(String cookie) {
        if (cookie == null || cookie.isEmpty()) {
            return cookie;
        }
        // Split by semicolon and take only parts that contain "=" (name=value pairs)
        String[] parts = cookie.split(";");
        StringBuilder cleaned = new StringBuilder();
        for (String part : parts) {
            part = part.trim();
            if (part.contains("=") && !part.toLowerCase().startsWith("path=")
                && !part.toLowerCase().startsWith("httponly")
                && !part.toLowerCase().startsWith("domain=")
                && !part.toLowerCase().startsWith("expires=")
                && !part.toLowerCase().startsWith("max-age=")
                && !part.toLowerCase().startsWith("secure")) {
                if (cleaned.length() > 0) {
                    cleaned.append("; ");
                }
                cleaned.append(part);
            }
        }
        return cleaned.toString();
    }

    public void updateCookie(String cookie) {
        // Parse and add cookie to cookieStore
        String cleanedCookie = parseCookieValue(cookie);
        if (cleanedCookie != null && !cleanedCookie.isEmpty()) {
            String[] parts = cleanedCookie.split(";");
            for (String part : parts) {
                part = part.trim();
                if (part.contains("=")) {
                    String name = part.substring(0, part.indexOf("="));
                    String value = part.substring(part.indexOf("=") + 1);
                    
                    BasicClientCookie basicCookie = new BasicClientCookie(name, value);
                    basicCookie.setDomain("hr-log.sberbank.ru");
                    basicCookie.setPath("/kibana");
                    basicCookie.setSecure(true);
                    cookieStore.addCookie(basicCookie);
                }
            }
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
        post.setHeader("Content-Type", "application/json");
        post.setHeader("Origin", "https://hr-log.sberbank.ru");
        post.setHeader("Pragma", "no-cache");
        post.setHeader("Sec-Fetch-Dest", "empty");
        post.setHeader("Sec-Fetch-Mode", "cors");
        post.setHeader("Sec-Fetch-Site", "same-origin");
        post.setHeader("osd-version", "1.3.15");
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
