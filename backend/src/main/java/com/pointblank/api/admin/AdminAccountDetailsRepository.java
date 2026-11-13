package com.pointblank.api.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AdminAccountDetailsRepository {

    private static final String ACCOUNT_DETAILS_COLUMNS = """
            account.player_id,
            account.login AS username,
            COALESCE(account.email, '') AS email,
            COALESCE(NULLIF(account.player_name, ''), '') AS nickname,
            COALESCE(account.rank, 0) AS rank,
            COALESCE(account.exp, 0) AS experience,
            COALESCE(account.gp, 0) AS gold,
            COALESCE(account.money, 0) AS cash,
            COALESCE(account.access_level, 0) AS access_level,
            (COALESCE(account.banned, 0) <> 0) AS banned,
            COALESCE(account.online, false) AS online,
            NULLIF(account.last_login, 0) AS last_login,
            COALESCE(account.fights, 0) AS matches,
            COALESCE(account.fights_win, 0) AS wins,
            COALESCE(account.fights_lost, 0) AS losses,
            COALESCE(account.fights_draw, 0) AS draws,
            COALESCE(account.kills_count, 0) AS kills,
            COALESCE(account.deaths_count, 0) AS deaths,
            COALESCE(account.assist, 0) AS assists,
            COALESCE(account.headshots_count, 0) AS headshots
            """;

    private final JdbcClient jdbcClient;

    public AdminAccountDetailsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<AdminAccountDetailsResponse> findByPlayerId(long playerId) {
        return jdbcClient.sql("""
                SELECT %s
                FROM public.accounts account
                WHERE account.player_id = :playerId
                """.formatted(ACCOUNT_DETAILS_COLUMNS))
                .param("playerId", playerId)
                .query((resultSet, rowNum) -> mapDetails(resultSet))
                .optional();
    }

    public Optional<AdminAccountDetailsResponse> findByUsername(String username) {
        return jdbcClient.sql("""
                SELECT %s
                FROM public.accounts account
                WHERE LOWER(account.login) = LOWER(:username)
                """.formatted(ACCOUNT_DETAILS_COLUMNS))
                .param("username", username)
                .query((resultSet, rowNum) -> mapDetails(resultSet))
                .optional();
    }

    private AdminAccountDetailsResponse mapDetails(ResultSet resultSet) throws SQLException {
        return new AdminAccountDetailsResponse(
                resultSet.getLong("player_id"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("nickname"),
                resultSet.getInt("rank"),
                resultSet.getLong("experience"),
                resultSet.getInt("gold"),
                resultSet.getInt("cash"),
                resultSet.getInt("access_level"),
                resultSet.getBoolean("banned"),
                resultSet.getBoolean("online"),
                resultSet.getObject("last_login", Long.class),
                resultSet.getInt("matches"),
                resultSet.getInt("wins"),
                resultSet.getInt("losses"),
                resultSet.getInt("draws"),
                resultSet.getInt("kills"),
                resultSet.getInt("deaths"),
                resultSet.getInt("assists"),
                resultSet.getInt("headshots"));
    }
}
