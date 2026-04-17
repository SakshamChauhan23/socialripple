# Brand Advocacy Media Service

## Overview
Backend service for the SocialRipple platform handling loyalty points, transactions, leaderboard rankings, and wallet management. Media upload APIs will be migrated to this service in the future.

## Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot 3.4.1
- **Database:** MySQL 8 (shared across all services)
- **Cache:** Redis
- **Build:** Maven
- **Push Notifications:** Firebase Admin SDK

## Architecture

```
User Management (8081) --> Media Service (8084) --> MySQL (shared DB)
                                                --> Redis (cache)
```

This service is called by other services (primarily user-management) for loyalty point operations when users create or share posts.

## Local Development

```bash
# Prerequisites: Java 17, Maven, MySQL 8, Redis

git clone <repo-url>
cd brand-advocacy-media-service
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local

# Default port: 8084
```

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | Service port | `8084` | No |
| `SPRING_PROFILES_ACTIVE` | Active profile | - | Yes |
| `DB_HOST` | MySQL host | `localhost` | Yes (prod) |
| `DB_PORT` | MySQL port | `3306` | No |
| `DB_NAME` | Database name | `social_ripple_dev` | Yes (prod) |
| `DB_USER` | Database username | `root` | Yes (prod) |
| `DB_PASSWORD` | Database password | - | Yes (prod) |
| `APP_CORS_ALLOWED_ORIGINS` | CORS allowed origins | `http://localhost:3000` | Yes (prod) |

## GitHub Secrets

| Secret | Purpose | Used By |
|--------|---------|---------|
| `DIGITALOCEAN_SSH_KEY` | SSH key for deployment | All workflows |
| `PAT_TOKEN` | GitHub PAT for workflow operations | All workflows |
| `DEV_NGINX_VM` | Dev2 server hostname/IP | dev2-deploy |
| `PROD_NGINX_VM` | Production server hostname/IP | prod-deploy |
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
- Dev2: `advocacy-media-service-dev2.service`
- Prod: `advocacy-media-service-prod.service`

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
- `loyalty_configuraction` -- credit type configuration
- `loyalty_points` -- user point balances
- `loyalty_transaction` -- point credit/debit history (30-day expiry)
- `wallet_details` -- wallet containers
- `leaderboard` -- user rankings

## API Endpoints

| Path | Auth | Description |
|------|------|-------------|
| `POST /v1/loyalty/award` | Internal | Award loyalty points to a user |
| `POST /v1/loyalty/estimate` | Internal | Estimate points for an action |
| `/actuator/health` | No | Health check |

## Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| Service won't start | Missing required config in `cnfg_config_parameters` | Check startup logs for missing keys |
| Redis connection refused | Redis not running on server | `systemctl start redis-server` |
| 502 from nginx | Service crashed or not started | `systemctl restart advocacy-media-service-{env}.service` |
