package com.pointblank.api.webstore;

import java.time.OffsetDateTime;

public record WebstoreHistoryItem(
        long purchaseId,
        int goodId,
        int itemId,
        String itemName,
        int priceCash,
        long itemCount,
        String countLabel,
        int cashAfter,
        OffsetDateTime purchasedAt,
        String imagePath,
        boolean delivered,
        boolean canRedeem,
        int recoveryCount) {
}
