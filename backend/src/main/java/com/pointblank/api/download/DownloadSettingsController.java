package com.pointblank.api.download;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/downloads/client")
public class DownloadSettingsController {

    private final DownloadSettingsService service;

    public DownloadSettingsController(DownloadSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<DownloadSettings> find() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.findPublicSettings());
    }
}
