package epigijon.devorapp.e2e.functional.tests.e2e;

import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import epigijon.devorapp.e2e.functional.pages.SideMenuPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Browser tests for the DevorApp side menu drawer.
 *
 * <p>Adapts {@code sidemenu.spec.ts} (Playwright) to Selenium + JUnit 5.
 * The side menu is accessed from {@code /home} via the hamburger button.
 *
 * <p>Base-Choice coverage:
 * <ul>
 *   <li>BASE   — clicking "Claro" activates it; "M" font is active by default.</li>
 *   <li>Caso 2 — clicking "Oscuro" activates it.</li>
 *   <li>Caso 3 — clicking "S" font-size applies {@code data-font-size="S"}.</li>
 *   <li>Caso 4 — clicking "L" font-size applies {@code data-font-size="L"}.</li>
 * </ul>
 */
class TestSideMenu extends BaseLoggedClass {

    @BeforeAll
    static void createTestUser() throws Exception {
        long ts = System.currentTimeMillis();
        setupTestUser("menuui" + (ts % 100000), "menuui" + ts + "@devorapp.test", "Test1234!");
    }

    @AfterAll
    static void cleanupTestUser() {
        tearDownTestUser();
    }

    /** Logs in, navigates to /home, and opens the side menu. */
    private SideMenuPage loginAndOpenMenu() throws Exception {
        driver.get(sutUrl + "/login");
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(testPassword)
                .submitLogin();
        return new SideMenuPage(driver, waiter).open();
    }

    // ── BASE: tema claro + letra M (por defecto) ──────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — clicking 'Claro' activates it; M font is active; data-theme='light'")
    void testBase_TemaClaro_LetraM() throws Exception {
        SideMenuPage menu = loginAndOpenMenu();

        menu.clickTheme("Claro");

        Assertions.assertAll(
                () -> Assertions.assertTrue(menu.isThemeActive("Claro"),
                        "The 'Claro' button must have the active class after clicking"),
                () -> Assertions.assertFalse(menu.isThemeActive("Oscuro"),
                        "The 'Oscuro' button must not be active when 'Claro' is selected"),
                () -> Assertions.assertEquals("light", menu.getHtmlDataTheme(),
                        "The <html> element must have data-theme='light'"),
                () -> Assertions.assertTrue(menu.isFontSizeActive("M"),
                        "M font-size must be active by default")
        );
    }

    // ── Caso 2: tema oscuro + letra M ────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 2 — clicking 'Oscuro' activates it; data-theme attribute absent (dark = default)")
    void testCaso2_TemaOscuro_LetraM() throws Exception {
        SideMenuPage menu = loginAndOpenMenu();

        menu.clickTheme("Oscuro");

        Assertions.assertAll(
                () -> Assertions.assertTrue(menu.isThemeActive("Oscuro"),
                        "The 'Oscuro' button must have the active class after clicking"),
                () -> Assertions.assertFalse(menu.isThemeActive("Claro"),
                        "The 'Claro' button must not be active"),
                () -> Assertions.assertEquals("", menu.getHtmlDataTheme(),
                        "Dark mode must have no data-theme attribute on <html>")
        );
    }

    // ── Caso 3: tema claro + letra S ─────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 3 — clicking 'Claro' + 'S' applies light theme and S font size")
    void testCaso3_TemaClaro_LetraS() throws Exception {
        SideMenuPage menu = loginAndOpenMenu();

        menu.clickTheme("Claro");
        menu.clickFontSize("S");

        Assertions.assertAll(
                () -> Assertions.assertEquals("light", menu.getHtmlDataTheme(),
                        "data-theme must be 'light'"),
                () -> Assertions.assertTrue(menu.isFontSizeActive("S"),
                        "'S' font-size button must be active"),
                () -> Assertions.assertFalse(menu.isFontSizeActive("M"),
                        "'M' font-size button must not be active"),
                () -> Assertions.assertEquals("S", menu.getHtmlDataFontSize(),
                        "data-font-size must be 'S'")
        );
    }

    // ── Caso 4: tema claro + letra L ─────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 4 — clicking 'Claro' + 'L' applies light theme and L font size")
    void testCaso4_TemaClaro_LetraL() throws Exception {
        SideMenuPage menu = loginAndOpenMenu();

        menu.clickTheme("Claro");
        menu.clickFontSize("L");

        Assertions.assertAll(
                () -> Assertions.assertEquals("light", menu.getHtmlDataTheme(),
                        "data-theme must be 'light'"),
                () -> Assertions.assertTrue(menu.isFontSizeActive("L"),
                        "'L' font-size button must be active"),
                () -> Assertions.assertFalse(menu.isFontSizeActive("M"),
                        "'M' font-size button must not be active"),
                () -> Assertions.assertEquals("L", menu.getHtmlDataFontSize(),
                        "data-font-size must be 'L'")
        );
    }
}
