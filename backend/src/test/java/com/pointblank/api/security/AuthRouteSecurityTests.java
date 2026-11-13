package com.pointblank.api.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pointblank.api.auth.AuthController;
import com.pointblank.api.auth.AuthService;
import com.pointblank.api.auth.EmailAvailabilityResponse;
import com.pointblank.api.auth.EmailVerificationException;
import com.pointblank.api.auth.EmailVerificationResponse;
import com.pointblank.api.auth.UsernameAvailabilityResponse;

@WebMvcTest(
        controllers = AuthController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class AuthRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void allowsUsernameAvailabilityWithoutAuthentication() throws Exception {
        when(authService.usernameAvailability("player_1"))
                .thenReturn(new UsernameAvailabilityResponse("player_1", false));

        mockMvc.perform(get("/api/v1/auth/username-availability")
                        .queryParam("username", "player_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("player_1"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void allowsEmailAvailabilityWithoutAuthentication() throws Exception {
        when(authService.emailAvailability("player@example.com"))
                .thenReturn(new EmailAvailabilityResponse("player@example.com", false));

        mockMvc.perform(get("/api/v1/auth/email-availability")
                        .queryParam("email", "player@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("player@example.com"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void allowsEmailVerificationWithoutAuthentication() throws Exception {
        when(authService.verifyEmail("activation-token"))
                .thenReturn(new EmailVerificationResponse(true, "E-mail confirmado"));

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .queryParam("token", "activation-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void returnsNotFoundForExpiredOrAlreadyUsedEmailVerificationToken() throws Exception {
        when(authService.verifyEmail("unavailable-token"))
                .thenThrow(new EmailVerificationException(
                        "O link de ativação é inválido ou já não está disponível."));

        mockMvc.perform(get("/api/v1/auth/verify-email")
                        .queryParam("token", "unavailable-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Link de ativação indisponível"))
                .andExpect(jsonPath("$.detail")
                        .value("O link de ativação é inválido ou já não está disponível."))
                .andExpect(jsonPath("$.type")
                        .value("https://pointblank.local/problems/email-verification-token-not-found"));
    }
}
