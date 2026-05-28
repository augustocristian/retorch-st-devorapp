package epigijon.devorapp.e2e.functional.tests.api;

import com.google.gson.JsonObject;
import epigijon.devorapp.e2e.functional.common.BaseApiClass;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Validates the favorites restaurant endpoints:
 * <ul>
 *   <li>POST   /api/favoritos/listas/{id}    — add restaurant to list (HTTP 201)</li>
 *   <li>DELETE /api/favoritos/{favorito_id}  — remove restaurant from list (HTTP 204)</li>
 *   <li>GET    /api/favoritos/listas/{id}    — get list detail with restaurants</li>
 * </ul>
 */
class TestApiFavoritos extends BaseApiClass {

    private int defaultListaId;

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @BeforeEach
    void resolveListaId() throws IOException {
        defaultListaId = getDefaultListaId();
        if (defaultListaId < 0) {
            defaultListaId = createLista("Favoritos");
        }
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/favoritos/listas/{id} returns HTTP 201 with id, place_id and lista_id")
    void testAddFavorito() throws IOException {
        int status = postStatus(favoritosUrl("/listas/" + defaultListaId),
                favoritoPayload(TEST_PLACE_ID));
        Assertions.assertEquals(201, status, "Adding a favorito must return HTTP 201");

        String response = post(favoritosUrl("/listas/" + defaultListaId),
                favoritoPayload(TEST_PLACE_ID));
        JsonObject fav = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
        Assertions.assertAll(
                () -> Assertions.assertTrue(fav.get("id").getAsInt() > 0,
                        "favorito id must be positive"),
                () -> Assertions.assertEquals(TEST_PLACE_ID, fav.get("place_id").getAsString(),
                        "place_id must match"),
                () -> Assertions.assertEquals(defaultListaId, fav.get("lista_id").getAsInt(),
                        "lista_id must match")
        );
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("DELETE /api/favoritos/{favorito_id} returns HTTP 204")
    void testDeleteFavorito() throws IOException {
        int favId = addFavorito(defaultListaId, TEST_PLACE_ID);

        int status = delete(favoritosUrl("/" + favId));
        Assertions.assertEquals(204, status, "DELETE favorito must return HTTP 204");
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/favoritos/listas/{id} returns HTTP 200 with lista and restaurantes fields")
    void testGetListaDetalle() throws IOException {
        int status = getStatus(favoritosUrl("/listas/" + defaultListaId));
        Assertions.assertEquals(200, status, "GET lista detail must return HTTP 200");

        JsonObject body = getJsonObject(favoritosUrl("/listas/" + defaultListaId));
        Assertions.assertAll(
                () -> Assertions.assertTrue(body.has("lista"),
                        "Response must have 'lista' field"),
                () -> Assertions.assertTrue(body.has("restaurantes"),
                        "Response must have 'restaurantes' field")
        );
    }
}
