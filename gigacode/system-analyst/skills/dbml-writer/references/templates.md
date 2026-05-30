# Шаблоны DBML

## Стандартная таблица с внешними ключами
```dbml
Table table_name {
  id bigint [pk, note: 'Первичный ключ']
  created_at timestamptz [not null, default: "now()", note: 'Дата создания']
  modified_at timestamptz [default: "now()", note: 'Дата изменения']
  
  // Обязательные поля
  name varchar [not null, note: 'Название']
  description text [note: 'Описание']
  
  // Булевы поля
  is_active boolean [default: "true", note: 'Активность']
  is_archive boolean [default: "false", note: 'Архив']
  
  // Внешний ключ
  parent_id bigint [not null, ref: > parent.id, note: 'Ссылка на parent']
  
  // Уникальное поле (ограничение уникальности)
  code varchar [not null, unique, note: 'Уникальный код']
}
```

## Таблицы с внешними ключами (рекомендуемый стиль)
Внешние ключи объявляются внутри таблиц через `ref: >`:
```dbml
Table orders {
  id bigint [pk, note: 'Первичный ключ']
  user_id bigint [not null, ref: > users.id, note: 'Ссылка на пользователя']
  product_id bigint [not null, ref: > products.id, note: 'Ссылка на товар']
  created_at timestamptz [not null, default: "now()", note: 'Дата создания']
}

Table users {
  id bigint [pk, note: 'Первичный ключ']
  email text [not null, unique, note: 'Email пользователя']
}

Table products {
  id bigint [pk, note: 'Первичный ключ']
  name varchar [not null, note: 'Название товара']
}
```

## Таблица с составным внешним ключом
Если поддерживается: `ref: > table.col1, table.col2`
```dbml
Table order_items {
  id bigint [pk, note: 'Первичный ключ']
  order_id bigint [not null, ref: > orders.id, note: 'Ссылка на заказ']
  product_id bigint [not null, ref: > products.id, note: 'Ссылка на товар']
}
```

## Пример из проекта (voe.dbml)
```dbml
Table structure {
  id_structure bigint [pk, note: 'Первичный ключ']
  name varchar [not null, note: 'Название подразделения']
  description text [note: 'Описание подразделения']
}

Table form_details {
  id_fos bigint [pk, note: 'Первичный ключ']
  author_id text [not null, note: 'Идентификатор автора формы']
  create_date timestamptz [not null, default: "now()", note: 'Дата создания формы']
  title text [not null, note: 'Заголовок формы']
  active boolean [not null, note: 'Флаг активности формы']
}

Table operation {
  id_operation bigint [pk, note: 'Первичный ключ']
  id_fos bigint [not null, ref: > form_details.id_fos, note: 'Ссылка на form_details']
  id_structure bigint [not null, ref: > structure.id_structure, note: 'Ссылка на structure']
  create_date timestamptz [not null, default: "now()", note: 'Дата создания операции']
  author_id text [not null, note: 'Идентификатор автора операции']
  type bigint [not null, ref: > dict.id, note: 'Тип операции']
  is_active boolean [default: "true", note: 'Активна']
}

Table collect_feedback {
  id_feedback bigint [pk, note: 'Первичный ключ']
  id_fos bigint [not null, ref: > form_details.id_fos, note: 'Ссылка на form_details']
  user_id bigint [not null, ref: > user_policy.id, note: 'Ссылка на user_policy']
  id_operation bigint [not null, ref: > operation.id_operation, note: 'Ссылка на operation']
  create_date timestamptz [not null, default: "now()", note: 'Дата создания']
  status bigint [not null, ref: > dict.id, note: 'Статус']
  is_active boolean [default: "true", note: 'Активна']
}

Table user_policy {
  id bigint [pk, note: 'Первичный ключ']
  person_id text [not null, note: 'Идентификатор персоны']
  last_active timestamptz [default: "now()", note: 'Последняя активность']
}

Table dict {
  id bigint [pk, note: 'Первичный ключ']
  value text [not null, note: 'Значение справочника']
}
```

## Что НЕ включать в .dbml файлы

### ❌ НЕ добавляйте индексы:
```dbml
// УДАЛИТЬ ВСЕ ЭТИ СТРОКИ!
Ref: idx_collect_feedback_user_operation < collect_feedback.user_id, collect_feedback.id_operation [name: idx_collect_feedback_user_operation]
Ref: idx_questions_fos < questions.id_fos [name: idx_questions_fos]
Ref: uk_user_policy_person_id < user_policy.person_id [unique: true, name: uk_user_policy_person_id]
Ref: uk_dict_value < dict.value [unique: true, name: uk_dict_value]
```

### ❌ НЕ добавляйте блоки `index { ... }`:
```dbml
// УДАЛИТЬ ВСЕ ЭТИ БЛОКИ!
index user_policy_person_idx {
  person_id
  unique
  note: 'Уникальный индекс'
}
```

**Почему?** Индексы — это физическая реализация в СУБД, не часть DBML спецификации.
