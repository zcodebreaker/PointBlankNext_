package com.pointblank.api.ranking;

import java.util.List;

public record ClanRankingPage(
        List<ClanRankingEntry> clans,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
