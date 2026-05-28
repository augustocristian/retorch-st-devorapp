# RETORCH DevorApp End-to-End Test Suite

End-to-end test suite for [DevorApp](https://gitlab.com/HP-SCDS/Observatorio/2025-2026/devorapp/epi-devorapp) — a restaurant discovery and recommendation application — used as a demonstrator of the [RETORCH Framework](https://github.com/giis-uniovi/retorch).

The suite targets the DevorApp REST API and React/Nginx frontend through two complementary test layers:

- **API tests** — HTTP-level tests that call the FastAPI backend directly (no browser), covering authentication, favorites, history, save-for-later, and ratings.
- **Browser tests** — Selenium WebDriver tests that exercise the React SPA through Chrome.

RETORCH orchestrates parallel test execution using `@AccessMode` annotations that declare which resources each test class reads or writes.

---

## Repository layout

```
retorch-st-devorapp/
├── src/test/java/epigijon/devorapp/e2e/functional/
│   ├── common/          BaseApiClass, BaseLoggedClass, ElementNotFoundException
│   ├── tests/api/       TestApiAuth, TestApiListas, TestApiFavoritos,
│   │                    TestApiMasTarde, TestApiValoraciones, TestApiHistorial
│   ├── tests/           TestLogin, TestHome  (browser/E2E)
│   └── utils/           Navigation, Waiter, Click
├── src/test/resources/  test.properties, log4j2.xml
├── .retorch/            RETORCH CI config and system resources
├── docker-compose.yml   Orchestrates db + backend + frontend
├── deploy-local.ps1     Windows deploy script
├── deploy-local.sh      Linux / macOS deploy script
└── pom.xml
```

The SUT is cloned from GitLab into `devorapp/` by the deploy scripts — it is **not** committed to this repository.

---

## Deployment

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows / macOS) or Docker Engine (Linux)
- Git
- `devorapp/backend/firebase-service-account.json` — Firebase service-account credentials. Place this file after the first clone (before building).

### Windows

```powershell
# First run: clones SUT from GitLab, builds images, starts containers
.\deploy-local.ps1

# Custom frontend port
.\deploy-local.ps1 -Port 8080

# Tear down all containers and volumes
.\deploy-local.ps1 -Down
```

### Linux / macOS

```bash
chmod +x deploy-local.sh   # first time only

./deploy-local.sh           # start
./deploy-local.sh --port 8080
./deploy-local.sh --down    # tear down
```

The scripts handle everything automatically:

1. Clone `epi-devorapp` from GitLab into `devorapp/` if the directory does not exist
2. Patch the SUT to enable test-user login without email verification (`SKIP_EMAIL_VERIFICATION`)
3. Create the external `jenkins_network` Docker network if absent
4. Build and start all containers
5. Wait up to 120 s for `http://localhost:8000/health` to return `{"status":"ok"}`

---

## Running the tests

> **The SUT must be deployed and healthy before running any test.**

```bash
# Full suite
mvn test

# Single test class
mvn test -Dtest=TestApiAuth
mvn test -Dtest=TestApiListas
mvn test -Dtest=TestLogin

# With an explicit SUT URL (CI / Jenkins)
mvn test -DSUT_URL=http://backend:8000 -DTJOB_NAME=tjob1
```

Test reports are written to `target/local/surefire-reports/`.

---

## CI — Jenkins / RETORCH

The `Jenkinsfile` at the repository root defines the full pipeline. Lifecycle scripts are in `.retorch/scripts/` and environment files in `.retorch/envfiles/`. The GitHub Actions workflow only compiles the project; actual test execution runs on Jenkins.
