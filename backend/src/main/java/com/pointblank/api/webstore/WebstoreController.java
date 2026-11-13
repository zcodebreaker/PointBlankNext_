package com.pointblank.api.webstore;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webstore")
public class WebstoreController {

    private final WebstoreService webstoreService;

    public WebstoreController(WebstoreService webstoreService) {
        this.webstoreService = webstoreService;
    }

    @GetMapping("/items")
    public List<WebstoreItem> findCatalog() {
        return webstoreService.findCatalog();
    }

    @GetMapping("/items/{goodId}/cover")
    public ResponseEntity<byte[]> findCover(@PathVariable int goodId) {
        WebstoreCover cover = webstoreService.findCover(goodId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .contentLength(cover.data().length)
                .cacheControl(CacheControl.noCache())
                .body(cover.data());
    }

    @PostMapping("/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public WebstorePurchaseResponse purchase(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WebstorePurchaseRequest request) {
        return webstoreService.purchase(playerId(jwt), request);
    }

    @GetMapping("/purchases")
    public List<WebstoreHistoryItem> findPurchaseHistory(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") int months) {
        return webstoreService.findPurchaseHistory(playerId(jwt), months);
    }

    @GetMapping("/purchases/items")
    public List<Integer> findPurchasedItemIds(@AuthenticationPrincipal Jwt jwt) {
        return webstoreService.findPurchasedItemIds(playerId(jwt));
    }

    @PostMapping("/purchases/{purchaseId}/verify")
    public WebstoreDeliveryStatus verifyDelivery(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long purchaseId) {
        return webstoreService.verifyDelivery(playerId(jwt), purchaseId);
    }

    @PostMapping("/purchases/{purchaseId}/redeem")
    public WebstoreDeliveryStatus redeemDelivery(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long purchaseId) {
        return webstoreService.redeemDelivery(playerId(jwt), purchaseId);
    }

    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new WebstoreException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}
