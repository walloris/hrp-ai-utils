# Reference Guide: OpenAPI CLI Commands

## Maven Commands

```bash
# Generate code from OpenAPI spec
mvn generate-sources

# Clean and regenerate
mvn clean generate-sources

# Check generated sources
ls -la target/generated-sources/openapi/src/main/kotlin/ru/sberbank/spine/voe/openapi/
```

## OpenAPI Generator CLI

### Download CLI
```bash
curl -O https://github.com/OpenAPITools/openapi-generator/releases/download/v7.8.0/openapi-generator-cli.jar
```

### Validate Specification
```bash
java -jar openapi-generator-cli.jar validate \
  -i src/main/resources/api/openapi.yml
```

### List Generators
```bash
java -jar openapi-generator-cli.jar list
```

### Show Generator Options
```bash
java -jar openapi-generator-cli.jar author config \
  -g kotlin-spring
```

### Generate Kotlin Spring Code
```bash
java -jar openapi-generator-cli.jar generate \
  -i src/main/resources/api/openapi.yml \
  -g kotlin-spring \
  -o target/generated-sources/openapi \
  --additional-properties=apiPackage=ru.sberbank.spine.voe.openapi.api, \
    modelPackage=ru.sberbank.spine.voe.openapi.model, \
    modelNameSuffix=Dto, \
    interfaceOnly=true, \
    useSpringController=false, \
    useTags=true, \
    skipDefaultInterface=true, \
    basePackage=ru.sberbank.spine.voe, \
    configPackage=ru.sberbank.spine.voe.config, \
    dateLibrary=java, \
    java11=true, \
    useBeanValidation=false
```

### Generate Documentation
```bash
# Markdown
java -jar openapi-generator-cli.jar generate \
  -i src/main/resources/api/openapi.yml \
  -g markdown \
  -o docs/api

# HTML
java -jar openapi-generator-cli.jar generate \
  -i src/main/resources/api/openapi.yml \
  -g html \
  -o docs/html

# Confluence
java -jar openapi-generator-cli.jar generate \
  -i src/main/resources/api/openapi.yml \
  -g confluence \
  -o docs/confluence
```

## Common Patterns

### Adding New Endpoint
```yaml
/voe/api/v1/endpoint:
  get:
    tags:
      - tag-name
    summary: Краткое описание
    description: Полное описание на русском
    operationId: operationName
    parameters:
      - name: param
        in: query/path/body
        required: true/false
        schema:
          type: string/integer/boolean
          description: Описание параметра
    responses:
      '200':
        description: Успешный ответ
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DtoName'
```

### Adding New Schema
```yaml
NewDto:
  type: object
  properties:
    id:
      type: integer
      format: int64
      description: Идентификатор
    name:
      type: string
      description: Название
  required:
    - id
    - name
```

## Validation

```bash
# Using OpenAPI Generator CLI
java -jar openapi-generator-cli.jar validate \
  -i src/main/resources/api/openapi.yml

# Using online Swagger Editor
# https://editor.swagger.io/
```

## Troubleshooting

### Clean Build
```bash
mvn clean generate-sources
```

### Check Generated Files
```bash
ls -la target/generated-sources/openapi/src/main/kotlin/ru/sberbank/spine/voe/openapi/
```

### Validate Spec
```bash
java -jar openapi-generator-cli.jar validate \
  -i src/main/resources/api/openapi.yml
```