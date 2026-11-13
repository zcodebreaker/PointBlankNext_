package com.pointblank.api.webstore;

public record WebstoreDeliveryStatus(
        long purchaseId,
        boolean delivered,
        boolean canRedeem,
        int recoveryCount,
        String message) {
}
