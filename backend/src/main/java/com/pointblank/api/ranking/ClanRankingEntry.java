package com.pointblank.api.ranking;

public record ClanRankingEntry(
        int position,
        int clanId,
        String name,
        String leader,
        int memberCount,
        int rank,
        long experience,
        long logo,
        int wins,
        int losses,
        int winRate) {
}
