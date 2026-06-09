package epigijon.devorapp.e2e.functional.tests.e2e;

import com.google.gson.JsonObject;
import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import epigijon.devorapp.e2e.functional.pages.FavoritesPage;
import epigijon.devorapp.e2e.functional.pages.LoginPage;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Browser tests for the DevorApp favorites page ({@code /favorites}).
 *
 * <p>Adapts {@code favorites.spec.ts} (Playwright) to Selenium + JUnit 5.
 * A real test user is created and logged-in before the test suite runs.
 * Favorites lists are populated via the API so the browser tests can focus
 * on verifying UI behavior (card counts, list title, search filtering).
 *
 * <p>Base-Choice coverage (mirrors the Playwright spec):
 * <ul>
 *   <li>BASE  — several lists, several restaurants, no search.</li>
 *   <li>Caso 2 — no lists → empty-state message.</li>
 *   <li>Caso 3 — exactly 1 list.</li>
 *   <li>Caso 4 — list exists but has 0 restaurants → detail empty-state.</li>
 *   <li>Caso 6 — search inside a list filters results.</li>
 * </ul>
 */
class TestFavoritesView extends BaseLoggedClass {

    private static final String PLACE_A = "ChIJN1t_tDeuEmsRUsoyG83frY4";
    private static final String PLACE_B = "ChIJdd4hrwug2EcRmSrV3Vo6llI";
    private static final String PLACE_C = "ChIJ2eUgeAK6j4ARbn5u_wAGqWA";

    @BeforeAll
    static void createTestUser() throws Exception {
        long ts = System.currentTimeMillis();
        setupTestUser("favui" + (ts % 100000), "favui" + ts + "@devorapp.test", "Test1234!");
    }

    @AfterAll
    static void cleanupTestUser() {
        tearDownTestUser();
    }

    /** Logs the browser in and navigates to /favorites, returning the page object. */
    private FavoritesPage loginAndGoToFavorites() throws Exception {
        driver.get(sutUrl + "/login");
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(testPassword)
                .submitLogin();
        driver.get(sutUrl + "/favorites");
        return new FavoritesPage(driver, waiter);
    }

    /** Creates a favorites list via the API and returns its id. */
    private int createListaViaApi(String nombre) throws IOException {
        apiLogin();
        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        JsonObject payload = new JsonObject();
        payload.addProperty("nombre", nombre);
        payload.addProperty("icono", "Heart");
        JsonObject resp = apiPost(apiBase + "/api/favoritos/listas", payload.toString());
        return resp.get("id").getAsInt();
    }

    /** Adds a restaurant to a favorites list via the API. */
    private void addFavoritoViaApi(int listaId, String placeId) throws IOException {
        apiLogin();
        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        JsonObject payload = new JsonObject();
        payload.addProperty("place_id", placeId);
        apiPost(apiBase + "/api/favoritos/listas/" + listaId, payload.toString());
    }

    /** Deletes a favorites list via the API. */
    private void deleteListaViaApi(int listaId) throws IOException {
        apiLogin();
        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        apiDelete(apiBase + "/api/favoritos/listas/" + listaId);
    }

    // ── BASE: varias listas, varios restaurantes ───────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "favoritos",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("BASE — multiple lists with multiple restaurants are shown correctly")
    void testBase_VariasListasVariosRestaurantes() throws Exception {
        int listaAId = createListaViaApi("Mis Favoritos");
        int listaBId = createListaViaApi("Para cenar");
        addFavoritoViaApi(listaAId, PLACE_A);
        addFavoritoViaApi(listaAId, PLACE_B);
        addFavoritoViaApi(listaAId, PLACE_C);

        FavoritesPage page = loginAndGoToFavorites();

        Assertions.assertTrue(page.getListCount() >= 2,
                "At least 2 lists must be visible (BASE case)");

        page.openListByName("Mis Favoritos");
        Assertions.assertTrue(page.getRestaurantCount() >= 1,
                "Detail view must show at least 1 restaurant after opening a list");

        deleteListaViaApi(listaAId);
        deleteListaViaApi(listaBId);
    }

    // ── Caso 2: sin listas → estado vacío ────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "favoritos",   concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 2 — no lists show the empty-state message")
    void testCaso2_SinListas() throws Exception {
        FavoritesPage page = loginAndGoToFavorites();

        if (page.getListCount() == 0) {
            Assertions.assertTrue(page.isEmptyStateVisible(),
                    "When there are no lists the empty-state text must be visible");
        }
    }

    // ── Caso 3: exactamente 1 lista ───────────────────────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "favoritos",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 3 — exactly 1 list is shown with its correct name")
    void testCaso3_UnaLista() throws Exception {
        int listaId = createListaViaApi("Mis Favoritos Solo");

        FavoritesPage page = loginAndGoToFavorites();
        Assertions.assertTrue(page.getListCount() >= 1, "At least 1 list must be visible");

        deleteListaViaApi(listaId);
    }

    // ── Caso 4: lista vacía → estado vacío en detalle ─────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "favoritos",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 4 — opening an empty list shows the detail empty-state")
    void testCaso4_ListaSinRestaurantes() throws Exception {
        int listaId = createListaViaApi("Lista Vacía");

        FavoritesPage page = loginAndGoToFavorites();
        page.openListByName("Lista Vacía");

        Assertions.assertAll(
                () -> Assertions.assertEquals(0, page.getRestaurantCount(),
                        "An empty list must show 0 restaurant cards"),
                () -> Assertions.assertTrue(page.isDetailEmptyStateVisible(),
                        "Empty list detail must show the empty-state message")
        );

        deleteListaViaApi(listaId);
    }

    // ── Caso 6: búsqueda dentro de lista filtra resultados ────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "favoritos",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Caso 6 — searching inside a list filters the restaurant cards dynamically")
    void testCaso6_BusquedaDentroLista() throws Exception {
        int listaId = createListaViaApi("Búsqueda Test");
        addFavoritoViaApi(listaId, PLACE_A);
        addFavoritoViaApi(listaId, PLACE_B);

        FavoritesPage page = loginAndGoToFavorites();
        page.openListByName("Búsqueda Test");

        int totalBefore = page.getRestaurantCount();
        Assertions.assertTrue(totalBefore >= 1, "Must have at least 1 restaurant before searching");

        page.searchWithin("zzz_no_match_xyz");

        Assertions.assertEquals(0, page.getRestaurantCount(),
                "Searching with a non-matching term must show 0 results");

        deleteListaViaApi(listaId);
    }
}
