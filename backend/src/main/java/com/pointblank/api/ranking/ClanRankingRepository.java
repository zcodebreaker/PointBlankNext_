package com.pointblank.api.ranking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ClanRankingRepository {

    private final JdbcClient jdbcClient;

    /**
     * Cria o repositório do ranking de clãs.
     *
     * @param jdbcClient cliente usado para consultar o PostgreSQL
     */
    public ClanRankingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Conta os clãs válidos disponíveis no ranking.
     *
     * @return quantidade total de clãs
     */
    public long countAll() {
        return jdbcClient.sql("""
                SELECT COUNT(*)
                FROM public.clan_data
                WHERE clan_id > 0 AND BTRIM(clan_name) <> ''
                """)
                .query(Long.class)
                .single();
    }

    /**
     * Carrega uma página do ranking geral ordenada pela experiência do clã.
     *
     * @param page índice da página, iniciado em zero
     * @param size quantidade de clãs por página
     * @return clãs da página solicitada
     */
    public List<ClanRankingEntry> findPage(int page, int size) {
        long offset = (long) page * size;

        return jdbcClient.sql("""
                SELECT c.clan_id AS id, c.clan_name AS name, c.clan_rank AS rank,
                       c.clan_exp AS exp, c.logo,
                       c.vitorias AS match_wins, c.derrotas AS match_loses,
                       COALESCE(NULLIF(owner.player_name, ''), owner.login, '') AS leader,
                       (SELECT COUNT(*) FROM public.accounts member WHERE member.clan_id = c.clan_id) AS member_count
                FROM public.clan_data c
                LEFT JOIN public.accounts owner ON owner.player_id = c.owner_id
                WHERE c.clan_id > 0 AND BTRIM(c.clan_name) <> ''
                ORDER BY c.clan_exp DESC, c.vitorias DESC, c.clan_id ASC
                LIMIT :size OFFSET :offset
                """)
                .param("size", size)
                .param("offset", offset)
                .query((resultSet, rowNum) -> mapEntry(resultSet, Math.toIntExact(offset) + rowNum + 1))
                .list();
    }

    /**
     * Converte uma linha do banco em uma posição pública do ranking.
     *
     * @param resultSet resultado posicionado na linha atual
     * @param position posição absoluta do clã no ranking
     * @return entrada pronta para a API
     * @throws SQLException quando uma coluna não puder ser lida
     */
    private ClanRankingEntry mapEntry(ResultSet resultSet, int position) throws SQLException {
        int wins = resultSet.getInt("match_wins");
        int losses = resultSet.getInt("match_loses");
        long decidedMatches = (long) wins + losses;
        int winRate = decidedMatches == 0 ? 0 : (int) ((wins * 100L) / decidedMatches);

        return new ClanRankingEntry(
                position,
                resultSet.getInt("id"),
                resultSet.getString("name"),
                resultSet.getString("leader"),
                resultSet.getInt("member_count"),
                resultSet.getInt("rank"),
                resultSet.getLong("exp"),
                resultSet.getLong("logo"),
                wins,
                losses,
                winRate);
    }
}
