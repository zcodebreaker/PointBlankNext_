package com.pointblank.api.download;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/downloads/client")
public class AdminDownloadSettingsController {

    private final DownloadSettingsService service;

    public AdminDownloadSettingsController(DownloadSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public DownloadSettings find(@AuthenticationPrincipal Jwt jwt) {
        return service.findAdminSettings(playerId(jwt));
    }

    @PutMapping
    public DownloadSettings update(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateDownloadSettingsRequest request) {
        return service.update(playerId(jwt), request);
    }

    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new DownloadSettingsException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}
