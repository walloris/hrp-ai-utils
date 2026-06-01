#!/bin/bash
# Script to initialize a new HRP-pipeline structure for a microservice
# Usage: init_hrp_pipeline.sh <service-name> <component-id> <owner-email>

set -e

if [ $# -ne 3 ]; then
    echo "Usage: $0 <service-name> <component-id> <owner-email>"
    echo "Example: $0 spine-landing-manager-app landing-manager email@sberbank.ru"
    exit 1
fi

SERVICE_NAME=$1
COMPONENT_ID=$2
OWNER_EMAIL=$3

HRP_DIR="HRP-pipeline/${SERVICE_NAME}"

echo "Initializing HRP-pipeline structure for ${SERVICE_NAME}..."

# Create directory structure
mkdir -p "${HRP_DIR}/config/DEV"
mkdir -p "${HRP_DIR}/config/QA"
mkdir -p "${HRP_DIR}/config/UAT"
mkdir -p "${HRP_DIR}/config/BLK"
mkdir -p "${HRP_DIR}/config/IFT"
mkdir -p "${HRP_DIR}/config/PROM"
mkdir -p "${HRP_DIR}/dbscripts/liquibase/classpath"
mkdir -p "${HRP_DIR}/dbscripts/liquibase/${SERVICE_NAME}"
mkdir -p "${HRP_DIR}/platform-config"
mkdir -p "${HRP_DIR}/platform-config-external/spine-authorization"
mkdir -p "${HRP_DIR}/platform-config-external/spine-content-api"
mkdir -p "${HRP_DIR}/platform-config-external/spine-audit-bridge/metamodels"

# Create params.yaml
cat > "${HRP_DIR}/params.yaml" << EOF
SONAR_LOGIN: <your-sonar-token>

INGRESS_LOCATIONS: |-
    location /api/${SERVICE_NAME}/api/ {
        proxy_pass http://${SERVICE_NAME}:8080/api/;
    }

certificate: yes
service_id: "${COMPONENT_ID}"
component_id: "${COMPONENT_ID}"
owner_email: "${OWNER_EMAIL}"

auth_config:
    name: "${SERVICE_NAME//_/ }"
    description: "Сервис <описание>"
    owners-email: "${OWNER_EMAIL}"
EOF

# Create probe.yaml
cat > "${HRP_DIR}/probe.yaml" << EOF
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
EOF

# Create platform-config/all.hrpcfg.yaml
cat > "${HRP_DIR}/platform-config/all.hrpcfg.yaml" << EOF
kind: HRPConfiguration
apiVersion: apps/v1
metadata:
  name: ${SERVICE_NAME}
  title: '${SERVICE_NAME}, настройки all'
  delivery:
    tags:
      - all-all

# Add common configuration here
EOF

# Create ConfigMap templates for each environment
for ENV in DEV QA UAT BLK IFT PROM; do
    cat > "${HRP_DIR}/config/${ENV}/configmap.yaml" << EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${SERVICE_NAME}
data:
  HRP_CONFIGMANAGER_CONFIG_ENABLED: "false"
  HRP_CONFIGMANAGER_CONFIG_API_ENABLED: "true"
  SPRING_PROFILES_ACTIVE: "sigma"
EOF
done

echo "HRP-pipeline structure initialized successfully!"
echo "Location: ${HRP_DIR}"
echo ""
echo "Next steps:"
echo "1. Configure environment-specific settings in config/<ENV>/configmap.yaml"
echo "2. Add platform-specific configurations in platform-config/"
echo "3. Set up database migrations in dbscripts/liquibase/"
echo "4. Configure Kafka ACLs in kafka-acl.yaml"
echo "5. Update service-mesh-sigma.yaml with dependencies"
