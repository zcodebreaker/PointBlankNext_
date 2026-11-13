package com.pointblank.api.account;

public record Account(
        long playerId,
        String username,
        String email,
        String nickname,
        int rank,
        long experience,
        int gold,
        int cash,
        int accessLevel,
        boolean banned) {
}

