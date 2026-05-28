# Reference Guide: PlantUML Conventions

## Table of Contents
- [Common Patterns](#common-patterns)
- [Basic Syntax](#basic-syntax)
- [Sequence Diagram Elements](#sequence-diagram-elements)
- [Conditional Flows](#conditional-flows)
- [Advanced Elements](#advanced-elements)
- [Project-Specific Participants](#project-specific-participants)
- [API Patterns](#api-patterns)
- [File Organization](#file-organization)

---

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

### Repeating while loop (with condition check at the beginning of the loop)
```puml
while (данные доступны?)
  :чтение данных;
  :построение диаграммы;
endwhile
```

### Parallelization of activity [multithreading mode: fork, fork again, end fork and merge]
```puml
start

if (многопоточность?) then (да)
  fork
    :Поток 1;
  fork again
    :Поток 2;
  end fork
else (всё в одном\nпотоке)
  :Поток 1;
  :Поток 2;
endif

stop
```

### Parallelization of activity [split mode: split, split again and end split]
```puml
start
split
  ->вар1;
   :Действие 1;
   kill
split again
  ->вар2;
   :Действие 2.1;
   :Действие 2.2;
   detach
split again
  ->вар3;
   (Z)
   detach
split again
  ->вар4;
   end
split again
  ->вар5;
   stop
end split
```

---

## Basic Syntax

### Activation/Deactivation
```puml
actor User
participant Service

User -> Service: Request
activate Service
Service -> Database: Query
activate Database
Database --> Service: Data
deactivate Database
Service --> User: Response
deactivate Service
```

### Notes
```puml
note right of participant: Important note
note left of participant: Another note
```

### Alt-Else-End (Conditional)
```puml
alt Success
    A -> B: Success path
else Failure
    A -> B: Error path
end
```

### Opt (Optional)
```puml
opt If condition is met
    A -> B: Optional action
end
```

### Loop
```puml
loop For each item
    A -> B: Process item
end
```

### While Loop
```puml
while (condition?) is (true)
  :action;
endwhile
```

---

## Sequence Diagram Elements

### Message Types

| Type | Syntax | Description |
|------|--------|-------------|
| Sync | `->` | Synchronous call (solid arrow) |
| Async | `->>` | Asynchronous call (solid arrow with head) |
| Return | `-->` | Return message (dashed arrow) |
| Async Return | `-->>` | Async return (dashed arrow with head) |

### Message Examples
```puml
A -> B: Sync call (solid arrow)
A ->> B: Async call
B --> A: Return (dashed)
B -->> A: Async return
```

### Self-Message
```puml
participant -> participant: Self call
```

### Participant Creation/Destroy
```puml
create participant2
participant1 -> participant2: Create
participant2 -> participant3: Use
destroy participant2
```

### Critical Region
```puml
critical
    participant1 -> participant2: Critical section
end
```

### Break
```puml
break Condition
    participant1 -> participant2: Break action
end
```

### Parallel (Par)
```puml
par First parallel
    participant1 -> participant2: Action 1
and Second parallel
    participant3 -> participant4: Action 2
end
```

---

## Advanced Elements

### Activity Diagram (State)
```puml
@startuml
[*] --> State1
State1 --> [*]
State1 <-> State2
State2 --> [*]
@enduml
```

### Component Diagram
```puml
@startuml
component Component1
component Component2
Component1 --> Component2
@enduml
```

### Class Diagram
```puml
@startuml
class Class1 {
  +method()
  -attribute
}
Class1 --> Class2
@enduml
```

### Deployment Diagram
```puml
@startuml
node Node1 {
  component Component1
}
node Node2 {
  component Component2
}
Node1 --> Node2
@enduml
```

### Artifact Diagram
```puml
@startuml
artifact Artifact1 {
  file "file.txt"
}
Artifact1 --> Artifact2
@enduml
```

### Package Diagram
```puml
@startuml
package "Package1" {
  class Class1
  class Class2
}
package "Package2" {
  class Class3
}
Package1 --> Package2
@enduml
```

---

## Project-Specific Participants

| Name | Usage | Alias Examples |
|------|-------|----------------|
| `Пользователь` | End user (actor) | `actor Пользователь` |
| `Фронтенд` / `Frontend` | Frontend application | `participant "Фронтенд" as FE` |
| `Бэкенд API` / `Backend` | Backend services | `participant "Бэкенд API" as BE` |
| `PostgreSQL` / `БД` / `DB` | Database | `participant "PostgreSQL" as DB` |
| `NotificationService` / `NS` | Notification service | `participant "NS" as NS` |
| `Колокольчик NotificationService` | Notification bell service | `participant "Колокольчик" as Bell` |

### Common Patterns
```puml
actor Пользователь
participant "Фронтенд" as FE
participant "Бэкенд API" as BE
participant "PostgreSQL" as DB
participant "NotificationService" as NS
```

---

## API Patterns

### REST API Requests
- Write the entire request on **one line** including method, path, and body
- Use format: `Frontend -> Backend: METHOD /path Body: {...}`

```puml
Frontend -> Backend: GET /voe/api/v1/feedback/{idFeedback}
Frontend -> Backend: POST /voe/api/v1/feedback/submit Body: {answers: [...]}
Frontend -> Backend: PUT /voe/api/v1/feedback/complete/{idFeedback} Body: {timeNotification, timeZone}
```

### SQL Database Queries
- **CRITICAL: Write entire SQL query on ONE LINE - no line breaks**
- Use abbreviated format

```puml
Backend -> DB: SELECT form, questions WHERE id = ?
Backend -> DB: INSERT INTO table (col1, col2) VALUES (val1, val2)
Backend -> DB: UPDATE table SET col = value WHERE id = ?
Backend -> DB: DELETE FROM table WHERE id = ?
Backend -> DB: SELECT SUM(column), AVG(column) FROM table
```

### Error Responses
```puml
Backend --> Frontend: 404 "Not Found"
Backend --> Frontend: 500 "Internal Server Error"
Backend --> Frontend: 400 "Bad Request"
```

---

## File Organization

### Directory Structure
```
doc-as-code/
├── system_functionality/      # Core functionality diagrams
├── admin-panel/              # Admin panel workflows
├── asyncapi/                 # Async messaging patterns
└── [module]/                 # Module-specific processes
```

### File Naming
| Pattern | Example | Use Case |
|---------|---------|----------|
| `kebab-case` | `feedback-collection.puml` | General processes |
| `snake_case` | `feedback_from_main_page.puml` | Page-specific flows |
| `module-process` | `operation-creation.puml` | Module workflows |

### Standard File Names
- `feedback-collection.puml` - Main feedback process
- `operation-creation.puml` - Operation creation flow
- `user-onboarding.puml` - User registration flow
- `notification-process.puml` - Notification handling

---

## Best Practices

### 1. Keep Messages Concise
- Use clear, short descriptions
- Focus on what happens, not implementation details

### 2. Use Proper Lifeline Termination
- Always close conditional blocks
- Deactivate participants when done

### 3. Consistent Naming
- Use project-standard participant names
- Keep aliases short and memorable

### 4. Document Edge Cases
- Include error paths
- Document timeout scenarios
- Add alternative flows

### 5. Color Coding (Optional)
```puml
actor User #FF6B6B
participant Backend #4ECDC4
participant DB #45B7D1
```
