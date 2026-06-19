package epigijon.devorapp.e2e.functional.tests.e2e;

import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import epigijon.devorapp.e2e.functional.pages.RecommendPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Browser tests for the DevorApp recommendation search page
 * ({@code /recommend-restaurants}).
 *
 * <p>Adapts {@code recommendation.spec.ts} (Playwright) to Selenium + JUnit 5.
 * Unlike the Playwright version (which mocks the backend), these tests call
 * the <em>real</em> backend API. Result counts are therefore not asserted to
 * exact numbers; instead we assert structural/functional behaviour.
 *
 * <p>Base-Choice coverage:
 * <ul>
 *   <li>BASE — search with base filters (categories + prices + ubicación preferida) works.</li>
 *   <li>S2 — no categories selected → request is still sent without error.</li>
 *   <li>S4 — no price selected → request is still sent without error.</li>
 *   <li>S6 — "Sin precio" unchecked → does not block search.</li>
 *   <li>S7 — "Abierto ahora" unchecked → does not block search.</li>
 *   <li>S8 — another valid location is chosen → search completes without error.</li>
 *   <li>S9 — other-location selected but left empty → error message shown.</li>
 * </ul>
 */
class TestRecommendView extends BaseLoggedClass {

    @BeforeAll
    static void createTestUser() throws Exception {
        long ts = System.currentTimeMillis();
        setupTestUser("recui" + (ts % 100000), "recui" + ts + "@devorapp.test", "Test1234!");
    }

    @AfterAll
    static void cleanupTestUser() {
        tearDownTestUser();
    }

    private void clearSessionAndLogin() {
        clearSession();
        driver.get(sutUrl + "/login");
    }

    /** Logs in and navigates to /recommend-restaurants. */
    private RecommendPage loginAndGoToRecommend() throws Exception {
        clearSessionAndLogin();
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(testPassword)
                .submitLogin();
        driver.get(sutUrl + "/recommend-restaurants");
        return new RecommendPage(driver, waiter);
    }

    // ── 1. BASE + S8: búsqueda con filtros base (ubicación preferida y alternativa) ────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — búsqueda con filtros base y ubicación preferida/alternativa (BASE, S8)")
    void testBase_BusquedaFiltrosBaseYUbicacionAlternativa() throws Exception {
        // BASE: preferred location + multiple categories + multiple prices
        RecommendPage page = loginAndGoToRecommend();
        page.addCategory("Mexicano", "Mexicano");
        page.clickPrice("€€");
        page.setIncludeNoPrice(true);
        page.setOpenNow(true);
        page.selectPreferredLocation();
        page.search();
        Assertions.assertFalse(page.hasErrorMessage(),
                "BASE: search with base filters must not produce a validation error");

        // S8: alternate location (autocomplete mock needed)
        page = loginAndGoToRecommend();
        page.addCategory("Mexicano",  "Mexicano");
        page.addCategory("Italiano",  "Italiano");
        page.clickPrice("€");
        page.clickPrice("€€");

        injectAutocompleteMock();
        page.selectOtherLocation("Barcelona, España");

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                        "return window.mockAutocompleteInstance !== undefined && " +
                        "window.mockAutocompleteInstance.listeners !== undefined && " +
                        "window.mockAutocompleteInstance.listeners['place_changed'] !== undefined;"));
        ((JavascriptExecutor) driver).executeScript(
                "window.mockAutocompleteInstance.listeners['place_changed'].forEach(cb => cb());");

        page.search();
        Assertions.assertFalse(page.hasErrorMessage(),
                "S8: search with custom location and multiple filters must not produce an error");
    }

    // ── 2. S2, S4, S6, S7: filtros opcionales sin categorías ni precio, booleanos en falso ─

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S2, S4, S6, S7 — búsqueda sin categorías ni precio y con booleanos en false")
    void testFiltrosOpcionales() throws Exception {
        // S2 + S4: no categories, no prices → search without frontend error
        RecommendPage page = loginAndGoToRecommend();
        page.selectPreferredLocation();
        page.search();
        Assertions.assertFalse(page.hasErrorMessage(),
                "S2/S4: searching without categories or prices must not block with an error");

        // S6 + S7: uncheck "sin precio" and "abierto ahora" — user is still logged in
        driver.get(sutUrl + "/recommend-restaurants");
        page = new RecommendPage(driver, waiter);
        page.addCategory("Italiano", "Italiano");
        page.setIncludeNoPrice(false);
        page.setOpenNow(false);
        page.selectPreferredLocation();
        page.search();
        Assertions.assertFalse(page.hasErrorMessage(),
                "S6/S7: unchecking boolean filters must not produce a validation error");
    }

    // ── 3. S9: ubicación alternativa vacía → error de validación ──────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S9 — seleccionar 'otra ubicación' vacía bloquea la búsqueda con un error")
    void testS9_OtraUbicacionVacia() throws Exception {
        RecommendPage page = loginAndGoToRecommend();
        page.addCategory("Mexicano", "Mexicano");
        page.selectOtherLocation(""); // empty location
        page.search();

        Assertions.assertAll(
                () -> Assertions.assertTrue(page.hasErrorMessage(),
                        "Searching without a location when 'otra ubicación' is selected must show an error"),
                () -> Assertions.assertTrue(
                        page.getErrorMessage().toLowerCase().contains("ubicación") ||
                        page.getErrorMessage().toLowerCase().contains("localiz"),
                        "Error message must mention location"));
    }
}
