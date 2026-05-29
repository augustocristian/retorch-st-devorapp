package epigijon.devorapp.e2e.functional.common;

import com.google.gson.JsonObject;
import epigijon.devorapp.e2e.functional.utils.Waiter;
import giis.selema.framework.junit5.LifecycleJunit5;
import giis.selema.manager.SeleManager;
import giis.selema.manager.SelemaConfig;
import giis.selema.services.browser.DynamicGridBrowserService;
import giis.selema.services.impl.WatermarkService;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Base class for all DevorApp browser (Selenium) tests.
 *
 * <p>Manages two concerns:
 * <ol>
 *   <li><b>Browser lifecycle</b> — initialises the Selema/Chrome driver once per class
 *       and hands a fresh {@link WebDriver} + {@link Waiter} to each test method.</li>
 *   <li><b>Test-user lifecycle</b> — provides {@link #setupTestUser} and
 *       {@link #tearDownTestUser} so every subclass creates and deletes a real Firebase
 *       user with a single line, without duplicating HTTP-client boilerplate.</li>
 * </ol>
 *
 * <p>Tests interact with the UI exclusively through page objects in
 * {@code epigijon.devorapp.e2e.functional.pages}.
 */
@ExtendWith(LifecycleJunit5.class)
public class BaseLoggedClass {

    protected static final Logger log = LoggerFactory.getLogger(BaseLoggedClass.class);

    // ── Browser infrastructure ────────────────────────────────────────────────────

    private static final SeleManager seleManager = new SeleManager(new SelemaConfig()
            .setReportSubdir("target/containerlogs/"
                    + (System.getProperty("TJOB_NAME") == null ? "" : System.getProperty("TJOB_NAME")))
            .setName(System.getProperty("TJOB_NAME") == null ? "locallogs" : System.getProperty("TJOB_NAME")));

    protected static String sutUrl;
    protected static Properties properties;

    protected WebDriver driver;
    protected Waiter waiter;

    // ── API client for test-user lifecycle ────────────────────────────────────────

    private static CloseableHttpClient apiClient;

    protected static String testUsername;
    protected static String testEmail;
    protected static String testPassword;

    // ── JUnit lifecycle ───────────────────────────────────────────────────────────

    @BeforeAll
    static void setupAll() throws IOException {
        properties = new Properties();
        properties.load(Files.newInputStream(Paths.get("src/test/resources/test.properties")));

        String envUrl = System.getProperty("SUT_URL") != null
                ? System.getProperty("SUT_URL") : System.getenv("SUT_URL");
        sutUrl = envUrl != null ? envUrl : properties.getProperty("FRONTEND_URL", "http://localhost");
        log.info("Browser base URL: {}", sutUrl);

        seleManager.setBrowser("chrome").setArguments(new String[]{"--start-maximized", "--incognito"});
        if (System.getenv("SELENOID_PRESENT") != null) {
            seleManager.setDriverUrl("http://selenium-hub:4444/wd/hub")
                    .add(new DynamicGridBrowserService().setVideo())
                    .add(new WatermarkService().setDelayOnFailure(3));
        }

        apiClient = HttpClients.custom().setDefaultCookieStore(new BasicCookieStore()).build();
        log.info("Browser and API client initialised.");
    }

    @BeforeEach
    void setup(TestInfo testInfo) {
        log.info("Starting: {}", testInfo.getDisplayName());
        driver = seleManager.getDriver();
        waiter = new Waiter(driver);
        driver.get(sutUrl);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.info("Finished: {}", testInfo.getDisplayName());
    }

    /** Closes the shared API client. Runs after all subclass {@code @AfterAll} methods. */
    @AfterAll
    static void tearDownAll() throws IOException {
        if (apiClient != null) {
            apiClient.close();
            apiClient = null;
        }
    }

    // ── Test-user helpers ────────────────────────────────────────────────────────

    /**
     * Registers a new test user via {@code POST /api/register} and stores the
     * credentials for later teardown.  Call from a subclass {@code @BeforeAll}.
     */
    protected static void setupTestUser(String username, String email, String password)
            throws IOException {
        testUsername = username;
        testEmail    = email;
        testPassword = password;

        String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");
        JsonObject payload = new JsonObject();
        payload.addProperty("username",  username);
        payload.addProperty("email",     email);
        payload.addProperty("password",  password);
        payload.addProperty("nombre",    "UITester");
        payload.addProperty("apellidos", "Test");
        payload.addProperty("ubicacion", "");

        HttpPost reg = new HttpPost(apiBase + "/api/register");
        reg.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        reg.addHeader("Accept", "application/json");
        apiClient.execute(reg);
        log.info("Registered browser test user: {}", email);
    }

    /**
     * Logs in and then calls {@code DELETE /api/profile} to permanently remove
     * the test user created by {@link #setupTestUser}.
     * Call from a subclass {@code @AfterAll} — the parent {@code @AfterAll}
     * ({@link #tearDownAll}) which closes the API client runs after this.
     */
    protected static void tearDownTestUser() {
        if (testEmail == null || testPassword == null || apiClient == null) return;
        try {
            String apiBase = properties.getProperty("LOCALHOST_URL", "http://localhost:8000");

            JsonObject loginPayload = new JsonObject();
            loginPayload.addProperty("identifier", testEmail);
            loginPayload.addProperty("password",   testPassword);
            HttpPost login = new HttpPost(apiBase + "/api/login");
            login.setEntity(new StringEntity(loginPayload.toString(), ContentType.APPLICATION_JSON));
            login.addHeader("Accept", "application/json");
            apiClient.execute(login);

            URIBuilder ub = new URIBuilder(apiBase + "/api/profile");
            ub.addParameter("password", testPassword);
            apiClient.execute(new HttpDelete(ub.build()));
            log.info("Deleted browser test user: {}", testEmail);
        } catch (Exception e) {
            log.warn("Could not delete browser test user {}: {}", testEmail, e.getMessage());
        } finally {
            testEmail    = null;
            testPassword = null;
        }
    }
}
