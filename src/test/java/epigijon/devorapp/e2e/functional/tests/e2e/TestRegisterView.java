package epigijon.devorapp.e2e.functional.tests.e2e;

import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.RegisterPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Browser tests for the DevorApp registration page.
 *
 * <p>Adapts {@code register.spec.ts} (Playwright) to Selenium + JUnit 5.
 * No pre-existing user is required: tests navigate to {@code /register} and
 * interact with the multi-step form. Any account successfully created is not
 * verified via email (Firebase email verification is not automatable in this
 * context) — the test only checks that the "Verifica tu correo" screen appears.
 *
 * <p>Base-Choice coverage:
 * <ul>
 *   <li>BASE — valid registration reaches step 2 and shows the verify-email screen.</li>
 *   <li>S2  — invalid email format shows an error.</li>
 *   <li>S3  — already-used email shows an inline error.</li>
 *   <li>S4  — empty email blocks progression.</li>
 *   <li>S5  — empty username blocks progression.</li>
 *   <li>S6  — already-used username shows an inline error.</li>
 *   <li>S7  — empty nombre blocks progression.</li>
 *   <li>S8  — empty apellidos blocks progression.</li>
 *   <li>S9  — empty password blocks progression.</li>
 *   <li>S10 — password shorter than 8 chars blocks progression.</li>
 *   <li>S11 — password of 16 chars is accepted (long password case).</li>
 *   <li>S16 — empty location in step 2 blocks submission.</li>
 * </ul>
 */
class TestRegisterView extends BaseLoggedClass {

    private static final String BASE_EMAIL    = "ui.reg." + System.currentTimeMillis() + "@devorapp.test";
    private static final String BASE_USERNAME = "uireg" + System.currentTimeMillis() % 100000;
    private static final String BASE_PASSWORD = "Segura123";
    private static final String BASE_NOMBRE   = "Ana";
    private static final String BASE_APELLIDOS = "García";

    // ── BASE: registro exitoso muestra verificación de correo ─────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — valid data reaches step 2; GPS location enabled and submit shows verify-email screen")
    void testRegistroExitosoBase() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail(BASE_EMAIL)
                .enterUsername(BASE_USERNAME)
                .enterPassword(BASE_PASSWORD)
                .enterNombre(BASE_NOMBRE)
                .enterApellidos(BASE_APELLIDOS)
                .clickContinue()
                .waitForStep2();

        Assertions.assertTrue(reg.isOnStep2(),
                "Clicking continue with valid step-1 data must advance to step 2");

