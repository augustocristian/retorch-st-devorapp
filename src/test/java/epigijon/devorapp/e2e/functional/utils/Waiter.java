package epigijon.devorapp.e2e.functional.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class Waiter {

    private static final Logger log = LoggerFactory.getLogger(Waiter.class);
    private static final int NAV_WAIT_SECONDS = 30;
    private static final int WAIT_DEFAULT = 10;

    private final WebDriverWait waiter;
    private final WebDriverWait navWaiter;

    public Waiter(WebDriver driver) {
        waiter = new WebDriverWait(driver, Duration.ofSeconds(WAIT_DEFAULT));
        navWaiter = new WebDriverWait(driver, Duration.ofSeconds(NAV_WAIT_SECONDS));
    }

    public void waitUntil(ExpectedCondition<?> condition, String errorMessage) {
        try {
            this.waiter.until(condition);
        } catch (org.openqa.selenium.TimeoutException timeout) {
            log.error(errorMessage);
            throw new org.openqa.selenium.TimeoutException(
                    "\"" + errorMessage + "\" > " + timeout.getMessage());
        }
    }

    public void navWait(String cssSelector, String errorMessage) {
        try {
            navWaiter.until(ExpectedConditions.elementToBeClickable(By.cssSelector(cssSelector)));
        } catch (org.openqa.selenium.TimeoutException e) {
            throw new org.openqa.selenium.TimeoutException(
                    "\"" + errorMessage + "\" > " + e.getMessage());
        }
    }

    public void waitForLoginPage() {
        log.debug("Waiting for login page to load");
        waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id("login-form")),
                "Login page did not load");
    }

    public void waitForHomePage() {
        log.debug("Waiting for home page to load");
        waitUntil(ExpectedConditions.urlContains("/home"),
                "Home page did not load after login");
    }

    public void waitForRegisterPage() {
        log.debug("Waiting for register page to load");
        waitUntil(ExpectedConditions.visibilityOfElementLocated(By.id("register-form")),
                "Register page did not load");
    }

    public void waitForLoginError() {
        log.debug("Waiting for login error message");
        waitUntil(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".message.error")), "Login error message did not appear");
    }

    public void waitForTopBar() {
        log.debug("Waiting for top bar");
        waitUntil(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".topbar, header, nav")), "Top bar did not appear");
    }
}
