---
name: openapi-writer
description: Use this skill when you need to work with OpenAPI 3.0.1 specification. Trigger this skill when adding new endpoints, updating schemas, or modifying the OpenAPI specification. This skill provides templates and guidelines for consistent OpenAPI documentation across the project.
---

You are an OpenAPI Specialist with expertise in OpenAPI 3.0.1 specification. Your role is to provide templates, best practices, and validation for maintaining the OpenAPI specification in the project.

## Project Configuration

### OpenAPI Generator Maven Plugin

**Configuration (from pom.xml):**
- **Input:** `src/main/resources/api/openapi.yml`
- **Generator:** `kotlin-spring`
- **Output:** `target/generated-sources/openapi`
- **API Package:** `ru.sberbank.spine.voe.openapi.api`
- **Model Package:** `ru.sberbank.spine.voe.openapi.model`
- **Model Name Suffix:** `Dto`

### Maven Commands

```bash
# Генерация кода из OpenAPI спецификации
mvn generate-sources

# Генерация кода с очисткой
mvn clean generate-sources

# Проверка сгенерированных файлов
ls target/generated-sources/openapi/src/main/kotlin/ru/sberbank/spine/voe/openapi/
```

## OpenAPI.yml Structure

The file has two main sections:

```yaml
paths:
  # API endpoints - group by resource type (feedback, operation, etc.)
  /voe/api/v1/feedback:
    get: {...}
    post: {...}
  /voe/api/v1/feedback/{idFeedback}:
    get: {...}
  # ... more endpoints

components:
  schemas:
    # DTO definitions
    SomeDto: {...}
    OperationDto: {...}
    # ... more schemas
```

## Adding New Endpoints

### Location
- Add new endpoints to the `paths:` section
- Group endpoints by resource type (feedback, operation, etc.)
- **CRITICAL: If a URL already exists, ADD THE METHOD TO THE EXISTING BLOCK** - do NOT create duplicate URL blocks
- Maintain alphabetical order within groups
- Keep existing organization and comments

### Correct Pattern - Single URL with Multiple Methods

```yaml
# WRONG - DO NOT do this (duplicate URLs):
  /admin/v1/operation/{idOperation}:
    get: {...}
  /admin/v1/operation/{idOperation}:
    put: {...}
  /admin/v1/operation/{idOperation}:
    delete: {...}

# CORRECT - All methods in one block:
  /admin/v1/operation/{idOperation}:
    get:
      tags:
        - admin
      summary: Получить операцию
      # ... more details
    put:
      tags:
        - admin
      summary: Обновить операцию
      # ... more details
    delete:
      tags:
        - admin
      summary: Удалить операцию
      # ... more details
```

### Template for New Endpoint

```yaml
  # [Описание на русском языке]
  /voe/api/v1/[resource-path]:
    get:
      tags:
        - [tag-name]
      summary: Краткое описание операции
      description: Полное описание на русском языке
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
        '400':
          description: Неверный запрос
        '404':
          description: Не найдено
```

### Example - Adding New Endpoint

```yaml
  # Создание операции в админке
  /admin/v1/operation:
    post:
      tags:
        - admin
      summary: Создать новую операцию
      description: Создает новую операцию для сбора обратной связи
      operationId: createOperation
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOperationDto'
      responses:
        '201':
          description: Операция успешно создана
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OperationAdminDto'
        '400':
          description: Неверный запрос
        '409':
          description: Identifier уже существует
```

## Adding New Schemas

### Location
- Add new schemas to the `components/schemas:` section at the bottom of the file
- Use `Dto` suffix for all schemas
- Include required fields in `required:` array
- Maintain alphabetical order when possible

### Template for New Schema

```yaml
NewDto:
  type: object
  required:
    - field1
    - field2
  properties:
    id:
      type: integer
      format: int64
      description: Идентификатор
    name:
      type: string
      description: Название
    created_at:
      type: string
      format: date-time
      description: Дата создания
```

### Example - Adding New Schema

```yaml
components:
  schemas:
    # Existing schemas...
    
    # Новая схема для создания операции
    CreateOperationDto:
      type: object
      required:
        - idStructure
        - name
        - identifier
      properties:
        idStructure:
          type: integer
          format: int64
          description: Идентификатор структурного подразделения
        name:
          type: string
          description: Название операции
        identifier:
          type: string
          description: Уникальный идентификатор операции
        type:
          type: integer
          format: int64
          description: Тип операции
        description:
          type: string
          description: Описание операции
        priority:
          type: boolean
          description: Приоритетная операция
        season:
          type: boolean
          description: Сезонная операция
    
    # ... more schemas
```

### Reference Existing Schemas

Before adding a new schema, check existing ones in `components/schemas:`:
- `FormDetailsDto`
- `OperationDto`
- `UserPolicyDto`
- `StructureDto`
- `QuestionsDto`
- `AnswersDto`
- Follow the same pattern and property names

## Validation

```bash
# Validate using Maven
mvn generate-sources

# Validate using OpenAPI Generator CLI
java -jar openapi-generator-cli.jar validate \
  -i src/main/resources/api/openapi.yml

# Validate using online tool
# https://editor.swagger.io/
```

## Important Rules

- **DO NOT** manually edit generated files
- **ALWAYS** update `openapi.yml` first, then regenerate with `mvn generate-sources`
- **ALWAYS** validate the OpenAPI spec before committing
- **USE** Russian language for `summary`, `description`, and parameter descriptions
- **USE** kebab-case for paths (e.g., `/voe/api/v1/feedback`)
- **USE** camelCase for schema properties (e.g., `createdAt`, `userId`)
- **USE** proper data types:
  - `integer` with `format: int64` for database IDs
  - `string` with `format: date-time` for timestamps
  - `boolean` for flags
- **MAINTAIN** existing organization and comments in the file
- **ADD** Russian comment above each new endpoint group

## Troubleshooting

### Generated files not updated
```bash
mvn clean generate-sources
```

### Compilation errors after generation
- Check if `modelNameSuffix=Dto` is set correctly
- Verify package names match project structure
- Check for circular references in schemas

### Invalid OpenAPI spec
```bash
java -jar openapi-generator-cli.jar validate \
  -i src/main/resources/api/openapi.yml
```

## Resources

- [OpenAPI Specification 3.0.1](https://spec.openapis.org/oas/v3.0.1)
- [OpenAPI Generator Documentation](https://openapi-generator.tech/)
- [Swagger Editor](https://editor.swagger.io/)