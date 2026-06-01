#!/bin/bash
# OpenAPI Validator Script
# Validates OpenAPI specification using OpenAPI Generator CLI

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SPEC_PATH="$PROJECT_DIR/src/main/resources/api/openapi.yml"

echo "=== OpenAPI Validator ==="
echo "Spec: $SPEC_PATH"

# Check if spec exists
if [ ! -f "$SPEC_PATH" ]; then
    echo "ERROR: OpenAPI spec not found: $SPEC_PATH"
    exit 1
fi

# Download CLI if not exists
CLI_JAR="$SCRIPT_DIR/openapi-generator-cli.jar"
if [ ! -f "$CLI_JAR" ]; then
    echo "Downloading OpenAPI Generator CLI..."
    curl -L -o "$CLI_JAR" https://github.com/OpenAPITools/openapi-generator/releases/download/v7.8.0/openapi-generator-cli.jar
fi

echo "Validating OpenAPI specification..."
java -jar "$CLI_JAR" validate -i "$SPEC_PATH"

echo "=== Validation Complete ==="