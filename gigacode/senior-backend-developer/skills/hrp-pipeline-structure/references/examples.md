# Примеры конфигураций

## params.yaml

```yaml
SONAR_LOGIN: squ_xxxxxxxxxxxxxxxxxxxxxx

INGRESS_LOCATIONS: |-
    location /api/<service-name>/api/ {
        proxy_pass http://<service-name>:8080/api/;
    }

certificate: yes
service_id: "<service-id>"
component_id: "<component-id>"
owner_email: "email@sberbank.ru"

auth_config:
    name: "<Service Name>"
    description: "Описание сервиса"
    owners-email: "email@sberbank.ru"
```

## probe.yaml

```yaml
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8090
    scheme: HTTP
  initialDelaySeconds: 30
  timeoutSeconds: 5
  periodSeconds: 10
  successThreshold: 1
  failureThreshold: 50

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8090
    scheme: HTTP
  initialDelaySeconds: 430
  timeoutSeconds: 5
  periodSeconds: 20
  successThreshold: 1
  failureThreshold: 3
```

## kafka-acl.yaml

```yaml
- cluster: common
  active: true
  topics:
    hrp.spine.search.landing.recruitment.messages: write
    hrp.spine.search.landing.sberu.messages: write

- cluster: events
  active: true
  groups:
    <group-name>:
      - all
  topics:
    '*':
      - describe
    hrp.spine.events.FileAllVerificationsFinishedByContentStorage:
      - read
```

## service-mesh-sigma.yaml

```yaml
has_access_to_resources:
  managed:
    database: [<db-name>]
    kafka: []
    general: []
  internal:
    - service: spine-authorization
      port: 8080
      protocol: http
    - service: spine-content-api
      port: 8080
      protocol: http
    - service: spine-search
      port: 8080
      protocol: http
```
