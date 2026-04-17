# Brand Advocacy Notification Service

## Overview
Handles outbound notifications for the SocialRipple platform including email delivery (SMTP), push notifications, and WebSocket messaging. Primarily invoked by other backend services for notification workflows.

## Tech Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.5.3
- **Database:** MySQL 8 (shared across all services)
- **Email:** SMTP (Gmail)
- **Build:** Maven Wrapper (`./mvnw`)

## Architecture

```
User Management (8081) --> Notification Service (8082) --> SMTP (Gmail)
Publication Service (8080) --> Notification Service (8082) --> MySQL (shared DB)
```

This service is not directly accessed by the frontend. It is called by user-management and publication services for sending emails (OTP, invitations, connection expiry alerts) and in-app notifications.

## Local Development

```bash
# Prerequisites: Java 21, Maven, MySQL 8

git clone <repo-url>
cd brand-advocacy-notification-service
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local --server.port=8082"

# Default port: 8082
```

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | Service port | `8082` | No |
| `SPRING_PROFILES_ACTIVE` | Active profile | - | Yes |
| `DB_HOST` | MySQL host | `localhost` | Yes (prod) |
| `DB_PORT` | MySQL port | `3306` | No |
| `DB_NAME` | Database name | `social_ripple_dev` | Yes (prod) |
| `DB_USER` | Database username | `root` | Yes (prod) |
| `DB_PASSWORD` | Database password | - | Yes (prod) |
| `APP_CORS_ALLOWED_ORIGINS` | CORS allowed origins | `http://localhost:3000` | Yes (prod) |

Email configuration (SMTP host, port, credentials) is loaded from the `cnfg_config_parameters` database table.

## GitHub Secrets

| Secret | Purpose | Used By |
|--------|---------|---------|
| `DIGITALOCEAN_SSH_KEY` | SSH deployment key | All workflows |
| `PAT_TOKEN` | GitHub PAT | All workflows |
| `DEV_NGINX_VM` | Dev2 server | dev2-deploy |
| `PROD_NGINX_VM` | Production server | prod-deploy |
| `PROD_DB_HOST` | Production MySQL host | prod-deploy |
| `PROD_DB_PORT` | Production MySQL port | prod-deploy |
| `PROD_DB_NAME` | Production database name | prod-deploy |
| `PROD_DB_USER` | Production database user | prod-deploy |
| `PROD_DB_PASSWORD` | Production database password | prod-deploy |
| `APP_CORS_ALLOWED_ORIGINS` | CORS allowed origins | prod-deploy |

## Deployment

### Branch Strategy
- **`development`** -- pushes auto-deploy to dev2
- **`main`** -- pushes auto-deploy to production

### CI/CD Workflows
| Workflow | Trigger | Target |
|----------|---------|--------|
| `dev2-deploy.yml` | Push to `development` | Dev2 VM |
| `prod-deploy.yml` | Push to `main` | Production VM |
| `prod-rollback.yml` | Manual dispatch | Production VM |

### Systemd Units
- Dev2: `advocacy-notification-service-dev2.service`
- Prod: `advocacy-notification-service-prod.service`

## Database

### Connection Pool (HikariCP)
| Setting | Prod Default |
|---------|-------------|
| Max Pool Size | 5 |
| Min Idle | 2 |
| Connection Timeout | 30s |
| Idle Timeout | 5min |
| Max Lifetime | 20min |

### Key Tables
- `notifications` -- notification history
- `otp_store` -- OTP codes for email verification
- `cnfg_config_parameters` -- SMTP config (host, port, credentials, mail templates)

## API Endpoints

| Path | Auth | Description |
|------|------|-------------|
| `POST /v1/notification/send` | Internal | Send notification (called by other services) |
| `/actuator/health` | No | Health check |

## Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| Email not sending | SMTP credentials missing in `cnfg_config_parameters` | Insert `SMTP_HOST`, `SMTP_PORT`, `SMTP_MAIL_PASSWORD`, `MAIL_ID` |
| Port conflict with user-management | Both default to 8081 | Set `SERVER_PORT=8082` in env |
| 502 from nginx | Service not running | `systemctl restart advocacy-notification-service-{env}.service` |
