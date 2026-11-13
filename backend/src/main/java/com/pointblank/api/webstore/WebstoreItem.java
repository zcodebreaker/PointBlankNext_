package com.pointblank.api.webstore;

public record WebstoreItem(
        int goodId,
        int itemId,
        String name,
        int priceCash,
        long count,
        String countLabel,
        String category,
        String categoryLabel,
        int tag,
        String tagLabel,
        String imagePath) {
}
