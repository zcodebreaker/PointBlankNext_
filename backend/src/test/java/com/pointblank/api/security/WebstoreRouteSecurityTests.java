package com.pointblank.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pointblank.api.webstore.WebstoreController;
import com.pointblank.api.webstore.WebstoreCover;
import com.pointblank.api.webstore.WebstoreDeliveryStatus;
import com.pointblank.api.webstore.WebstoreHistoryItem;
import com.pointblank.api.webstore.WebstoreItem;
import com.pointblank.api.webstore.WebstorePurchaseResponse;
import com.pointblank.api.webstore.WebstoreService;

@WebMvcTest(
        controllers = WebstoreController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class WebstoreRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebstoreService webstoreService;

    @Test
    void exposesCatalogWithoutAuthentication() throws Exception {
        when(webstoreService.findCatalog()).thenReturn(List.of(new WebstoreItem(
                10352312, 103523, "Pindad SS2 V5 Zepetto", 4000, 2_592_000,
                "30 Dias", "WEAPON", "Arma", 1, "Novo", "/images/webstore/item.png")));

        mockMvc.perform(get("/api/v1/webstore/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].goodId").value(10352312));
    }

    @Test
    void exposesUploadedCoverWithoutAuthentication() throws Exception {
        when(webstoreService.findCover(10352312))
                .thenReturn(new WebstoreCover(new byte[] {1, 2, 3}, "image/png"));

        mockMvc.perform(get("/api/v1/webstore/items/10352312/cover"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsPurchaseWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/webstore/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"goodId":10352312,"requestId":"8d1edc3a-b889-4a33-bcbc-685b8426fd78"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsPurchaseHistoryWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/webstore/purchases"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsPurchaseHistoryForAuthenticatedPlayer() throws Exception {
        when(webstoreService.findPurchaseHistory(35L, 3)).thenReturn(List.of(new WebstoreHistoryItem(
                1, 10352312, 103523, "Pindad SS2 V5 Zepetto", 4000, 2_592_000,
                "30 Dias", 6000, OffsetDateTime.now(), "/images/webstore/item.png", true, false, 0)));

        mockMvc.perform(get("/api/v1/webstore/purchases?months=3")
                        .with(jwt()
                                .jwt(token -> token.subject("35"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itemName").value("Pindad SS2 V5 Zepetto"))
                .andExpect(jsonPath("$[0].cashAfter").value(6000));
    }

    @Test
    void returnsPurchasedItemIdsForAuthenticatedPlayer() throws Exception {
        when(webstoreService.findPurchasedItemIds(35L)).thenReturn(List.of(103523, 200004));

        mockMvc.perform(get("/api/v1/webstore/purchases/items")
                        .with(jwt()
                                .jwt(token -> token.subject("35"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(103523))
                .andExpect(jsonPath("$[1]").value(200004));
    }

    @Test
    void verifiesAndRedeemsDeliveryForAuthenticatedPlayer() throws Exception {
        when(webstoreService.verifyDelivery(35L, 9L)).thenReturn(new WebstoreDeliveryStatus(
                9L, false, true, 0, "Item não localizado"));
        when(webstoreService.redeemDelivery(35L, 9L)).thenReturn(new WebstoreDeliveryStatus(
                9L, true, false, 1, "Item resgatado"));

        mockMvc.perform(post("/api/v1/webstore/purchases/9/verify")
                        .with(jwt().jwt(token -> token.subject("35"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canRedeem").value(true));

        mockMvc.perform(post("/api/v1/webstore/purchases/9/redeem")
                        .with(jwt().jwt(token -> token.subject("35"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(true))
                .andExpect(jsonPath("$.recoveryCount").value(1));
    }

    @Test
    void purchasesForAuthenticatedPlayer() throws Exception {
        when(webstoreService.purchase(anyLong(), any())).thenReturn(new WebstorePurchaseResponse(
                1, 10352312, 103523, "Pindad SS2 V5 Zepetto", 4000, 2_592_000,
                "30 Dias", 6000, OffsetDateTime.now(), false));

        mockMvc.perform(post("/api/v1/webstore/purchases")
                        .with(jwt()
                                .jwt(token -> token.subject("35"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"goodId":10352312,"requestId":"8d1edc3a-b889-4a33-bcbc-685b8426fd78"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cashBalance").value(6000));
    }
}
