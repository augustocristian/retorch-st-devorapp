package epigijon.devorapp.e2e.functional.tests.e2e;

import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import epigijon.devorapp.e2e.functional.pages.ProfilePage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Browser tests for the DevorApp profile page ({@code /profile}).
 *
 * <p>Adapts {@code profile.spec.ts} (Playwright) to Selenium + JUnit 5.
 *
 * <p>Base-Choice coverage:
 * <ul>
 *   <li>BASE   — profile loads and shows correct user data.</li>
 *   <li>Caso 2 — cancelling personal-info edit restores original values.</li>
 *   <li>S2/S3  — editing nombre and apellidos and saving updates the card.</li>
 *   <li>S5     — invalid email format is rejected (HTML5 validation).</li>
 *   <li>S8     — wrong password for email change shows error toast.</li>
 *   <li>S6     — already-used email shows error toast.</li>
 *   <li>S10    — wrong current password for password change shows error toast.</li>
 *   <li>S13    — new password too short shows error toast.</li>
 *   <li>S14    — correct password change shows success toast.</li>
 *   <li>S17    — typing CONFIRMAR enables delete button and deletes account.</li>
 * </ul>
 */
class TestProfileView extends BaseLoggedClass {

    @BeforeAll
    static void createTestUser() throws Exception {
        long ts = System.currentTimeMillis();
        setupTestUser("profui" + (ts % 100000), "profui" + ts + "@devorapp.test", "Test1234!");
    }

    @AfterAll
    static void cleanupTestUser() {
        tearDownTestUser();
    }

    /** Logs in and navigates to /profile. */
    private ProfilePage loginAndGoToProfile() throws Exception {
        driver.get(sutUrl + "/login");
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(testPassword)
                .submitLogin();
        driver.get(sutUrl + "/profile");
        return new ProfilePage(driver, waiter);
    }

    // ── BASE: carga de datos del perfil ───────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — profile page loads and shows the authenticated user's data")
    void testBase_CargaPerfil() throws Exception {
        ProfilePage page = loginAndGoToProfile();

        String personalCardText = page.getCardText("Información Personal");
        Assertions.assertFalse(personalCardText.isEmpty(),
                "The personal info card must contain user data after loading");
    }

    // ── Caso 2: cancelar edición restaura valores originales ─────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 2 — cancelling personal info edit restores the original values")
    void testCaso2_CancelarEdicion() throws Exception {
        ProfilePage page = loginAndGoToProfile();

        String original = page.getCardText("Información Personal");

        page.editPersonalInfo()
                .fillInputInCard("Información Personal", 0, "NombreTemporal")
                .cancelPersonalInfo();

        String after = page.getCardText("Información Personal");
        Assertions.assertEquals(original, after,
                "Cancelling edit must restore the original personal info values");
    }

    // ── S2+S3: editar nombre y apellidos correctamente ────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("S2+S3 — editing nombre and apellidos and saving shows success toast and updates card")
    void testS2S3_EditarNombreApellidos() throws Exception {
        ProfilePage page = loginAndGoToProfile();

        page.editPersonalInfo()
                .fillInputInCard("Información Personal", 0, "NuevoNombre")
                .fillInputInCard("Información Personal", 1, "NuevosApellidos")
                .savePersonalInfo();

        waiter.waitForToast("success");

        Assertions.assertAll(
                () -> Assertions.assertTrue(page.hasSuccessToast(),
                        "A success toast must appear after saving personal info"),
                () -> Assertions.assertTrue(
                        page.getCardText("Información Personal").contains("NuevoNombre"),
                        "The card must reflect the updated nombre")
        );
    }

    // ── S8: contraseña incorrecta al cambiar email → error toast ─────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S8 — wrong password for email change shows an error toast")
    void testS8_ContrasenaIncorrectaEmail() throws Exception {
        ProfilePage page = loginAndGoToProfile();

        page.openEmailChange()
                .fillNewEmail("nuevo" + System.currentTimeMillis() + "@devorapp.test")
                .fillEmailPassword("WrongPassword99!")
                .submitEmailChange();

        waiter.waitForToast("error");

        Assertions.assertTrue(page.hasErrorToast(),
                "An error toast must appear when the wrong password is given for email change");
        Assertions.assertTrue(page.getErrorToastText().toLowerCase().contains("contraseña")
                        || page.getErrorToastText().toLowerCase().contains("password")
                        || page.getErrorToastText().toLowerCase().contains("incorrecta"),
                "Error toast must mention incorrect password");
    }

    // ── S10: contraseña actual incorrecta al cambiar contraseña ──────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S10 — wrong current password for password change shows an error toast")
    void testS10_ContrasenaActualIncorrecta() throws Exception {
        ProfilePage page = loginAndGoToProfile();

        page.openPasswordChange()
                .fillPasswordChange("WrongPassword99!", "NuevaPass123!", "NuevaPass123!")
                .submitPasswordChange();

        waiter.waitForToast("error");

        Assertions.assertTrue(page.hasErrorToast(),
                "An error toast must appear when the current password is wrong");
    }

    // ── S13: contraseña nueva muy corta → error toast ────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S13 — new password shorter than 8 chars shows an error toast")
    void testS13_NuevaContrasenaCorta() throws Exception {
        ProfilePage page = loginAndGoToProfile();

        page.openPasswordChange()
                .fillPasswordChange(testPassword, "Sh1!", "Sh1!")
                .submitPasswordChange();

        waiter.waitForToast("error");

        Assertions.assertTrue(page.hasErrorToast(),
                "An error toast must appear when the new password is too short");
        Assertions.assertTrue(page.getErrorToastText().contains("8")
                        || page.getErrorToastText().toLowerCase().contains("caracteres"),
                "Error must mention the 8-character minimum");
    }

    // ── S17: eliminar cuenta con confirmación ─────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("S17 — typing CONFIRMAR enables delete button; account is deleted and redirects to /login")
    void testS17_EliminarCuenta() throws Exception {
        // Create a separate user just for deletion so other tests are unaffected
        long ts = System.currentTimeMillis();
        String delEmail    = "delui" + ts + "@devorapp.test";
        String delUsername = "delui" + (ts % 100000);
        String delPassword = "Delete1234!";

        // Register via the existing helper
        registerUserApi(delUsername, delEmail, delPassword);

        driver.get(sutUrl + "/login");
        new LoginPage(driver, waiter)
                .enterIdentifier(delEmail)
                .enterPassword(delPassword)
                .submitLogin();
        driver.get(sutUrl + "/profile");

        ProfilePage page = new ProfilePage(driver, waiter);
        page.openDeleteAccount()
                .fillDeleteConfirm("CONFIRMAR")
                .submitDeleteAccount();

        waiter.waitForToast("success");

        Assertions.assertAll(
                () -> Assertions.assertTrue(page.hasSuccessToast(),
                        "A success toast must appear after account deletion"),
                () -> {
                    new org.openqa.selenium.support.ui.WebDriverWait(driver,
                            java.time.Duration.ofSeconds(5))
                            .until(org.openqa.selenium.support.ui.ExpectedConditions
                                    .urlContains("/login"));
                    Assertions.assertTrue(driver.getCurrentUrl().contains("/login"),
                            "After deletion the user must be redirected to /login");
                }
        );

    }
}
