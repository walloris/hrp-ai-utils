# Справочник по DBML

## Валидные конструкции

### Таблицы
```dbml
Table table_name {
  id bigint [pk, note: 'Первичный ключ']
  field varchar [not null, note: 'Поле']
  created_at timestamptz [default: "now()", note: 'Дата создания']
}
```

### Внешние ключи (Foreign Keys)
Внешние ключи объявляются внутри таблицы через `ref: >`:
```dbml
Table orders {
  id bigint [pk]
  user_id bigint [not null, ref: > users.id, note: 'Ссылка на пользователя']
}
```

### Внешние ключи как отдельные `Ref` (опционально)
Можно объявить внешний ключ как отдельный `Ref`:
```dbml
Ref: fk_orders_user < orders.user_id > users.id
```

## НЕВАЛИДНЫЕ конструкции

### ❌ НЕВАЛИДНО - `Ref` для индексов
```dbml
-- Это вызовет ошибку: "A Ref field must be a binary relationship"
Ref: idx_user_email < users.email [name: idx_user_email]

-- Это тоже невалидно - составные колонки в Ref
Ref: idx_multi_col < table.col1, table.col2 [name: idx_multi_col]

-- Unique constraint в Ref тоже невалидно
Ref: uk_user_email < users.email [unique: true, name: uk_user_email]
```

### ❌ НЕВАЛИДНО - `index { ... }` блоки
```dbml
Table user_policy {
  id bigint [pk]
  person_id text [not null]
  index user_policy_person_idx {
    person_id
    unique
    note: 'Уникальный индекс'
  }
}
```

## Объяснение

### Что такое DBML?
DBML (Database Markup Language) — это **язык описания схемы базы данных**, а не язык создания базы данных.

### Что поддерживает DBML?
- ✅ Определение таблиц
- ✅ Определение полей с типами и ограничениями
- ✅ Первичные ключи
- ✅ Внешние ключи (внутри таблиц или отдельно)
- ✅ Комментарии в атрибуте `note`

### Что НЕ поддерживает DBML?
- ❌ Индексы (index definitions)
- ❌ Уникальные ограничения через `Ref: uk_...`
- ❌ Триггеры
- ❌ Процедуры и функции
- ❌ Views

### Почему нет индексов в DBML?
Индексы — это **физическая реализация** в СУБД для оптимизации запросов. Они:
- Не влияют на логическую структуру данных
- Могут быть созданы и удалены без изменения схемы
- Разные СУБД используют разные синтаксисы для индексов
- DBML фокусируется на логической модели, а не на физической реализации

## Правила для проекта

1. **Таблицы**: Определяйте все таблицы с полями
2. **Внешние ключи**: Используйте `ref: > table.column` внутри таблиц
3. **Индексы**: **НЕ добавляйте их в .dbml файлы**
4. **Уникальные ограничения**: Объявляйте как `unique` внутри поля или через `unique` constraint в таблице (если поддерживается)

## Пример валидного файла

```dbml
Table users {
  id bigint [pk, note: 'Первичный ключ']
  email text [not null, unique, note: 'Email пользователя']
  created_at timestamptz [default: "now()", note: 'Дата создания']
}

Table orders {
  id bigint [pk, note: 'Первичный ключ']
  user_id bigint [not null, ref: > users.id, note: 'Ссылка на пользователя']
  total double precision [note: 'Сумма заказа']
  created_at timestamptz [default: "now()", note: 'Дата создания']
}
```

**Важно:** Никаких `Ref: idx_...` и `index { ... }` блоков!
