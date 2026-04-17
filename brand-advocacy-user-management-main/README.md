# Brand Advocacy User Management Service

## Overview

This service is the primary application-facing backend for SocialRipple. It owns authentication, user profiles, teams, categories, dashboard data, timeline aggregation, notification retrieval for the UI, media upload (via Bunny CDN), Gemini-backed AI content generation, and WebSocket delivery.

It is the main backend behind the frontend's `/v1/*` application routes and also acts as a reverse proxy for other backend services.

## Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.5.3 | Application framework |
| Spring Security + JWT | - | Authentication and authorization |
| Spring Data JPA / Hibernate | - | ORM and data access |
| Spring Data Redis | - | Caching layer |
| Spring WebSocket | - | Real-time messaging |
| MySQL | 8.x | Shared database |
| HikariCP | - | Connection pooling |
| Google Gemini 2.5 Pro/Flash | - | AI content generation |
| Bunny CDN | - | Media file storage and delivery |

## Repository Layout

```
src/main/java/com/social/ripple/usermanagement/
  controller/     # REST controllers and WebSocket endpoints
  service/        # Business logic, external clients, WebSocket services
  dao/            # JPA entities and Spring Data repositories
src/main/resources/
  application.properties            # Base config with env var placeholders
  application-local.properties      # Local dev overrides (captcha disabled)
  application-dev.properties        # Dev2 profile
  application-prod.properties       # Production profile (HikariCP tuning)
deploy/
  dev2/           # Dev2 systemd unit, deploy scripts
  prod/           # Production systemd unit, deploy scripts, rollback
.github/workflows/
  dev2-deploy.yml     # Auto-deploy development branch to Dev2
  prod-deploy.yml     # Auto-deploy main branch to production
  prod-rollback.yml   # Manual rollback workflow
  compile-check.yml   # Build verification
```

## Key Responsibilities

- **Authentication**: Username/password login, Google SSO, Microsoft SSO, JWT token issuance and validation
- **reCAPTCHA**: Server-side verification on login and SSO-provider discovery
- **User management**: Profiles, roles, teams, invitations, password recovery
- **Teams and categories**: Team creation/management, category assignment
- **Dashboard**: Aggregated analytics and metrics for the frontend
- **Timeline**: Activity feed aggregation across the platform
- **Notifications**: Retrieval and delivery of in-app notifications to the UI
- **WebSocket**: Real-time messaging via `/ws` endpoint
- **AI content generation**: Gemini 2.5 Pro and Flash models for trending topics, post suggestions
- **Media upload**: File uploads proxied through to Bunny CDN
- **Reverse proxy**: Routes requests to publication-service, notification-service via internal proxy
- **Leaderboard and loyalty**: Leaderboard data aggregation, loyalty point display
- **Social connections**: LinkedIn OAuth client ID management, social media tracking

## API Endpoint Groups

| Controller | Base Path | Purpose |
|-----------|-----------|---------|
| AuthController | `/v1/auth` | Login, signup, token refresh |
| SsoAuthController | `/v1/auth/sso` | Google/Microsoft SSO flows |
| UserController | `/v1/users` | User CRUD operations |
| ProfileController | `/v1/profiles` | Profile management |
| TeamController | `/v1/teams` | Team operations |
| UserTeamController | `/v1/user-teams` | User-team associations |
| CategoryController | `/v1/categories` | Category management |
| DashboardController | `/v1/dashboard` | Dashboard metrics |
| TimelineController | `/v1/timeline` | Activity feed |
| NotificationController | `/v1/notifications` | Notification retrieval |
| GenerateContentController | `/v1/generate-content` | AI content generation |
| ContentController | `/v1/content` | Content management |
| MediaController | `/v1/media` | Media upload operations |
| MediaContentController | `/api/media` | Media content serving |
| LeaderboardController | `/v1/leaderboard` | Leaderboard data |
| LoyaltyController | `/v1/loyalty` | Loyalty points display |
| ConnectionController | `/v1/connections` | Social media connections |
| TrackSocialMediaController | `/v1/track-social` | Social media tracking |
| InviteUserController | `/v1/invites` | User invitations |
| PasswordRecoveryController | `/v1/password-recovery` | Password reset flow |
| UserPasswordController | `/v1/user-password` | Password change |
| ActivityController | `/v1/activities` | Activity logging |
| ArchiveController | `/v1/archive` | Content archival |
| ExternalShareController | `/v1/external-share` | External sharing |
| ReferenceDataController | `/v1/reference-data` | Reference/lookup data |
| RefreshCacheController | `/v1/cache` | Redis cache management |
| ProxyController2 | `/v1/proxy` | Reverse proxy to other services |
| WebSocketController | `/v1/websocket` | WebSocket message handling |

