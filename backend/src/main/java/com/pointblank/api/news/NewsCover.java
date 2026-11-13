package com.pointblank.api.news;

public record NewsCover(
        byte[] data,
        String contentType,
        int width,
        int height) {
}
