package com.pointblank.api.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;
import com.pointblank.api.account.GameProfileController;
import com.pointblank.api.account.GameProfileRepository;
import com.pointblank.api.account.GameProfileResponse;
import com.pointblank.api.account.GameProfileResponse.WeaponStatistics;

@WebMvcTest(
        controllers = GameProfileController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class GameProfileRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private GameProfileRepository gameProfileRepository;

    @Test
    void rejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me/game-profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsDatabaseProfileForAuthenticatedAccount() throws Exception {
        Account account = new Account(
                35L, "player_1", "player@example.com", "Soldado1",
                12, 1200, 100000, 80000, 0, false);
        GameProfileResponse profile = new GameProfileResponse(
                35L, "player_1", 0, "Soldado1", 12, 1200,
                20, 12, 7, 1, 90, 15, 45, 30,
                600, 480, "MeuClã",
                new WeaponStatistics(30, 10, 20, 8, 25, 12, 5, 5, 10, 10),
                List.of(), List.of());

        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(account));
        when(gameProfileRepository.findByPlayerId(35L)).thenReturn(Optional.of(profile));

        mockMvc.perform(get("/api/v1/accounts/me/game-profile")
                        .with(jwt()
                                .jwt(token -> token.subject("35"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Soldado1"))
                .andExpect(jsonPath("$.kills").value(90))
                .andExpect(jsonPath("$.weapons.assaultKills").value(30));
    }
}
