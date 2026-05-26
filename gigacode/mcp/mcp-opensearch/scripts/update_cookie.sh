#!/bin/bash

# Script to update cookie in MCP OpenSearch Server
# Usage: ./update_cookie.sh "new_cookie_value"

COOKIE="$1"

if [ -z "$COOKIE" ]; then
    echo "Usage: $0 \"new_security_authentication_cookie_value\""
    echo ""
    echo "Example:"
    echo "  $0 \"security_authentication=Fe26.2**...\""
    exit 1
fi

# Try common ports for cookie server
for port in 8080 8081 8082 8083 8084 8085; do
    echo "Trying port $port..."
    
    # Try to update cookie
    response=$(curl -s -X POST "http://localhost:$port/update_cookie" \
        -H 'Content-Type: application/json' \
        -d "{\"cookie\":\"$COOKIE\"}" 2>/dev/null)
    
    if echo "$response" | grep -q "success"; then
        echo "Cookie updated successfully on port $port!"
        echo "Response: $response"
        exit 0
    fi
done

echo "Error: Could not connect to MCP server."
echo "Make sure the server is running and listening on port 8080-8085."
echo ""
echo "Start the server first:"
echo "  export OPENSEARCH_COOKIE='your_cookie' && ./run.sh"
echo "  # or"
echo "  java -DOPENSEARCH_COOKIE='your_cookie' -jar build/libs/mcp-opensearch.jar"
