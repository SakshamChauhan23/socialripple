# Brand Advocacy Publication Service

## Overview
Handles social media integrations for the SocialRipple platform. Manages OAuth connections to X (Twitter), LinkedIn, Facebook, and Instagram. Ingests posts from connected platforms, schedules content, and fetches external media to store on Bunny CDN.

## Tech Stack
- **Language:** Java 21
- **Framework:** Spring Boot 3.5.3
- **Database:** MySQL 8 (shared across all services)
- **Build:** Maven
- **CDN:** BunnyCDN (fetched external media)
- **OAuth:** X (OAuth 1.0a), LinkedIn, Facebook, Instagram (OAuth 2.0)

## Architecture

```
Frontend (React) --> Nginx --> Publication Service (8080) --> MySQL (shared DB)
                                    |-- X/Twitter API (OAuth 1.0a)
                                    |-- LinkedIn API (OAuth 2.0)
                                    |-- Facebook/Instagram Graph API
                                    |-- Bunny CDN (external media storage)
                                    |-- Notification Service (8082) (webhooks)
```

## Local Development

```bash
# Prerequisites: Java 21, Maven, MySQL 8

git clone <repo-url>
cd brand-advocacy-publication-service
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=local

# Default port: 8080
```

## Environment Variables

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | Service port | `8080` | No |
| `SPRING_PROFILES_ACTIVE` | Active profile | - | Yes |
| `DB_HOST` | MySQL host | `localhost` | Yes (prod) |
| `DB_PORT` | MySQL port | `3306` | No |
| `DB_NAME` | Database name | `social_ripple_dev` | Yes (prod) |
| `DB_USER` | Database username | `root` | Yes (prod) |
| `DB_PASSWORD` | Database password | - | Yes (prod) |
| `APP_CORS_ALLOWED_ORIGINS` | CORS allowed origins | `http://localhost:3000` | Yes (prod) |
| `APP_FRONTEND_BASE_URL` | Frontend base URL | `https://dashboard.socialripple.ai` | No |
| `APP_API_BASE_URL` | API base URL (used for OAuth callbacks) | `https://api.socialripple.ai` | No |
| `APP_OAUTH_X_CONSUMER_KEY` | X/Twitter OAuth 1.0a consumer key | - | Yes |
| `APP_OAUTH_X_CONSUMER_SECRET` | X/Twitter OAuth 1.0a consumer secret | - | Yes |
| `APP_OAUTH_META_APP_ID` | Meta/Facebook app ID | `1910917019454230` | No |
| `APP_OAUTH_META_APP_SECRET` | Meta/Facebook app secret | - | Yes |
| `APP_OAUTH_LINKEDIN_CLIENT_ID` | LinkedIn OAuth client ID | - | Yes |
| `APP_OAUTH_LINKEDIN_CLIENT_SECRET` | LinkedIn OAuth client secret | - | Yes |
| `APP_OAUTH_LINKEDIN_SCOPE` | LinkedIn OAuth scopes | `openid profile w_member_social rw_organization_admin r_organization_social` | No |

OAuth callback URLs are auto-constructed from `APP_API_BASE_URL`:
- X: `{API_BASE}/external-ingestion/v1/api/x/callback`
- LinkedIn: `{API_BASE}/external-ingestion/v1/api/linkedin/callback`
- Facebook: `{API_BASE}/external-ingestion/v1/api/fb/callback`
- Instagram: `{API_BASE}/external-ingestion/v1/api/instagram/callback`
- Org-level callbacks follow the pattern: `{API_BASE}/external-ingestion/v1/api/org/business-pages/{platform}/callback`

## GitHub Secrets

