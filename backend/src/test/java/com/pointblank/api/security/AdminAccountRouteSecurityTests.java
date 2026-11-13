package com.pointblank.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pointblank.api.admin.AdminAccountController;
import com.pointblank.api.admin.AdminAccountDetailsResponse;
import com.pointblank.api.admin.AdminAccountPageResponse;
import com.pointblank.api.admin.AdminAccountResponse;
import com.pointblank.api.admin.AdminAccountService;
import com.pointblank.api.admin.AddAccountCurrenciesRequest;
import com.pointblank.api.admin.UpdateAccountCurrenciesRequest;

@WebMvcTest(
        controllers = AdminAccountController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class AdminAccountRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAccountService adminAccountService;

    @Test
    void rejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithoutPlayerScope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/accounts")
                        .with(jwt().jwt(token -> token.subject("1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsAccountsForAuthorizedToken() throws Exception {
        AdminAccountResponse account = new AdminAccountResponse(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 100000, 80000, 0, false);
        when(adminAccountService.findAll(1L, "", 0, 20))
                .thenReturn(new AdminAccountPageResponse(List.of(account), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/admin/accounts")
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].username").value("player_1"));
    }

    @Test
    void returnsAccountDetailsForAuthorizedToken() throws Exception {
        AdminAccountDetailsResponse account = new AdminAccountDetailsResponse(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 100000, 80000, 0, false, false, 2610152038L,
                42, 25, 15, 2, 310, 190, 80, 120);
        when(adminAccountService.findOne(1L, "player_1")).thenReturn(account);

        mockMvc.perform(get("/api/v1/admin/accounts/lookup")
                        .queryParam("query", "player_1")
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(35))
                .andExpect(jsonPath("$.matches").value(42))
                .andExpect(jsonPath("$.kills").value(310));
    }

    @Test
    void updatesCurrenciesForAuthorizedToken() throws Exception {
        AdminAccountResponse account = new AdminAccountResponse(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 250000, 90000, 0, false);
        when(adminAccountService.updateCurrencies(eq(1L), eq(35L), any(UpdateAccountCurrenciesRequest.class)))
                .thenReturn(account);

        mockMvc.perform(patch("/api/v1/admin/accounts/35/currencies")
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player")))
                        .contentType("application/json")
                        .content("""
                                {"gold": 250000, "cash": 90000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(250000))
                .andExpect(jsonPath("$.cash").value(90000));
    }

    @Test
    void addsCurrenciesForAuthorizedToken() throws Exception {
        AdminAccountResponse account = new AdminAccountResponse(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 125000, 85000, 0, false);
        when(adminAccountService.addCurrencies(eq(1L), eq(35L), any(AddAccountCurrenciesRequest.class)))
                .thenReturn(account);

        mockMvc.perform(patch("/api/v1/admin/accounts/35/currency-grants")
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player")))
                        .contentType("application/json")
                        .content("""
                                {"gold": 25000, "cash": 5000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(125000))
                .andExpect(jsonPath("$.cash").value(85000));
    }
}
