package epigijon.devorapp.e2e.functional.common;

import epigijon.devorapp.e2e.functional.utils.Click;
import epigijon.devorapp.e2e.functional.utils.Navigation;
import epigijon.devorapp.e2e.functional.utils.Waiter;
import giis.selema.framework.junit5.LifecycleJunit5;
import giis.selema.manager.SeleManager;
import giis.selema.manager.SelemaConfig;
import giis.selema.services.browser.DynamicGridBrowserService;
import giis.selema.services.impl.WatermarkService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Base class for DevorApp browser (Selenium) tests. Manages the WebDriver lifecycle,
 * reads the frontend URL from {@code FRONTEND_URL} in test.properties, and provides
 * helpers for filling form fields and clicking elements.
 */
@ExtendWith(LifecycleJunit5.class)
public class BaseLoggedClass {

    protected static final Logger log = LoggerFactory.getLogger(BaseLoggedClass.class);

    private static final SeleManager seleManager = new SeleManager(new SelemaConfig()
            .setReportSubdir("target/containerlogs/"
                    + (System.getProperty("TJOB_NAME") == null ? "" : System.getProperty("TJOB_NAME")))
            .setName(System.getProperty("TJOB_NAME") == null ? "locallogs" : System.getProperty("TJOB_NAME")));

    protected static String sutUrl;
    protected static String tJobName = "DEFAULT_TJOB";
    protected static Properties properties;
    protected WebDriver driver;
    protected Waiter waiter;
    protected Navigation navUtils;

    @BeforeAll
    static void setupAll() throws IOException {
        log.info("Starting global browser setup");
        properties = new Properties();
        properties.load(Files.newInputStream(Paths.get("src/test/resources/test.properties")));
        tJobName = System.getProperty("TJOB_NAME");

        // Browser tests use the nginx-served React frontend
        String envUrl = System.getProperty("SUT_URL") != null
                ? System.getProperty("SUT_URL")
                : System.getenv("SUT_URL");
        sutUrl = envUrl != null ? envUrl : properties.getProperty("FRONTEND_URL", "http://localhost");
        log.info("Browser base URL: {}", sutUrl);
        setupBrowser();
        log.info("Global browser setup done.");
    }

    protected static void setupBrowser() {
        String browserUser = properties.getProperty("BROWSER_USER", "CHROME");
        log.debug("Starting browser ({})", browserUser);
        seleManager.setBrowser("chrome").setArguments(new String[]{"--start-maximized", "--incognito"});
        if (System.getenv("SELENOID_PRESENT") != null) {
            log.debug("Setting up Selenium WebDriver with Selenium-hub");
            seleManager.setDriverUrl("http://selenium-hub:4444/wd/hub")
                    .add(new DynamicGridBrowserService().setVideo())
                    .add(new WatermarkService().setDelayOnFailure(3));
        }
        log.debug("Browser configured ({})", browserUser);
    }

    @BeforeEach
    void setup(TestInfo testInfo) {
        log.info("Starting individual setup for: {}", testInfo.getDisplayName());
        driver = seleManager.getDriver();
        waiter = new Waiter(driver);
        navUtils = new Navigation();
        driver.get(sutUrl);
        log.info("Setup done, starting test: {}", testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown(TestInfo testInfo) throws ElementNotFoundException {
        log.info("Tearing down test: {}", testInfo.getDisplayName());
    }

    // ── Browser helpers ──────────────────────────────────────────────────────────

    /** Fills an input by its element id. */
    protected void fillById(String elementId, String value) {
        log.debug("Filling #{} with value", elementId);
        WebElement field = driver.findElement(By.id(elementId));
        field.clear();
        field.sendKeys(value);
    }

    /** Fills an input by name attribute. */
    protected void fillByName(String name, String value) {
        log.debug("Filling [name={}] with value", name);
        WebElement field = driver.findElement(By.name(name));
        field.clear();
        field.sendKeys(value);
    }

    /**
     * Logs in via the browser UI using the given credentials.
     * Assumes the browser is already on a page where the login form is visible
     * (either the /login page or after a redirect).
     */
    protected void loginViaForm(String email, String password) throws ElementNotFoundException {
        log.debug("Logging in via browser form as {}", email);
        waiter.waitForLoginPage();
        fillById("identifier", email);
        fillById("password", password);
        Click.element(driver, waiter, driver.findElement(By.id("login-submit-btn")));
        waiter.waitForHomePage();
        log.debug("Login successful, now on home page");
    }

    /**
     * Waits for the login page and verifies an error message is visible after
     * a failed login attempt.
     */
    protected void waitForLoginError() {
        waiter.waitForLoginError();
    }

    /** Waits until the current URL contains the given fragment. */
    protected void waitForUrl(String fragment) {
        waiter.waitUntil(ExpectedConditions.urlContains(fragment),
                "URL did not contain '" + fragment + "'");
    }
}
