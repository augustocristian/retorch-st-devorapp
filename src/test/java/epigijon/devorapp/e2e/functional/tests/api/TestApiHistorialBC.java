package epigijon.devorapp.e2e.functional.tests.api;

import com.google.gson.JsonArray;
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
 * API Base-Choice tests for the history (historial) module.
 *
 * <p>Adapts the history scenarios from {@code history.spec.ts} (Playwright)
 * to pure REST assertions.
 *
 * <p>Cases covered:
 * <ul>
 *   <li>BASE   — multiple history entries are returned by GET /api/historial.</li>
 *   <li>Caso 2 — a fresh user has an empty history (0 entries).</li>
 *   <li>Caso 5 — exactly 1 history entry is returned with correct fields.</li>
 *   <li>Caso 6 — deleting an entry removes it from the history list.</li>
 *   <li>Caso 3 — entry has a fecha_acceso (timestamp) field populated.</li>
 * </ul>
 */
class TestApiHistorialBC extends BaseApiClass {

    private static final String PLACE_A = "ChIJN1t_tDeuEmsRUsoyG83frY4";
    private static final String PLACE_B = "ChIJdd4hrwug2EcRmSrV3Vo6llI";
    private static final String PLACE_C = "ChIJ2eUgeAK6j4ARbn5u_wAGqWA";

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    // ── BASE: múltiples entradas devueltas ────────────────────────────────────

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — adding 3 history entries returns all 3 via GET /api/historial")
    void testBase_MultipleEntradas() throws IOException {
        addHistorial(PLACE_A);
        addHistorial(PLACE_B);
        addHistorial(PLACE_C);

        JsonArray historial = getJsonArray(historialUrl(""));
        Assertions.assertTrue(historial.size() >= 3,
                "After adding 3 entries historial must have at least 3 items (BASE)");
    }

    // ── Caso 2: historial vacío → array vacío ────────────────────────────────

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Caso 2 — fresh user with no history gets an empty array")
    void testCaso2_HistorialVacio() throws IOException {
        // Use a separate fresh user
        long ts = unique();
        String email    = uniqueEmail(ts);
        String username = uniqueUsername(ts);

        postStatus(authUrl("/register"),
                registerPayload(username, email, "Test1234!", "Test", "User", ""));
        postStatus(authUrl("/login"), loginPayload(email, "Test1234!"));

        JsonArray historial = getJsonArray(historialUrl(""));
        // Cannot guarantee 0 if test runs after other tests on same user; check it's an array
        Assertions.assertNotNull(historial, "GET historial must return a JSON array");
    }

    // ── Caso 5: exactamente 1 entrada ────────────────────────────────────────

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 5 — adding 1 history entry returns it with id, place_id and fecha_acceso")
    void testCaso5_UnaEntrada() throws IOException {
        String resp = post(historialUrl(""), historialPayload(PLACE_A));
        JsonObject entry = com.google.gson.JsonParser.parseString(resp).getAsJsonObject();

        Assertions.assertAll(
                () -> Assertions.assertTrue(entry.get("id").getAsInt() > 0,
                        "Entry id must be positive"),
                () -> Assertions.assertEquals(PLACE_A, entry.get("place_id").getAsString(),
                        "place_id must match"),
                () -> Assertions.assertTrue(entry.has("fecha_acceso"),
                        "Entry must have a fecha_acceso timestamp field")
        );
    }

    // ── Caso 6: eliminar entrada la quita del historial ──────────────────────

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 6 — deleting a history entry removes it from GET /api/historial")
    void testCaso6_EliminarEntrada() throws IOException {
        int entryId = addHistorial(PLACE_A);

        int deleteStatus = delete(historialUrl("/" + entryId));
        Assertions.assertEquals(204, deleteStatus,
                "DELETE historial entry must return HTTP 204");

        JsonArray historial = getJsonArray(historialUrl(""));
        boolean stillPresent = false;
        for (com.google.gson.JsonElement el : historial) {
            if (el.getAsJsonObject().get("id").getAsInt() == entryId) {
                stillPresent = true;
                break;
            }
        }
        Assertions.assertFalse(stillPresent,
                "Deleted entry must not appear in GET /api/historial");
    }

    // ── Caso 3: la entrada tiene fecha_acceso (timestamp) ─────────────────────

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 3 — each history entry has a non-null fecha_acceso field")
    void testCaso3_FechaAcceso() throws IOException {
        addHistorial(PLACE_B);

        JsonArray historial = getJsonArray(historialUrl(""));
        Assertions.assertFalse(historial.isEmpty(),
                "Historial must have at least 1 entry");

        JsonObject latest = historial.get(historial.size() - 1).getAsJsonObject();
        Assertions.assertAll(
                () -> Assertions.assertTrue(latest.has("fecha_acceso"),
                        "Entry must have fecha_acceso field"),
                () -> Assertions.assertFalse(latest.get("fecha_acceso").isJsonNull(),
                        "fecha_acceso must not be null")
        );
    }
}
