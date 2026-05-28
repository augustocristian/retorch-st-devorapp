package epigijon.devorapp.e2e.functional.utils;

import epigijon.devorapp.e2e.functional.common.ElementNotFoundException;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Navigation {

    private static final Logger log = LoggerFactory.getLogger(Navigation.class);

    public void goToLoginPage(WebDriver driver, Waiter waiter) throws ElementNotFoundException {
        log.debug("Navigating to Login page");
        driver.navigate().to(driver.getCurrentUrl().replaceAll("/[^/]*$", "/login"));
        waiter.waitForLoginPage();
    }

    public void goToHomePage(WebDriver driver, Waiter waiter) throws ElementNotFoundException {
        log.debug("Navigating to Home page");
        String base = extractBase(driver.getCurrentUrl());
        driver.navigate().to(base + "/home");
        waiter.waitForHomePage();
    }

    private String extractBase(String currentUrl) {
        try {
            java.net.URI uri = new java.net.URI(currentUrl);
            return uri.getScheme() + "://" + uri.getHost()
                    + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        } catch (java.net.URISyntaxException e) {
            return currentUrl;
        }
    }
}
