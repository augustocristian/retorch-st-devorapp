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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * Browser-level tests for the DevorApp login page.
 * A test user is created via API in {@code @BeforeAll} and cleaned up in {@code @AfterAll}.
 * Each test exercises the login form behaviour directly in Chrome.
 */
class TestLogin extends BaseLoggedClass {

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
        payload.addProperty("nombre", "UI");
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

            // Log in first to get the auth cookie
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

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",   concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",       concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("Login form redirects to home page when valid credentials are submitted")
    void testSuccessfulLogin() throws Exception {
        driver.get(sutUrl + "/login");
        waiter.waitForLoginPage();

        fillById("identifier", testEmail);
        fillById("password", PASSWORD);
        epigijon.devorapp.e2e.functional.utils.Click.element(
                driver, waiter, driver.findElement(By.id("login-submit-btn")));

        waiter.waitForHomePage();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/home"),
                "After login the URL must contain /home");
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",   concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",       concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Login form shows an error message when wrong credentials are submitted")
    void testLoginWithWrongPassword() throws Exception {
        driver.get(sutUrl + "/login");
        waiter.waitForLoginPage();

        fillById("identifier", testEmail);
        fillById("password", "WrongPassword99!");
        epigijon.devorapp.e2e.functional.utils.Click.element(
                driver, waiter, driver.findElement(By.id("login-submit-btn")));

        waiter.waitForLoginError();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/login"),
                "After failed login the URL must still be on /login");
    }

    @AccessMode(resID = "web-browser", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @AccessMode(resID = "frontend",   concurrency = 1, sharing = false, accessMode = "READONLY")
    @AccessMode(resID = "user",       concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("Login page shows the registration link that navigates to /register")
    void testRegisterLink() throws Exception {
        driver.get(sutUrl + "/login");
        waiter.waitForLoginPage();

        epigijon.devorapp.e2e.functional.utils.Click.element(
                driver, waiter, driver.findElement(By.id("go-register-link")));

        waiter.waitForRegisterPage();
        Assertions.assertTrue(driver.getCurrentUrl().contains("/register"),
                "Clicking the register link must navigate to /register");
    }
}
