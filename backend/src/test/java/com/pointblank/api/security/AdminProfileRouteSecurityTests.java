package com.pointblank.api.security;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.pointblank.api.admin.*;

@WebMvcTest(controllers = AdminProfileController.class, properties = {
        "app.jwt.secret=01234567890123456789012345678901", "app.jwt.issuer=pointblank-api",
        "app.jwt.access-token-ttl=PT2H", "app.jwt.remember-token-ttl=P30D",
        "app.cors.allowed-origins=http://localhost:3000"})
@Import(SecurityConfig.class)
class AdminProfileRouteSecurityTests {
    @Autowired private MockMvc mvc;
    @MockitoBean private AdminProfileService service;

    @Test
    void profileAndOriginalImageRequireAuthentication() throws Exception {
        mvc.perform(get("/api/v1/admin/profile")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/profile/avatar")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/admin/profile")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void usesAuthenticatedSubjectInsteadOfSuppliedPlayerId() throws Exception {
        when(service.update(eq(7L), any(), any())).thenReturn(AdminProfile.empty());
        var part = new MockMultipartFile("profile", "profile.json", "application/json",
                "{\"displayName\":\"Admin\",\"removeAvatar\":false,\"playerId\":999}".getBytes());
        mvc.perform(multipart(HttpMethod.PUT, "/api/v1/admin/profile").file(part)
                .param("playerId", "999").with(jwt().jwt(token -> token.subject("7"))
                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk());
        verify(service).update(eq(7L), any(), isNull());
    }

    @Test
    void servesOriginalBytesPrivately() throws Exception {
        byte[] image = {1, 2, 3, 4};
        when(service.findImage(7L)).thenReturn(new AdminProfileImage(image, "image/png", 100, 100, "version"));
        mvc.perform(get("/api/v1/admin/profile/avatar").with(jwt().jwt(token -> token.subject("7"))
                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk()).andExpect(content().bytes(image))
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
