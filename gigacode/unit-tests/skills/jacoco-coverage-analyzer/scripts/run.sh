#!/bin/bash
# jacoco-coverage-analyzer: Анализ отчёта JaCoCo и вывод топ-10 классов с наименьшим покрытием
# Usage: ./run.sh <project_path> [project_name]

# Проверка Python
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 не найден"
    exit 1
fi

# Параметры
PROJECT_PATH="${1:-.}"
PROJECT_NAME="${2:-}"

# Если путь не абсолютный, строим относительно IdeaProjects
if [[ "$PROJECT_PATH" != /* ]]; then
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
    echo "Usage: ./run.sh [project_path] [project_name]"
    echo "Примеры:"
    echo "  ./run.sh                    - текущая директория"
    echo "  ./run.sh app-learning-book-summarization  - из IdeaProjects"
    echo "  ./run.sh ../other-project   - соседний проект"
    exit 1
fi

# Получаем имя проекта из пути, если не указано
if [ -z "$PROJECT_NAME" ]; then
    PROJECT_NAME=$(basename "$PROJECT_PATH")
fi

# Запускаем Python-скрипт
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "$SCRIPT_DIR/analyze_coverage.py" "$PROJECT_PATH" "$PROJECT_NAME"