## Environment Variables

### Application Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | `8081` | HTTP listen port |
| `APP_JWT_SECRET` | (none) | JWT signing key (min 256-bit) |
| `APP_JWT_EXPIRATION_MS` | `86400000` (24h) | JWT token expiry in milliseconds |
| `APP_AI_GEMINI_API_KEY` | (none) | Google Gemini API key |
| `APP_AI_GEMINI_API_URL` | (none) | Gemini 2.5 Pro model endpoint |
| `APP_AI_GEMINI_FLASH_API_URL` | (none) | Gemini 2.5 Flash model endpoint |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated allowed CORS origins |
| `APP_FRONTEND_BASE_URL` | `https://dashboard.socialripple.ai` | Frontend URL for email links, CORS |
| `APP_API_BASE_URL` | `https://api.socialripple.ai` | API URL for OAuth callback construction |
| `APP_OAUTH_LINKEDIN_CLIENT_ID` | (none) | LinkedIn OAuth 2.0 client ID |
| `APP_OAUTH_LINKEDIN_CALLBACK_URL` | `{API_BASE}/external-ingestion/v1/api/linkedin/callback` | LinkedIn OAuth redirect |
| `APP_OAUTH_FACEBOOK_CALLBACK_URL` | `{API_BASE}/external-ingestion/v1/api/fb/callback` | Facebook OAuth redirect |
| `APP_OAUTH_INSTAGRAM_CALLBACK_URL` | `{API_BASE}/external-ingestion/v1/api/instagram/callback` | Instagram OAuth redirect |
| `APP_OAUTH_META_APP_ID` | `1910917019454230` | Meta/Facebook application ID |
| `APP_SECURITY_RECAPTCHA_SECRET_KEY` | (none) | Google reCAPTCHA v3 secret key |
| `APP_SECURITY_RECAPTCHA_VERIFY_URL` | `https://www.google.com/recaptcha/api/siteverify` | reCAPTCHA verification endpoint |
| `APP_SECURITY_RECAPTCHA_MIN_SCORE` | `0.1` | Minimum reCAPTCHA score threshold |
| `APP_PROXY_EXTERNAL_INGESTION_URL` | `http://localhost:8080` | Publication service proxy target |
| `APP_PROXY_PRODUCTS_URL` | `http://localhost:8082` | Notification service proxy target |
| `APP_PROXY_ORDERS_URL` | `http://localhost:8083` | Orders service proxy target |

### Database Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | (none) | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | (none) | Database name |
| `DB_USER` | (none) | Database username |
| `DB_PASSWORD` | (none) | Database password |
| `DB_MAX_POOL_SIZE` | `15` | HikariCP max pool size |
| `DB_MIN_IDLE` | `5` | HikariCP min idle connections |
| `DB_CONNECTION_TIMEOUT_MS` | `30000` | Connection timeout (ms) |
| `DB_IDLE_TIMEOUT_MS` | `300000` | Idle connection timeout (ms) |
| `DB_MAX_LIFETIME_MS` | `1200000` | Max connection lifetime (ms) |

## This Service's GitHub Secrets (26 secrets)

| Secret | Used In | Description |
|--------|---------|-------------|
| `DIGITALOCEAN_SSH_KEY` | dev2, prod | SSH private key for deployment |
| `PAT_TOKEN` | dev2, prod | GitHub PAT for workflow concurrency |
| `DEV_NGINX_VM` | dev2 | Dev2 server IP |
| `PROD_NGINX_VM` | prod | Production server IP |
| `DEV_DB_HOST` | dev2 | Dev2 MySQL host |
| `DEV_DB_PORT` | dev2 | Dev2 MySQL port |
| `DEV_DB_NAME` | dev2 | Dev2 database name |
| `DEV_DB_USER` | dev2 | Dev2 database user |
| `DEV_DB_PASSWORD` | dev2 | Dev2 database password |
| `PROD_DB_HOST` | prod | Production MySQL host |
| `PROD_DB_PORT` | prod | Production MySQL port |
| `PROD_DB_NAME` | prod | Production database name |
| `PROD_DB_USER` | prod | Production database user |
| `PROD_DB_PASSWORD` | prod | Production database password |
| `DEV_API_DOMAIN` | dev2 | Dev2 API domain for OAuth callbacks |
| `DEV_FRONTEND_DOMAIN` | dev2 | Dev2 frontend domain for CORS/email links |
| `PROD_API_DOMAIN` | prod | Production API domain |
| `PROD_FRONTEND_DOMAIN` | prod | Production frontend domain |
| `APP_JWT_SECRET` | prod | JWT signing key |
| `APP_AI_GEMINI_API_KEY` | dev2, prod | Gemini API key |
| `APP_AI_GEMINI_API_URL` | dev2, prod | Gemini 2.5 Pro endpoint |
| `APP_AI_GEMINI_FLASH_API_URL` | dev2, prod | Gemini 2.5 Flash endpoint |
| `APP_OAUTH_LINKEDIN_CLIENT_ID` | dev2, prod | LinkedIn OAuth client ID |
| `APP_SECURITY_RECAPTCHA_SECRET_KEY` | dev2, prod | reCAPTCHA secret key |
| `APP_SECURITY_RECAPTCHA_VERIFY_URL` | prod | reCAPTCHA verify URL |
| `APP_CORS_ALLOWED_ORIGINS` | prod | Allowed CORS origins |

