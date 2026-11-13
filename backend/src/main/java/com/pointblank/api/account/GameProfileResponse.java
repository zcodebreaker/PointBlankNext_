package com.pointblank.api.account;

import java.util.List;

/** Dados de jogo da conta autenticada exibidos no perfil do site. */
public record GameProfileResponse(
        long playerId,
        String username,
        int accessLevel,
        String nickname,
        int rank,
        long experience,
        int matches,
        int wins,
        int losses,
        int draws,
        int kills,
        int assists,
        int deaths,
        int headshots,
        long totalPlaytime,
        long matchPlaytime,
        String clanName,
        WeaponStatistics weapons,
        List<PlayerSummary> friends,
        List<PlayerSummary> clanMembers) {

    /** Estatísticas de eliminações e mortes agrupadas por categoria de arma. */
    public record WeaponStatistics(
            int assaultKills,
            int assaultDeaths,
            int smgKills,
            int smgDeaths,
            int sniperKills,
            int sniperDeaths,
            int mgKills,
            int mgDeaths,
            int shotgunKills,
            int shotgunDeaths) {
    }

    /** Resumo seguro de outro jogador relacionado à conta. */
    public record PlayerSummary(long playerId, String nickname, int rank, boolean online) {
    }
}
