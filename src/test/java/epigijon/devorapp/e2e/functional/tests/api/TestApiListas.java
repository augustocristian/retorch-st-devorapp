package epigijon.devorapp.e2e.functional.tests.api;

import com.google.gson.JsonArray;
import epigijon.devorapp.e2e.functional.common.BaseApiClass;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Validates the favorites-list CRUD endpoints:
 * <ul>
 *   <li>GET    /api/favoritos/listas            — list all user's lists</li>
 *   <li>POST   /api/favoritos/listas            — create a new list (HTTP 201)</li>
 *   <li>PATCH  /api/favoritos/listas/{id}       — update list name</li>
 *   <li>DELETE /api/favoritos/listas/{id}       — delete a list (HTTP 204)</li>
 * </ul>
 */
class TestApiListas extends BaseApiClass {

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/favoritos/listas returns HTTP 200 with the default Favoritos list")
    void testGetListas() throws IOException {
        int status = getStatus(favoritosUrl("/listas"));
        Assertions.assertEquals(200, status, "GET listas must return 200");

        JsonArray listas = getJsonArray(favoritosUrl("/listas"));
        Assertions.assertTrue(containsByField(listas, "nombre", "Favoritos"),
                "Default 'Favoritos' list must exist after registration");
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/favoritos/listas returns HTTP 201 with the new list's data")
    void testCreateLista() throws IOException {
        String nombre = "MiLista" + unique();

        int status = postStatus(favoritosUrl("/listas"), listaPayload(nombre));
        Assertions.assertEquals(201, status, "Creating a lista must return HTTP 201");

        int listaId = createLista("Verify" + unique());
        Assertions.assertTrue(listaId > 0, "Created lista must have a positive id");
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("PATCH /api/favoritos/listas/{id} returns HTTP 200 with the updated nombre")
    void testUpdateLista() throws IOException {
        int listaId = createLista("Original" + unique());
        String newNombre = "Renamed" + unique();

        int status = patch(favoritosUrl("/listas/" + listaId), listaPayload(newNombre));
        Assertions.assertEquals(200, status, "PATCH lista must return 200");

        JsonArray listas = getJsonArray(favoritosUrl("/listas"));
        Assertions.assertTrue(containsByField(listas, "nombre", newNombre),
                "Renamed lista must appear in the list");
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("DELETE /api/favoritos/listas/{id} returns HTTP 204 and removes the list")
    void testDeleteLista() throws IOException {
        String nombre = "ToDelete" + unique();
        int listaId = createLista(nombre);

        int status = delete(favoritosUrl("/listas/" + listaId));
        Assertions.assertEquals(204, status, "DELETE lista must return HTTP 204");

        JsonArray listas = getJsonArray(favoritosUrl("/listas"));
        Assertions.assertFalse(containsByField(listas, "nombre", nombre),
                "Deleted lista must no longer appear in the list");
    }
}
