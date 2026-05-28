package epigijon.devorapp.e2e.functional.tests.e2e;

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
 * Validates the complete restaurant evaluation (valoracion) workflow:
 * <ol>
 *   <li><b>Create</b> — POST /api/valoraciones with all four dimension scores and a comment.
 *       Assert that the returned object echoes back all submitted values and has a
 *       positive {@code id}.</li>
 *   <li><b>Read own</b> — GET /api/valoraciones/{place_id} returns exactly the same
 *       scores that were submitted.</li>
 *   <li><b>Public reviews</b> — GET /api/valoraciones/restaurante/{place_id} exposes
 *       the review to other users with the correct {@code username} and
 *       {@code ha_dado_me_gusta = false} initially.</li>
 *   <li><b>Like</b> — POST /api/valoraciones/{id}/like toggles the like on. The
 *       returned review shows {@code me_gustas = 1} and {@code ha_dado_me_gusta = true}.</li>
 *   <li><b>Unlike</b> — A second POST to the same like endpoint toggles it off again,
 *       restoring {@code me_gustas = 0} and {@code ha_dado_me_gusta = false}.</li>
 *   <li><b>Update</b> — A second POST to /api/valoraciones with the same place_id but
 *       different scores replaces the original rating. The GET reflects the new values.</li>
 *   <li><b>Delete</b> — DELETE /api/valoraciones/{place_id} returns HTTP 204 and the
 *       place_id no longer appears in either the personal or public review endpoints.</li>
 * </ol>
 *
 * <p>Each test method uses a unique fake {@code place_id} (prefixed {@code eval_}) so
 * tests are independent and can run in any order without state interference.</p>
 */
class TestEvaluateRestaurant extends BaseApiClass {

    // Initial rating values
    private static final int CALIDAD_INICIAL  = 4;
    private static final int PRECIO_INICIAL   = 3;
    private static final int HIGIENE_INICIAL  = 5;
    private static final int TRATO_INICIAL    = 4;
    private static final String COMMENT_INICIAL = "Excelente ambiente y muy buena atención al cliente.";

    // Updated rating values
    private static final int CALIDAD_UPDATED  = 2;
    private static final int PRECIO_UPDATED   = 4;
    private static final int HIGIENE_UPDATED  = 3;
    private static final int TRATO_UPDATED    = 2;
    private static final String COMMENT_UPDATED = "La segunda visita no estuvo a la altura de la primera.";

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    /**
     * Creates a valoracion and returns the full response body as a JsonObject.
     * The returned object contains {@code id}, {@code place_id}, all four dimension
     * scores, {@code comentario}, {@code me_gustas}, and {@code fecha}.
     */
    private JsonObject rate(String placeId, int calidad, int precio,
                            int higiene, int trato, String comentario) throws IOException {
        String body = valoracionPayload(placeId, calidad, precio, higiene, trato, comentario);
        String response = post(valoracionesUrl(""), body);
        return JsonParser.parseString(response).getAsJsonObject();
    }

    /**
     * Returns the first review in the public review list for {@code placeId} that
     * belongs to the current test user (matched by username), or null if not found.
     */
    private JsonObject findMyPublicReview(String placeId) throws IOException {
        JsonArray reviews = getJsonArray(valoracionesUrl("/restaurante/" + placeId));
        for (com.google.gson.JsonElement el : reviews) {
            JsonObject review = el.getAsJsonObject();
            if (testUsername.equals(review.get("username").getAsString())) {
                return review;
            }
        }
        return null;
    }

