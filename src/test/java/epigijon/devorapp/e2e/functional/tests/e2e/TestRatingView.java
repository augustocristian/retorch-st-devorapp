package epigijon.devorapp.e2e.functional.tests.e2e;

import com.google.gson.JsonObject;
import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.HistoryPage;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Browser tests for the restaurant rating modal, accessible from the history page.
 *
 * <p>Adapts {@code rating.spec.ts} (Playwright) to Selenium + JUnit 5.
 * A history entry is created via the API before each test, so the
 * browser can navigate to {@code /history} and open the rating modal.
 *
 * <p>Base-Choice coverage:
 * <ul>
 *   <li>BASE  — all 4 aspects at max stars + comment → submit succeeds.</li>
 *   <li>S2    — calidad = 0 → submit button is disabled.</li>
 *   <li>S5    — precio = 0 → submit button is disabled.</li>
 *   <li>S8    — higiene = 0 → submit button is disabled.</li>
 *   <li>S11   — trato = 0 → submit button is disabled.</li>
 *   <li>S14   — empty comment is accepted (comment is optional).</li>
 * </ul>
 */
class TestRatingView extends BaseLoggedClass {

    private static final String PLACE_ID = "ChIJN1t_tDeuEmsRUsoyG83frY4";

    @BeforeAll
    static void createTestUser() throws Exception {
        long ts = System.currentTimeMillis();
        setupTestUser("ratingui" + (ts % 100000), "ratingui" + ts + "@devorapp.test", "Test1234!");
    }

    @BeforeEach
    void ensureHistorialEntry() throws IOException {
        apiLogin();
        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        apiDelete(apiBase + "/api/valoraciones/" + PLACE_ID);
        JsonObject payload = new JsonObject();
        payload.addProperty("place_id", PLACE_ID);
        apiPost(apiBase + "/api/historial", payload.toString());
    }

    @AfterAll
    static void cleanupTestUser() {
        tearDownTestUser();
    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    private HistoryPage loginAndGoToHistory() throws Exception {
        driver.get(sutUrl + "/login");
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(testPassword)
                .submitLogin();
        driver.get(sutUrl + "/history");
        return new HistoryPage(driver, waiter);
    }

    private void openRatingModal(HistoryPage page) throws Exception {
        page.openCardMenu(0);
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        WebElement rateBtn = shortWait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[contains(.,'Valorar restaurante')]")));
        rateBtn.click();
        shortWait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".valuation-content")));
    }

    private void selectStars(String aspect, int stars) {
        if (stars <= 0) return;
        List<WebElement> rows = driver.findElements(By.cssSelector(".aspect-row-premium"));
        WebElement row = rows.stream()
                .filter(r -> r.getText().toLowerCase().contains(aspect.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new org.openqa.selenium.NoSuchElementException(
                        "Aspect row not found: " + aspect));
        List<WebElement> svgs = row.findElements(By.tagName("svg"));
        if (stars <= svgs.size()) svgs.get(stars - 1).click();
    }

    private boolean isSubmitEnabled() {
        List<WebElement> btns = driver.findElements(By.cssSelector("button.btn-submit-valuation"));
        return !btns.isEmpty() && btns.get(0).isEnabled();
    }

    // ── BASE: todos los aspectos al máximo + comentario ───────────────────────

    @AccessMode(resID = "web-browser",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "valoraciones",  concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "historial",     concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",          concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — all aspects at max stars and a comment → submit is enabled and succeeds")
    void testBase_ValoracionCompleta() throws Exception {
        HistoryPage page = loginAndGoToHistory();
        openRatingModal(page);

        selectStars("calidad", 5);
        selectStars("precio", 5);
        selectStars("higiene", 5);
        selectStars("trato", 5);

        WebElement textarea = driver.findElement(By.cssSelector("textarea.textarea-premium"));
        textarea.clear();
        textarea.sendKeys("Excelente servicio y comida deliciosa");

        Assertions.assertTrue(isSubmitEnabled(),
                "Submit button must be enabled when all aspects are rated");

        driver.findElement(By.cssSelector("button.btn-submit-valuation")).click();

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector(".valuation-content")));

        waiter.waitForToast("success");

        Assertions.assertFalse(driver.findElements(By.cssSelector(".toast.success")).isEmpty(),
                "A success toast must appear after a successful rating submission");
    }

    // ── S2: calidad = 0 → botón deshabilitado ─────────────────────────────────

    @AccessMode(resID = "web-browser",  concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",     concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial",    concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S2 — calidad=0 (no stars selected) disables the submit button")
    void testS2_CalidadCero() throws Exception {
        HistoryPage page = loginAndGoToHistory();
        openRatingModal(page);

        selectStars("precio", 5);
        selectStars("higiene", 5);
        selectStars("trato", 5);

        Assertions.assertFalse(isSubmitEnabled(),
                "Submit must be disabled when calidad has 0 stars");
    }

    // ── S5: precio = 0 → botón deshabilitado ──────────────────────────────────

    @AccessMode(resID = "web-browser",  concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",     concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial",    concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S5 — precio=0 (no stars selected) disables the submit button")
    void testS5_PrecioCero() throws Exception {
        HistoryPage page = loginAndGoToHistory();
        openRatingModal(page);

        selectStars("calidad", 5);
        selectStars("higiene", 5);
        selectStars("trato", 5);

        Assertions.assertFalse(isSubmitEnabled(),
                "Submit must be disabled when precio has 0 stars");
    }

    // ── S8: higiene = 0 → botón deshabilitado ────────────────────────────────

    @AccessMode(resID = "web-browser",  concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",     concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial",    concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S8 — higiene=0 (no stars selected) disables the submit button")
    void testS8_HigieneCero() throws Exception {
        HistoryPage page = loginAndGoToHistory();
        openRatingModal(page);

        selectStars("calidad", 5);
        selectStars("precio", 5);
        selectStars("trato", 5);

        Assertions.assertFalse(isSubmitEnabled(),
                "Submit must be disabled when higiene has 0 stars");
    }

    // ── S11: trato = 0 → botón deshabilitado ──────────────────────────────────

    @AccessMode(resID = "web-browser",  concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",     concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial",    concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S11 — trato=0 (no stars selected) disables the submit button")
    void testS11_TratoCero() throws Exception {
        HistoryPage page = loginAndGoToHistory();
        openRatingModal(page);

        selectStars("calidad", 5);
        selectStars("precio", 5);
        selectStars("higiene", 5);

        Assertions.assertFalse(isSubmitEnabled(),
                "Submit must be disabled when trato has 0 stars");
    }

    // ── S14: comentario vacío es aceptado ─────────────────────────────────────

    @AccessMode(resID = "web-browser",  concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",     concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial",    concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S14 — empty comment with all aspects rated still enables the submit button")
    void testS14_ComentarioVacio() throws Exception {
        HistoryPage page = loginAndGoToHistory();
        openRatingModal(page);

        selectStars("calidad", 5);
        selectStars("precio", 5);
        selectStars("higiene", 5);
        selectStars("trato", 5);

        WebElement textarea = driver.findElement(By.cssSelector("textarea.textarea-premium"));
        textarea.clear();

        Assertions.assertTrue(isSubmitEnabled(),
                "Submit must be enabled even with an empty comment when all stars are rated");
    }
}
