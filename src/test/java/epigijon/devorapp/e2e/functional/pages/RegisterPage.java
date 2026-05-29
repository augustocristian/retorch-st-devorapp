package epigijon.devorapp.e2e.functional.pages;

import epigijon.devorapp.e2e.functional.utils.Waiter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Page object for {@code /register}.
 * The constructor waits until the registration form is visible.
 */
public class RegisterPage extends BasePage {

    private static final By FORM = By.id("register-form");

    public RegisterPage(WebDriver driver, Waiter waiter) {
        super(driver, waiter);
        waiter.waitForRegisterPage();
    }

    /** Returns {@code true} if the registration form is present in the DOM. */
    public boolean isLoaded() {
        return isVisible(FORM);
    }
}
