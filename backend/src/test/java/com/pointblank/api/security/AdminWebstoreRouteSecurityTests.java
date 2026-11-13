package com.pointblank.api.security;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import com.pointblank.api.webstore.AdminWebstoreController;
import com.pointblank.api.webstore.AdminWebstoreItem;
import com.pointblank.api.webstore.WebstoreService;

@WebMvcTest(
        controllers = AdminWebstoreController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class AdminWebstoreRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebstoreService webstoreService;

    @Test
    void rejectsCatalogManagementWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/webstore/items"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsCatalogForAuthenticatedAdministrator() throws Exception {
        when(webstoreService.findAdminCatalog(1L)).thenReturn(List.of(new AdminWebstoreItem(
                10352312, 103523, "Pindad Premium", 4000, 2_592_000,
                "30 Dias", "WEAPON", "Arma", "/api/v1/webstore/items/10352312/cover",
                true, 10, true)));

        mockMvc.perform(get("/api/v1/admin/webstore/items")
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goodId").value(10352312));
    }
}