    // ── Tests ─────────────────────────────────────────────────────────────────────

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/valoraciones creates a rating with all four dimension scores and returns them in the response")
    void testCreateRatingReturnsAllFields() throws IOException {
        String placeId = "eval_" + unique();
        JsonObject val = rate(placeId, CALIDAD_INICIAL, PRECIO_INICIAL, HIGIENE_INICIAL,
                TRATO_INICIAL, COMMENT_INICIAL);

        Assertions.assertAll(
                () -> Assertions.assertTrue(val.get("id").getAsInt() > 0,
                        "Created valoracion must have a positive id"),
                () -> Assertions.assertEquals(placeId,        val.get("place_id").getAsString(),   "place_id must match"),
                () -> Assertions.assertEquals(CALIDAD_INICIAL, val.get("calidad").getAsInt(),       "calidad must match"),
                () -> Assertions.assertEquals(PRECIO_INICIAL,  val.get("precio").getAsInt(),        "precio must match"),
                () -> Assertions.assertEquals(HIGIENE_INICIAL, val.get("higiene").getAsInt(),       "higiene must match"),
                () -> Assertions.assertEquals(TRATO_INICIAL,   val.get("trato").getAsInt(),         "trato must match"),
                () -> Assertions.assertEquals(COMMENT_INICIAL, val.get("comentario").getAsString(), "comentario must match")
        );
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/valoraciones/{place_id} returns exactly the scores that were submitted")
    void testGetOwnRatingMatchesSubmittedScores() throws IOException {
        String placeId = "eval_" + unique();
        rate(placeId, CALIDAD_INICIAL, PRECIO_INICIAL, HIGIENE_INICIAL,
                TRATO_INICIAL, COMMENT_INICIAL);

        JsonObject myVal = getJsonObject(valoracionesUrl("/" + placeId));

        Assertions.assertAll(
                () -> Assertions.assertEquals(CALIDAD_INICIAL, myVal.get("calidad").getAsInt(),  "calidad must match"),
                () -> Assertions.assertEquals(PRECIO_INICIAL,  myVal.get("precio").getAsInt(),   "precio must match"),
                () -> Assertions.assertEquals(HIGIENE_INICIAL, myVal.get("higiene").getAsInt(),  "higiene must match"),
                () -> Assertions.assertEquals(TRATO_INICIAL,   myVal.get("trato").getAsInt(),    "trato must match"),
                () -> Assertions.assertEquals(COMMENT_INICIAL, myVal.get("comentario").getAsString(), "comentario must match")
        );
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/valoraciones/restaurante/{place_id} exposes the review publicly with the user's username")
    void testPublicReviewsShowUsernameAndInitialLikes() throws IOException {
        String placeId = "eval_" + unique();
        rate(placeId, CALIDAD_INICIAL, PRECIO_INICIAL, HIGIENE_INICIAL,
                TRATO_INICIAL, COMMENT_INICIAL);

        JsonObject myPublicReview = findMyPublicReview(placeId);

        Assertions.assertNotNull(myPublicReview,
                "The user's review must appear in the public review list");
        Assertions.assertAll(
                () -> Assertions.assertEquals(testUsername, myPublicReview.get("username").getAsString(),
                        "Public review must show the correct username"),
                () -> Assertions.assertEquals(CALIDAD_INICIAL, myPublicReview.get("calidad").getAsInt(),
                        "Public review calidad must match"),
                () -> Assertions.assertEquals(0, myPublicReview.get("me_gustas").getAsInt(),
                        "A new review must have zero likes"),
                () -> Assertions.assertFalse(myPublicReview.get("ha_dado_me_gusta").getAsBoolean(),
                        "ha_dado_me_gusta must be false before the user likes the review")
        );
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("POST /api/valoraciones/{id}/like toggles the like on and off correctly")
    void testLikeAndUnlikeReview() throws IOException {
        String placeId = "eval_" + unique();
        JsonObject created = rate(placeId, CALIDAD_INICIAL, PRECIO_INICIAL, HIGIENE_INICIAL,
                TRATO_INICIAL, COMMENT_INICIAL);
        int valoracionId = created.get("id").getAsInt();

        // First like → me_gustas becomes 1, ha_dado_me_gusta becomes true
        String likeResponse = post(valoracionesUrl("/" + valoracionId + "/like"), "");
        JsonObject afterLike = JsonParser.parseString(likeResponse).getAsJsonObject();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, afterLike.get("me_gustas").getAsInt(),
                        "me_gustas must be 1 after first like"),
                () -> Assertions.assertTrue(afterLike.get("ha_dado_me_gusta").getAsBoolean(),
                        "ha_dado_me_gusta must be true after liking")
        );

        // Second like on the same review → toggles it off
        String unlikeResponse = post(valoracionesUrl("/" + valoracionId + "/like"), "");
        JsonObject afterUnlike = JsonParser.parseString(unlikeResponse).getAsJsonObject();
        Assertions.assertAll(
                () -> Assertions.assertEquals(0, afterUnlike.get("me_gustas").getAsInt(),
                        "me_gustas must return to 0 after unliking"),
                () -> Assertions.assertFalse(afterUnlike.get("ha_dado_me_gusta").getAsBoolean(),
                        "ha_dado_me_gusta must be false after unliking")
        );
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Re-posting a rating for the same place_id updates all dimension scores")
    void testUpdateRatingReplacesScores() throws IOException {
        String placeId = "eval_" + unique();

        // Create initial rating
        rate(placeId, CALIDAD_INICIAL, PRECIO_INICIAL, HIGIENE_INICIAL,
                TRATO_INICIAL, COMMENT_INICIAL);

        // Update: POST again to the same place_id with different values
        int updateStatus = postStatus(valoracionesUrl(""),
                valoracionPayload(placeId, CALIDAD_UPDATED, PRECIO_UPDATED,
                        HIGIENE_UPDATED, TRATO_UPDATED, COMMENT_UPDATED));
        Assertions.assertEquals(201, updateStatus,
                "Updating a valoracion via re-POST must return HTTP 201");

        // Read back and verify the new scores
        JsonObject updated = getJsonObject(valoracionesUrl("/" + placeId));
        Assertions.assertAll(
                () -> Assertions.assertEquals(CALIDAD_UPDATED,  updated.get("calidad").getAsInt(),        "calidad must reflect the update"),
                () -> Assertions.assertEquals(PRECIO_UPDATED,   updated.get("precio").getAsInt(),         "precio must reflect the update"),
                () -> Assertions.assertEquals(HIGIENE_UPDATED,  updated.get("higiene").getAsInt(),        "higiene must reflect the update"),
                () -> Assertions.assertEquals(TRATO_UPDATED,    updated.get("trato").getAsInt(),          "trato must reflect the update"),
                () -> Assertions.assertEquals(COMMENT_UPDATED,  updated.get("comentario").getAsString(),  "comentario must reflect the update")
        );
    }

    @AccessMode(resID = "valoraciones", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",         concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("DELETE /api/valoraciones/{place_id} removes the review from personal and public views")
    void testDeleteRatingDisappearsEverywhere() throws IOException {
        String placeId = "eval_" + unique();
        rate(placeId, CALIDAD_INICIAL, PRECIO_INICIAL, HIGIENE_INICIAL,
                TRATO_INICIAL, COMMENT_INICIAL);

        // Confirm it exists before deletion
        Assertions.assertNotNull(findMyPublicReview(placeId),
                "Review must be visible in public list before deletion");

        // Delete
        int deleteStatus = delete(valoracionesUrl("/" + placeId));
        Assertions.assertEquals(204, deleteStatus,
                "DELETE valoracion must return HTTP 204");

        // Personal view: endpoint returns empty object {}
        JsonObject myVal = getJsonObject(valoracionesUrl("/" + placeId));
        Assertions.assertTrue(myVal.entrySet().isEmpty(),
                "Personal valoracion endpoint must return an empty object after deletion");

        // Public view: no review by this user any more
        Assertions.assertNull(findMyPublicReview(placeId),
                "Review must not appear in the public review list after deletion");
    }
}
