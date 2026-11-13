package com.pointblank.api.ranking;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordena a paginação do ranking público de jogadores.
 */
@Service
public class PlayerRankingService {

    private final PlayerRankingRepository playerRankingRepository;

    /**
     * Cria o serviço de ranking de jogadores.
     *
     * @param playerRankingRepository repositório que consulta as estatísticas
     */
    public PlayerRankingService(PlayerRankingRepository playerRankingRepository) {
        this.playerRankingRepository = playerRankingRepository;
    }

    /**
     * Retorna uma página válida do ranking geral de jogadores.
     *
     * @param requestedPage página solicitada, iniciada em zero
     * @param size quantidade de itens por página
     * @param keyword apelido usado para filtrar o ranking
     * @return página de ranking com totais
     */
    @Transactional(readOnly = true)
    public PlayerRankingPage findGeneralRanking(int requestedPage, int size, String keyword) {
        long totalElements = playerRankingRepository.countAll(keyword);
        int totalPages = totalElements == 0 ? 0 : Math.toIntExact((totalElements + size - 1) / size);
        int page = totalPages == 0 ? 0 : Math.min(requestedPage, totalPages - 1);
        List<PlayerRankingEntry> players = totalElements == 0
                ? List.of()
                : playerRankingRepository.findPage(page, size, keyword);

        return new PlayerRankingPage(players, page, size, totalElements, totalPages);
    }

    /**
     * Retorna a entrada pessoal de um jogador elegível no ranking geral.
     *
     * @param playerId identificador do jogador autenticado
     * @return dados da posição pessoal, quando o jogador participa do ranking
     */
    @Transactional(readOnly = true)
    public Optional<PlayerRankingEntry> findCurrentPlayerRanking(long playerId) {
        return playerRankingRepository.findByPlayerId(playerId);
    }
}
