#!/bin/bash
# run-automation-tests: Запуск всех автотестов и проверка результатов
# Usage: ./run-tests.sh <project_path> [project_name]

set -e

# Используем Maven из GIGA IDE
# Поддержка нескольких версий GIGA IDE
if [ -f "/Applications/GIGA IDE PRO 2025.1.app/Contents/plugins/maven/lib/maven3/bin/mvn" ]; then
    MVN_CMD="/Applications/GIGA IDE PRO 2025.1.app/Contents/plugins/maven/lib/maven3/bin/mvn"
elif [ -f "/Applications/GIGA IDE PRO 2024.5.app/Contents/plugins/maven/lib/maven3/bin/mvn" ]; then
    MVN_CMD="/Applications/GIGA IDE PRO 2024.5.app/Contents/plugins/maven/lib/maven3/bin/mvn"
elif [ -f "/Applications/GIGA IDE BACK 2024.2 EAP.app/Contents/plugins/maven/lib/maven3/bin/mvn" ]; then
    MVN_CMD="/Applications/GIGA IDE BACK 2024.2 EAP.app/Contents/plugins/maven/lib/maven3/bin/mvn"
else
    echo "❌ Maven не найден в GIGA IDE"
    exit 1
fi

# Параметры
PROJECT_PATH="${1:-.}"
PROJECT_NAME="${2:-$(basename "$PROJECT_PATH")}"

# Если путь не абсолютный, строим относительно IdeaProjects
if [[ "$PROJECT_PATH" != /* ]]; then
    # Если это просто имя проекта (без ./ или ../), ищем в IdeaProjects
    if [[ "$PROJECT_PATH" != ./* ]] && [[ "$PROJECT_PATH" != ../ ]]; then
        PROJECT_PATH="$HOME/IdeaProjects/$PROJECT_PATH"
    fi
fi

# Приводим к абсолютному пути
if [ -d "$PROJECT_PATH" ]; then
    PROJECT_PATH=$(cd "$PROJECT_PATH" && pwd)
fi

# Проверка аргументов
if [ -z "$PROJECT_PATH" ] || [ ! -d "$PROJECT_PATH" ]; then
    echo "❌ Директория не найдена: $PROJECT_PATH"
    echo "Usage: ./run-tests.sh [project_path] [project_name]"
    echo "Примеры:"
    echo "  ./run-tests.sh                    - текущая директория"
    echo "  ./run-tests.sh app-learning-book-summarization  - из IdeaProjects"
    echo "  ./run-tests.sh ../other-project   - соседний проект"
    exit 1
fi

if [ ! -d "$PROJECT_PATH" ]; then
    echo "❌ Директория не найдена: $PROJECT_PATH"
    exit 1
fi

# Проверка наличия pom.xml
if [ ! -f "$PROJECT_PATH/pom.xml" ]; then
    echo "❌ pom.xml не найден в: $PROJECT_PATH"
    exit 1
fi

cd "$PROJECT_PATH"

echo "=== Запуск автотестов ==="
echo "Project: $PROJECT_PATH ($PROJECT_NAME)"
echo "Time: $(date)"
echo "Maven: $MVN_CMD"
echo ""

# Запуск тестов с сохранением output
MVN_OUTPUT=$(mktemp)
ERRORS=0

echo "Выполнение: mvn clean test"
echo "----------------------------------------"

if ! "$MVN_CMD" clean test > "$MVN_OUTPUT" 2>&1; then
    ERRORS=1
fi

echo "----------------------------------------"
echo ""

# Парсинг результатов (POSIX-совместимый способ)
# Ищем строки вида: "Tests run: 42, Failures: 0, Errors: 0, Skipped: 0"
TESTS_LINE=$(grep -E "Tests run:" "$MVN_OUTPUT" | tail -1 || echo "")

if [ -n "$TESTS_LINE" ]; then
    TOTAL_TESTS=$(echo "$TESTS_LINE" | sed 's/.*Tests run: \([0-9]*\).*/\1/')
    FAILED=$(echo "$TESTS_LINE" | sed 's/.*Failures: \([0-9]*\).*/\1/')
    ERRORS_COUNT=$(echo "$TESTS_LINE" | sed 's/.*Errors: \([0-9]*\).*/\1/')
    SKIPPED=$(echo "$TESTS_LINE" | sed 's/.*Skipped: \([0-9]*\).*/\1/')
else
    TOTAL_TESTS="неизвестно"
    FAILED=0
    ERRORS_COUNT=0
    SKIPPED=0
fi

# Проверка времени выполнения
DURATION=$(grep "Total time:" "$MVN_OUTPUT" | sed -E 's/.*Total time: +([0-9]+m)? *([0-9.]+s).*/\1\2/' | sed 's/mins/min/' | sed 's/mins/ min/' | tail -1 || echo "unknown")
if [ -z "$DURATION" ]; then
    DURATION="unknown"
fi

echo "=== Результаты тестирования ==="
if [ "$TOTAL_TESTS" != "неизвестно" ] && [ -n "$TOTAL_TESTS" ]; then
    PASSED=$((TOTAL_TESTS - FAILED - ERRORS_COUNT - SKIPPED))
    echo "Прошло: $PASSED"
else
    echo "Прошло: неизвестно"
fi
echo "Всего тестов: ${TOTAL_TESTS:-неизвестно}"
echo "Провалено: ${FAILED:-0}"
echo "Ошибок: ${ERRORS_COUNT:-0}"
echo "Пропущено: ${SKIPPED:-0}"
echo "Время: ${DURATION:-неизвестно}s"
echo ""

# Проверка критериев успеха
SUCCESS=true

if [ "$ERRORS" -eq 1 ]; then
    echo "❌ Ошибка выполнения mvn test (см. подробный лог выше)"
    SUCCESS=false
fi

FAILED_COUNT=${FAILED:-0}
if [ "$FAILED_COUNT" -gt 0 ] 2>/dev/null; then
    echo "❌ Провалено тестов: $FAILED_COUNT"
    SUCCESS=false
fi

ERRORS_COUNT=${ERRORS_COUNT:-0}
if [ "$ERRORS_COUNT" -gt 0 ] 2>/dev/null; then
    echo "❌ Ошибок в тестах: $ERRORS_COUNT"
    SUCCESS=false
fi

SKIPPED_COUNT=${SKIPPED:-0}
if [ "$SKIPPED_COUNT" -gt 0 ] 2>/dev/null; then
    echo "⚠️  Пропущено тестов: $SKIPPED_COUNT"
fi

if [ "$SUCCESS" = true ]; then
    echo "✅ Все тесты успешно прошли"
    rm -f "$MVN_OUTPUT"
    exit 0
else
    echo "❌ Тесты завершились с ошибками"
    echo ""
    echo "Полный лог сохранён в: $MVN_OUTPUT"
    exit 1
fi
