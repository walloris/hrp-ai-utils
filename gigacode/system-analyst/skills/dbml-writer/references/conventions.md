# Reference Guide: DBML Conventions

## Common Table Patterns

### Audit Fields
```dbml
created_at timestamp with time zone [default: now()]
updated_at timestamp with time zone [default: now()]
created_by text [not null]
updated_by text
```

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
create_date timestamptz [not null, default: 'now()']
modify_date timestamptz [default: 'now()']
```

## Foreign Key Reference Patterns

```dbml
-- Simple reference
ref: > table.column

-- With note
ref: > table.column [note: 'Описание связи']
```

## Index Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Regular Index | `idx_<table>_<column>` | `idx_operation_identifier` |
| Unique Index | `uk_<table>_<column>` | `uk_user_policy_person_id` |
| Foreign Key | `fk_<table>_<column>` | `fk_operation_id_structure` |
| Composite | `idx_<table>_<col1>_<col2>` | `idx_collect_feedback_user_id` |

## Common Timestamp Patterns

| Field | Type | Default | Usage |
|-------|------|---------|-------|
| `created_at` | `timestamp with time zone` | `now()` | Record creation |
| `updated_at` | `timestamp with time zone` | `now()` | Last modification |
| `create_date` | `timestamptz` | `'now()'` | Record creation (alternative) |
| `modify_date` | `timestamptz` | `'now()'` | Last modification (alternative) |
| `complete_date` | `timestamptz` | - | Business completion date |
| `time_notification` | `timestamptz` | - | Notification delivery time |

## Boolean Naming Patterns

| Pattern | Example | Description |
|---------|---------|-------------|
| `is_<feature>` | `is_active`, `is_archive` | State flags |
| `has_<feature>` | - | Less common |
| `<feature>` | `active_qa`, `season` | Short form for simple features |

## Primary Key Patterns

| Pattern | Example | Notes |
|---------|---------|-------|
| `id` | `id` | Generic, single table |
| `id_<table>` | `id_structure`, `id_fos`, `id_operation` | Project standard |
| `id_<feature>` | `id_question`, `id_answer`, `id_feedback` | Feature-specific |

## File Organization

```
doc-as-code/database_dbml/
├── voe.dbml              # Core application schema
├── admin_panel_schema.dbml  # Admin panel schema
├── quartz-schema.dbml    # Quartz scheduler tables
└── [module]-schema.dbml  # Module-specific schemas
```

## Migration Notes

When documenting schema changes:

1. **Add a section** at the top of the file:
```dbml
-- ==========================================
-- Migration Notes
-- ==========================================
-- [date]: [description of changes]
-- [date]: [description of changes]
```

2. **Document breaking changes** with migration steps
3. **Note data migration requirements** if applicable