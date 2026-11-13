package com.pointblank.api.admin;

public record AdminProfileImage(byte[] data, String contentType, int width, int height, String version) {
}
