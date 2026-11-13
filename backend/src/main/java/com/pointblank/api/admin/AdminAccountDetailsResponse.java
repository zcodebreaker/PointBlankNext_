package com.pointblank.api.admin;

public record AdminAccountDetailsResponse(
        long playerId,
        String username,
        String email,
        String nickname,
        int rank,
        long experience,
        int gold,
        int cash,
        int accessLevel,
        boolean banned,
        boolean online,
        Long lastLogin,
        int matches,
        int wins,
        int losses,
        int draws,
        int kills,
        int deaths,
        int assists,
        int headshots) {
}
