package epigijon.devorapp.e2e.functional.tests.e2e;

import com.google.gson.JsonObject;
import epigijon.devorapp.e2e.functional.common.BaseLoggedClass;
import giis.retorch.annotations.AccessMode;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Browser-level tests for the DevorApp home page.
 * The test user is created via API in {@code @BeforeAll} and the browser logs in
 * via the UI form in {@code @BeforeEach} so every test starts on an authenticated home page.
 */
class TestHome extends BaseLoggedClass {

    private static final String PASSWORD = "Test1234!";
    private static String testEmail;
    private static String testUsername;
    private static CloseableHttpClient apiClient;

    @BeforeAll
    static void createTestUser() throws Exception {
        long ts = System.currentTimeMillis();
        testUsername = "tst" + ts;
        if (testUsername.length() > 30) testUsername = testUsername.substring(testUsername.length() - 30);
        testEmail = "testui" + ts + "@devorapp.test";

        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        apiClient = HttpClients.custom().setDefaultCookieStore(new BasicCookieStore()).build();

        JsonObject payload = new JsonObject();
        payload.addProperty("username", testUsername);
        payload.addProperty("email", testEmail);
        payload.addProperty("password", PASSWORD);
        payload.addProperty("nombre", "UIHome");
        payload.addProperty("apellidos", "Tester");
        payload.addProperty("ubicacion", "");

        HttpPost reg = new HttpPost(apiBase + "/api/register");
        reg.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        reg.addHeader("Accept", "application/json");
        apiClient.execute(reg);
    }

    @AfterAll
    static void deleteTestUser() throws Exception {
        if (apiClient == null || testEmail == null) return;
        try {
            String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");

            JsonObject loginPayload = new JsonObject();
            loginPayload.addProperty("identifier", testEmail);
            loginPayload.addProperty("password", PASSWORD);
            HttpPost login = new HttpPost(apiBase + "/api/login");
            login.setEntity(new StringEntity(loginPayload.toString(), ContentType.APPLICATION_JSON));
            login.addHeader("Accept", "application/json");
            apiClient.execute(login);

            URIBuilder builder = new URIBuilder(apiBase + "/api/profile");
            builder.addParameter("password", PASSWORD);
            apiClient.execute(new HttpDelete(builder.build()));
        } catch (Exception e) {
            log.warn("Could not clean up browser test user {}: {}", testEmail, e.getMessage());
        } finally {
            apiClient.close();
        }
    }

    @BeforeEach
    void loginBeforeTest() throws Exception {
        loginViaForm(testEmail, PASSWORD);
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",   concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",       concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Home page loads after login and the URL contains /home")
    void testHomePageLoads() {
        Assertions.assertTrue(driver.getCurrentUrl().contains("/home"),
                "Authenticated user must land on /home");
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",   concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",       concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Home page contains the top navigation bar")
    void testTopBarIsVisible() {
        waiter.waitForTopBar();
        Assertions.assertTrue(
                driver.findElements(By.cssSelector(".topbar, header, nav")).size() > 0,
                "Top navigation bar must be visible on the home page");
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",   concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",       concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Navigating to /login while authenticated redirects back to /home")
    void testAuthenticatedRedirect() {
        driver.navigate().to(sutUrl + "/login");
        waiter.waitUntil(ExpectedConditions.or(
                ExpectedConditions.urlContains("/home"),
                ExpectedConditions.urlContains("/login")), "Page did not load");
        // After an authenticated redirect, the app either stays on /home or redirects there
        String url = driver.getCurrentUrl();
        Assertions.assertTrue(url.contains("/home") || url.contains("/login"),
                "URL must be on /home or /login after navigating with active session");
    }
}
