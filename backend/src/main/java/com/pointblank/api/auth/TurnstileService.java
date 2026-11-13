package com.pointblank.api.auth;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;

@Service
public class TurnstileService {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final Set<String> TEST_SECRET_KEYS = Set.of(
            "1x0000000000000000000000000000000AA",
            "2x0000000000000000000000000000000AA",
            "3x0000000000000000000000000000000AA");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI siteverifyUri;
    private final String secretKey;
    private final boolean usingTestSecretKey;
    private final Set<String> allowedHostnames;

    public TurnstileService(
            ObjectMapper objectMapper,
            @Value("${app.turnstile.secret-key}") String secretKey,
            @Value("${app.turnstile.siteverify-url}") URI siteverifyUri,
            @Value("${app.turnstile.allowed-hostnames}") String allowedHostnames) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
        this.objectMapper = objectMapper;
        this.siteverifyUri = siteverifyUri;
        this.secretKey = secretKey.trim();
        this.usingTestSecretKey = TEST_SECRET_KEYS.contains(this.secretKey);
        this.allowedHostnames = Arrays.stream(allowedHostnames.split(","))
                .map(String::trim)
                .filter(hostname -> !hostname.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        if (this.secretKey.isEmpty()) {
            throw new IllegalStateException("TURNSTILE_SECRET_KEY não foi configurada");
        }
        if (this.allowedHostnames.isEmpty()) {
            throw new IllegalStateException("TURNSTILE_ALLOWED_HOSTNAMES não foi configurada");
        }
    }

    public void verify(String token, String expectedAction) {
        if (token == null || token.isBlank() || token.length() > 2_048) {
            throw new TurnstileVerificationException();
        }

        String requestBody = "secret=" + encode(secretKey) + "&response=" + encode(token);
        HttpRequest request = HttpRequest.newBuilder(siteverifyUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new TurnstileUnavailableException();
            }

            validateResponse(response.body(), expectedAction);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TurnstileUnavailableException(exception);
        } catch (IOException exception) {
            throw new TurnstileUnavailableException(exception);
        }
    }

    void validateResponse(String responseBody, String expectedAction) throws IOException {
        SiteverifyResponse verification = objectMapper.readValue(responseBody, SiteverifyResponse.class);
        if (!verification.success()) {
            throw new TurnstileVerificationException();
        }

        // As chaves oficiais de teste retornam metadados fictícios (sem action
        // e com hostname genérico). A autenticidade do token ainda é validada
        // pelo Siteverify; apenas as verificações de contexto ficam para chaves reais.
        if (!usingTestSecretKey
                && (!expectedAction.equals(verification.action())
                        || !allowedHostnames.contains(verification.hostname()))) {
            throw new TurnstileVerificationException();
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SiteverifyResponse(
            boolean success,
            String hostname,
            String action,
            @JsonProperty("error-codes") List<String> errorCodes) {
    }
}
