package com.pointblank.api.ranking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Consulta as estatísticas públicas armazenadas na tabela accounts do 3.108.
 */
@Repository
public class PlayerRankingRepository {

    private final JdbcClient jdbcClient;

    /**
     * Cria o repositório de ranking de jogadores.
     *
     * @param jdbcClient cliente usado para consultar o PostgreSQL
     */
    public PlayerRankingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Conta jogadores elegíveis para o ranking público: com apelido, até a
     * patente 51 e sem acesso interno de administrador.
     *
     * @return quantidade de jogadores classificados
     */
    public long countAll(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return jdbcClient.sql("""
                SELECT COUNT(*)
                FROM public.accounts player
                WHERE BTRIM(COALESCE(player.player_name, '')) <> ''
                  AND COALESCE(player.rank, 0) <= 51
                  AND COALESCE(player.access_level, 0) = 0
                  AND (:keyword = '' OR LOWER(player.player_name) LIKE :keywordPattern)
                """)
                .param("keyword", normalizedKeyword)
                .param("keywordPattern", "%" + normalizedKeyword + "%")
                .query(Long.class)
                .single();
    }

    /**
     * Carrega uma página ordenada pela experiência acumulada do jogador.
     *
     * @param page índice da página, iniciado em zero
     * @param size quantidade de jogadores por página
     * @return jogadores encontrados na página solicitada
     */
    public List<PlayerRankingEntry> findPage(int page, int size, String keyword) {
        long offset = (long) page * size;
        String normalizedKeyword = normalizeKeyword(keyword);

        return jdbcClient.sql("""
                SELECT player.player_name AS nickname,
                       COALESCE(player.rank, 0) AS player_rank,
                       COALESCE(player.exp, 0) AS experience,
                       COALESCE(player.fights, 0) AS matches,
                       COALESCE(player.fights_win, 0) AS wins,
                       COALESCE(player.fights_lost, 0) AS losses,
                       COALESCE(player.kills_count, 0) AS kills,
                       COALESCE(player.deaths_count, 0) AS deaths
                FROM public.accounts player
                WHERE BTRIM(COALESCE(player.player_name, '')) <> ''
                  AND COALESCE(player.rank, 0) <= 51
                  AND COALESCE(player.access_level, 0) = 0
                  AND (:keyword = '' OR LOWER(player.player_name) LIKE :keywordPattern)
                ORDER BY player.exp DESC, player.rank DESC, player.player_id ASC
                LIMIT :size OFFSET :offset
                """)
                .param("offset", offset)
                .param("size", size)
                .param("keyword", normalizedKeyword)
                .param("keywordPattern", "%" + normalizedKeyword + "%")
                .query((resultSet, rowNum) -> mapEntry(resultSet, Math.toIntExact(offset) + rowNum + 1))
                .list();
    }

    /**
     * Carrega a posição e os dados de um jogador específico dentro do ranking
     * público. A mesma regra de elegibilidade da listagem é aplicada aqui.
     *
     * @param playerId identificador interno do jogador autenticado
     * @return posição do jogador, quando ele participa do ranking
     */
    public Optional<PlayerRankingEntry> findByPlayerId(long playerId) {
        return jdbcClient.sql("""
                WITH ranked_players AS (
                    SELECT player.player_id,
                           ROW_NUMBER() OVER (
                               ORDER BY player.exp DESC, player.rank DESC, player.player_id ASC
                           ) AS ranking_position,
                           player.player_name AS nickname,
                           COALESCE(player.rank, 0) AS player_rank,
                           COALESCE(player.exp, 0) AS experience,
                           COALESCE(player.fights, 0) AS matches,
                           COALESCE(player.fights_win, 0) AS wins,
                           COALESCE(player.fights_lost, 0) AS losses,
                           COALESCE(player.kills_count, 0) AS kills,
                           COALESCE(player.deaths_count, 0) AS deaths
                    FROM public.accounts player
                    WHERE BTRIM(COALESCE(player.player_name, '')) <> ''
                      AND COALESCE(player.rank, 0) <= 51
                      AND COALESCE(player.access_level, 0) = 0
                )
                SELECT nickname, player_rank, experience, matches, wins, losses, kills, deaths,
                       ranking_position
                FROM ranked_players
                WHERE player_id = :playerId
                """)
                .param("playerId", playerId)
                .query((resultSet, rowNum) -> mapEntry(resultSet, resultSet.getInt("ranking_position")))
                .optional();
    }

    /**
     * Converte uma linha do banco em uma posição pública do ranking.
     *
     * @param resultSet resultado posicionado na linha atual
     * @param position posição absoluta do jogador no ranking
     * @return entrada pronta para a API
     * @throws SQLException quando uma coluna não puder ser lida
     */
    private PlayerRankingEntry mapEntry(ResultSet resultSet, int position) throws SQLException {
        int wins = resultSet.getInt("wins");
        int losses = resultSet.getInt("losses");
        long decidedMatches = (long) wins + losses;
        int winRate = decidedMatches == 0 ? 0 : (int) ((wins * 100L) / decidedMatches);

        return new PlayerRankingEntry(
                position,
                resultSet.getString("nickname"),
                resultSet.getInt("player_rank"),
                resultSet.getLong("experience"),
                resultSet.getInt("matches"),
                wins,
                losses,
                resultSet.getInt("kills"),
                resultSet.getInt("deaths"),
                winRate);
    }

    /**
     * Normaliza a consulta de apelido antes do uso como parâmetro SQL.
     *
     * @param keyword texto informado na busca
     * @return texto em minúsculas, limitado ao tamanho aceito pela tela
     */
    private String normalizeKeyword(String keyword) {
        String value = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return value.length() > 32 ? value.substring(0, 32) : value;
    }
}
