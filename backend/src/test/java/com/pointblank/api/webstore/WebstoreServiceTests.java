package com.pointblank.api.webstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;
import com.pointblank.api.webstore.WebstoreRepository.PurchaseItem;
import com.pointblank.api.webstore.WebstoreRepository.PurchaseDelivery;
import com.pointblank.api.webstore.WebstoreRepository.ShopOffer;

class WebstoreServiceTests {

    private WebstoreRepository webstoreRepository;
    private AccountRepository accountRepository;
    private WebstoreService webstoreService;

    @BeforeEach
    void setUp() {
        webstoreRepository = mock(WebstoreRepository.class);
        accountRepository = mock(AccountRepository.class);
        webstoreService = new WebstoreService(webstoreRepository, accountRepository);
    }

    @Test
    void purchasesItemAndDeductsCash() {
        UUID requestId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(10352312, 103523, "Pindad SS2 V5 Zepetto", 4000, 2_592_000);
        WebstorePurchaseResponse response = new WebstorePurchaseResponse(
                9, item.goodId(), item.itemId(), item.itemName(), item.priceCash(), item.count(),
                "30 Dias", 6000, OffsetDateTime.now(), false);

        when(webstoreRepository.findPurchase(35L, requestId)).thenReturn(Optional.empty());
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(player(10_000, false)));
        when(webstoreRepository.findPurchasableByGoodId(item.goodId())).thenReturn(Optional.of(item));
        when(webstoreRepository.reservePurchaseEntitlement(35L, item.itemId())).thenReturn(true);
        when(webstoreRepository.deductCash(35L, 4000)).thenReturn(Optional.of(6000));
        when(webstoreRepository.deliverItem(35L, item)).thenReturn(901L);
        when(webstoreRepository.recordPurchase(requestId, 35L, item, 6000, 901L)).thenReturn(response);

        WebstorePurchaseResponse purchased = webstoreService.purchase(
                35L, new WebstorePurchaseRequest(item.goodId(), requestId));

