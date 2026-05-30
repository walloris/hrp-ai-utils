# Платформенная конфигурация

## Структура HRPConfiguration

Платформенная конфигурация хранится в `platform-config/` и использует формат YAML с метаданными HRP.

```yaml
kind: HRPConfiguration
apiVersion: apps/v1
metadata:
  name: <service-name>
  title: '<service-name>, описание'
  delivery:
    tags:
      - <tag>
```

## Поле delivery.tags

Метки доставки определяют, на какие среды применяется конфигурация:

| Тег | Описание |
|-----|----------|
| `sigma-dev` | Sigma dev-среда |
| `sigma-qa` | Sigma QA-среда |
| `sigma-ift` | Sigma IFT-среда |
| `sigma-prom` | Sigma production |
| `sigma-uat` | Sigma UAT-среда |
| `all-all` | Все среды |

## Примеры конфигураций

### all.hrpcfg.yaml - общие настройки

```yaml
kind: HRPConfiguration
apiVersion: apps/v1
metadata:
  name: spine-landing-manager-app
  title: 'spine-landing-manager-app, настройки all'
  delivery:
    tags:
      - all-all

SPINE_PERSON_PROFILE_SERVICE_URL: 'http://spine-person-profile:8080'
LANDING_MANAGER_NODEJS_BASEURL: 'http://app-landing-manager-node:8080/api/v1/'
```

### sigma-dev.hrpcfg.yaml - настройки dev

```yaml
kind: HRPConfiguration
apiVersion: apps/v1
metadata:
  name: spine-landing-manager-app
  title: 'spine-landing-manager-app, настройки sigma-dev'
  delivery:
    tags:
      - sigma-dev

CONTENT_STORAGE_SCRIPT_BASEURL: 'https://hr-dev.sberbank.ru/api-web/cs/api/1'
CONTENT_STORAGE_PUBLISH_URLPREFIX: 'https://hr-dev.sberbank.ru/'
```

## Приоритет конфигураций

1. `all.hrpcfg.yaml` - базовые настройки для всех сред
2. `<environment>.hrpcfg.yaml` - специфичные настройки для конкретной среды

Специфичные настройки переопределяют общие.
