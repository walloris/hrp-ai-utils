---
name: hrp-spring-boot-update
description: Выполняет комплексное обновление Spring Boot до рекомендованной версии и всех зависимостей.
---

# hrp-spring-boot-update

> Выполняет комплексное обновление Spring Boot до рекомендованной версии и всех зависимостей

## Входные параметры
- **project_path**: текущий проект

## Шаги выполнения

### 1. Получение рекомендованных версий с Confluence
```python
page = mcp__Atlassian__confluence_get_page(
    page_id="17151460968",
    convert_to_markdown=True
)
```

### 2. Обновление корневого pom.xml

**Обновляемые версии:**
- parent version: Рекомендуемая версия Spring Boot из page в 1 пункте
- spring-cloud-dependencies: Рекомендуемая версия Spring Cloud из page в 1 пункте
- новые версии зависимостей из page в 1 пункте

### 3. Обновление параметров конфигурации

**Замена устаревших параметров management.endpoint:**

| Старый                                       | Новый                                           |
|----------------------------------------------|-------------------------------------------------|
| management.endpoint.prometheus.enabled=true  | management.endpoint.prometheus.access=read-only |
| management.endpoint.metrics.enabled=true  | management.endpoint.metrics.access=read-only |
| management.endpoint.health.enabled=true  | management.endpoint.health.access=read-only |
| management.endpoint.loggers.enabled=true  | management.endpoint.loggers.access=read-only |

### 4. Обновление тестовых файлов


**Исключение автоконфигураций:**
- в application-test настройки добавить исключение spring.autoconfigure.exclude
- org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
- org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration

**Замены:**
- `@MockBean` → `@MockitoBean`
- `@SpyBean` → `@MockitoSpyBean`

**Добавляем импорты:**
- если в тесте используется @MockitoBean
```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;
```
- если в тесте используется @MockitoSpyBean
```java
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
```

**Пример обновления класса теста:**
```java
// До
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SpringBootTest
public class MyServiceTest {
    @MockBean
    private SomeService someService;
    @SpyBean
    private OtherService otherService;
}

// После
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
public class MyServiceTest {
    @MockitoBean
    private SomeService someService;
    @MockitoSpyBean
    private OtherService otherService;
}
```

### 5. Компиляция проекта
```bash
mvn clean compile -DskipTests
```

Проверяем выходной код:
- Exit code = 0 → Успех
- Exit code ≠ 0 → Анализ ошибок

---

## Ожидаемые результаты

| Критерий | Статус |
|----------|--------|
| pom.xml обновлен | ✅/❌ |
| Аннотации в тестах обновлены | ✅/❌ |
| Компиляция успешна | ✅/❌ |

---

## Проверка успешности

1. **POM версии:**
   ```bash
   grep -A 2 "spring-boot-starter-parent" pom.xml
   # Должно быть: Рекомендуемая версия Spring Boot из page в 1 пункте
   
   grep "spring-cloud-dependencies" pom.xml
   # Должно быть: Рекомендуемая версия Spring Cloud из page в 1 пункте
   ```

2. **Компиляция:**
   ```bash
   mvn clean compile -DskipTests
   # Должен завершиться без ошибок
   ```

---

## При возникновении ошибок

1. Проверить совместимость версий зависимостей
2. Удалить кэш Maven: `mvn clean install -U`
3. Проверить подключение к Maven репозиториям
4. Проверить Java версию (должна быть 21)

---

## Дополнительные команды

```bash
# Проверка зависимостей
mvn dependency:tree | grep spring-boot

# Проверка обновляемых артефактов
mvn versions:display-dependency-updates

# Запуск тестов после обновления
mvn test

# Генерация отчета о покрытии
mvn clean test jacoco:report
```

