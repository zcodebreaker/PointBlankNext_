package com.pointblank.api.webstore;

import java.util.List;

import jakarta.validation.constraints.Positive;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/admin/webstore/items")
public class AdminWebstoreController {

    private final WebstoreService webstoreService;

    public AdminWebstoreController(WebstoreService webstoreService) {
        this.webstoreService = webstoreService;
    }

    @GetMapping
    public List<AdminWebstoreItem> findAll(@AuthenticationPrincipal Jwt jwt) {
        return webstoreService.findAdminCatalog(playerId(jwt));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AdminWebstoreItem save(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @Positive int goodId,
            @RequestParam @Positive int priceCash,
            @RequestParam String name,
            @RequestParam MultipartFile cover) {
        return webstoreService.saveCatalogItem(playerId(jwt), goodId, priceCash, name, cover);
    }

    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new WebstoreException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}
