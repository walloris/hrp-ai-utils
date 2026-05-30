---
name: jooq-writer
description: Work with jOOQ in the project. Teaches correct usage of jOOQ API, including case-insensitive comparisons and error handling.
---
# jOOQ Writer

jOOQ writer skill for working with jOOQ queries in PostgreSQL database.

## Important: Case-Insensitive Comparisons

jOOQ does NOT support `ilike()` or `containsIgnoreCase()` methods directly.

### Use `lower().like()` for Case-Insensitive Search

```java
// WRONG - ilike() not supported
LANDING.ALIAS.ilike(alias)

// CORRECT - use lower().like()
LANDING.ALIAS.lower().like("%" + alias.toLowerCase() + "%")

// WRONG - containsIgnoreCase() not supported
LANDING.NAME.containsIgnoreCase(filter.getFilter())

// CORRECT - use lower().like()
LANDING.NAME.lower().like("%" + filter.getFilter().toLowerCase() + "%")
```

### Pattern Summary

| Pattern | Correct jOOQ |
|---------|-------------|
| Contains (anywhere) | `field.lower().like("%" + value.toLowerCase() + "%")` |
| Starts with | `field.lower().like(value.toLowerCase() + "%")` |
| Ends with | `field.lower().like("%" + value.toLowerCase())` |
| Exact match | `field.lower().eq(value.toLowerCase())` |

## Best Practices

1. **Always use `.lower()` before `.like()` for PostgreSQL**
2. **Never use `ilike()` or `containsIgnoreCase()` - not supported**
3. **Verify compilation after jOOQ code changes**
4. **Use Maven with SSL flags for corporate environment:**

```bash
./mvnw -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true -U clean install
```

## Common Error Messages

| Error | Solution |
|-------|----------|
| `cannot find symbol: method ilike` | Replace with `.lower().like()` |
| `cannot find symbol: method containsIgnoreCase` | Replace with `.lower().like()` |
| `unexpected token: ILIKE` | jOOQ generates ILIKE, use `.lower().like()` instead |

## Notes

- This is a corporate project with PostgreSQL database
- PostgreSQL does not support ILIKE in jOOQ directly
- All case-insensitive comparisons must use `.lower().like()`
- The `.lower()` method is marked as deprecated but it's the only working solution
