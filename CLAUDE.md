# CLAUDE.md — retorch-st-devorapp

This file gives Claude Code everything it needs to work in this repository without re-deriving context.

---

## MANDATORY RULES — read before touching any code

> **PACKAGE NAME IS `epigijon.devorapp.e2e.functional`.**
> Every Java source file must declare `package epigijon.devorapp.e2e.functional.*` and all
> imports must use the same prefix. **Never use `giis.*` or any other package root.**
> If you create or move a Java file, verify the `package` declaration before saving.

> **ALWAYS UPDATE `CLAUDE.md`.** Any change that affects project structure, test patterns,
> resources, configuration, or deployment must be reflected here before the task is done.
> CLAUDE.md is the single source of truth for future sessions.

---

## Project purpose

End-to-end test suite for **DevorApp** — a restaurant discovery and recommendation application — orchestrated with the [RETORCH](https://github.com/giis-uniovi/retorch) framework.

- **API tests** — HTTP-level tests that call the FastAPI backend directly.
- **E2E/System tests** — multi-step user-workflow tests (API-level) and browser tests (Selenium).

---

## System Under Test (SUT)

Cloned from:
**`https://gitlab.com/HP-SCDS/Observatorio/2025-2026/devorapp/epi-devorapp`**

The deploy scripts clone it into `devorapp/` on first run. Do **not** commit the `devorapp/` directory — it is gitignored.

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
| `frontend` | 80 | Nginx serving the React SPA + `/api/` proxy |
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
| `/health` | `{"status":"ok"}` — health probe |

Authentication uses Firebase + JWT in an HttpOnly cookie (`access_token`). `SKIP_EMAIL_VERIFICATION=true` is set in `docker-compose.yml` so test users can log in immediately after registration.

---

## Repository layout

```
retorch-st-devorapp/
├── devorapp/                              SUT source (cloned, not committed)
├── src/test/java/epigijon/devorapp/e2e/functional/
│   ├── common/
│   │   ├── BaseApiClass.java              API base: cookie-aware HTTP client, auth lifecycle,
│   │   │                                  payload builders, URL builders, CRUD helpers
│   │   ├── BaseLoggedClass.java           Browser base: Selema/Chrome lifecycle, test-user
│   │   │                                  lifecycle (setupTestUser / tearDownTestUser)
│   │   └── ElementNotFoundException.java
│   ├── pages/                             Page Object Model — one class per application page
│   │   ├── BasePage.java                  Abstract base: driver + waiter, fill/click/isVisible
│   │   ├── LoginPage.java                 /login — enter credentials, submit, navigate to register
│   │   ├── RegisterPage.java              /register — readiness check
│   │   └── HomePage.java                  /home — top-bar visibility, current URL
│   ├── tests/
│   │   ├── api/                           Single-endpoint API tests
│   │   │   ├── TestApiAuth.java             Health, welcome, availability, me, profile update
│   │   │   ├── TestApiListas.java           Favorites list CRUD
│   │   │   ├── TestApiFavoritos.java        Favorites restaurant CRUD
│   │   │   ├── TestApiMasTarde.java         Save-for-later CRUD
│   │   │   ├── TestApiValoraciones.java     Ratings CRUD
│   │   │   └── TestApiHistorial.java        History CRUD + popular places
│   │   └── e2e/                           Multi-step workflow tests + browser tests
│   │       ├── TestSearchRestaurant.java    Search → add result to historial
│   │       ├── TestAddRestaurantToFavorites.java  Full favorites workflow
│   │       ├── TestEvaluateRestaurant.java  Full rating lifecycle
│   │       ├── TestLogin.java               Browser: login form, error handling, register link
│   │       └── TestHome.java                Browser: home page after login
│   └── utils/
│       ├── Click.java                     Safe click (native then JS fallback)
│       └── Waiter.java                    Explicit-wait conditions for each page
├── src/test/resources/
│   ├── test.properties                    LOCALHOST_URL (API), FRONTEND_URL (browser)
│   └── log4j2.xml
├── .retorch/
│   ├── configurations/
│   │   ├── retorchCI.properties
│   │   └── DevorAppSystemResources.json
│   ├── customscriptscode/
│   └── envfiles/local.env                 Auto-created (add FIREBASE_API_KEY etc. here)
├── docker-compose.yml
├── deploy-local.ps1
├── deploy-local.sh
└── pom.xml
```

### Package split rationale

| Package | Concern | Classes |
|---|---|---|
| `common` | Shared base classes | `BaseApiClass`, `BaseLoggedClass` |
| `pages` | Page Object Model — encapsulates all DOM interaction | `BasePage`, `LoginPage`, `RegisterPage`, `HomePage` |
| `tests.api` | One endpoint, one concern per test | `TestApi*` |
| `tests.e2e` | Multi-step user journeys + browser tests | `TestSearch*`, `TestAdd*`, `TestEvaluate*`, `TestLogin`, `TestHome` |
| `utils` | Low-level Selenium helpers used by page objects | `Click`, `Waiter` |

---

## Page Object Model

Browser tests **never** call `driver.findElement(...)` or import `By` directly. All DOM interaction goes through the `pages/` classes:

```
BasePage          holds driver + waiter; provides fill(), click(), isVisible()
  ├─ LoginPage    waits for login form in constructor; fluent enterIdentifier/
  │               enterPassword/submitLogin/submitLoginExpectingFailure/goToRegister
  ├─ RegisterPage waits for register form; isLoaded()
  └─ HomePage     waits for /home URL and top-bar; isTopBarVisible(), getCurrentUrl()
```

Page transitions return the resulting page object:
- `loginPage.submitLogin()` → `HomePage`
- `loginPage.goToRegister()` → `RegisterPage`

---

## Key dependencies

| Dependency | Purpose |
|---|---|
| JUnit 5 (Jupiter) | Test runner |
| Selenium WebDriver 4.x | Browser automation |
| Selema 4.x | Browser lifecycle manager (wraps WebDriver) |
| Apache HttpClient 4.5.14 | HTTP client (API tests and user lifecycle in browser tests) |
| Gson 2.14.0 | JSON parsing |
| RETORCH annotations 1.2.0 | `@AccessMode` resource declarations |
| Log4j2 + SLF4J | Structured logging |

---

## RETORCH resource model

| Resource ID | Represents |
|---|---|
| `web-browser` | Chrome browser instance |
| `frontend` | React SPA via Nginx |
| `user` | Firebase user + JWT session |
| `favoritos` | Favorite lists + restaurants in DB |
| `historial` | Restaurant history in DB |
| `mas-tarde` | Save-for-later in DB |
| `valoraciones` | Ratings in DB |

---

## Configuration

### `src/test/resources/test.properties`
```properties
BROWSER_USER=CHROME
LOCALHOST_URL=http://localhost:8000   # API tests base URL
FRONTEND_URL=http://localhost         # Browser tests base URL (Nginx on :80)
```

Override with `-DSUT_URL=<url>` or the `SUT_URL` environment variable.

### `.retorch/envfiles/local.env`
Auto-created by deploy scripts. Add:
```env
FIREBASE_API_KEY=<your-key>
GOOGLE_API_KEY=<your-key>
SECRET_KEY=<your-jwt-secret>
```

### `devorapp/backend/firebase-service-account.json`
Place in `devorapp/backend/` after cloning. Never committed.

---

## Deploying the SUT

```powershell
# Windows
.\deploy-local.ps1            # start (clones SUT on first run, default port 80)
.\deploy-local.ps1 -Port 8080 # custom frontend port
.\deploy-local.ps1 -Down      # tear down + remove volumes
```

```bash
# Linux / macOS
./deploy-local.sh
./deploy-local.sh --port 8080
./deploy-local.sh --down
```

The scripts:
1. Clone `epi-devorapp` from GitLab into `devorapp/` if absent
2. Patch `config.py` and `auth_service.py` for `SKIP_EMAIL_VERIFICATION`
3. Create the `jenkins_network` Docker network if absent
4. Build and start containers (`docker compose up -d`)
5. Poll `http://localhost:8000/health` until `{"status":"ok"}` (120 s timeout)

---

## Running the tests

```bash
mvn test                          # full suite
mvn test -Dtest=TestApiAuth       # single API class
mvn test -Dtest=TestLogin         # single browser class
# CI
mvn test -DSUT_URL=http://backend:8000 -DTJOB_NAME=tjob1
```

Reports → `target/local/surefire-reports/`

---

## Auth lifecycle in API tests

```
@BeforeAll  registerAndLogin(username, email, password)
            — POST /api/register  → creates Firebase + Firestore user
            — POST /api/login     → JWT cookie captured by BasicCookieStore

@BeforeEach instance setup (e.g. resolve lista id in TestApiFavoritos)

@Test       exercises endpoint via the authenticated HTTP client

@AfterAll   deleteTestUser()
            — DELETE /api/profile?password=...
```

## Auth lifecycle in browser tests

```
@BeforeAll  setupTestUser(username, email, password)
            — POST /api/register via static API client in BaseLoggedClass

@BeforeEach (TestHome) driver.get(sutUrl + "/login")
            new LoginPage(driver, waiter)
                .enterIdentifier(email).enterPassword(password).submitLogin()

@AfterAll   tearDownTestUser()
            — POST /api/login then DELETE /api/profile
```

---

## Design decisions

### SKIP_EMAIL_VERIFICATION
Firebase rejects login for unverified emails. The deploy scripts patch the cloned SUT to add a `SKIP_EMAIL_VERIFICATION` field to `config.py` and guard the check in `auth_service.py`. Docker-compose sets `SKIP_EMAIL_VERIFICATION=true`. The GitLab source is untouched.

### Fake `place_id` values
Tests that need isolated state use `"test_place_" + unique()` or `"eval_" + unique()`. The backend stores any string as `place_id`; Google Places enrichment returns `null` for unknown IDs, but all CRUD operations work correctly.

### Cookie-aware HTTP client
`BaseApiClass` builds `CloseableHttpClient` with a `BasicCookieStore`. The `access_token` JWT cookie from `/api/login` is automatically included in all subsequent requests.

### Parallel build isolation
Maven `<directory>target/${TJOB_NAME}</directory>` prevents parallel CI jobs from overwriting each other's output.
