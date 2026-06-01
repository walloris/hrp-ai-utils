#!/bin/bash
# OpenAPI Generator Helper Script
# Generates Kotlin Spring code from OpenAPI specification

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
SPEC_PATH="$PROJECT_DIR/src/main/resources/api/openapi.yml"
OUTPUT_DIR="$PROJECT_DIR/target/generated-sources/openapi"

echo "=== OpenAPI Generator Helper ==="
echo "Spec: $SPEC_PATH"
echo "Output: $OUTPUT_DIR"

# Check if spec exists
if [ ! -f "$SPEC_PATH" ]; then
    echo "ERROR: OpenAPI spec not found: $SPEC_PATH"
    exit 1
fi

# Clean and generate
echo "Cleaning generated sources..."
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

echo "Generating Kotlin Spring code..."
mvn generate-sources

echo "=== Generation Complete ==="
echo "Generated files:"
find "$OUTPUT_DIR" -type f -name "*.kt" | head -20