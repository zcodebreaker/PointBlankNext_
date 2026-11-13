package com.pointblank.api.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountController;
import com.pointblank.api.account.AccountPasswordService;
import com.pointblank.api.account.AccountRepository;

@WebMvcTest(
        controllers = AccountController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class AccountRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private AccountPasswordService accountPasswordService;

    @Test
    void rejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutPlayerScope() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me")
                        .with(jwt().jwt(token -> token.subject("35"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsPasswordChangeWithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/accounts/me/password")
                        .contentType("application/json")
                        .content("""
                                {
                                  "currentPassword": "senha-atual",
                                  "newPassword": "nova-senha",
                                  "newPasswordConfirmation": "nova-senha"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsPlayerToken() throws Exception {
        Account account = new Account(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 100000, 80000, 0, false);
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/v1/accounts/me")
                        .with(jwt()
                                .jwt(token -> token.subject("35"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("player_1"));
    }

    @Test
    void rejectsPreviouslyIssuedTokenAfterAccountIsBanned() throws Exception {
        Account account = new Account(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 100000, 80000, 0, true);
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/api/v1/accounts/me")
                        .with(jwt()
                                .jwt(token -> token.subject("35"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Conta banida"));
    }
}
