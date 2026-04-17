# AI Advocacy Application Frontend

React single-page application for the SocialRipple brand advocacy platform. Provides admin and employee dashboards for authentication, analytics, timeline browsing, content creation, social sharing, team management, notifications, and media management.

## Tech Stack

- **React 18** with **Create React App** (`react-scripts 5`)
- **Material UI 6** (MUI) for component library
- **React Router 6** for client-side routing
- **Axios** for HTTP requests with centralized interceptors
- **Formik + Yup** for form management and validation
- **MSAL** (@azure/msal-browser, @azure/msal-react) for Microsoft SSO
- **@react-oauth/google** for Google SSO
- **react-google-recaptcha-v3** for login/signup captcha
- **Recharts** for dashboard analytics charts
- **React Quill** for rich text editing in content creation

## Port

- Development: `3000` (CRA dev server)
- Production: `80/443` (nginx serves the static build)

## Quick Start

```bash
# Install dependencies
npm install

# Start dev server (port 3000)
npm start

# Run tests (Jest, watch mode)
npm test

# Production build
npm run build
```

## Environment Variables

All environment variables are **build-time** -- they are baked into the JavaScript bundle during `npm run build` or `npm start`. Restart the dev server after changing `.env` or `.env.local`.

### Core API Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `REACT_APP_API_URL` | Base URL for user-management API calls | `/v1/` |
| `REACT_APP_API_URL_EXTERNAL` | Base URL for publication/external-ingestion routes | `/` |
| `REACT_APP_WS_URL` | WebSocket base URL (optional, falls back to API host) | (derived) |
| `REACT_APP_WS_PATH` | WebSocket path | `/ws` |

### Authentication

| Variable | Description |
|----------|-------------|
| `REACT_APP_RECAPTCHA_SITE_KEY` | Google reCAPTCHA v3 site key for login/signup flows |
| `REACT_APP_GOOGLE_SSO_CLIENT_ID` | Google OAuth client ID for "Sign in with Google" |
| `REACT_APP_MICROSOFT_CLIENT_ID` | Microsoft/Azure AD client ID for "Sign in with Microsoft" |
| `REACT_APP_MICROSOFT_TENANT_ID` | Azure AD tenant ID |

### Example Local Configuration (.env.local)

```env
REACT_APP_API_URL=/v1/
REACT_APP_API_URL_EXTERNAL=/
REACT_APP_WS_URL=ws://localhost:3100
REACT_APP_WS_PATH=/ws-api
REACT_APP_RECAPTCHA_SITE_KEY=your-site-key
REACT_APP_GOOGLE_SSO_CLIENT_ID=your-google-client-id
REACT_APP_MICROSOFT_CLIENT_ID=your-microsoft-client-id
REACT_APP_MICROSOFT_TENANT_ID=your-tenant-id
```

## Project Structure

```
src/
  App.js                    # Root app component with router setup
  Root.js                   # Root wrapper
  index.js                  # Entry point
  msalConfig.js             # Microsoft MSAL configuration
  setupProxy.js             # CRA proxy configuration for local dev

  assets/                   # Static assets (images, icons)

  components/
    Auth/
      Signup.js/css         # Login, signup, SSO provider discovery
    Dashboard/
      Dashboard.js/css      # Main dashboard with analytics
    Layout/
      Timeline/             # Post timeline feed
      Postcards/            # Post card components
      Postcreation/         # Content creation and AI-assisted generation
      PostDailog/           # Post detail dialog
      ShareDailog/          # Social sharing dialog
      Teams/                # Team management
      Categories/           # Content category management
      Settings/             # User and org settings
      Userdetails/          # User profile details
      Admin/                # Admin-specific components
      notifications.jsx     # In-app notification panel
      ActivitiesAndTopPosts/# Activity feed and top posts
    sharePostPopup/         # Share post popup component
    userMangement/          # User management components

  contexts/
    AuthContext.js           # Authentication state (JWT, user info)
    errorBoundry.js          # Error boundary wrapper

  pages/
    profile.jsx              # Profile page
    AdminLayout.jsx          # Admin layout wrapper
    AdminTeams.jsx           # Admin teams page
    OrganizationOnboarding.jsx # Org onboarding flow
    admin/                   # Admin sub-pages

  routes/                    # Route definitions

  services/
    apiClient.js             # Centralized Axios client (JWT Bearer, trace IDs, language headers)
    authClient.js            # Auth-specific API client
    authService.js           # Authentication service calls
    ai_post_generation.js    # AI content generation API calls
    categoryService.js       # Category CRUD
    invitaionApis.js         # Team invitation APIs
    platformIntegrations.js  # Social platform connection APIs
    postService.js           # Post CRUD and sharing
    recaptchaService.js      # reCAPTCHA token management
    runtimeConfig.js         # Runtime configuration loader
    settingsService.js       # Settings API calls
    teamServices.js          # Team management API calls
    adminServices.js         # Admin-specific API calls

  shared/
    authSession.js           # Session management utilities
    businessPageComposer.js  # Business page data composition
    constants.js             # Application constants
    mediaResolver.js         # Media URL resolution (Bunny CDN support)
    notificationFormatter.js # Notification display formatting
    useScreensize.js         # Responsive screen size hook

  styles/                    # Global styles
```

