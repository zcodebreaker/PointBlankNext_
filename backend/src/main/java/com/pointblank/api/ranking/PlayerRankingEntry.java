package com.pointblank.api.ranking;

/**
 * Representa um jogador exibido no ranking público.
 *
 * @param position posição absoluta no ranking
 * @param nickname apelido público do jogador
 * @param rank patente atual
 * @param experience experiência acumulada
 * @param matches total de partidas registradas
 * @param wins total de vitórias registradas
 * @param losses total de derrotas registradas
 * @param kills total de eliminações registradas
 * @param deaths total de mortes registradas
 * @param winRate percentual de vitórias em partidas decididas
 */
public record PlayerRankingEntry(
        int position,
        String nickname,
        int rank,
        long experience,
        int matches,
        int wins,
        int losses,
        int kills,
        int deaths,
        int winRate) {
}
