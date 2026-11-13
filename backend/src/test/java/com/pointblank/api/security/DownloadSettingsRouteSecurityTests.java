package com.pointblank.api.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pointblank.api.download.DownloadSettings;
import com.pointblank.api.download.DownloadSettingsController;
import com.pointblank.api.download.DownloadSettingsService;

@WebMvcTest(
        controllers = DownloadSettingsController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class DownloadSettingsRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DownloadSettingsService service;

    @Test
    void exposesClientDownloadSettingsWithoutAuthentication() throws Exception {
        OffsetDateTime date = OffsetDateTime.parse("2026-10-20T18:30:00Z");
        when(service.findPublicSettings()).thenReturn(new DownloadSettings(
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

        mockMvc.perform(get("/api/v1/downloads/client"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").value("https://cdn.example.com/client.exe"))
                .andExpect(jsonPath("$.clientVersion").value("0.3.0"))
                .andExpect(jsonPath("$.mirrorGoogleDriveUrl").value("https://drive.google.com/client"))
                .andExpect(jsonPath("$.patchSecondaryUrl").value("https://cdn.example.com/patch-260.exe"))
                .andExpect(jsonPath("$.fileSizeBytes").value(2048));
    }
}