## Media Handling (Bunny CDN)

The frontend resolves media URLs through `src/shared/mediaResolver.js`. Media assets (images, videos) are stored on Bunny CDN and referenced by URL in API responses. Video embeds use Bunny CDN streaming URLs. The `mediaResolver` handles URL construction and fallback logic for different media types.

## Authentication Flow

1. User visits login page -- reCAPTCHA v3 token is generated silently
2. User can authenticate via:
   - Email/password with reCAPTCHA validation
   - Google SSO (`@react-oauth/google`)
   - Microsoft SSO (MSAL browser flow)
3. On success, JWT token is stored in localStorage
4. `AuthContext.js` provides auth state to the component tree
5. `apiClient.js` automatically attaches `Authorization: Bearer <token>` to all API requests
6. API client also adds trace/correlation IDs and language headers

## Deployment

Deployments are automated via GitHub Actions. Never deploy manually via SSH.

| Workflow | Trigger | Target |
|----------|---------|--------|
| `dev2-deploy.yml` | Push to `develop` | Dev2 server (206.189.130.17) |
| `prod-deploy.yml` | Push to `main` | Production server (68.183.93.135) |
| `prod-rollback.yml` | Manual | Rollback production |
| `main.yml` | Push to `main` | Additional CI/CD pipeline |

### Deployment Process

1. GitHub Actions triggers on push to `develop` (dev2) or `main` (production)
2. Repo is synced to the VM via rsync over SSH
3. `npm install` and `npm run build` run on the VM with build-time env vars injected
4. Built static files are placed in the nginx webroot (e.g., `/var/www/dashboard.socialripple.ai`)
5. Nginx serves the SPA with appropriate routing rules

### Branch Strategy

- Development branch: `develop`
- Production branch: `main`
- Feature branches: branch off `develop`, PR target is `develop`

## GitHub Secrets (12 secrets used by this service)

This service uses these secrets in its GitHub Actions workflows:

`DIGITALOCEAN_SSH_KEY`, `PAT_TOKEN`, `DEV_NGINX_VM`, `PROD_NGINX_VM`, `DEV_API_DOMAIN`, `PROD_API_DOMAIN`, `REACT_APP_RECAPTCHA_SITE_KEY`, `REACT_APP_GOOGLE_SSO_CLIENT_ID`, `REACT_APP_MICROSOFT_CLIENT_ID`, `REACT_APP_MICROSOFT_TENANT_ID`, `PROD_FE_WEBROOT`, `PROD_FRONTEND_DOMAIN`

## Troubleshooting

### Blank page after build
- Check that `REACT_APP_API_URL` is set correctly for the target environment
- Verify the nginx config serves `index.html` for all routes (SPA fallback)
- Check browser console for JavaScript errors

### Authentication failures
- Verify `REACT_APP_RECAPTCHA_SITE_KEY` matches the backend's `APP_SECURITY_RECAPTCHA_SECRET_KEY` pair
- For Google SSO: ensure the domain is registered in Google Cloud Console
- For Microsoft SSO: verify `REACT_APP_MICROSOFT_CLIENT_ID` and `REACT_APP_MICROSOFT_TENANT_ID` are correct
- Check that JWT tokens in localStorage are not expired

### API calls failing
- Verify `REACT_APP_API_URL` points to the correct backend
- Check CORS: the backend's `APP_CORS_ALLOWED_ORIGINS` must include the frontend's origin
- Review network tab for 401/403 errors indicating auth issues

### WebSocket notifications not connecting
- Default path is `/ws` on the API host
- Override with `REACT_APP_WS_URL` if the WebSocket server is on a different host
- Check that nginx proxies `/ws` to user-management (port 8081)

