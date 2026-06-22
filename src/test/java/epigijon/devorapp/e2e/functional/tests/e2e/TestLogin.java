package epigijon.devorapp.e2e.functional.tests.e2e;

import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Browser tests for the DevorApp login page.
 *
 * <p>Base-Choice coverage:
 * <ul>
 *   <li>BASE — valid credentials redirect to /home (S7 - Happy Path).</li>
 *   <li>S3–S6 — empty identifier and/or password trigger a validation error (BASE, S3, S4, S5, S6).</li>
 *   <li>S8 — wrong password shows an error and stays on /login.</li>
 * </ul>
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

    private void clearSessionAndLogin() {
        clearSession();
        driver.get(sutUrl + "/login");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────────

    /**
     * BASE / S7 — valid credentials redirect the user to /home.
     */
    @Test
    @DisplayName("Valid credentials redirect the user to the home page (BASE / S7 - Happy Path)")
    void testSuccessfulLogin() throws Exception {
        clearSessionAndLogin();
        String url = new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(PASSWORD)
                .submitLogin()
                .getCurrentUrl();

        Assertions.assertTrue(url.contains("/home"),
                "After login the URL must contain /home");
    }

    /**
     * S3–S6, S8 — empty fields and wrong password are rejected with an error.
     *
     * <p>Combines:
     * <ul>
     *   <li>BASE / S4: existing identifier, empty password → «Rellene todos los campos».</li>
     *   <li>S6: both fields empty → same error.</li>
     *   <li>S8: correct identifier, wrong password → «Credenciales incorrectas».</li>
     * </ul>
     */
    @Test
    @DisplayName("Empty fields (BASE, S3–S6) and wrong password (S8) trigger validation errors")
    void testLoginValidationErrors() throws Exception {
        // 1. BASE / S4: identifier filled, password empty
        clearSessionAndLogin();
        LoginPage page = new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword("");

        page.submitLoginExpectingFailure();
        Assertions.assertTrue(page.hasErrorMessage(),
                "Error message must show for empty password (BASE/S4)");

        // 2. S6: both fields empty
        clearSessionAndLogin();
        page = new LoginPage(driver, waiter)
                .enterIdentifier("")
                .enterPassword("");

        page.submitLoginExpectingFailure();
        Assertions.assertTrue(page.hasErrorMessage(),
                "Error message must show when both fields are empty (S6)");

        // 3. S8: correct identifier, wrong password
        clearSessionAndLogin();
        final LoginPage finalPage = new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword("WrongPassword99!")
                .submitLoginExpectingFailure();

        Assertions.assertAll(
                () -> Assertions.assertTrue(finalPage.hasErrorMessage(),
                        "An error message must be visible after a failed login (S8)"),
                () -> Assertions.assertTrue(driver.getCurrentUrl().contains("/login"),
                        "The URL must remain on /login after a failed attempt (S8)")
        );
    }
}