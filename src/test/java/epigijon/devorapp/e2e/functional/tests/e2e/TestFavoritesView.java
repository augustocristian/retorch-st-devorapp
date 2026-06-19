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
 * <p>Base-Choice coverage:
 * <ul>
 *   <li>BASE — multiple lists, multiple restaurants, no search filter.</li>
 *   <li>Caso 2 — no lists created → empty state visible.</li>
 *   <li>Caso 3–5 — single list, empty list, list with 1 restaurant.</li>
 *   <li>Caso 6 — search within a list filters results.</li>
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

    private void clearSessionAndLogin() {
        clearSession();
        driver.get(sutUrl + "/login");
    }

    private FavoritesPage loginAndGoToFavorites() throws Exception {
        clearSessionAndLogin();
        new LoginPage(driver, waiter)
                .enterIdentifier(testEmail)
                .enterPassword(testPassword)
                .submitLogin();
        driver.get(sutUrl + "/favorites");
        return new FavoritesPage(driver, waiter);
    }

    private int createListaViaApi(String nombre) throws IOException {
        apiLogin();
        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        JsonObject payload = new JsonObject();
        payload.addProperty("nombre", nombre);
        payload.addProperty("icono", "Heart");
        JsonObject resp = apiPost(apiBase + "/api/favoritos/listas", payload.toString());
        return resp.get("id").getAsInt();
    }

    private void addFavoritoViaApi(int listaId, String placeId) throws IOException {
        apiLogin();
        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        JsonObject payload = new JsonObject();
        payload.addProperty("place_id", placeId);
        apiPost(apiBase + "/api/favoritos/listas/" + listaId, payload.toString());
    }

    private void deleteListaViaApi(int listaId) throws IOException {
        apiLogin();
        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        apiDelete(apiBase + "/api/favoritos/listas/" + listaId);
    }

    // ── 1. BASE: varias listas y varios restaurantes sin búsqueda ──────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "favoritos",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("debe mostrar varias listas y varios restaurantes sin búsqueda (BASE)")
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

    // ── 2. Caso 2 y Casos 3–5 condensados: sin listas, lista única, lista vacía, lista con 1 elem. ──

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "favoritos",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("debe gestionar correctamente listas vacías y unitarias (Caso 2, 3, 4, 5)")
    void testCasosVacioYUnitario() throws Exception {
        // Caso 2: no lists → empty state
        FavoritesPage page = loginAndGoToFavorites();
        if (page.getListCount() == 0) {
            Assertions.assertTrue(page.isEmptyStateVisible(),
                    "Caso 2: when there are no lists the empty-state text must be visible");
        }

        // Caso 3: exactly 1 list
        int listaId = createListaViaApi("Mis Favoritos Solo");
        driver.get(sutUrl + "/favorites");
        page = new FavoritesPage(driver, waiter);
        Assertions.assertTrue(page.getListCount() >= 1, "Caso 3: at least 1 list must be visible");
        deleteListaViaApi(listaId);

        // Caso 4: list with 0 restaurants
        int emptyListaId = createListaViaApi("Lista Vacía");
        driver.get(sutUrl + "/favorites");
        page = new FavoritesPage(driver, waiter);
        page.openListByName("Lista Vacía");
        Assertions.assertEquals(0, page.getRestaurantCount(),
                "Caso 4: an empty list must show 0 restaurant cards");
        Assertions.assertTrue(page.isDetailEmptyStateVisible(),
                "Caso 4: empty list detail must show the empty-state message");
        deleteListaViaApi(emptyListaId);

        // Caso 5: list with exactly 1 restaurant
        int oneListaId = createListaViaApi("Lista Unitaria");
        addFavoritoViaApi(oneListaId, PLACE_A);
        driver.get(sutUrl + "/favorites");
        page = new FavoritesPage(driver, waiter);
        page.openListByName("Lista Unitaria");
        Assertions.assertEquals(1, page.getRestaurantCount(),
                "Caso 5: a list with 1 restaurant must show exactly 1 card");
        deleteListaViaApi(oneListaId);
    }

    // ── 3. Caso 6: búsqueda dentro de lista filtra resultados ──────────────────────────

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",    concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "favoritos",   concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "user",        concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("debe filtrar dinámicamente al buscar dentro de una lista (Caso 6)")
    void testCaso6_BusquedaDentroLista() throws Exception {
        int listaId = createListaViaApi("Búsqueda Test");
        addFavoritoViaApi(listaId, PLACE_A);
        addFavoritoViaApi(listaId, PLACE_B);

        FavoritesPage page = loginAndGoToFavorites();
        page.openListByName("Búsqueda Test");

        int totalBefore = page.getRestaurantCount();
        Assertions.assertTrue(totalBefore >= 1,
                "Caso 6: must have at least 1 restaurant before searching");

        page.searchWithin("zzz_no_match_xyz");
        Assertions.assertEquals(0, page.getRestaurantCount(),
                "Caso 6: searching with a non-matching term must show 0 results");

        deleteListaViaApi(listaId);
    }
}
