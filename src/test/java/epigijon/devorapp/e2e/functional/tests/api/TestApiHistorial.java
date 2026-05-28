package epigijon.devorapp.e2e.functional.tests.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import epigijon.devorapp.e2e.functional.common.BaseApiClass;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Validates the restaurant history endpoints:
 * <ul>
 *   <li>POST   /api/historial            — add restaurant to history (HTTP 201)</li>
 *   <li>GET    /api/historial            — get user's history (HTTP 200)</li>
 *   <li>DELETE /api/historial/{id}       — remove history entry (HTTP 204)</li>
 *   <li>POST   /api/historial/populares  — get globally popular restaurants</li>
 * </ul>
 */
class TestApiHistorial extends BaseApiClass {

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/historial returns HTTP 201 with id and place_id")
    void testAddToHistorial() throws IOException {
        String placeId = "test_place_" + unique();

        int status = postStatus(historialUrl(""), historialPayload(placeId));
        Assertions.assertEquals(201, status, "Adding to historial must return HTTP 201");

        String response = post(historialUrl(""), historialPayload(placeId));
        JsonObject entry = JsonParser.parseString(response).getAsJsonObject();
        Assertions.assertAll(
                () -> Assertions.assertTrue(entry.get("id").getAsInt() > 0,
                        "historial entry id must be positive"),
                () -> Assertions.assertEquals(placeId, entry.get("place_id").getAsString(),
                        "place_id must match")
        );
    }

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/historial returns HTTP 200 with a JSON array")
    void testGetHistorial() throws IOException {
        post(historialUrl(""), historialPayload("test_place_" + unique()));

        int status = getStatus(historialUrl(""));
        Assertions.assertEquals(200, status, "GET historial must return HTTP 200");

        JsonArray entries = getJsonArray(historialUrl(""));
        Assertions.assertNotNull(entries, "Historial response must be a JSON array");
    }

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("DELETE /api/historial/{id} returns HTTP 204")
    void testDeleteFromHistorial() throws IOException {
        int entryId = addHistorial("test_place_" + unique());

        int status = delete(historialUrl("/" + entryId));
        Assertions.assertEquals(204, status, "DELETE historial entry must return HTTP 204");
    }

    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/historial/populares returns HTTP 200 with a JSON array")
    void testGetPopulares() throws IOException {
        int status = postStatus(historialUrl("/populares"), popularesPayload(5));
        Assertions.assertEquals(200, status, "POST historial/populares must return HTTP 200");

        JsonArray results = getJsonArray_post(historialUrl("/populares"), popularesPayload(5));
        Assertions.assertNotNull(results, "Populares response must be a JSON array");
    }

    private JsonArray getJsonArray_post(String url, String body) throws IOException {
        return JsonParser.parseString(post(url, body)).getAsJsonArray();
    }
}
