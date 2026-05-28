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
 * Validates the ratings (valoraciones) endpoints:
 * <ul>
 *   <li>POST   /api/valoraciones                         — create/update rating (HTTP 201)</li>
 *   <li>GET    /api/valoraciones                         — list all user ratings</li>
 *   <li>GET    /api/valoraciones/{place_id}              — get user's rating for a place</li>
 *   <li>GET    /api/valoraciones/restaurante/{place_id}  — public reviews for a place</li>
 *   <li>DELETE /api/valoraciones/{place_id}              — delete rating (HTTP 204)</li>
 * </ul>
 * Each test uses a unique fake place_id to avoid cross-test state interference.
 */
class TestApiValoraciones extends BaseApiClass {

    private static final int CALIDAD  = 4;
    private static final int PRECIO   = 3;
    private static final int HIGIENE  = 5;
    private static final int TRATO    = 4;
    private static final String COMMENT = "Great place for testing";

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/valoraciones returns HTTP 201 with matching scores and id")
    void testCreateValoracion() throws IOException {
        String placeId = "test_place_" + unique();

        int status = postStatus(valoracionesUrl(""),
                valoracionPayload(placeId, CALIDAD, PRECIO, HIGIENE, TRATO, COMMENT));
        Assertions.assertEquals(201, status, "Creating a valoracion must return HTTP 201");

        JsonObject val = createValoracion(placeId, CALIDAD, PRECIO, HIGIENE, TRATO, COMMENT);
        Assertions.assertAll(
                () -> Assertions.assertTrue(val.get("id").getAsInt() > 0,
                        "valoracion id must be positive"),
                () -> Assertions.assertEquals(CALIDAD,  val.get("calidad").getAsInt(),  "calidad must match"),
                () -> Assertions.assertEquals(PRECIO,   val.get("precio").getAsInt(),   "precio must match"),
                () -> Assertions.assertEquals(HIGIENE,  val.get("higiene").getAsInt(),  "higiene must match"),
                () -> Assertions.assertEquals(TRATO,    val.get("trato").getAsInt(),    "trato must match"),
                () -> Assertions.assertEquals(COMMENT,  val.get("comentario").getAsString(), "comentario must match")
        );
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/valoraciones returns HTTP 200 with a JSON list")
    void testGetMisValoraciones() throws IOException {
        createValoracion("test_place_" + unique(), CALIDAD, PRECIO, HIGIENE, TRATO, COMMENT);

        int status = getStatus(valoracionesUrl(""));
        Assertions.assertEquals(200, status, "GET all valoraciones must return 200");
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/valoraciones/{place_id} returns the user's rating for that place")
    void testGetMiValoracion() throws IOException {
        String placeId = "test_place_" + unique();
        createValoracion(placeId, CALIDAD, PRECIO, HIGIENE, TRATO, COMMENT);

        int status = getStatus(valoracionesUrl("/" + placeId));
        Assertions.assertEquals(200, status, "GET my valoracion must return 200");

        JsonObject val = getJsonObject(valoracionesUrl("/" + placeId));
        Assertions.assertEquals(CALIDAD, val.get("calidad").getAsInt(),
                "calidad returned must match the created rating");
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/valoraciones/restaurante/{place_id} returns HTTP 200 with a list of reviews")
    void testGetResenas() throws IOException {
        String placeId = "test_place_" + unique();
        createValoracion(placeId, CALIDAD, PRECIO, HIGIENE, TRATO, COMMENT);

        int status = getStatus(valoracionesUrl("/restaurante/" + placeId));
        Assertions.assertEquals(200, status, "GET restaurante resenas must return 200");

        JsonArray resenas = getJsonArray(valoracionesUrl("/restaurante/" + placeId));
        Assertions.assertFalse(resenas.isEmpty(), "Review list must not be empty after creation");
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("DELETE /api/valoraciones/{place_id} returns HTTP 204")
    void testDeleteValoracion() throws IOException {
        String placeId = "test_place_" + unique();
        createValoracion(placeId, CALIDAD, PRECIO, HIGIENE, TRATO, COMMENT);

        int status = delete(valoracionesUrl("/" + placeId));
        Assertions.assertEquals(204, status, "DELETE valoracion must return HTTP 204");
    }
}
