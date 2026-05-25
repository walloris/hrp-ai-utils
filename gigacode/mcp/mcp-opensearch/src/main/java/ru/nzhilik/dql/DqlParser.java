package ru.nzhilik.dql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DqlParser {

    public static String extractValue(String dql) {
        if (dql == null || dql.isEmpty()) {
            return dql;
        }
        // Parse DQL format: field:value or just value
        // Extract value after first colon that's not inside quotes
        int colonIdx = dql.indexOf(':');
        if (colonIdx > 0) {
            String beforeColon = dql.substring(0, colonIdx).trim();
            String afterColon = dql.substring(colonIdx + 1).trim();

            if (!beforeColon.contains(" ") && !beforeColon.contains("\"") && !beforeColon.contains("*")) {
                // Remove surrounding quotes if present
                if (afterColon.startsWith("\"") && afterColon.endsWith("\"")) {
                    return afterColon.substring(1, afterColon.length() - 1);
                }
                return afterColon;
            }
        }
        return dql;
    }

    public static List<String> parseAndTerms(String dql) {
        List<String> terms = new ArrayList<>();
        if (dql == null || dql.isEmpty()) {
            return terms;
        }

        String cleaned = dql.trim();
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        String[] parts = cleaned.split("\\s+AND\\s+", -1);
        for (String part : parts) {
            String trimmed = cleanTerm(part.trim());
            if (!trimmed.isEmpty()) {
                terms.add(trimmed);
            }
        }

        return terms;
    }

    public static List<String> parseOrTerms(String dql) {
        List<String> terms = new ArrayList<>();
        if (dql == null || dql.isEmpty()) {
            return terms;
        }

        String cleaned = dql.trim();
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1).trim();
        }

        String[] parts = cleaned.split("\\s+OR\\s+", -1);
        for (String part : parts) {
            String trimmed = cleanTerm(part.trim());
            if (!trimmed.isEmpty()) {
                terms.add(trimmed);
            }
        }

        return terms;
    }

    private static String cleanTerm(String term) {
        // First try extractValue to handle field:value format
        String value = extractValue(term);
        // Then remove surrounding quotes
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        log.debug("cleanTerm: '{}' -> '{}'", term, value);
        return value;
    }

    public static ObjectNode buildDqlFilter(ObjectMapper mapper, String dql) {
        String value = extractValue(dql);

        // Check for OR operator
        if (value.contains(" OR ")) {
            return buildFilterWithTerms(mapper, parseOrTerms(value), "should");
        }

        // Check for AND operator
        if (value.contains(" AND ")) {
            return buildFilterWithTerms(mapper, parseAndTerms(value), "must");
        }

        // Simple query (no AND/OR)
        List<String> terms = parseAndTerms(value);
        if (terms.isEmpty()) {
            // Empty terms - return match_all
            ObjectNode matchAll = mapper.createObjectNode();
            matchAll.set("match_all", mapper.createObjectNode());
            return matchAll;
        } else if (terms.size() == 1) {
            // Single term - use must for exact matching
            ObjectNode mustBool = mapper.createObjectNode();
            ArrayNode mustArray = mapper.createArrayNode();
            ObjectNode wrappedMatch = createWrappedMatch(mapper, terms.get(0));
            mustArray.add(wrappedMatch);
            mustBool.set("must", mustArray);
            return mustBool;
        } else {
            // Multiple terms - use must
            return buildFilterWithTerms(mapper, terms, "must");
        }
    }

    private static ObjectNode buildFilterWithTerms(ObjectMapper mapper, List<String> terms, String clauseType) {
        ArrayNode clauseArray = mapper.createArrayNode();

        for (String term : terms) {
            ObjectNode wrappedMatch = createWrappedMatch(mapper, term);
            clauseArray.add(wrappedMatch);
        }

        ObjectNode clauseBool = mapper.createObjectNode();
        clauseBool.set(clauseType, clauseArray);
        // For should clause, set minimum_should_match
        if ("should".equals(clauseType)) {
            clauseBool.put("minimum_should_match", 1);
        }
        return clauseBool;
    }

    private static ObjectNode createWrappedMatch(ObjectMapper mapper, String term) {
        // Use multi_match with type: phrase for all terms
        ObjectNode multiMatch = mapper.createObjectNode();
        multiMatch.put("type", "phrase");
        multiMatch.put("query", term);
        multiMatch.put("lenient", true);
        ObjectNode matchQuery = mapper.createObjectNode();
        matchQuery.set("multi_match", multiMatch);
        return matchQuery;
    }
}