        assertThat(purchased.cashBalance()).isEqualTo(6000);
        verify(webstoreRepository).deliverItem(35L, item);
        verify(webstoreRepository).linkPurchaseEntitlement(35L, item.itemId(), response.purchaseId());
    }

    @Test
    void rejectsPurchaseWithInsufficientCash() {
        UUID requestId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(10352312, 103523, "Pindad SS2 V5 Zepetto", 4000, 2_592_000);
        when(webstoreRepository.findPurchase(35L, requestId)).thenReturn(Optional.empty());
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(player(1000, false)));
        when(webstoreRepository.findPurchasableByGoodId(item.goodId())).thenReturn(Optional.of(item));
        when(webstoreRepository.reservePurchaseEntitlement(35L, item.itemId())).thenReturn(true);

        assertThatThrownBy(() -> webstoreService.purchase(
                35L, new WebstorePurchaseRequest(item.goodId(), requestId)))
                .isInstanceOfSatisfying(WebstoreException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT));

        verify(webstoreRepository, never()).deductCash(35L, 4000);
        verify(webstoreRepository, never()).deliverItem(35L, item);
    }

    @Test
    void rejectsItemPreviouslyPurchasedBySameAccount() {
        UUID requestId = UUID.randomUUID();
        PurchaseItem item = new PurchaseItem(10352312, 103523, "Pindad SS2 V5 Zepetto", 4000, 2_592_000);
        when(webstoreRepository.findPurchase(35L, requestId)).thenReturn(Optional.empty());
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(player(10_000, false)));
        when(webstoreRepository.findPurchasableByGoodId(item.goodId())).thenReturn(Optional.of(item));
        when(webstoreRepository.reservePurchaseEntitlement(35L, item.itemId())).thenReturn(false);

        assertThatThrownBy(() -> webstoreService.purchase(
                35L, new WebstorePurchaseRequest(item.goodId(), requestId)))
                .isInstanceOfSatisfying(WebstoreException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("já foi adquirido");
                });

        verify(webstoreRepository, never()).deductCash(35L, item.priceCash());
        verify(webstoreRepository, never()).deliverItem(35L, item);
    }

    @Test
    void repeatsAnIdempotentPurchaseWithoutChargingAgain() {
        UUID requestId = UUID.randomUUID();
        WebstorePurchaseResponse previous = new WebstorePurchaseResponse(
                9, 10352312, 103523, "Pindad SS2 V5 Zepetto", 4000, 2_592_000,
                "30 Dias", 6000, OffsetDateTime.now(), true);
        when(webstoreRepository.findPurchase(35L, requestId)).thenReturn(Optional.of(previous));

        WebstorePurchaseResponse response = webstoreService.purchase(
                35L, new WebstorePurchaseRequest(10352312, requestId));

        assertThat(response.repeated()).isTrue();
        verify(accountRepository, never()).findByPlayerId(35L);
    }

    @Test
    void rejectsUnsupportedHistoryPeriod() {
        assertThatThrownBy(() -> webstoreService.findPurchaseHistory(35L, 12))
                .isInstanceOfSatisfying(WebstoreException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void verifiesExistingDeliveryWithoutDuplicatingItem() {
        PurchaseDelivery purchase = purchaseDelivery(901L, 0);
        when(webstoreRepository.findPurchaseDeliveryForUpdate(35L, 9L)).thenReturn(Optional.of(purchase));
        when(webstoreRepository.deliveryExists(35L, 901L, purchase.itemId())).thenReturn(true);

        WebstoreDeliveryStatus status = webstoreService.verifyDelivery(35L, 9L);

        assertThat(status.delivered()).isTrue();
        assertThat(status.canRedeem()).isFalse();
        verify(webstoreRepository, never()).deliverItem(35L, new PurchaseItem(
                purchase.goodId(), purchase.itemId(), purchase.itemName(), purchase.priceCash(), purchase.itemCount()));
    }

    @Test
    void redeemsMissingDeliveryOnlyOnce() {
        PurchaseDelivery purchase = purchaseDelivery(901L, 0);
        PurchaseItem item = new PurchaseItem(
                purchase.goodId(), purchase.itemId(), purchase.itemName(), purchase.priceCash(), purchase.itemCount());
        when(webstoreRepository.findPurchaseDeliveryForUpdate(35L, 9L)).thenReturn(Optional.of(purchase));
        when(webstoreRepository.deliveryExists(35L, 901L, purchase.itemId())).thenReturn(false);
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(player(6000, false)));
        when(webstoreRepository.deliverItem(35L, item)).thenReturn(902L);

        WebstoreDeliveryStatus status = webstoreService.redeemDelivery(35L, 9L);

        assertThat(status.delivered()).isTrue();
        assertThat(status.recoveryCount()).isEqualTo(1);
        verify(webstoreRepository).recordRecovery(9L, 902L);
    }

    @Test
    void rejectsSecondRecoveryForMissingDelivery() {
        PurchaseDelivery purchase = purchaseDelivery(901L, 1);
        when(webstoreRepository.findPurchaseDeliveryForUpdate(35L, 9L)).thenReturn(Optional.of(purchase));
        when(webstoreRepository.deliveryExists(35L, 901L, purchase.itemId())).thenReturn(false);

        assertThatThrownBy(() -> webstoreService.redeemDelivery(35L, 9L))
                .isInstanceOfSatisfying(WebstoreException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void administratorPublishesCatalogItemWithCustomCover() {
        byte[] png = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        MockMultipartFile cover = new MockMultipartFile("cover", "weapon.png", "image/png", png);
        ShopOffer offer = new ShopOffer(10352312, 103523, "Pindad SS2", 4000, 2_592_000);
        AdminWebstoreItem saved = new AdminWebstoreItem(
                offer.goodId(), offer.itemId(), "Pindad Premium", offer.priceCash(), offer.count(),
                "30 Dias", "WEAPON", "Arma", "/api/v1/webstore/items/10352312/cover",
                true, 10, true);

        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(webstoreRepository.findShopOffer(offer.goodId())).thenReturn(Optional.of(offer));
        when(webstoreRepository.updateShopOfferPrice(offer.goodId(), 5500)).thenReturn(true);
        when(webstoreRepository.findAdminCatalogItem(offer.goodId())).thenReturn(Optional.of(saved));

        AdminWebstoreItem response = webstoreService.saveCatalogItem(
                1L, offer.goodId(), 5500, " Pindad Premium ", cover);

        assertThat(response).isEqualTo(saved);
        verify(webstoreRepository).updateShopOfferPrice(offer.goodId(), 5500);
        verify(webstoreRepository).saveCatalogItem(
                offer.goodId(), "Pindad Premium", "weapon.png", "image/png", png);
    }

    private Account player(int cash, boolean banned) {
        return new Account(35L, "player", "player@example.com", "Soldado", 1, 0, 1000, cash, 0, banned);
    }

    private Account administrator() {
        return new Account(1L, "gm", "gm@example.com", "GameMaster", 53, 0, 0, 0, 5, false);
    }

    private PurchaseDelivery purchaseDelivery(Long objectId, int recoveryCount) {
        return new PurchaseDelivery(
                9L, 10352312, 103523, "Pindad SS2 V5 Zepetto", 4000, 2_592_000,
                objectId, recoveryCount);
    }
}