**Note**: `APP_SECURITY_RECAPTCHA_MIN_SCORE` is a GitHub Actions **variable** (not a secret).

## Deployment

### CI/CD Workflows

| Workflow | Trigger | Target |
|----------|---------|--------|
| `dev2-deploy.yml` | Push to `development` or manual | Dev2 VM (`DEV_NGINX_VM`) |
| `prod-deploy.yml` | Push to `main` or manual | Production VM (`PROD_NGINX_VM`), requires `production` environment approval |
| `prod-rollback.yml` | Manual only | Production VM |
| `compile-check.yml` | PR / push | Build verification only |

### Deployment Process

1. GitHub Actions checks out the code
2. Connects via SSH to the target VM
3. Rsyncs the full repo to `/github/{dev2,prod}/user-management/`
4. Executes the deploy script with all secrets as environment variables
5. Deploy script builds the JAR with Maven and restarts the systemd service

### Systemd Units

| Environment | Unit Name | Config Template |
|-------------|-----------|-----------------|
| Dev2 | `advocacy-user-management-dev2.service` | `deploy/dev2/advocacy-user-management-dev2.service.example` |
| Production | `advocacy-user-management-prod.service` | `deploy/prod/advocacy-user-management-prod.service.example` |

## Database

- **Shared database**: All 4 backend services share one MySQL database
- **DDL management**: `spring.jpa.hibernate.ddl-auto=none` -- schema changes are managed manually via SQL scripts in `deploy/*/sql/`
- **Connection pool (production)**: HikariCP with max 15 connections, min 5 idle

## WebSocket

- Clients connect to `/ws`
- Nginx must proxy `/ws` to this service (port 8081)
- Messages can also be sent via REST at `/v1/websocket/message`

## Local Development

### Prerequisites

- Java 21
- Maven (or use the included Maven Wrapper)
- MySQL running on `localhost:3306`
- Database `social_ripple_dev` created
- Redis running locally (for caching features)

### Run Locally

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile disables reCAPTCHA verification and uses local database defaults.

### Build

```bash
./mvnw clean package
```

### Run Tests

```bash
./mvnw test
```

### Default Port

`8081`

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `401 Unauthorized` on all requests | Missing or expired JWT token | Check `APP_JWT_SECRET` is set; token may need refresh |
| `403 CORS error` in browser | Frontend origin not in `APP_CORS_ALLOWED_ORIGINS` | Add the origin to the comma-separated list |
| AI content generation returns empty | Missing Gemini API key or URL | Verify `APP_AI_GEMINI_API_KEY` and `APP_AI_GEMINI_API_URL` are set |
| reCAPTCHA always fails | Secret key mismatch with frontend site key | Ensure `APP_SECURITY_RECAPTCHA_SECRET_KEY` pairs with `REACT_APP_RECAPTCHA_SITE_KEY` |
| Database connection pool exhausted | Too many concurrent requests | Increase `DB_MAX_POOL_SIZE` (default 15) |
| WebSocket connection fails | Nginx not proxying `/ws` | Check nginx config routes `/ws` to port 8081 |
| Proxy to publication-service fails | Wrong proxy URL | Verify `APP_PROXY_EXTERNAL_INGESTION_URL` points to port 8080 |
| Deploy script fails on VM | SSH key or VM IP incorrect | Check `DIGITALOCEAN_SSH_KEY` and `DEV_NGINX_VM`/`PROD_NGINX_VM` secrets |

---

## SocialRipple Platform Overview

SocialRipple is a brand advocacy platform with 4 Java backend microservices and a React frontend, all sharing a MySQL database.

### Platform Architecture

