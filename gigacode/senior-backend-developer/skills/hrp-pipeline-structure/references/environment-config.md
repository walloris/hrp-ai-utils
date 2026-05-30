# Конфигурация среды

## Структура ConfigMap

Конфигурация среды хранится в `config/<ENV>/configmap.yaml` в формате Kubernetes ConfigMap.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: <service-name>
data:
  <KEY>: "<value>"
```

## Поддерживаемые среды

| Код | Описание | Использование |
|-----|----------|---------------|
| DEV | Development | Локальная разработка |
| QA | Quality Assurance | Тестирование функционала |
| UAT | User Acceptance Testing | Приёмочное тестирование |
| BLK | Blackbox Testing | Внешнее тестирование |
| IFT | Integration Form Testing | Интеграционное тестирование |
| PROM | Production | Продакшн |

## Пример конфигурации

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: spine-landing-manager-app
data:
  HRP_CONFIGMANAGER_CONFIG_ENABLED: "false"
  HRP_CONFIGMANAGER_CONFIG_API_ENABLED: "true"
  SPRING_PROFILES_ACTIVE: "sigma"
```
