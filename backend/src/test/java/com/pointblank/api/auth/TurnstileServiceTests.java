package com.pointblank.api.auth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class TurnstileServiceTests {

    private static final String TEST_SECRET_KEY = "1x0000000000000000000000000000000AA";

    @Test
    void acceptsSuccessfulTestTokenWithoutProductionMetadata() {
        TurnstileService service = serviceWithSecret(TEST_SECRET_KEY);

        assertThatCode(() -> service.validateResponse("""
                {"success":true,"hostname":"example.com","action":"","error-codes":[]}
                """, "login"))
                .doesNotThrowAnyException();
    }

    @Test
    void keepsMetadataValidationEnabledForRealKeys() {
        TurnstileService service = serviceWithSecret("real-secret-key");

        assertThatThrownBy(() -> service.validateResponse("""
                {"success":true,"hostname":"example.com","action":"","error-codes":[]}
                """, "login"))
                .isInstanceOf(TurnstileVerificationException.class);
    }

    @Test
    void rejectsFailedValidationEvenWithTestSecret() {
        TurnstileService service = serviceWithSecret(TEST_SECRET_KEY);

        assertThatThrownBy(() -> service.validateResponse("""
                {"success":false,"error-codes":["invalid-input-response"]}
                """, "login"))
                .isInstanceOf(TurnstileVerificationException.class);
    }

    private TurnstileService serviceWithSecret(String secretKey) {
        return new TurnstileService(
                new ObjectMapper(),
                secretKey,
                URI.create("https://challenges.cloudflare.com/turnstile/v0/siteverify"),
                "localhost,127.0.0.1");
    }
}
