# База данных и миграции

## Liquibase

Все миграции базы данных управляются через Liquibase.

## Структура

```
dbscripts/liquibase/
├── classpath/              # Java classpath resources
├── sland/                  # Основные changelogs
│   ├── changelog/          # Версионированные changelogs
│   │   ├── v1.0/
│   │   ├── v1.1/
│   │   └── ...
│   └── changelog.xml       # Главный changelog
└── liquibase.properties    # Конфигурация Liquibase
```

## Главный changelog

Файл `changelog.xml` содержит все инклюды версионированных changelogs:

```xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="...">
    
    <include file="changelog/v1.0/changelog-v1.0-cumulative.xml"/>
    <include file="changelog/v1.1/changelog-v1.1-cumulative.xml"/>
    <include file="changelog/v1.2/changelog-v1.2-cumulative.xml"/>
    
    <!-- Environment-specific -->
    <include file="changelog/pangolin-ready/pangolin-ready.yaml" context="sigma"/>
    
</databaseChangeLog>
```

## Naming Conventions

- **Cumulative changelogs**: `changelog-v<version>-cumulative.xml`
- **Environment-specific**: файлы с `context="<env>"` в инклюдах

## Поддерживаемые версии

Версии базы данных организованы по мейн-версиям:
- v1.0 - v1.6 (ранние версии)
- v2.0+ (текущая серия)

## Конфигурация liquibase.properties

```properties
url=jdbc:postgresql://localhost:5432/database
username=user
password=password
changeLogFile=changelog.xml
```