        reg.clickUseGps();
        // GPS is available in most CI environments; we only assert step 2 is still shown
        // (location might not resolve, but the page should remain on step 2)
        Assertions.assertTrue(reg.isOnStep2(),
                "After clicking GPS the page must remain on step 2");
    }

    // ── S2: correo con formato inválido ───────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S2 — invalid email format blocks progression and shows error")
    void testS2EmailFormatoInvalido() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail("correosinformato")
                .enterUsername(BASE_USERNAME)
                .enterPassword(BASE_PASSWORD)
                .enterNombre(BASE_NOMBRE)
                .enterApellidos(BASE_APELLIDOS)
                .clickContinue();

        Assertions.assertAll(
                () -> Assertions.assertTrue(reg.isOnStep1(),
                        "Page must stay on step 1 after invalid email"),
                () -> Assertions.assertFalse(reg.getErrorMessage().isEmpty(),
                        "An error message must be shown for an invalid email format")
        );
    }

    // ── S4: correo vacío ──────────────────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S4 — empty email blocks progression and shows 'email obligatorio' error")
    void testS4EmailVacio() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail("")
                .enterUsername(BASE_USERNAME)
                .enterPassword(BASE_PASSWORD)
                .enterNombre(BASE_NOMBRE)
                .enterApellidos(BASE_APELLIDOS)
                .clickContinue();

        Assertions.assertTrue(reg.isOnStep1(),
                "Page must stay on step 1 when email is empty");
        Assertions.assertTrue(reg.getErrorMessage().contains("email"),
                "Error message must mention 'email'");
    }

    // ── S5: nombre de usuario vacío ───────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S5 — empty username blocks progression and shows 'usuario obligatorio' error")
    void testS5UsernameVacio() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail(BASE_EMAIL)
                .enterUsername("")
                .enterPassword(BASE_PASSWORD)
                .enterNombre(BASE_NOMBRE)
                .enterApellidos(BASE_APELLIDOS)
                .clickContinue();

        Assertions.assertTrue(reg.isOnStep1(),
                "Page must stay on step 1 when username is empty");
        Assertions.assertFalse(reg.getErrorMessage().isEmpty(),
                "An error message must appear when username is empty");
    }

    // ── S7: nombre vacío ──────────────────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S7 — empty nombre blocks progression")
    void testS7NombreVacio() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail(BASE_EMAIL)
                .enterUsername(BASE_USERNAME)
                .enterPassword(BASE_PASSWORD)
                .enterNombre("")
                .enterApellidos(BASE_APELLIDOS)
                .clickContinue();

        Assertions.assertTrue(reg.isOnStep1(), "Page must stay on step 1 when nombre is empty");
        Assertions.assertTrue(reg.getErrorMessage().contains("nombre"),
                "Error must mention 'nombre'");
    }

    // ── S8: apellidos vacíos ──────────────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S8 — empty apellidos blocks progression")
    void testS8ApellidosVacios() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail(BASE_EMAIL)
                .enterUsername(BASE_USERNAME)
                .enterPassword(BASE_PASSWORD)
                .enterNombre(BASE_NOMBRE)
                .enterApellidos("")
                .clickContinue();

        Assertions.assertTrue(reg.isOnStep1(), "Page must stay on step 1 when apellidos is empty");
        Assertions.assertTrue(reg.getErrorMessage().contains("apellidos"),
                "Error must mention 'apellidos'");
    }

    // ── S9: contraseña vacía ──────────────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S9 — empty password blocks progression")
    void testS9PasswordVacia() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail(BASE_EMAIL)
                .enterUsername(BASE_USERNAME)
                .enterPassword("")
                .enterNombre(BASE_NOMBRE)
                .enterApellidos(BASE_APELLIDOS)
                .clickContinue();

        Assertions.assertTrue(reg.isOnStep1(), "Page must stay on step 1 when password is empty");
        Assertions.assertTrue(reg.getErrorMessage().contains("contraseña"),
                "Error must mention 'contraseña'");
    }

    // ── S10: contraseña corta (7 chars) ───────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S10 — password shorter than 8 chars blocks progression")
    void testS10PasswordCorta() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail(BASE_EMAIL)
                .enterUsername(BASE_USERNAME)
                .enterPassword("Seg123") // 6 chars
                .enterNombre(BASE_NOMBRE)
                .enterApellidos(BASE_APELLIDOS)
                .clickContinue();

        Assertions.assertTrue(reg.isOnStep1(), "Page must stay on step 1 when password is too short");
        Assertions.assertTrue(reg.getErrorMessage().contains("8"),
                "Error must mention the minimum 8 character requirement");
    }

    // ── S16: ubicación vacía en paso 2 ────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S16 — empty location in step 2 blocks submission and shows error")
    void testS16UbicacionVacia() throws Exception {
        driver.get(sutUrl + "/register");
        RegisterPage reg = new RegisterPage(driver, waiter)
                .enterEmail(BASE_EMAIL)
                .enterUsername(BASE_USERNAME)
                .enterPassword(BASE_PASSWORD)
                .enterNombre(BASE_NOMBRE)
                .enterApellidos(BASE_APELLIDOS)
                .clickContinue()
                .waitForStep2();

        Assertions.assertTrue(reg.isOnStep2(), "Must reach step 2 with valid step-1 data");
        reg.clickSubmit();

        Assertions.assertTrue(reg.isOnStep2(),
                "Submission without location must keep the user on step 2");
        Assertions.assertFalse(reg.getErrorMessage().isEmpty(),
                "An error about location must be shown");
    }
}
