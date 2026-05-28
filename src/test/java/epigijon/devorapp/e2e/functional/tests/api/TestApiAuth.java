package epigijon.devorapp.e2e.functional.tests.api;

import epigijon.devorapp.e2e.functional.common.BaseApiClass;
import giis.retorch.annotations.AccessMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import com.google.gson.JsonObject;

/**
 * Validates the DevorApp authentication and user-profile endpoints:
 * <ul>
 *   <li>GET  /health                        — public health check</li>
 *   <li>GET  /                              — welcome message</li>
 *   <li>GET  /api/check-availability        — email/username availability</li>
 *   <li>GET  /api/me                        — current authenticated user</li>
 *   <li>PATCH /api/profile                  — update profile fields</li>
 * </ul>
 */
class TestApiAuth extends BaseApiClass {

    @BeforeAll
    static void authSetup() throws IOException {
        long ts = unique();
        registerAndLogin(uniqueUsername(ts), uniqueEmail(ts), "Test1234!");
    }

    @AfterAll
    static void authTeardown() throws IOException {
        deleteTestUser();
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /health returns HTTP 200 with status ok")
    void testHealthEndpoint() throws IOException {
        int status = getStatus(sutUrl + "/health");
        Assertions.assertEquals(200, status, "Health endpoint must return 200");

        JsonObject body = getJsonObject(sutUrl + "/health");
        Assertions.assertEquals("ok", body.get("status").getAsString(), "status field must be 'ok'");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET / returns HTTP 200 with a welcome message")
    void testWelcomeEndpoint() throws IOException {
        int status = getStatus(sutUrl + "/");
        Assertions.assertEquals(200, status, "Root endpoint must return 200");

        JsonObject body = getJsonObject(sutUrl + "/");
        Assertions.assertTrue(body.has("message"), "Root endpoint must include a 'message' field");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/check-availability returns email_taken false for a nonexistent email")
    void testCheckAvailabilityEmailFree() throws IOException {
        JsonObject result = getJsonObject(
                authUrl("/check-availability?email=nobody_" + unique() + "@devorapp.test"));
        Assertions.assertFalse(result.get("email_taken").getAsBoolean(),
                "A never-registered email must not be taken");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/check-availability returns username_taken true for the registered test user")
    void testCheckAvailabilityUsernameTaken() throws IOException {
        JsonObject result = getJsonObject(
                authUrl("/check-availability?username=" + testUsername));
        Assertions.assertTrue(result.get("username_taken").getAsBoolean(),
                "The registered username must be reported as taken");
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READONLY")
    @Test
    @DisplayName("GET /api/me returns HTTP 200 with the authenticated user's username and email")
    void testGetCurrentUser() throws IOException {
        int status = getStatus(authUrl("/me"));
        Assertions.assertEquals(200, status, "GET /api/me must return 200 when authenticated");

        JsonObject user = getJsonObject(authUrl("/me"));
        Assertions.assertAll(
                () -> Assertions.assertEquals(testUsername, user.get("username").getAsString(),
                        "username must match the registered user"),
                () -> Assertions.assertEquals(testEmail, user.get("email").getAsString(),
                        "email must match the registered user")
        );
    }

    @AccessMode(resID = "user", concurrency = 1, sharing = false, accessMode = "READWRITE")
    @Test
    @DisplayName("PATCH /api/profile returns HTTP 200 and persists the updated nombre")
    void testUpdateProfile() throws IOException {
        String newNombre = "Updated" + unique();
        int status = patch(authUrl("/profile"),
                profileUpdatePayload(newNombre, "User", "", testPassword));
        Assertions.assertEquals(200, status, "PATCH /api/profile must return 200");

        JsonObject updated = getJsonObject(authUrl("/me"));
        Assertions.assertEquals(newNombre, updated.get("nombre").getAsString(),
                "nombre must reflect the update");
    }
}