| Secret | Purpose | Used By |
|--------|---------|---------|
| `DIGITALOCEAN_SSH_KEY` | SSH deployment key | All workflows |
| `PAT_TOKEN` | GitHub PAT | All workflows |
| `DEV_NGINX_VM` | Dev2 server | dev2-deploy |
| `DEV_API_DOMAIN` | Dev2 API domain | dev2-deploy |
| `DEV_FRONTEND_DOMAIN` | Dev2 frontend domain | dev2-deploy |
| `PROD_NGINX_VM` | Production server | prod-deploy |
| `PROD_API_DOMAIN` | Production API domain | prod-deploy |
| `PROD_FRONTEND_DOMAIN` | Production frontend domain | prod-deploy |
| `PROD_DB_HOST` | Production MySQL host | prod-deploy |
| `PROD_DB_PORT` | Production MySQL port | prod-deploy |
| `PROD_DB_NAME` | Production database name | prod-deploy |
| `PROD_DB_USER` | Production database user | prod-deploy |
| `PROD_DB_PASSWORD` | Production database password | prod-deploy |
| `PROD_DB_VM` | Production database VM | prod-ops-bootstrap |
| `APP_OAUTH_X_CONSUMER_KEY` | X/Twitter consumer key | prod-deploy, dev2-deploy |
| `APP_OAUTH_X_CONSUMER_SECRET` | X/Twitter consumer secret | prod-deploy, dev2-deploy |
| `APP_OAUTH_LINKEDIN_CLIENT_ID` | LinkedIn OAuth client ID | prod-deploy, dev2-deploy |
| `APP_OAUTH_LINKEDIN_CLIENT_SECRET` | LinkedIn OAuth client secret | prod-deploy, dev2-deploy |
| `APP_OAUTH_LINKEDIN_SCOPE` | LinkedIn OAuth scopes | dev2-deploy |
| `APP_OAUTH_META_APP_ID` | Meta app ID | dev2-deploy |
| `APP_OAUTH_META_APP_SECRET` | Meta app secret | dev2-deploy |
| `APP_OAUTH_ORG_LINKEDIN_CALLBACK_URL` | Org LinkedIn callback | dev2-deploy |
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
| `prod-ops-bootstrap.yml` | Manual dispatch | Production infrastructure setup |
| `prod-performance-baseline.yml` | Manual dispatch | Performance testing |

### Systemd Units
- Dev2: `advocacy-publication-service-dev2.service`
- Prod: `advocacy-publication-service-prod.service`

## Database

### Connection Pool (HikariCP)
| Setting | Prod Default |
|---------|-------------|
| Max Pool Size | 10 |
| Min Idle | 3 |
| Connection Timeout | 30s |
| Idle Timeout | 5min |
| Max Lifetime | 20min |

### Key Tables
- `posts` -- published and scheduled posts
- `post_media` -- media attached to posts
- `post_hashtags`, `post_tags`, `post_category_map` -- post metadata
- `user_auth_token` -- OAuth tokens for connected platforms
- `external_platforms` -- organization-level platform settings
- `external_shares` -- shared post tracking
- `schedules` -- scheduled post queue

## API Endpoints (Key Groups)

| Path | Auth | Description |
|------|------|-------------|
| `GET /v1/api/x/auth` | JWT | Initiate X/Twitter OAuth |
| `GET /v1/api/x/callback` | No | X OAuth callback |
| `GET /v1/api/linkedin/auth` | JWT | Initiate LinkedIn OAuth |
| `GET /v1/api/linkedin/callback` | No | LinkedIn OAuth callback |
| `GET /v1/api/fb/auth` | JWT | Initiate Facebook OAuth |
| `GET /v1/api/fb/callback` | No | Facebook OAuth callback |
| `GET /v1/api/instagram/auth` | JWT | Initiate Instagram OAuth |
| `GET /v1/api/instagram/callback` | No | Instagram OAuth callback |
| `POST /v1/api/posts/share` | JWT | Share a post to connected platforms |
| `GET /v1/api/dashboard/*` | JWT | Analytics and dashboard data |
| `/actuator/health` | No | Health check |

## Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| X auth returns 400 | Empty or invalid `APP_OAUTH_X_CONSUMER_KEY` | Set correct X consumer key in GitHub secrets and redeploy |
| `No property 'tenantId'` on startup | Missing `tenant_id` column in `user_auth_token` | Run ALTER TABLE to add the column |
| Missing columns in `external_platforms` | DB schema not migrated | Add `last_imported_count`, `last_sync_*` columns |
| Fetched media upload fails | Bunny CDN not configured in `cnfg_config_parameters` | Insert `MEDIA_IMAGE_PROVIDER=BUNNY_STORAGE` and Bunny keys |
| CORS 403 on OPTIONS | `APP_CORS_ALLOWED_ORIGINS` not set | Add frontend domain to CORS env var |