| Service | Repo | Port | Purpose |
|---------|------|------|---------|
| User Management | `brand-advocacy-user-management` | 8081 | Auth, profiles, teams, media upload, AI content, proxy gateway |
| Publication Service | `brand-advocacy-publication-service` | 8080 | Social media OAuth (X, LinkedIn, Facebook, Instagram), post ingestion |
| Notification Service | `brand-advocacy-notification-service` | 8082 | Email (SMTP), push notifications, WebSocket |
| Media Service | `brand-advocacy-media-service` | 8084 | Loyalty points, leaderboard, media APIs (planned) |
| Frontend | `Ai-Advocacy-Application-fe` | 80/443 | React SPA served by nginx |

### Infrastructure

| Environment | App Server | DB Server | Frontend Domain | API Domain |
|-------------|-----------|-----------|-----------------|------------|
| Dev2 | `DEV_NGINX_VM` (206.189.130.17) | Same VM | advocacy.moonhive-server.in.net | advocacy-api.moonhive-server.in.net |
| Production | `PROD_NGINX_VM` (68.183.93.135) | `PROD_DB_HOST` (134.209.148.15) | dashboard.socialripple.ai | api.socialripple.ai |

### Nginx Routing (both environments)

| Path | Backend |
|------|---------|
| `/v1/*` | User Management (8081) |
| `/external-ingestion/*` | Publication Service (8080) |
| `/notifications/*` | Notification Service (8082) |
| `/media/*` | Media Service (8084) |
| `/ws` | WebSocket (User Management 8081) |
| `/api/media/*` | Media content (User Management 8081) |

### Complete GitHub Secrets Reference (39 secrets across all repos)

#### Infrastructure (all 5 repos)

| Secret | Description | Example | Repos |
|--------|-------------|---------|-------|
| `DIGITALOCEAN_SSH_KEY` | SSH private key for deployment to DigitalOcean VMs. Used by GitHub Actions to rsync code and execute deploy scripts. | RSA/Ed25519 private key | All 5 repos |
| `PAT_TOKEN` | GitHub Personal Access Token. Used for workflow concurrency management. | `ghp_xxxx` | All 5 repos |
| `DEV_NGINX_VM` | Dev2 server IP where all dev services and nginx run. | `206.189.130.17` | All 5 repos |
| `PROD_NGINX_VM` | Production server IP where all prod services and nginx run. | `68.183.93.135` | All 5 repos |

#### Database -- Production (4 backend repos)

| Secret | Description | Example | Repos |
|--------|-------------|---------|-------|
| `PROD_DB_HOST` | Production MySQL server IP. Separate VM from app server. | `134.209.148.15` | user-mgmt, publication, media, notification |
| `PROD_DB_PORT` | Production MySQL port. | `3306` | user-mgmt, publication, media, notification |
| `PROD_DB_NAME` | Production database name. All services share one DB. | `advocacy_db` | user-mgmt, publication, media, notification |
| `PROD_DB_USER` | Production database username. | (credential) | user-mgmt, publication, media, notification |
| `PROD_DB_PASSWORD` | Production database password. | (credential) | user-mgmt, publication, media, notification |

#### Database -- Dev2 (user-management only)

| Secret | Description | Repos |
|--------|-------------|-------|
| `DEV_DB_HOST` | Dev2 MySQL host | user-mgmt |
| `DEV_DB_PORT` | Dev2 MySQL port | user-mgmt |
| `DEV_DB_NAME` | Dev2 database name | user-mgmt |
| `DEV_DB_USER` | Dev2 database user | user-mgmt |
| `DEV_DB_PASSWORD` | Dev2 database password | user-mgmt |

#### Domain Configuration

| Secret | Description | Example | Repos |
|--------|-------------|---------|-------|
| `DEV_API_DOMAIN` | Dev2 API domain. Used to construct OAuth callback URLs. | `advocacy-api.moonhive-server.in.net` | user-mgmt, publication, frontend |
| `DEV_FRONTEND_DOMAIN` | Dev2 frontend domain. Used for CORS and email links. | `advocacy.moonhive-server.in.net` | user-mgmt, publication |
| `PROD_API_DOMAIN` | Production API domain. Used for OAuth callbacks and nginx. | `api.socialripple.ai` | user-mgmt, publication, frontend |
| `PROD_FRONTEND_DOMAIN` | Production frontend domain. Used for CORS, email links, nginx. | `dashboard.socialripple.ai` | user-mgmt, publication, frontend |

