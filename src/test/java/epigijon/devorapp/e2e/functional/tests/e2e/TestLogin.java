package epigijon.devorapp.e2e.functional.tests.e2e;

import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Browser tests for the DevorApp login page.
 *
 * <p>Every test obtains a fresh, incognito browser session (managed by
 * {@link BaseLoggedClass}) and navigates to {@code /login} before creating
 * a {@link LoginPage} object.  The test user is created via API in
 * {@code @BeforeAll} and cleaned up in {@code @AfterAll}.
 */
class TestLogin extends BaseLoggedClass {

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

    // ── Tests ─────────────────────────────────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Valid credentials redirect the user to the home page")
    void testSuccessfulLogin() throws Exception {
        driver.get(sutUrl + "/login");
        String url = new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(PASSWORD)
                .submitLogin()
                .getCurrentUrl();

        Assertions.assertTrue(url.contains("/home"),
                "After login the URL must contain /home");
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Wrong password shows an error message and stays on /login")
    void testLoginWithWrongPassword() throws Exception {
        driver.get(sutUrl + "/login");
        LoginPage page = new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword("WrongPassword99!")
                .submitLoginExpectingFailure();

        Assertions.assertAll(
                () -> Assertions.assertTrue(page.hasErrorMessage(),
                        "An error message must be visible after a failed login"),
                () -> Assertions.assertTrue(driver.getCurrentUrl().contains("/login"),
                        "The URL must remain on /login after a failed attempt")
        );
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("The register link navigates to /register")
    void testRegisterLink() throws Exception {
        driver.get(sutUrl + "/login");
        new LoginPage(driver, waiter).goToRegister();

        Assertions.assertTrue(driver.getCurrentUrl().contains("/register"),
                "Clicking the register link must navigate to /register");
    }
}
