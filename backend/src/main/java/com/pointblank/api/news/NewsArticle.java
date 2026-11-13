package com.pointblank.api.news;

import java.time.OffsetDateTime;

public record NewsArticle(
        long newsId,
        long authorId,
        String authorUsername,
        NewsCategory category,
        String title,
        String summary,
        String content,
        String coverUrl,
        String carouselUrl,
        String bannerUrl,
        boolean published,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
