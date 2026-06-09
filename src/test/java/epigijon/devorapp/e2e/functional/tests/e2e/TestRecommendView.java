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
 *   <li>BASE  — search with base filters (categories + prices + ubicación preferida) works.</li>
 *   <li>S2    — no categories selected → request is still sent.</li>
 *   <li>S4    — no price selected → request is still sent.</li>
 *   <li>S6    — "Sin precio" unchecked → does not block search.</li>
 *   <li>S7    — "Abierto ahora" unchecked → does not block search.</li>
 *   <li>S9    — other-location selected but left empty → error message shown.</li>
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

    /** Logs in and navigates to /recommend-restaurants. */
    private RecommendPage loginAndGoToRecommend() throws Exception {
        driver.get(sutUrl + "/login");
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(testPassword)
                .submitLogin();
        driver.get(sutUrl + "/recommend-restaurants");
        return new RecommendPage(driver, waiter);
    }

    // ── BASE: filtros base con ubicación preferida ────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — searching with base filters and preferred location does not error out")
    void testBase_BusquedaFiltrosBase() throws Exception {
        RecommendPage page = loginAndGoToRecommend();

        page.addCategory("Mexicano", "Mexicano");
        page.clickPrice("€€");
        page.setIncludeNoPrice(true);
        page.setOpenNow(true);
        page.selectPreferredLocation();
        page.search();

        // Either results are shown OR the page remains error-free
        Assertions.assertFalse(page.hasErrorMessage(),
                "BASE search must not produce a validation error");
    }

    // ── S2+S4: sin categorías ni precio seleccionados ─────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S2+S4 — searching with no categories and no price is allowed (no frontend error)")
    void testS2S4_SinCategoriasNiPrecio() throws Exception {
        RecommendPage page = loginAndGoToRecommend();

        // No categories, no prices — just search with preferred location
        page.selectPreferredLocation();
        page.search();

        Assertions.assertFalse(page.hasErrorMessage(),
                "Searching without categories or prices must not block with an error");
    }

    // ── S6+S7: sin precio y sin abierto-ahora ────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S6+S7 — unchecking 'sin precio' and 'abierto ahora' still allows search")
    void testS6S7_BooleanosFalse() throws Exception {
        RecommendPage page = loginAndGoToRecommend();

        page.addCategory("Italiano", "Italiano");
        page.setIncludeNoPrice(false);
        page.setOpenNow(false);
        page.selectPreferredLocation();
        page.search();

        Assertions.assertFalse(page.hasErrorMessage(),
                "Unchecking boolean filters must not produce a validation error");
    }

    // ── S9: ubicación alternativa vacía → error ───────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S9 — selecting 'otra ubicación' but leaving it empty blocks search with an error")
    void testS9_OtraUbicacionVacia() throws Exception {
        RecommendPage page = loginAndGoToRecommend();

        page.addCategory("Mexicano", "Mexicano");
        page.selectOtherLocation(""); // empty location
        page.search();

        Assertions.assertAll(
                () -> Assertions.assertTrue(page.hasErrorMessage(),
                        "Searching without a location when 'otra ubicación' is selected must show an error"),
                () -> Assertions.assertTrue(
                        page.getErrorMessage().toLowerCase().contains("ubicación")
                                || page.getErrorMessage().toLowerCase().contains("localiz"),
                        "Error message must mention location")
        );
    }
}
