package com.pointblank.api.ranking;

import java.util.List;

/**
 * Agrupa uma página paginada do ranking público de jogadores.
 *
 * @param players jogadores retornados para a página atual
 * @param page índice da página, iniciado em zero
 * @param size tamanho solicitado para a página
 * @param totalElements quantidade total de jogadores classificados
 * @param totalPages quantidade total de páginas disponíveis
 */
public record PlayerRankingPage(
        List<PlayerRankingEntry> players,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
