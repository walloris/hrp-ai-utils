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
public class SearchLogsTool {

    private final OpenSearchClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static final String TOOL_NAME = "search_logs";
    public static final String TOOL_DESCRIPTION = "Search logs and return count in OpenSearch with optional time, DQL query, and structured filters. " +
        "Log documents are stored in _source with nested structure: _source.doc.{field}. " +
        "Use dql for complex text searches (e.g., 'severity:ERROR AND service:app-onboarding'). " +
        "IMPORTANT: You MUST specify the index pattern to search (e.g., 'log_ci04183799-app-onboarding*'). " +
        "Default time range: last 90 days (if startTime not provided). " +
        "Available fields: doc.service, doc.severity, doc.class, doc.thread, doc.podname, doc.traceid, doc.message, doc.timestamp. " +
        "Use this tool to get count of matching logs, not the actual log content. " +
        "Returns both JSON (for programmatic processing with total, aggregation, dql) and text (markdown table) formats.";

    public JsonNode execute(String index, String startTimeStr, String endTimeStr, int limit, String dql, JsonNode filters, String bucketSize, boolean withoutAgg) throws IOException, ParseException {
        ObjectNode queryBody = objectMapper.createObjectNode();

        log.debug("Executing search_logs: index={}, startTime={}, endTime={}, dql={}, filters={}, bucketSize={}, withoutAgg={}", index, startTimeStr, endTimeStr, dql, filters, bucketSize, withoutAgg);

        // Set defaults if not provided (use MSK timezone)
        ZoneId msk = ZoneId.of("Europe/Moscow");
        long now = System.currentTimeMillis();
        long startMillis;
        long endMillis;

        if (startTimeStr == null || startTimeStr.isEmpty()) {
            // Default: last 90 days for SearchLogsTool
            startMillis = now - (90L * 24 * 60 * 60 * 1000); // 90 days ago
        } else {
            startMillis = parseIsoString(startTimeStr);
        }

        if (endTimeStr == null || endTimeStr.isEmpty()) {
            endMillis = now;
        } else {
            endMillis = parseIsoString(endTimeStr);
        }

        if (limit <= 0) {
            limit = 5; // Default limit
        }

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

        // Size: use 0 for count-only queries (SearchLogsTool)
        queryBody.put("size", 0);

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

        // Add date histogram aggregation (unless withoutAgg=true)
        if (!withoutAgg) {
            ObjectNode aggs = objectMapper.createObjectNode();
            ObjectNode dateHistogram = objectMapper.createObjectNode();
            ObjectNode dateHistogramInner = objectMapper.createObjectNode();
            dateHistogramInner.put("field", "@timestamp");
            dateHistogramInner.put("fixed_interval", bucketSize != null && !bucketSize.isEmpty() ? bucketSize : "1d");
            dateHistogramInner.put("time_zone", "Europe/Moscow");
            dateHistogramInner.put("min_doc_count", 1);
            dateHistogram.set("date_histogram", dateHistogramInner);
            aggs.set("2", dateHistogram);
            queryBody.set("aggs", aggs);
            log.debug("Added date_histogram aggregation with fixed_interval: {}", bucketSize != null && !bucketSize.isEmpty() ? bucketSize : "1d");
        } else {
            log.debug("Aggregation skipped (withoutAgg=true)");
        }

        // Execute search
        log.debug("Executing search: index={}, size={}", index, queryBody.path("size").asInt());
        JsonNode response = client.search(index, queryBody);
        JsonNode rawResponse = response.path("rawResponse");

        // Extract hits array from response
        JsonNode rawHits = rawResponse.path("hits").path("hits");
        JsonNode rawTotal = rawResponse.path("hits").path("total");

        // Handle both formats: {"value": N} and just N
        long totalHits;
        if (rawTotal.isObject() && rawTotal.has("value")) {
            totalHits = rawTotal.path("value").asLong(0);
        } else {
            totalHits = rawTotal.asLong(0);
        }

        log.info("Found {} log messages matching the criteria. Query: \"{}\" Filters: {}", totalHits, dql, filters);

        // Extract aggregation buckets if present
        JsonNode aggsNode = response.path("rawResponse").path("aggregations").path("2").path("buckets");
        
        // Build JSON data as string for programmatic processing
        ObjectNode jsonData = objectMapper.createObjectNode();
        jsonData.put("total", totalHits);

        // Build aggregation array
        ArrayNode aggregationArray = objectMapper.createArrayNode();
        if (aggsNode.isArray() && aggsNode.size() > 0) {
            for (int i = 0; i < aggsNode.size(); i++) {
                JsonNode bucket = aggsNode.path(i);
                ObjectNode aggObj = objectMapper.createObjectNode();
                aggObj.put("date", bucket.path("key_as_string").asText(""));
                aggObj.put("count", bucket.path("doc_count").asLong(0));
                aggregationArray.add(aggObj);
            }
        }
        jsonData.set("aggregation", aggregationArray);

        if (dql != null && !dql.isEmpty()) {
            jsonData.put("dql", dql);
        }

        // Return JSON data as string only
        ArrayNode contentArray = objectMapper.createArrayNode();

        // JSON content for programmatic processing
        ObjectNode jsonContent = objectMapper.createObjectNode();
        jsonContent.put("type", "text");
        jsonContent.put("text", objectMapper.writeValueAsString(jsonData));
        contentArray.add(jsonContent);

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
        parameters.set("required", required);

        ObjectNode properties = mapper.createObjectNode();

        ObjectNode indexParam = mapper.createObjectNode();
        indexParam.put("type", "string");
        indexParam.put("description", "REQUIRED: OpenSearch index pattern to search (e.g., log_ci04183799-app-onboarding* or log_app-2026-05-01). Use wildcards for multiple indices.");
        properties.set("index", indexParam);

        ObjectNode startTimeParam = mapper.createObjectNode();
        startTimeParam.put("type", "string");
        startTimeParam.put("description", "Start time in ISO 8601 format (MSK timezone, defaults to 90 days ago for search_logs)");
        properties.set("startTime", startTimeParam);

        ObjectNode endTimeParam = mapper.createObjectNode();
        endTimeParam.put("type", "string");
        endTimeParam.put("description", "End time in ISO 8601 format (MSK timezone, defaults to now if not provided)");
        properties.set("endTime", endTimeParam);

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

        ObjectNode bucketSizeParam = mapper.createObjectNode();
        bucketSizeParam.put("type", "string");
        bucketSizeParam.put("description", "Interval for date histogram aggregation (e.g., '1d', '30m', '5m', '30s'). Default: '1d'");
        properties.set("bucketSize", bucketSizeParam);

        ObjectNode withoutAggParam = mapper.createObjectNode();
        withoutAggParam.put("type", "boolean");
        withoutAggParam.put("description", "If true, skip date histogram aggregation. Default: false");
        properties.set("withoutAgg", withoutAggParam);

        parameters.set("properties", properties);

        tool.set("parameters", parameters);
        tool.set("inputSchema", parameters);

        return tool;
    }
}
