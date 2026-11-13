package com.pointblank.api.webstore;

import java.time.OffsetDateTime;

public record WebstorePurchaseResponse(
        long purchaseId,
        int goodId,
        int itemId,
        String itemName,
        int priceCash,
        long itemCount,
        String countLabel,
        int cashBalance,
        OffsetDateTime purchasedAt,
        boolean repeated) {
}
