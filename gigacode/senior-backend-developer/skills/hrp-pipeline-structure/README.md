# HRP-pipeline Structure

This skill documents the universal directory structure for Spine microservices pipeline configuration.

## Files

- `SKILL.md` - Overview and quick reference
- `references/environment-config.md` - Environment-specific configuration details
- `references/platform-config.md` - Platform configuration files
- `references/database.md` - Database migrations with Liquibase
- `references/examples.md` - Example configurations

## Structure

```
HRP-pipeline/<service-name>/
├── config/                    # Environment configurations
├── dbscripts/                 # Database migrations
├── platform-config/           # Platform configuration
├── platform-config-external/  # External service configs
├── Dockerfile                 # Docker build
├── params.yaml                # Pipeline parameters
├── probe.yaml                 # Health probes
├── kafka-acl.yaml             # Kafka ACL
└── service-mesh-sigma.yaml    # Service mesh
```

## Usage

This skill was triggered automatically because you asked about the HRP-pipeline structure.
