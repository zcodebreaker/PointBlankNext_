package com.pointblank.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pointblank.api.download.AdminDownloadSettingsController;
import com.pointblank.api.download.DownloadSettings;
import com.pointblank.api.download.DownloadSettingsService;

@WebMvcTest(
        controllers = AdminDownloadSettingsController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class AdminDownloadSettingsRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DownloadSettingsService service;

    @Test
    void rejectsManagementWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/downloads/client"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatesSettingsForAuthenticatedAdministrator() throws Exception {
        OffsetDateTime date = OffsetDateTime.parse("2026-10-20T18:30:00Z");
        when(service.update(eq(1L), any())).thenReturn(new DownloadSettings(
                "https://cdn.example.com/client.exe",
                "0.3.0",
                "https://cdn.example.com/client.torrent",
                "https://drive.google.com/client",
                "https://1drv.ms/client",
                "https://cdn.example.com/patch-261.exe",
                "https://cdn.example.com/patch-260.exe",
                2048L,
                date,
                date));

        mockMvc.perform(put("/api/v1/admin/downloads/client")
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileUrl":"https://cdn.example.com/client.exe",
                                  "clientVersion":"0.3.0",
                                  "mirrorTorrentUrl":"https://cdn.example.com/client.torrent",
                                  "mirrorGoogleDriveUrl":"https://drive.google.com/client",
                                  "mirrorOneDriveUrl":"https://1drv.ms/client",
                                  "patchPrimaryUrl":"https://cdn.example.com/patch-261.exe",
                                  "patchSecondaryUrl":"https://cdn.example.com/patch-260.exe"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientVersion").value("0.3.0"))
                .andExpect(jsonPath("$.mirrorTorrentUrl").value("https://cdn.example.com/client.torrent"))
                .andExpect(jsonPath("$.patchPrimaryUrl").value("https://cdn.example.com/patch-261.exe"))
                .andExpect(jsonPath("$.fileSizeBytes").value(2048));
    }
}
