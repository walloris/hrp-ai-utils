package ru.nzhilik.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import ru.nzhilik.dql.DqlParser;
import ru.nzhilik.mcp.OpenSearchClient;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class GetMessagesTool {

    private final OpenSearchClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String TOOL_NAME = "get_messages";
    public static final String TOOL_DESCRIPTION = "Get log messages from OpenSearch with optional time, DQL query, and structured filters. " +
        "Log documents are stored in _source with nested structure: _source.doc.{field}. " +
        "Use dql for filtering messages (e.g., 'severity:ERROR', 'traceid:abc123'). " +
        "IMPORTANT: You MUST specify the index pattern to search (e.g., 'log_ci04183799-app-onboarding*'). " +
        "REQUIRED: You MUST provide startTime and endTime in ISO 8601 format (MSK timezone). " +
        "Available fields: doc.service, doc.severity, doc.class, doc.thread, doc.podname, doc.traceid, doc.message, doc.timestamp. " +
        "Use this tool to retrieve actual log messages content.";

    public JsonNode execute(String index, String startTimeStr, String endTimeStr, int limit, String dql, JsonNode filters) throws IOException, ParseException {
        ObjectNode queryBody = objectMapper.createObjectNode();

        log.debug("Executing get_messages: index={}, startTime={}, endTime={}, limit={}, dql={}", index, startTimeStr, endTimeStr, limit, dql);

        // Parse timestamps (no defaults - required parameters)
        ZoneId msk = ZoneId.of("Europe/Moscow");
        long startMillis = parseIsoString(startTimeStr);
        long endMillis = parseIsoString(endTimeStr);

        // Query structure
        ObjectNode query = objectMapper.createObjectNode();
        ObjectNode boolQuery = objectMapper.createObjectNode();

        List<JsonNode> filterClauses = new ArrayList<>();

        // Always add match_all filter to match all documents
        ObjectNode matchAll = objectMapper.createObjectNode();
        matchAll.set("match_all", objectMapper.createObjectNode());
        filterClauses.add(matchAll);

        // Time range filter - always required (with defaults applied)
        ObjectNode rangeQuery = objectMapper.createObjectNode();
        ObjectNode timestampQuery = objectMapper.createObjectNode();

        // Use ISO format for timestamps (like Kibana does)
        String startTimeISO = formatTimestamp(startMillis);
        String endTimeISO = formatTimestamp(endMillis);
        log.debug("Timestamps: startTime={}, endTime={}", startTimeISO, endTimeISO);
        timestampQuery.put("gte", startTimeISO);
        timestampQuery.put("lte", endTimeISO);
        timestampQuery.put("format", "strict_date_optional_time");

        rangeQuery.set("@timestamp", timestampQuery);

        // Wrap range in "range" key for filter context
        ObjectNode wrappedRange = objectMapper.createObjectNode();
        wrappedRange.set("range", rangeQuery);
        filterClauses.add(wrappedRange);

        // Add filters from filters parameter (field/value pairs)
        if (filters != null && filters.isObject()) {
            filters.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode fieldValue = entry.getValue();

                // Use match_phrase for text fields, term for keyword/numeric
                // match_phrase is more flexible and works for most log fields
                ObjectNode phraseQuery = objectMapper.createObjectNode();
                phraseQuery.put("query", fieldValue.asText());
                ObjectNode phraseNode = objectMapper.createObjectNode();
                phraseNode.set(fieldName, phraseQuery);

                ObjectNode wrappedPhrase = objectMapper.createObjectNode();
                wrappedPhrase.set("match_phrase", phraseNode);
                filterClauses.add(wrappedPhrase);
            });
        }

        // Build bool query - add filters first
        if (!filterClauses.isEmpty()) {
            boolQuery.set("filter", objectMapper.valueToTree(filterClauses));
        }

        // Add dql if provided - build filter using DqlParser
        if (dql != null && !dql.isEmpty()) {
            ObjectNode dqlFilter = DqlParser.buildDqlFilter(objectMapper, dql);
            // Extract the must/should array from dqlFilter and add to appropriate bool clause
            if (dqlFilter.has("must")) {
                boolQuery.set("must", dqlFilter.get("must"));
            } else if (dqlFilter.has("should")) {
                boolQuery.set("should", dqlFilter.get("should"));
                if (dqlFilter.has("minimum_should_match")) {
                    boolQuery.put("minimum_should_match", dqlFilter.get("minimum_should_match").asInt());
                }
            }
        }

        query.set("bool", boolQuery);
        queryBody.set("query", query);

        // Sort by timestamp descending
        ArrayNode sortArray = objectMapper.createArrayNode();
        ObjectNode timestampSort = objectMapper.createObjectNode();
        ObjectNode timestampOrder = objectMapper.createObjectNode();
        timestampOrder.put("order", "desc");
        timestampOrder.put("unmapped_type", "boolean");
        timestampSort.set("@timestamp", timestampOrder);
        sortArray.add(timestampSort);
        queryBody.set("sort", sortArray);

        // Size limit
        queryBody.put("size", limit);

        // Return source document (contains message and all other fields)
        // Use excludes object like Kibana does
        ObjectNode sourceConfig = objectMapper.createObjectNode();
        sourceConfig.set("excludes", objectMapper.createArrayNode());
        queryBody.set("_source", sourceConfig);

        // Add version field
        queryBody.put("version", true);

        // Script fields (empty like Kibana)
        queryBody.set("script_fields", objectMapper.createObjectNode());

        // Highlight config (like Kibana)
        ObjectNode highlight = objectMapper.createObjectNode();
        ArrayNode preTags = objectMapper.createArrayNode();
        preTags.add("@opensearch-dashboards-highlighted-field@");
        ArrayNode postTags = objectMapper.createArrayNode();
        postTags.add("@/opensearch-dashboards-highlighted-field@");
        highlight.set("pre_tags", preTags);
        highlight.set("post_tags", postTags);

        ObjectNode highlightFields = objectMapper.createObjectNode();
        highlightFields.set("*", objectMapper.createObjectNode());
        highlight.set("fields", highlightFields);
        highlight.put("fragment_size", 2147483647);
        queryBody.set("highlight", highlight);

        // Fields to return from stored fields
        ArrayNode storedFields = objectMapper.createArrayNode();
        storedFields.add("*");
        queryBody.set("stored_fields", storedFields);

        ArrayNode docvalueFields = objectMapper.createArrayNode();

        ObjectNode timestampField = objectMapper.createObjectNode();
        timestampField.put("field", "@timestamp");
        timestampField.put("format", "date_time");
        docvalueFields.add(timestampField);

        // Add time2 field if it exists in the index
        ObjectNode time2Field = objectMapper.createObjectNode();
        time2Field.put("field", "doc.time2");
        time2Field.put("format", "date_time");
        docvalueFields.add(time2Field);

        queryBody.set("docvalue_fields", docvalueFields);

        // Execute search
        log.debug("Executing search: size={}, sort={}", queryBody.path("size").asInt(), queryBody.path("sort"));
        JsonNode response = client.search(index, queryBody);

        // Extract hits array from response
        JsonNode rawHits = response.path("rawResponse").path("hits").path("hits");
        JsonNode rawTotal = response.path("rawResponse").path("hits").path("total");

        // Handle both formats: {"value": N} and just N
        long totalHits;
        if (rawTotal.isObject() && rawTotal.has("value")) {
            totalHits = rawTotal.path("value").asLong(0);
        } else {
            totalHits = rawTotal.asLong(0);
        }

        log.debug("Found {} total hits, returning {} results", totalHits, rawHits.size());

        // Convert hits to MCP-compatible format (array of text objects)
        ArrayNode contentArray = objectMapper.createArrayNode();
        for (JsonNode hit : rawHits) {
            ObjectNode textContent = objectMapper.createObjectNode();
            textContent.put("type", "text");
            textContent.put("text", hit.toString());
            contentArray.add(textContent);
        }

        return contentArray;
    }

    private long parseIsoString(String isoStr) {
        // Parse ISO 8601 string and convert to epoch milliseconds
        try {
            Instant instant = Instant.parse(isoStr);
            return instant.toEpochMilli();
        } catch (Exception e) {
            // Try with timezone offset
            try {
                LocalDateTime ldt = LocalDateTime.parse(isoStr, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
                return ldt.atZone(ZoneId.of("Europe/Moscow")).toInstant().toEpochMilli();
            } catch (Exception e2) {
                // Fallback: try with space separator
                String normalized = isoStr.replace(" ", "T");
                return Instant.parse(normalized).toEpochMilli();
            }
        }
    }

    private String formatTimestamp(long epochMillis) {
        Instant instant = Instant.ofEpochMilli(epochMillis);
        // Use ISO format with MSK timezone offset like Kibana does (+03:00)
        ZoneId msk = ZoneId.of("Europe/Moscow");
        return instant.atZone(msk).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static ObjectNode getToolDefinition() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode tool = mapper.createObjectNode();
        tool.put("type", "function");
        tool.put("name", TOOL_NAME);
        tool.put("description", TOOL_DESCRIPTION);

        ObjectNode parameters = mapper.createObjectNode();
        parameters.put("type", "object");

        ArrayNode required = mapper.createArrayNode();
        required.add("index");
        required.add("startTime");
        required.add("endTime");
        parameters.set("required", required);

        ObjectNode properties = mapper.createObjectNode();

        ObjectNode indexParam = mapper.createObjectNode();
        indexParam.put("type", "string");
        indexParam.put("description", "REQUIRED: OpenSearch index pattern to search (e.g., log_ci04183799-app-onboarding* or log_app-2026-05-01). Use wildcards for multiple indices.");
        properties.set("index", indexParam);

        ObjectNode startTimeParam = mapper.createObjectNode();
        startTimeParam.put("type", "string");
        startTimeParam.put("description", "REQUIRED: Start time in ISO 8601 format (MSK timezone)");
        properties.set("startTime", startTimeParam);

        ObjectNode endTimeParam = mapper.createObjectNode();
        endTimeParam.put("type", "string");
        endTimeParam.put("description", "REQUIRED: End time in ISO 8601 format (MSK timezone)");
        properties.set("endTime", endTimeParam);

        ObjectNode limitParam = mapper.createObjectNode();
        limitParam.put("type", "integer");
        limitParam.put("description", "Maximum number of results (default: 5)");
        limitParam.put("default", 5);
        properties.set("limit", limitParam);

        ObjectNode dqlParam = mapper.createObjectNode();
        dqlParam.put("type", "string");
        dqlParam.put("description", "Dashboards Query Language (DQL) for advanced text filtering. " +
            "Examples: 'severity:ERROR OR severity:WARN', 'service:app-onboarding AND message:\"Unexpected error\"', 'NOT traceid:*'. " +
            "Use flat field names (without 'doc.' prefix): message, class, service, severity, thread, traceid, podname, timestamp. " +
            "For wildcard searches, use: 'message:*' or 'class:PaymentService*'. " +
            "For more details, see: https://docs.opensearch.org/1.3/dashboards/discover/dql/");
        properties.set("dql", dqlParam);

//        ObjectNode filtersParam = mapper.createObjectNode();
//        filtersParam.put("type", "object");
//        filtersParam.put("description", "Exact field matching using match_phrase queries. Works for text, keyword, and numeric fields. " +
//            "Use nested 'doc.' prefix for field names: doc.message, doc.class, doc.service, doc.severity, etc. " +
//            "Example: {\"doc.traceid\": \"abc123\", \"doc.severity\": \"ERROR\"} " +
//            "Use this when you need exact matches on specific fields. Can be combined with dql.");
//        properties.set("filters", filtersParam);

        parameters.set("properties", properties);

        tool.set("parameters", parameters);
        tool.set("inputSchema", parameters);

        return tool;
    }
}
