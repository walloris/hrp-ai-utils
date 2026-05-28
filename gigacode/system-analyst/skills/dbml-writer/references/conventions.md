# Reference Guide: DBML Conventions

## Table of Contents
- [Common Table Patterns](#common-table-patterns)
- [Official DBML Syntax](#official-dbml-syntax)
- [Foreign Key Reference Patterns](#foreign-key-reference-patterns)
- [Index Naming Conventions](#index-naming-conventions)
- [Timestamp Patterns](#timestamp-patterns)
- [Boolean Naming Patterns](#boolean-naming-patterns)
- [Primary Key Patterns](#primary-key-patterns)
- [File Organization](#file-organization)
- [Migration Notes](#migration-notes)

---

## Common Table Patterns

### Status Fields
```dbml
status bigint [not null, ref: > dict.id]
is_active boolean [default: true]
is_deleted boolean [default: false]
```

### Versioning
```dbml
version int [default: 0]
```

### Timestamps (Alternative Naming)
```dbml
create_date timestamptz [not null, default: "now()"]
modify_date timestamptz [default: "now()"]
```

---

## Official DBML Syntax

### Basic Table Structure
```dbml
Table table_name {
  column_name datatype [constraints] [note: 'Comment']
}
```

### Data Types

| Type | Description | Example |
|------|-------------|---------|
| `bigint` | 64-bit integer | `id bigint [primary key]` |
| `int` / `integer` | 32-bit integer | `count int` |
| `double precision` | 64-bit floating point | `price double precision` |
| `varchar(n)` | Variable-length string | `name varchar(255)` |
| `text` | Unlimited length text | `description text` |
| `boolean` | True/false value | `is_active boolean` |
| **`timestamptz`** | **Timestamp with timezone (shorthand)** | `created_at timestamptz` |
| `date` | Date only | `birth_date date` |
| `time` | Time only | `start_time time` |
| `decimal(p,s)` | Decimal number | `amount decimal(10,2)` |
| `json` | JSON data | `config json` |

### Column Constraints

| Constraint | Description | Example |
|------------|-------------|---------|
| `primary key` / `pk` | Primary key column | `id bigint [pk]` |
| `not null` | Value is required | `email varchar [not null]` |
| `null` / `nullable` | Value can be empty | `phone varchar [nullable]` |
| `unique` | All values must be unique | `email varchar [unique]` |
| `increment` / `incremental` | Auto-incrementing | `id bigint [increment]` |
| `index` | Create index on column | `status int [index]` |
| `ref: > table.column` | Foreign key reference | `user_id bigint [ref: > users.id]` |
| **`default: "now()"`** | **Default value (with quotes and parentheses)** | `created_at timestamptz [default: "now()"]` |
| `note: 'text'` | Comment in Russian | `name varchar [note: 'Имя пользователя']` |

### Composite Primary Key
```dbml
Table user_roles {
  user_id bigint [not null]
  role_id bigint [not null]
  
  pk [user_id, role_id]  -- Composite primary key
}
```

---

## Foreign Key Reference Patterns

### Simple Reference
```dbml
Table orders {
  id bigint [primary key]
  user_id bigint [not null, ref: > users.id]
}
```

### Reference with Note
```dbml
Table orders {
  id bigint [primary key]
  user_id bigint [not null, ref: > users.id, note: 'Ссылка на пользователя']
}
```

### Separate Ref Block
```dbml
Table orders {
  id bigint [primary key]
  user_id bigint [not null]
}

Table users {
  id bigint [primary key]
}

Ref: orders.user_id > users.id
```

---

## Index Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Regular Index | `idx_<table>_<column>` | `idx_orders_user_id` |
| Unique Index | `uk_<table>_<column>` | `uk_users_email` |
| Foreign Key | `fk_<table>_<column>` | `fk_orders_user_id` |
| Composite | `idx_<table>_<col1>_<col2>` | `idx_orders_user_date` |

### Index Syntax
```dbml
-- Simple index
Ref: idx_users_email < users.email [name: idx_users_email]

-- Composite index
Ref: idx_orders_user_date < orders.user_id, orders.created_at [name: idx_orders_user_date]

-- Unique index
Ref: uk_users_phone < users.phone [name: uk_users_phone]
```

---

## Timestamp Patterns

| Field | Type | Default | Usage |
|-------|------|---------|-------|
| `created_at` | `timestamptz` | `"now()"` | Record creation |
| `updated_at` | `timestamptz` | `"now()"` | Last modification |
| `create_date` | `timestamptz` | `"now()"` | Record creation (alternative) |
| `modify_date` | `timestamptz` | `"now()"` | Last modification (alternative) |
| `complete_date` | `timestamptz` | - | Business completion date |
| `time_notification` | `timestamptz` | - | Notification delivery time |
| `start_at` | `timestamptz` | - | Start time |
| `end_at` | `timestamptz` | - | End time |
| `valid_from` | `timestamptz` | - | Validity start |
| `valid_to` | `timestamptz` | - | Validity end |

### Important Note on Timestamp Default Values
- Use `timestamptz` (not `timestamp with time zone`)
- Use `default: "now()"` with quotes and parentheses

### Example with Timestamps
```dbml
Table users {
  id bigint [primary key]
  created_at timestamptz [default: "now()"]
  updated_at timestamptz [default: "now()"]
}
```

---

## Boolean Naming Patterns

| Pattern | Example | Description |
|---------|---------|-------------|
| `is_<feature>` | `is_active`, `is_archive` | State flags |
| `has_<feature>` | `has_children` | Existence flags |
| `<feature>` | `active_qa`, `season` | Short form for simple features |
| `<status>` | `is_deleted`, `is_locked` | Status flags |

### Default Values for Booleans
```dbml
-- Active/inactive flags - default true (with quotes)
is_active boolean [default: "true"]

-- Archive/delete flags - default false (with quotes)
is_archive boolean [default: "false"]
is_deleted boolean [default: "false"]

-- Optional features - default false
has_notifications boolean [default: "false"]
```

---

## Primary Key Patterns

| Pattern | Example | Notes |
|---------|---------|-------|
| `id` | `id` | Generic, single table |
| `id_<table>` | `id_structure`, `id_fos`, `id_operation` | Project standard |
| `id_<feature>` | `id_question`, `id_answer`, `id_feedback` | Feature-specific |

### Single vs Composite Keys
```dbml
-- Single primary key
Table users {
  id bigint [primary key]
}

-- Composite primary key
Table user_roles {
  user_id bigint [not null]
  role_id bigint [not null]
  pk [user_id, role_id]
}
```

---

## File Organization

```
doc-as-code/database_dbml/
├── service-name.dbml              # Core application schema
├── admin_panel_schema.dbml        # Admin panel schema
├── quartz-schema.dbml             # Quartz scheduler tables
├── [module]-schema.dbml           # Module-specific schemas
└── [domain]-schema.dbml           # Domain-specific schemas
```

---

## Migration Notes

When documenting schema changes:

1. **Add a section** at the top of the file:
```dbml
-- ==========================================
-- Migration Notes
-- ==========================================
-- 2024-01-15: Added users table and orders relationship
-- 2024-02-20: Added is_archive flag to products
```

2. **Document breaking changes** with migration steps
3. **Note data migration requirements** if applicable

---

## Advanced Syntax

### Table Grouping
```dbml
TableGroup main {
  Table users
  Table orders
  Table products
}
```

### Color Coding (Visualization)
```dbml
Table users {
  id bigint [primary key, color: '#3498db']
  name varchar(255) [not null]
}

Table orders {
  id bigint [primary key, color: '#e74c3c']
  user_id bigint [not null, ref: > users.id]
}
```

Color options: blue, red, green, yellow, purple, orange, pink, cyan, lime, indigo, teal
Or use hex colors: `color: '#FF5733'`

### Database Settings
```dbml
// Database: myapp
// DBMS: PostgreSQL

Table users {
  id bigint [primary key]
}
```

---

## Summary of Key Conventions

### Timestamp Fields
- **Data type**: `timestamptz` (NOT `timestamp with time zone`)
- **Default value**: `default: "now()"` (with quotes and parentheses)

### Boolean Fields
- **Default values**: Use quotes for boolean defaults
  - `default: "true"`
  - `default: "false"`

### Examples of Correct Syntax
```dbml
-- Correct timestamp syntax
created_at timestamptz [default: "now()"]
updated_at timestamptz [default: "now()"]

-- Correct boolean syntax
is_active boolean [default: "true"]
is_archive boolean [default: "false"]

-- Full table example
Table users {
  id bigint [primary key]
  created_at timestamptz [default: "now()"]
  updated_at timestamptz [default: "now()"]
  is_active boolean [default: "true"]
  email varchar(255) [not null, unique]
}
```
