package epigijon.devorapp.e2e.functional.tests.api;

import com.google.gson.JsonObject;
import epigijon.devorapp.e2e.functional.common.BaseApiClass;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * API Base-Choice tests for the user profile module.
 *
 * <p>Adapts the profile scenarios from {@code profile.spec.ts} (Playwright)
 * to pure REST assertions.
 *
 * <p>Cases covered:
 * <ul>
 *   <li>BASE   — GET /api/me returns the registered user's data.</li>
 *   <li>S2+S3  — PATCH /api/profile updates nombre and apellidos.</li>
 *   <li>S4     — PATCH /api/profile with correct ubicacion updates it.</li>
 *   <li>S8     — PATCH /api/profile/email with wrong password → HTTP 401.</li>
 *   <li>S6     — PATCH /api/profile/email with a used email → HTTP 400.</li>
 *   <li>S10    — PATCH /api/profile/password with wrong current password → HTTP 400.</li>
 *   <li>S13    — PATCH /api/profile/password with new password < 8 chars → HTTP 400.</li>
 *   <li>S14    — PATCH /api/profile/password with valid new password → HTTP 200.</li>
 *   <li>S17    — DELETE /api/profile removes the account → subsequent GET /api/me → 401.</li>
 * </ul>
 */
class TestApiProfileBC extends BaseApiClass {

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    // ── BASE: GET /api/me devuelve los datos del usuario ──────────────────────

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — GET /api/me returns the authenticated user's username and email")
    void testBase_GetMe() throws IOException {
        JsonObject me = getJsonObject(authUrl("/me"));

        Assertions.assertAll(
                () -> Assertions.assertEquals(testUsername, me.get("username").getAsString(),
                        "username must match"),
                () -> Assertions.assertEquals(testEmail, me.get("email").getAsString(),
                        "email must match")
        );
    }

    // ── S2+S3: actualizar nombre y apellidos ──────────────────────────────────

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("S2+S3 — PATCH /api/profile updates nombre and apellidos; GET /api/me reflects them")
    void testS2S3_ActualizarNombreApellidos() throws IOException {
        String newNombre    = "NuevoNombre" + unique();
        String newApellidos = "NuevosApellidos";

        int status = patch(authUrl("/profile"),
                profileUpdatePayload(newNombre, newApellidos, "", testPassword));
        Assertions.assertEquals(200, status, "PATCH /api/profile must return 200");

        JsonObject me = getJsonObject(authUrl("/me"));
        Assertions.assertAll(
                () -> Assertions.assertEquals(newNombre, me.get("nombre").getAsString(),
                        "nombre must be updated"),
                () -> Assertions.assertEquals(newApellidos, me.get("apellidos").getAsString(),
                        "apellidos must be updated")
        );
    }

    // ── S8: contraseña incorrecta al cambiar email → HTTP 401 ────────────────

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S8 — PATCH /api/profile/email with wrong password returns HTTP 401")
    void testS8_ContrasenaWrongParaEmail() throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("new_email", "nuevo" + unique() + "@devorapp.test");
        payload.addProperty("password", "WrongPassword99!");

        int status = patch(authUrl("/profile/email"), payload.toString());
        Assertions.assertEquals(401, status,
                "Wrong password for email change must return HTTP 401");
    }

    // ── S10: contraseña actual incorrecta al cambiar contraseña → HTTP 400 ───

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S10 — PATCH /api/profile/password with wrong current password returns HTTP 400")
    void testS10_ContrasenaActualIncorrecta() throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("old_password", "WrongCurrent99!");
        payload.addProperty("new_password", "NuevaPass123!");

        int status = patch(authUrl("/profile/password"), payload.toString());
        Assertions.assertEquals(401, status,
                "Wrong current password must return HTTP 401");
    }

    // ── S13: contraseña nueva muy corta → HTTP 400 ───────────────────────────

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("S13 — PATCH /api/profile/password with new password < 8 chars returns HTTP 400")
    void testS13_NuevaContrasenaMuyCorta() throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("old_password", testPassword);
        payload.addProperty("new_password", "Sh1!"); // 4 chars

        int status = patch(authUrl("/profile/password"), payload.toString());
        Assertions.assertEquals(400, status,
                "New password shorter than 8 chars must return HTTP 400");
    }

    // ── S14: cambio de contraseña correcto → HTTP 200 ────────────────────────

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("S14 — PATCH /api/profile/password with valid credentials returns HTTP 200")
    void testS14_CambioContrasenaOk() throws IOException {
        String newPass = "NuevaPassword1234!";
        JsonObject payload = new JsonObject();
        payload.addProperty("old_password", testPassword);
        payload.addProperty("new_password", newPass);

        int status = patch(authUrl("/profile/password"), payload.toString());
        Assertions.assertEquals(200, status,
                "Valid password change must return HTTP 200");

        // Restore original password so other tests and teardown are not affected
        JsonObject restore = new JsonObject();
        restore.addProperty("old_password", newPass);
        restore.addProperty("new_password", testPassword);
        patch(authUrl("/profile/password"), restore.toString());
    }

    // ── S17: eliminar cuenta → GET /api/me → 401 ─────────────────────────────

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("S17 — DELETE /api/profile removes the account; subsequent GET /api/me returns 401")
    void testS17_EliminarCuenta() throws IOException {
        // Create and login a separate user dedicated to deletion
        long ts = unique();
        String email    = uniqueEmail(ts);
        String username = uniqueUsername(ts);
        String password = "Delete1234!";

        postStatus(authUrl("/register"),
                registerPayload(username, email, password, "Del", "User", ""));
        postStatus(authUrl("/login"), loginPayload(email, password));

        int deleteStatus = deleteWithQuery(authUrl("/profile"), "password", password);
        Assertions.assertEquals(200, deleteStatus,
                "DELETE /api/profile must return HTTP 200");

        // After deletion, GET /api/me for the same session should return 401
        int meStatus = getStatus(authUrl("/me"));
        Assertions.assertEquals(401, meStatus,
                "After account deletion GET /api/me must return 401");
    }
}
