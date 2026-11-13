package com.pointblank.api.ticket;

import static org.mockito.Mockito.verifyNoInteractions;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pointblank.api.security.SecurityConfig;

@WebMvcTest(
        controllers = { TicketController.class, AdminTicketController.class },
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class TicketRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private AdminTicketService adminTicketService;

    @Test
    void rejectsAnonymousPlayerTicketRequest() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsPlayerTicketRequestWithoutPlayerScope() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")
                        .with(jwt().jwt(token -> token.subject("38"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsOnlyAuthenticatedPlayersTickets() throws Exception {
        when(ticketService.findMine(38L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tickets")
                        .with(jwt()
                                .jwt(token -> token.subject("38"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void rejectsAnonymousAdministrativeTicketRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forwardsAdministratorIdentityToLiveAccessCheck() throws Exception {
        when(adminTicketService.findAll(5L, "", "", 0, 20))
                .thenReturn(new TicketPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/admin/tickets")
                        .with(jwt()
                                .jwt(token -> token.subject("5"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickets").isArray());
    }

    @Test
    void rejectsAdministrativeReplyWithoutSubject() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/tickets/12/reply")
                        .with(jwt()
                                .jwt(token -> token.subject("5"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Resposta válida","close":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.subject").value("Informe o assunto da resposta"));

        verifyNoInteractions(adminTicketService);
    }
}
