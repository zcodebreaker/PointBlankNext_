package com.pointblank.api.admin;

public record AdminProfile(String displayName, String avatarVersion, Integer avatarWidth,
        Integer avatarHeight, Long avatarSize) {
    public static AdminProfile empty() {
        return new AdminProfile("", null, null, null, null);
    }
}