#### Authentication & Security (user-management)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `APP_JWT_SECRET` | JWT signing key (min 256-bit random). | Signs all auth tokens. If compromised, all user sessions become invalid. Must be rotated carefully. | user-mgmt |
| `APP_SECURITY_RECAPTCHA_SECRET_KEY` | Google reCAPTCHA v3 secret key. | Validates captcha tokens on login/signup server-side. Must pair with frontend site key (`REACT_APP_RECAPTCHA_SITE_KEY`). | user-mgmt |
| `APP_SECURITY_RECAPTCHA_VERIFY_URL` | reCAPTCHA verification API endpoint. | Google's server-side verification URL. Default: `https://www.google.com/recaptcha/api/siteverify` | user-mgmt |

#### AI Integration (user-management)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `APP_AI_GEMINI_API_KEY` | Google Gemini API key. | Powers AI content generation (trending topics, post suggestions). | user-mgmt |
| `APP_AI_GEMINI_API_URL` | Gemini 2.5 Pro model endpoint. | Primary AI model for content generation. | user-mgmt |
| `APP_AI_GEMINI_FLASH_API_URL` | Gemini 2.5 Flash model endpoint. | Faster/cheaper model for lighter AI tasks. | user-mgmt |

#### Social Media OAuth (publication-service)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `APP_OAUTH_X_CONSUMER_KEY` | X/Twitter OAuth 1.0a consumer key. | Authenticates the app to Twitter API. Required for X connect/share flow. | publication |
| `APP_OAUTH_X_CONSUMER_SECRET` | X/Twitter OAuth 1.0a consumer secret. | Signs all Twitter API requests. | publication |
| `APP_OAUTH_LINKEDIN_CLIENT_ID` | LinkedIn OAuth 2.0 client ID. | LinkedIn login and post sharing. Also used by user-mgmt for profile OAuth. | user-mgmt, publication |
| `APP_OAUTH_LINKEDIN_CLIENT_SECRET` | LinkedIn OAuth 2.0 client secret. | Server-side LinkedIn API authentication. | publication |
| `APP_OAUTH_LINKEDIN_SCOPE` | LinkedIn OAuth scopes. | Controls what LinkedIn data/actions the app can access. Default: `openid profile w_member_social rw_organization_admin r_organization_social` | publication |
| `APP_OAUTH_META_APP_ID` | Meta/Facebook application ID. | Facebook and Instagram OAuth flows. | publication |
| `APP_OAUTH_META_APP_SECRET` | Meta/Facebook application secret. | Server-side Facebook/Instagram API authentication. | publication |
| `APP_OAUTH_ORG_LINKEDIN_CALLBACK_URL` | Organization-level LinkedIn OAuth callback URL. | Where LinkedIn redirects after org business page OAuth. | publication |

#### CORS (all 4 backend repos)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins. | Browser CORS preflight validation. Without this, the frontend cannot make API calls. Example: `https://dashboard.socialripple.ai,http://localhost:3000` | user-mgmt, publication, media, notification |

#### Frontend Build-time (frontend only)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `REACT_APP_RECAPTCHA_SITE_KEY` | Google reCAPTCHA v3 site key. | Rendered in browser for captcha challenge. Must pair with backend's `APP_SECURITY_RECAPTCHA_SECRET_KEY`. | frontend |
| `REACT_APP_GOOGLE_SSO_CLIENT_ID` | Google OAuth client ID. | Powers the "Sign in with Google" button. Domain must be authorized in Google Cloud Console. | frontend |
| `REACT_APP_MICROSOFT_CLIENT_ID` | Microsoft/Azure AD client ID. | Powers the "Sign in with Microsoft" button. | frontend |
| `REACT_APP_MICROSOFT_TENANT_ID` | Microsoft/Azure AD tenant ID. | Identifies which Azure AD tenant to authenticate against. | frontend |
| `PROD_FE_WEBROOT` | Production webroot path on server. | Where nginx serves the built React app from. Default: `/var/www/dashboard.socialripple.ai` | frontend |

#### Publication-specific Infrastructure

| Secret | Description | Repos |
|--------|-------------|-------|
| `PROD_DB_VM` | Production database VM IP (separate from app VM). Used by ops bootstrap workflow. | publication |

### Branch Strategy

| Repo | Dev Branch | Prod Branch | Dev Deploy | Prod Deploy |
|------|-----------|-------------|------------|-------------|
| user-management | `development` | `main` | Auto on push | Auto on push (env protection) |
| publication-service | `development` | `main` | Auto on push | Auto on push (env protection) |
| media-service | `development` | `main` | Auto on push | Auto on push |
| notification-service | `development` | `main` | Auto on push | Auto on push |
| frontend | `develop` | `main` | Auto on push | Auto on push |
