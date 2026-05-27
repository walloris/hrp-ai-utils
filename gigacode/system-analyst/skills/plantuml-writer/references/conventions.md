# Reference Guide: PlantUML Conventions

## Common Patterns

### Standard lifeline definition
```puml
actor Участник
participant "Участник2" as participant2
participant "Участник3" as participant3
```

### Numbered steps
```puml
== 1. Инициализация ==
== 2. Получение данных ==
== 3. Обработка ==
```

### Conditional flows
```puml
alt Условие 1
    participant1 --> participant2: Результат 1
else Условие 2
    participant1 --> participant2: Результат 2
end
```

### Optional steps
```puml
opt Если условие выполнено
    participant1 -> participant2: Действие
end
```

### Loop
```puml
loop Для каждого элемента
    participant1 -> participant2: Обработка
end
```

## Project-Specific Participants

| Name | Usage |
|------|-------|
| `Пользователь` | End user (actor) |
| `Фронтенд` / `Frontend` | Frontend application |
| `Бэкенд API` / `Backend` | Backend services |
| `PostgreSQL` / `БД` / `DB` | Database |
| `NotificationService` / `NS` | Notification service |
| `Колокольчик NotificationService` | Notification bell service |

## Common API Patterns

```puml
Frontend -> Backend: GET /voe/api/v1/feedback/{idFeedback}
Backend -> Database: SELECT * FROM table WHERE id = ?
Database --> Backend: Data
Backend --> Frontend: Response
```

## Feedback Collection Flow

Standard feedback collection process includes:
1. **Инициализация** - Initial page load/form initialization
2. **Получение данных** - Retrieve operation, form, questions
3. **Заполнение формы** - User fills answers
4. **Отправка** - Submit feedback
   - **Отправить** - Complete feedback
   - **Отложить** - Postpone with notification
   - **Закрыть** - Cancel

## File Naming

| Pattern | Example | Use Case |
|---------|---------|----------|
| `kebab-case` | `feedback-collection.puml` | General processes |
| `snake_case` | `feedback_from_main_page.puml` | Page-specific flows |
| `module-process` | `operation-creation.puml` | Module workflows |

## Directory Structure

```
doc-as-code/
├── system_functionality/      # Core functionality diagrams
├── admin-panel/              # Admin panel workflows
├── asyncapi/                 # Async messaging patterns
└── [module]/                 # Module-specific processes
```