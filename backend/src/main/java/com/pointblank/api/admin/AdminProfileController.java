package com.pointblank.api.admin;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/profile")
public class AdminProfileController {
    private final AdminProfileService service;

    public AdminProfileController(AdminProfileService service) {
        this.service = service;
    }

    @GetMapping
    public AdminProfile me(@AuthenticationPrincipal Jwt jwt) {
        return service.find(playerId(jwt));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminProfile update(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestPart("profile") UpdateAdminProfileRequest request,
            @RequestPart(name = "photo", required = false) MultipartFile photo) {
        return service.update(playerId(jwt), request, photo);
    }

    @GetMapping("/avatar")
    public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal Jwt jwt) {
        var image = service.findImage(playerId(jwt));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(image.contentType()))
                .contentLength(image.data().length).body(image.data());
    }

    private long playerId(Jwt jwt) {
        try {
            long playerId = Long.parseLong(jwt.getSubject());
            if (playerId > 0) return playerId;
        } catch (NumberFormatException ignored) {
        }
        throw new AdminAccountException(HttpStatus.UNAUTHORIZED, "Token inválido");
    }
}
