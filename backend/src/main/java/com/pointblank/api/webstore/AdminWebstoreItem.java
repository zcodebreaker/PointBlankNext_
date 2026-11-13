package com.pointblank.api.webstore;

public record AdminWebstoreItem(
        int goodId,
        int itemId,
        String name,
        int priceCash,
        long count,
        String countLabel,
        String category,
        String categoryLabel,
        String imagePath,
        boolean active,
        int sortOrder,
        boolean customCover) {
}

