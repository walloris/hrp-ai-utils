---
name: HRP-pipeline Structure
description: Document the universal HRP-pipeline directory structure for Spine microservices. Use when explaining or documenting the standardized pipeline configuration for microservice deployments.
---

# HRP-pipeline Structure

Universal directory structure for Spine microservices pipeline configuration.

## Structure Overview

```
HRP-pipeline/<service-name>/
├── config/                    # Environment-specific configurations
│   ├── DEV/                   # Development environment
│   ├── QA/                    # QA environment
│   ├── UAT/                   # User Acceptance Testing
│   ├── BLK/                   # Blackbox testing
│   ├── IFT/                   # Integration Form Testing
│   ├── PROM/                  # Production
│   └── <ENV>/                 # Additional environments
├── dbscripts/                 # Database migration scripts
│   └── liquibase/             # Liquibase migrations
│       ├── classpath/         # Java classpath resources
│       ├── sland/             # Main database changelogs
│       │   ├── changelog/     # Versioned changelogs
│       │   │   ├── v1.0/
│       │   │   ├── v1.1/
│       │   │   └── ...
│       │   └── changelog.xml  # Main changelog file
│       └── liquibase.properties
├── platform-config/           # Platform configuration files
│   ├── all.hrpcfg.yaml        # All environments config
│   ├── sigma-dev.hrpcfg.yaml  # Sigma Dev config
│   ├── sigma-ift.hrpcfg.yaml  # Sigma IFT config
│   └── ...                    # Other platform configs
├── platform-config-external/  # External service configurations
│   ├── spine-authorization/
│   ├── spine-content-api/
│   └── spine-audit-bridge/
├── Dockerfile                 # Docker build configuration
├── QGSonar.Dockerfile         # SonarQube Dockerfile
├── params.yaml                # Pipeline parameters
├── probe.yaml                 # Health check probes
├── kafka-acl.yaml             # Kafka ACL configuration
├── service-mesh-sigma.yaml    # Service mesh configuration
└── README.md                  # Service documentation
```

## Configuration Files

### config/<ENV>/configmap.yaml
Kubernetes ConfigMap for specific environment.

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: <service-name>
data:
  <CONFIG_KEY>: "<value>"
```

### platform-config/*.hrpcfg.yaml
HRP platform configuration with metadata tags for delivery.

```yaml
kind: HRPConfiguration
apiVersion: apps/v1
metadata:
  name: <service-name>
  title: '<service-name>, description'
  delivery:
    tags:
      - <tag>
```

### platform-config-external/<service>/
Configuration for external Spine services dependencies.

### params.yaml
Pipeline parameters including Sonar login, ingress locations, certificates.

### probe.yaml
Kubernetes readiness and liveness probes configuration.

### kafka-acl.yaml
Kafka cluster ACL configuration for topics and consumer groups.

### service-mesh-sigma.yaml
Service mesh configuration defining service dependencies and access rules.

## Database Migrations

Database scripts use Liquibase with versioned changelogs organized by release versions (v1.0, v1.1, etc.) and environment-specific directories.

## Environment Tags

- `sigma-dev` - Sigma development environment
- `sigma-qa` - Sigma QA environment
- `sigma-ift` - Sigma Integration Form Testing
- `sigma-prom` - Sigma production environment
- `sigma-uat` - Sigma User Acceptance Testing

## Usage

When documenting or explaining HRP-pipeline structure:
1. Reference this skill for the canonical structure
2. Use reference files for detailed configuration examples
3. Follow this pattern for new microservices
