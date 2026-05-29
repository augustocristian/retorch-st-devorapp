# retorch-st-devorapp

End-to-end test suite for [DevorApp](https://gitlab.com/HP-SCDS/Observatorio/2025-2026/devorapp/epi-devorapp), orchestrated with [RETORCH](https://github.com/giis-uniovi/retorch).

---

## Prerequisites

| Tool | Purpose |
|---|---|
| Docker Desktop / Docker Engine | Run the SUT containers |
| Git | Clone the SUT on first deploy |
| Java 8+ and Maven 3.x | Compile and run the tests |
| `devorapp/backend/firebase-service-account.json` | Firebase credentials — place here after the first deploy |

---

## 1 — Deploy the SUT

The deploy scripts clone the SUT automatically on first run, patch it for test-user login, build the images, and wait for the backend to be healthy.

**Windows**
```powershell
.\deploy-local.ps1          # start (default frontend port 80)
.\deploy-local.ps1 -Port 8080  # custom port
.\deploy-local.ps1 -Down    # stop and remove volumes
```

**Linux / macOS**
```bash
chmod +x deploy-local.sh   # first time only
./deploy-local.sh
./deploy-local.sh --port 8080
./deploy-local.sh --down
```

The SUT is ready when you see:
```
[+] DevorApp backend is ready at http://localhost:8000/health
```

---

## 2 — Run the tests

```bash
# All tests
mvn test

# API tests only
mvn test -Dtest="TestApiAuth,TestApiListas,TestApiFavoritos,TestApiMasTarde,TestApiValoraciones,TestApiHistorial"

# E2E / workflow tests only
mvn test -Dtest="TestSearchRestaurant,TestAddRestaurantToFavorites,TestEvaluateRestaurant"

# Browser tests only
mvn test -Dtest="TestLogin,TestHome"

# Single test class
mvn test -Dtest=TestApiAuth

# Single test method
mvn test -Dtest="TestApiAuth#testHealthEndpoint"
```

Reports are written to `target/local/surefire-reports/`.

---

## Optional configuration

Add keys to `.retorch/envfiles/local.env` (created automatically on first deploy):

```env
FIREBASE_API_KEY=<your-key>   # required for full auth flow
GOOGLE_API_KEY=<your-key>     # required for search and place enrichment
SECRET_KEY=<your-jwt-secret>  # override the default dev secret
```

---

## CI — Jenkins / RETORCH

```bash
mvn test -DSUT_URL=http://backend:8000 -DTJOB_NAME=tjob1
```

The `Jenkinsfile` at the repo root defines the full pipeline.
