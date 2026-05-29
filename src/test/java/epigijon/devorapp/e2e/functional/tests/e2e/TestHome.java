package epigijon.devorapp.e2e.functional.tests.e2e;

import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.common.ElementNotFoundException;
import epigijon.devorapp.e2e.functional.pages.HomePage;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Browser tests for the DevorApp home page.
 *
 * <p>A test user is registered via API in {@code @BeforeAll}.  The inherited
 * {@code @BeforeEach} navigates to the base URL; the overriding
 * {@code @BeforeEach} then logs in through the UI so every test starts on an
 * authenticated home page.
 */
class TestHome extends BaseLoggedClass {

    private static final String PASSWORD = "Test1234!";

    @BeforeAll
    static void createTestUser() throws Exception {
        long ts = System.currentTimeMillis();
        String username = "tst" + ts;
        if (username.length() > 30) username = username.substring(username.length() - 30);
        setupTestUser(username, "testui" + ts + "@devorapp.test", PASSWORD);
    }

    @AfterAll
    static void cleanupTestUser() {
        tearDownTestUser();
    }

    @BeforeEach
    void loginBeforeEachTest() throws ElementNotFoundException {
        driver.get(sutUrl + "/login");
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(PASSWORD)
                .submitLogin();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Authenticated user lands on /home after login")
    void testHomePageLoads() {
        Assertions.assertTrue(driver.getCurrentUrl().contains("/home"),
                "Authenticated user must land on /home");
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Home page shows the top navigation bar")
    void testTopBarIsVisible() {
        Assertions.assertTrue(new HomePage(driver, waiter).isTopBarVisible(),
                "Top navigation bar must be visible on the home page");
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Navigating to /login while authenticated stays on /home or /login")
    void testNavigatingToLoginWhileAuthenticated() {
        driver.navigate().to(sutUrl + "/login");
        waiter.waitUntil(
                ExpectedConditions.or(
                        ExpectedConditions.urlContains("/home"),
                        ExpectedConditions.urlContains("/login")),
                "Page did not settle after navigating to /login");

        String url = driver.getCurrentUrl();
        Assertions.assertTrue(url.contains("/home") || url.contains("/login"),
                "URL must be /home or /login when navigating with an active session");
    }
}