### Environment variable changes not taking effect
- CRA env vars are build-time only; restart `npm start` or rebuild after changes
- Variables must be prefixed with `REACT_APP_` to be included in the bundle

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
| `APP_JWT_SECRET` | JWT signing key (min 256-bit random). | Signs all auth tokens. If compromised, all user sessions become invalid. | user-mgmt |
| `APP_SECURITY_RECAPTCHA_SECRET_KEY` | Google reCAPTCHA v3 secret key. | Validates captcha tokens on login/signup. Must pair with frontend's `REACT_APP_RECAPTCHA_SITE_KEY`. | user-mgmt |
| `APP_SECURITY_RECAPTCHA_VERIFY_URL` | reCAPTCHA verification API endpoint. | Default: `https://www.google.com/recaptcha/api/siteverify` | user-mgmt |

#### AI Integration (user-management)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `APP_AI_GEMINI_API_KEY` | Google Gemini API key. | Powers AI content generation. | user-mgmt |
| `APP_AI_GEMINI_API_URL` | Gemini 2.5 Pro model endpoint. | Primary AI model. | user-mgmt |
| `APP_AI_GEMINI_FLASH_API_URL` | Gemini 2.5 Flash model endpoint. | Faster model for lighter tasks. | user-mgmt |

#### Social Media OAuth (publication-service)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `APP_OAUTH_X_CONSUMER_KEY` | X/Twitter OAuth 1.0a consumer key. | Required for X connect/share flow. | publication |
| `APP_OAUTH_X_CONSUMER_SECRET` | X/Twitter OAuth 1.0a consumer secret. | Signs Twitter API requests. | publication |
| `APP_OAUTH_LINKEDIN_CLIENT_ID` | LinkedIn OAuth 2.0 client ID. | LinkedIn login and post sharing. | user-mgmt, publication |
| `APP_OAUTH_LINKEDIN_CLIENT_SECRET` | LinkedIn OAuth 2.0 client secret. | LinkedIn API authentication. | publication |
| `APP_OAUTH_LINKEDIN_SCOPE` | LinkedIn OAuth scopes. | Default: `openid profile w_member_social rw_organization_admin r_organization_social` | publication |
| `APP_OAUTH_META_APP_ID` | Meta/Facebook application ID. | Facebook and Instagram OAuth. | publication |
| `APP_OAUTH_META_APP_SECRET` | Meta/Facebook application secret. | Facebook/Instagram API authentication. | publication |
| `APP_OAUTH_ORG_LINKEDIN_CALLBACK_URL` | Org-level LinkedIn OAuth callback URL. | Where LinkedIn redirects after org OAuth. | publication |

#### CORS (all 4 backend repos)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins. | CORS preflight validation. Example: `https://dashboard.socialripple.ai,http://localhost:3000` | user-mgmt, publication, media, notification |

#### Frontend Build-time (frontend only)

| Secret | Description | Why needed | Repos |
|--------|-------------|------------|-------|
| `REACT_APP_RECAPTCHA_SITE_KEY` | Google reCAPTCHA v3 site key. | Captcha challenge in browser. Pairs with backend's secret key. | frontend |
| `REACT_APP_GOOGLE_SSO_CLIENT_ID` | Google OAuth client ID. | "Sign in with Google" button. Domain must be in Google Cloud Console. | frontend |
| `REACT_APP_MICROSOFT_CLIENT_ID` | Microsoft/Azure AD client ID. | "Sign in with Microsoft" button. | frontend |
| `REACT_APP_MICROSOFT_TENANT_ID` | Microsoft/Azure AD tenant ID. | Azure AD tenant for authentication. | frontend |
| `PROD_FE_WEBROOT` | Production webroot path. | Where nginx serves React build. Default: `/var/www/dashboard.socialripple.ai` | frontend |

#### Publication-specific Infrastructure

| Secret | Description | Repos |
|--------|-------------|-------|
| `PROD_DB_VM` | Production database VM IP. Used by ops bootstrap. | publication |

### Branch Strategy

| Repo | Dev Branch | Prod Branch | Dev Deploy | Prod Deploy |
|------|-----------|-------------|------------|-------------|
| user-management | `development` | `main` | Auto on push | Auto on push (env protection) |
| publication-service | `development` | `main` | Auto on push | Auto on push (env protection) |
| media-service | `development` | `main` | Auto on push | Auto on push |
| notification-service | `development` | `main` | Auto on push | Auto on push |
| frontend | `develop` | `main` | Auto on push | Auto on push |
