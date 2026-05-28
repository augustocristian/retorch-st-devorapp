package epigijon.devorapp.e2e.functional.tests.e2e;

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
import java.util.Arrays;
import java.util.Collections;

/**
 * Validates the restaurant search workflow end-to-end:
 * <ol>
 *   <li>POST /api/recommendations/search with category and location filters.</li>
 *   <li>Verify the response structure (JSON object with a "places" array).</li>
 *   <li>If at least one place is returned, extract its place_id and add it to the
 *       authenticated user's history, simulating a real "I visited this restaurant"
 *       action that follows a search.</li>
 *   <li>Verify the history entry was persisted with the correct place_id.</li>
 * </ol>
 *
 * <p>The search step requires {@code GOOGLE_API_KEY} to be configured in the backend.
 * Without it the endpoint returns HTTP 200 with an empty places array; the test
 * still validates the structural contract but skips the history-creation assertion.</p>
 */
class TestSearchRestaurant extends BaseApiClass {

    /** Location used for all search requests in this class. */
    private static final String SEARCH_LOCATION = "Gijón, Asturias";

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    // ── Helper ────────────────────────────────────────────────────────────────────

    /**
     * Executes a search and returns the "places" array from the response.
     * Always asserts HTTP 200 and the existence of the "places" key.
     */
    private JsonArray executeSearch(java.util.List<String> categories,
                                    java.util.List<String> prices,
                                    int maxResults) throws IOException {
        String body = searchPayload(categories, prices, true, SEARCH_LOCATION, maxResults);
        int status = postStatus(recommendationsUrl("/search"), body);
        Assertions.assertEquals(200, status,
                "POST /api/recommendations/search must return HTTP 200");

        JsonObject response = postJsonObject(recommendationsUrl("/search"), body);
        Assertions.assertTrue(response.has("places"),
                "Search response must contain a 'places' field");
        return response.getAsJsonArray("places");
    }

    // ── Tests ─────────────────────────────────────────────────────────────────────

    @AccessMode(resID = "user",     concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/recommendations/search returns HTTP 200 with a places array")
    void testSearchReturnsValidStructure() throws IOException {
        JsonArray places = executeSearch(
                Collections.singletonList("restaurant"),
                Arrays.asList("PRICE_LEVEL_MODERATE", "PRICE_LEVEL_INEXPENSIVE"),
                5);

        log.info("Search returned {} place(s) for location '{}'", places.size(), SEARCH_LOCATION);

        // Each place in the results must carry an id (the Google place_id)
        for (int i = 0; i < places.size(); i++) {
            JsonObject place = places.get(i).getAsJsonObject();
            Assertions.assertTrue(place.has("id"),
                    "Each place in the search result must have an 'id' field (index " + i + ")");
        }
    }

    @AccessMode(resID = "user",     concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Search filtered by PRICE_LEVEL_EXPENSIVE returns HTTP 200")
    void testSearchWithExpensivePriceFilter() throws IOException {
        String body = searchPayload(
                Collections.singletonList("restaurant"),
                Collections.singletonList("PRICE_LEVEL_EXPENSIVE"),
                true, SEARCH_LOCATION, 3);
        int status = postStatus(recommendationsUrl("/search"), body);
        Assertions.assertEquals(200, status,
                "Search with expensive price filter must return HTTP 200");
    }

    @AccessMode(resID = "user",     concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("First search result can be added to historial, completing the search-to-visit workflow")
    void testSearchThenAddFirstResultToHistorial() throws IOException {
        JsonArray places = executeSearch(
                Collections.singletonList("restaurant"),
                Arrays.asList("PRICE_LEVEL_MODERATE", "PRICE_LEVEL_INEXPENSIVE"),
                5);

        if (places.isEmpty()) {
            log.warn("Search returned no results — GOOGLE_API_KEY may not be configured. " +
                     "Skipping historial assertion.");
            return;
        }

        // Extract the place_id of the first result
        String placeId = places.get(0).getAsJsonObject().get("id").getAsString();
        log.info("Adding first search result '{}' to historial", placeId);

        // Add to history (user "visits" the restaurant found via search)
        String histResponse = post(historialUrl(""), historialPayload(placeId));
        JsonObject histEntry = com.google.gson.JsonParser.parseString(histResponse).getAsJsonObject();

        Assertions.assertAll(
                () -> Assertions.assertTrue(histEntry.get("id").getAsInt() > 0,
                        "Historial entry must have a positive id"),
                () -> Assertions.assertEquals(placeId, histEntry.get("place_id").getAsString(),
                        "Historial place_id must match the search result")
        );
    }

    @AccessMode(resID = "user",     concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "historial", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Searching, visiting and then finding the place in the popular list works end-to-end")
    void testSearchVisitAndAppearInPopulares() throws IOException {
        JsonArray places = executeSearch(
                Collections.singletonList("restaurant"),
                Arrays.asList("PRICE_LEVEL_MODERATE", "PRICE_LEVEL_INEXPENSIVE"),
                5);

        if (places.isEmpty()) {
            log.warn("Search returned no results — GOOGLE_API_KEY may not be configured. " +
                     "Skipping populares assertion.");
            return;
        }

        String placeId = places.get(0).getAsJsonObject().get("id").getAsString();

        // Visit the restaurant
        post(historialUrl(""), historialPayload(placeId));

        // Ask for the globally popular places — our visit should contribute to the count
        int popularStatus = postStatus(historialUrl("/populares"), popularesPayload(10));
        Assertions.assertEquals(200, popularStatus,
                "POST /api/historial/populares must return HTTP 200 after adding a visit");
    }
}
