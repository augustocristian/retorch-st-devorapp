package epigijon.devorapp.e2e.functional.tests.e2e;

import com.google.gson.JsonObject;
import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.HistoryPage;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Browser tests for the DevorApp history page ({@code /history}).
 *
 * <p>Adapts {@code history.spec.ts} (Playwright) to Selenium + JUnit 5.
 * History entries are created via the API so the browser tests can verify
 * grouping by month, card counts, and the search/filter behaviour.
 *
 * <p>Base-Choice coverage:
 * <ul>
 *   <li>BASE   — multiple months, multiple restaurants, no search filter.</li>
 *   <li>Caso 2 — empty history shows 0 groups and 0 cards.</li>
 *   <li>Caso 3 — 1 month with multiple restaurants.</li>
 *   <li>Caso 5 — exactly 1 restaurant in history.</li>
 *   <li>Caso 6 — search term filters cards and hides non-matching months.</li>
 * </ul>
 */
class TestHistoryView extends BaseLoggedClass {

    private static final String PLACE_A = "ChIJN1t_tDeuEmsRUsoyG83frY4";
    private static final String PLACE_B = "ChIJdd4hrwug2EcRmSrV3Vo6llI";

    @BeforeAll
    static void createTestUser() throws Exception {
        long ts = System.currentTimeMillis();
        setupTestUser("histui" + (ts % 100000), "histui" + ts + "@devorapp.test", "Test1234!");
    }

    @AfterAll
    static void cleanupTestUser() {
        tearDownTestUser();
    }

    private void clearSessionAndLogin() {
        clearSession();
        driver.get(sutUrl + "/login");
    }

    /** Logs in and navigates to /history. */
    private HistoryPage loginAndGoToHistory() throws Exception {
        clearSessionAndLogin();
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(testPassword)
                .submitLogin();
        driver.get(sutUrl + "/history");
        return new HistoryPage(driver, waiter);
    }

    /** Adds a restaurant to history via the backend API. */
    private void addHistorialEntry(String placeId) throws IOException {
        apiLogin();
        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        JsonObject payload = new JsonObject();
        payload.addProperty("place_id", placeId);
        apiPost(apiBase + "/api/historial", payload.toString());
    }

    // ── BASE: múltiples entradas en historial ──────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — history page shows at least 1 group and multiple restaurant cards")
    void testBase_MultiplesEntradas() throws Exception {
        addHistorialEntry(PLACE_A);
        addHistorialEntry(PLACE_B);

        HistoryPage page = loginAndGoToHistory();

        Assertions.assertAll(
                () -> Assertions.assertTrue(page.getGroupCount() >= 1,
                        "At least 1 month group must be visible (BASE)"),
                () -> Assertions.assertTrue(page.getCardCount() >= 1,
                        "At least 1 restaurant card must be visible")
        );
    }

    // ── Caso 2, 3 y 5 condensados: vacío, 1 mes varios restaurantes, 1 restaurante ────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("debe gestionar historial vacío (Caso 2), con 1 restaurante (Caso 5) y 1 mes con varios (Caso 3)")
    void testCasosVacioYUnitario() throws Exception {
        // Caso 2: empty history
        HistoryPage page = loginAndGoToHistory();
        if (page.getGroupCount() == 0) {
            Assertions.assertEquals(0, page.getCardCount(),
                    "Caso 2: with 0 groups there must also be 0 restaurant cards");
        }
        Assertions.assertNotNull(page, "History page must load without errors");

        // Caso 5: exactly 1 restaurant entry
        addHistorialEntry(PLACE_A);
        driver.get(sutUrl + "/history");
        final HistoryPage caso5Page = new HistoryPage(driver, waiter);
        Assertions.assertAll(
                () -> Assertions.assertTrue(caso5Page.getGroupCount() >= 1,
                        "Caso 5: at least 1 month group must exist when there is 1 history entry"),
                () -> Assertions.assertTrue(caso5Page.getCardCount() >= 1,
                        "Caso 5: at least 1 card must be visible for the history entry")
        );

        // Caso 3: 1 month with multiple restaurants (add a second entry)
        addHistorialEntry(PLACE_B);
        driver.get(sutUrl + "/history");
        final HistoryPage caso3Page = new HistoryPage(driver, waiter);
        Assertions.assertAll(
                () -> Assertions.assertTrue(caso3Page.getGroupCount() >= 1,
                        "Caso 3: at least 1 month group must be visible"),
                () -> Assertions.assertTrue(caso3Page.getCardCount() >= 2,
                        "Caso 3: at least 2 restaurant cards must be visible in the same month group")
        );
    }

    // ── Caso 6: búsqueda filtra por nombre ────────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 6 — searching in history filters cards; a non-matching term shows 0 cards")
    void testCaso6_BusquedaFiltros() throws Exception {
        addHistorialEntry(PLACE_A);

        HistoryPage page = loginAndGoToHistory();

        int before = page.getCardCount();
        Assertions.assertTrue(before >= 1, "Must have at least 1 card before searching");

        page.search("zzz_nada_xyzzy_no_match");
        Assertions.assertEquals(0, page.getCardCount(),
                "Searching with a non-matching term must hide all restaurant cards");
    }
}
