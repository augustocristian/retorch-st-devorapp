package epigijon.devorapp.e2e.functional.tests.e2e;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
 * Validates the end-to-end workflow of adding a restaurant to a personal favorites list:
 * <ol>
 *   <li>Create a new favorites list.</li>
 *   <li>Verify the list starts empty.</li>
 *   <li>Add a restaurant and assert the returned favorito has the correct
 *       {@code id}, {@code lista_id} and {@code place_id}.</li>
 *   <li>Retrieve the full list detail and confirm the restaurant appears in
 *       the {@code restaurantes} array.</li>
 *   <li>Add a second restaurant to the same list and verify both are present.</li>
 *   <li>Remove one restaurant and verify only one remains.</li>
 *   <li>Delete the entire list (with its remaining restaurant) and confirm
 *       it no longer appears in the user's lists.</li>
 * </ol>
 */
class TestAddRestaurantToFavorites extends BaseApiClass {

    private static final String PLACE_A = TEST_PLACE_ID;
    private static final String PLACE_B = "ChIJdd4hrwug2EcRmSrV3Vo6llI";

    private int listaId;

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @BeforeEach
    void createFreshList() throws IOException {
        listaId = createLista("MisFavoritos" + unique());
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /** Returns the restaurantes array from GET /api/favoritos/listas/{id}. */
    private JsonArray getRestaurantes(int id) throws IOException {
        JsonObject detail = getJsonObject(favoritosUrl("/listas/" + id));
        return detail.getAsJsonArray("restaurantes");
    }

    /** Returns true if the restaurantes array contains an entry for the given placeId. */
    private boolean restaurantesContains(JsonArray restaurantes, String placeId) {
        return containsByField(restaurantes, "place_id", placeId);
    }

    // ── Tests ─────────────────────────────────────────────────────────────────────

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("A newly created favorites list is empty")
    void testNewListIsEmpty() throws IOException {
        JsonArray restaurantes = getRestaurantes(listaId);
        Assertions.assertTrue(restaurantes.isEmpty(),
                "A brand-new favorites list must contain no restaurants");
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Adding a restaurant returns the favorito with correct id, lista_id and place_id")
    void testAddRestaurantReturnsFavoritoFields() throws IOException {
        String response = post(favoritosUrl("/listas/" + listaId), favoritoPayload(PLACE_A));
        JsonObject fav = JsonParser.parseString(response).getAsJsonObject();

        Assertions.assertAll(
                () -> Assertions.assertTrue(fav.get("id").getAsInt() > 0,
                        "favorito id must be a positive integer"),
                () -> Assertions.assertEquals(listaId, fav.get("lista_id").getAsInt(),
                        "lista_id must match the target list"),
                () -> Assertions.assertEquals(PLACE_A, fav.get("place_id").getAsString(),
                        "place_id must match the restaurant that was added")
        );
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("After adding a restaurant it appears in the list detail view")
    void testAddedRestaurantAppearsInListDetail() throws IOException {
        post(favoritosUrl("/listas/" + listaId), favoritoPayload(PLACE_A));

        JsonArray restaurantes = getRestaurantes(listaId);
        Assertions.assertFalse(restaurantes.isEmpty(),
                "List detail must contain at least one restaurant after adding");
        Assertions.assertTrue(restaurantesContains(restaurantes, PLACE_A),
                "The added place_id must appear in the list's restaurantes array");
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Adding two different restaurants to the same list shows both in detail view")
    void testAddTwoRestaurantsAndBothAppear() throws IOException {
        post(favoritosUrl("/listas/" + listaId), favoritoPayload(PLACE_A));
        post(favoritosUrl("/listas/" + listaId), favoritoPayload(PLACE_B));

        JsonArray restaurantes = getRestaurantes(listaId);
        Assertions.assertAll(
                () -> Assertions.assertTrue(restaurantesContains(restaurantes, PLACE_A),
                        "First restaurant must appear in the list"),
                () -> Assertions.assertTrue(restaurantesContains(restaurantes, PLACE_B),
                        "Second restaurant must appear in the list")
        );
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Removing a restaurant from the list leaves it absent from the detail view")
    void testRemoveRestaurantDisappearsFromList() throws IOException {
        // Add two restaurants
        int favIdA = addFavorito(listaId, PLACE_A);
        post(favoritosUrl("/listas/" + listaId), favoritoPayload(PLACE_B));

        // Remove only PLACE_A
        int deleteStatus = delete(favoritosUrl("/" + favIdA));
        Assertions.assertEquals(204, deleteStatus,
                "DELETE favorito must return HTTP 204");

        // Verify PLACE_A is gone but PLACE_B remains
        JsonArray restaurantes = getRestaurantes(listaId);
        Assertions.assertAll(
                () -> Assertions.assertFalse(restaurantesContains(restaurantes, PLACE_A),
                        "Removed restaurant must no longer appear in the list"),
                () -> Assertions.assertTrue(restaurantesContains(restaurantes, PLACE_B),
                        "The other restaurant must still be present")
        );
    }

    @AccessMode(resID = "favoritos", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",      concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Deleting the list removes it from the user's list collection")
    void testDeleteListRemovesItFromUserLists() throws IOException {
        // Put something in the list first
        post(favoritosUrl("/listas/" + listaId), favoritoPayload(PLACE_A));

        // Delete the whole list (cascade removes its restaurants)
        int deleteStatus = delete(favoritosUrl("/listas/" + listaId));
        Assertions.assertEquals(204, deleteStatus,
                "DELETE lista must return HTTP 204");

        // The list must no longer appear in the user's collections
        JsonArray listas = getJsonArray(favoritosUrl("/listas"));
        boolean stillPresent = false;
        for (com.google.gson.JsonElement el : listas) {
            if (el.getAsJsonObject().get("id").getAsInt() == listaId) {
                stillPresent = true;
                break;
            }
        }
        Assertions.assertFalse(stillPresent,
                "Deleted list must not appear in the user's favorites lists");
    }
}
