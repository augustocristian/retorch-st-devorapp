# CLAUDE.md — retorch-st-devorapp

This file gives Claude Code everything it needs to work in this repository without re-deriving context.

---

## MANDATORY RULES — read before touching any code

> **PACKAGE NAME IS `epigijon.devorapp.e2e.functional`.**
> Every Java source file in this project must declare `package epigijon.devorapp.e2e.functional.*`
> and all imports must use the same prefix. **Never use `giis.*`, `petclinic.*`, or any other
> package root.** If you create a new Java file or move an existing one, verify the `package`
> declaration at the top of the file matches this prefix before saving.

> **ALWAYS UPDATE `CLAUDE.md`.** Any change that affects the project structure, test patterns,
> resources, configuration, or deployment must be reflected in this file before the task is
> considered done. CLAUDE.md is the single source of truth for future sessions.

---

## Project purpose

End-to-end test suite for **DevorApp** — a restaurant discovery and recommendation application — orchestrated with the [RETORCH](https://github.com/giis-uniovi/retorch) framework. The suite contains:

- **API tests** — HTTP-level tests that call FastAPI REST endpoints directly (no browser).
- **Browser tests (E2E)** — Selenium WebDriver tests that drive the React/Nginx frontend through Chrome.

Both suites share the same RETORCH resource model and run under the same CI pipeline.

---

## System Under Test (SUT)

The SUT source lives at:
**`https://gitlab.com/HP-SCDS/Observatorio/2025-2026/devorapp/epi-devorapp`**

The deploy scripts clone it into `devorapp/` automatically on first run. Do **not** commit the `devorapp/` directory — it is cloned at deploy time and is listed in `.gitignore`.

```
devorapp/
├── backend/   FastAPI (Python 3.12) + PostgreSQL 16
├── frontend/  React 19 + TypeScript, served via Nginx
└── keras-api/ ML recommendation microservice (optional)
```

### Services and ports

| Service | Port | Purpose |
|---|---|---|
| `backend` | 8000 | FastAPI REST API (`/api/...`) |
| `frontend` | 80 | Nginx serving the React SPA + proxying `/api/` to backend |
| `db` | 5432 | PostgreSQL 16 (internal only) |

### Key API routes

| Prefix | Endpoints |
|---|---|
| `/api` | Auth: `/login`, `/register`, `/me`, `/profile`, `/logout` |
| `/api/favoritos` | Favorite lists and restaurants |
| `/api/historial` | Restaurant view history |
| `/api/mas-tarde` | Save-for-later |
| `/api/valoraciones` | Ratings and reviews |
| `/api/recommendations` | Restaurant search (`POST /search`) — requires `GOOGLE_API_KEY` |
| `/health` | `{"status":"ok"}` — used by deploy scripts to confirm readiness |

Authentication uses **Firebase** (server-side) + **JWT** stored in an HttpOnly cookie (`access_token`). All protected endpoints require this cookie. The env var `SKIP_EMAIL_VERIFICATION=true` is set in `docker-compose.yml` so that test users can log in without clicking a verification email link.

---

## Repository layout

```
retorch-st-devorapp/
├── devorapp/                              SUT source (cloned from GitLab, not committed)
├── src/test/java/epigijon/devorapp/e2e/functional/
│   ├── common/
│   │   ├── BaseApiClass.java              API base: HTTP client, auth lifecycle, payload/URL builders
│   │   ├── BaseLoggedClass.java           Browser base: Selenium/Selema lifecycle, form helpers
│   │   └── ElementNotFoundException.java
│   ├── tests/
│   │   ├── api/                           Single-endpoint API tests (one concern per test method)
│   │   │   ├── TestApiAuth.java             Health, welcome, availability, me, profile update
│   │   │   ├── TestApiListas.java           Favorites list CRUD
│   │   │   ├── TestApiFavoritos.java        Favorites restaurant CRUD
│   │   │   ├── TestApiMasTarde.java         Save-for-later CRUD
│   │   │   ├── TestApiValoraciones.java     Ratings CRUD
│   │   │   └── TestApiHistorial.java        History CRUD + popular places
│   │   └── e2e/                           System/E2E tests (multi-step user workflows + browser)
│   │       ├── TestSearchRestaurant.java    Search → visit workflow
│   │       ├── TestAddRestaurantToFavorites.java  Full favorites workflow
│   │       ├── TestEvaluateRestaurant.java  Full rating lifecycle (create/read/like/update/delete)
│   │       ├── TestLogin.java               Browser: login form, error handling, register link
│   │       └── TestHome.java                Browser: home page after authentication
│   └── utils/
│       ├── Navigation.java                Page navigation helpers
│       ├── Waiter.java                    Explicit wait conditions for DevorApp pages
│       └── Click.java                     Safe click with JS fallback
├── src/test/resources/
│   ├── test.properties                    LOCALHOST_URL (API), FRONTEND_URL (browser)
│   └── log4j2.xml
├── .retorch/
│   ├── configurations/
│   │   ├── retorchCI.properties
│   │   └── DevorAppSystemResources.json
│   ├── customscriptscode/
│   └── envfiles/local.env                 Auto-created by deploy scripts (add API keys here)
├── docker-compose.yml                     DevorApp services with jenkins_network
├── deploy-local.ps1                       Windows deploy/teardown script
├── deploy-local.sh                        Linux/macOS deploy/teardown script
└── pom.xml
```

### Package split rationale

| Package | Purpose | Test classes |
|---|---|---|
| `tests.api` | One endpoint, one concern per test — fast, focused, no browser | `TestApi*` |
| `tests.e2e` | Multi-step user journeys and browser-level tests | `TestSearch*`, `TestAdd*`, `TestEvaluate*`, `TestLogin`, `TestHome` |

---

## Key dependencies

| Dependency | Purpose |
|---|---|
| JUnit 5 (Jupiter) | Test runner |
| Selenium WebDriver 4.x | Browser automation (browser tests) |
| Selema 4.x | Browser lifecycle manager (wraps WebDriver) |
| Apache HttpClient 4.5.14 | HTTP client (API tests) |
| Gson 2.14.0 | JSON parsing |
| RETORCH annotations 1.2.0 | `@AccessMode` resource declarations |
| Log4j2 + SLF4J | Structured logging |

---

## RETORCH resource model

| Resource ID | Represents | Typical access |
|---|---|---|
| `web-browser` | Chrome browser instance | `READWRITE, concurrency=1, sharing=false` |
| `frontend` | React SPA via Nginx | `READONLY, concurrency=1` |
| `user` | Firebase user + JWT session | `READWRITE, concurrency=1, sharing=false` |
| `favoritos` | Favorite lists + restaurants in DB | `READWRITE, concurrency=1, sharing=false` |
| `historial` | Restaurant history in DB | `READWRITE, concurrency=1, sharing=false` |
| `mas-tarde` | Save-for-later in DB | `READWRITE, concurrency=1, sharing=false` |
| `valoraciones` | Ratings in DB | `READWRITE, concurrency=1, sharing=false` |

---

## Configuration

### `src/test/resources/test.properties`
```properties
BROWSER_USER=CHROME
LOCALHOST_URL=http://localhost:8000   # API tests base URL
FRONTEND_URL=http://localhost         # Browser tests base URL (Nginx)
```

Both `BaseApiClass` and `BaseLoggedClass` read their URL from this file. Override via `-DSUT_URL=<url>` or the `SUT_URL` environment variable.

### `.retorch/envfiles/local.env`
Created automatically by the deploy scripts if absent. Add optional keys:
```env
FIREBASE_API_KEY=<your-key>
GOOGLE_API_KEY=<your-key>
SECRET_KEY=<your-jwt-secret>
```

### `devorapp/backend/firebase-service-account.json`
Required for Firebase authentication to work. Place this file in `devorapp/backend/` after cloning the SUT and before starting containers. It is never committed to any repository.

---

## Deploying the SUT

### Windows
```powershell
.\deploy-local.ps1          # start (default frontend port 80)
.\deploy-local.ps1 -Port 8080
.\deploy-local.ps1 -Down    # tear down + remove volumes
```

### Linux / macOS
```bash
./deploy-local.sh
./deploy-local.sh --port 8080
./deploy-local.sh --down
```

The scripts:
1. Check Docker and git are installed and Docker is running
2. Auto-create `.retorch/envfiles/local.env` if absent
3. Clone `epi-devorapp` from GitLab into `devorapp/` if the directory does not exist
4. After a fresh clone, apply two patches to the SUT source so that test users can log in without email verification (adds `SKIP_EMAIL_VERIFICATION` to `config.py` and guards the check in `auth_service.py`)
5. Create the external `jenkins_network` if absent
6. Build `backend` and `frontend` images from `devorapp/`
7. Start all containers with `docker compose up -d`
8. Poll `http://localhost:8000/health` until `{"status":"ok"}` (up to 120 s)
9. On timeout: dump logs and tear down

---

## Running the tests

### Prerequisites
- SUT deployed and healthy (see above)
- `devorapp/backend/firebase-service-account.json` present
- Java 8+, Maven 3.x

### Full suite
```bash
mvn test
# Reports → target/local/surefire-reports/
```

### Specific class
```bash
mvn test -Dtest=TestApiAuth
mvn test -Dtest=TestApiListas
mvn test -Dtest=TestLogin
```

### CI (Jenkins / RETORCH)
```bash
mvn test -Dtest="TestApiAuth#testHealthEndpoint" \
         -DTJOB_NAME="tjob1" \
         -DSUT_URL="http://backend:8000"
```

---

## Auth lifecycle in API tests

Every test class that calls protected endpoints follows this pattern:

```
@BeforeAll  → registerAndLogin(username, email, password)
              — POST /api/register  (creates Firebase + Firestore user)
              — POST /api/login     (JWT cookie captured by BasicCookieStore)

@BeforeEach → instance setup (e.g. resolve lista id in TestApiFavoritos)

@Test       → exercises the endpoint via the authenticated HTTP client

@AfterAll   → deleteTestUser()
              — DELETE /api/profile?password=...  (removes Firebase + DB records)
```

Each test class creates its own isolated user so RETORCH can run them concurrently without cross-class state pollution.

Browser tests (`TestLogin`, `TestHome`) create the user via a private API client in `@BeforeAll` and log in through the browser UI form in each test or `@BeforeEach`.

---

## Design decisions and known constraints

### `SKIP_EMAIL_VERIFICATION`
Firebase requires email verification before login, which blocks test users from logging in. The deploy scripts patch two files in the freshly-cloned SUT:
- `devorapp/backend/app/core/config.py` — adds `SKIP_EMAIL_VERIFICATION: bool = False` to `Settings`
- `devorapp/backend/app/services/auth_service.py` — wraps the email-verified check with `if not settings.SKIP_EMAIL_VERIFICATION and not user_record.email_verified`

`docker-compose.yml` then sets `SKIP_EMAIL_VERIFICATION: "true"` in the backend environment so the check is bypassed in the test container. The SUT's GitLab source is unmodified.

### Fake `place_id` values
Tests that need fresh state (e.g. `testAddSameRestaurantTwice` in `TestApiMasTarde`) use unique fake place IDs (`"test_place_" + unique()`) instead of real Google Places IDs. The backend stores any string as `place_id`; Google Places enrichment returns `null` for unknown IDs, but all CRUD operations work correctly.

### Cookie-aware HTTP client
`BaseApiClass` builds the `CloseableHttpClient` with a `BasicCookieStore` so the `access_token` JWT cookie set by `POST /api/login` is automatically included in all subsequent requests — no manual header management required.

### Parallel build isolation
The Maven `<directory>` override in `pom.xml` writes compiled classes and surefire reports under `target/${TJOB_NAME}/`, preventing parallel CI invocations from overwriting each other's output.

### Log files
Written to `target/testlogs/log${sys:TJOB_NAME:-testinglocal}-test.log` via Log4j2. Each parallel TJob gets its own file.
