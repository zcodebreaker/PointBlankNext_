package com.pointblank.api.account;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.pointblank.api.account.GameProfileResponse.PlayerSummary;
import com.pointblank.api.account.GameProfileResponse.WeaponStatistics;

/** Consulta as estatísticas de jogo armazenadas pelo servidor Point Blank. */
@Repository
public class GameProfileRepository {

    private final JdbcClient jdbcClient;

    public GameProfileRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<GameProfileResponse> findByPlayerId(long playerId) {
        Optional<ProfileRow> profile = jdbcClient.sql("""
                SELECT account.player_id,
                       account.login AS username,
                       COALESCE(account.access_level, 0) AS access_level,
                       COALESCE(NULLIF(account.player_name, ''), account.login) AS nickname,
                       COALESCE(account.rank, 0) AS player_rank,
                       COALESCE(account.exp, 0) AS experience,
                       COALESCE(account.fights, 0) AS matches,
                       COALESCE(account.fights_win, 0) AS wins,
                       COALESCE(account.fights_lost, 0) AS losses,
                       COALESCE(account.fights_draw, 0) AS draws,
                       COALESCE(account.kills_count, 0) AS kills,
                       COALESCE(account.assist, 0) AS assists,
                       COALESCE(account.deaths_count, 0) AS deaths,
                       COALESCE(account.headshots_count, 0) AS headshots,
                       COALESCE((
                           SELECT SUM(character.playtime)
                           FROM public.player_characters character
                           WHERE character.player_id = account.player_id
                       ), 0) AS total_playtime,
                       COALESCE((
                           SELECT SUM(daily.playtime)
                           FROM public.player_dailyrecord daily
                           WHERE daily.player_id = account.player_id
                       ), 0) AS match_playtime,
                       COALESCE(account.clan_id, 0) AS clan_id,
                       COALESCE(NULLIF(clan.clan_name, ''), 'Sem clã') AS clan_name,
                       COALESCE(weapon.assault_kill, 0) AS assault_kills,
                       COALESCE(weapon.assault_death, 0) AS assault_deaths,
                       COALESCE(weapon.sub_kill, 0) AS smg_kills,
                       COALESCE(weapon.sub_death, 0) AS smg_deaths,
                       COALESCE(weapon.sniper_kill, 0) AS sniper_kills,
                       COALESCE(weapon.sniper_death, 0) AS sniper_deaths,
                       COALESCE(weapon.gun_kill, 0) AS mg_kills,
                       COALESCE(weapon.gun_death, 0) AS mg_deaths,
                       COALESCE(weapon.shotgun_kill, 0) AS shotgun_kills,
                       COALESCE(weapon.shotgun_death, 0) AS shotgun_deaths
                FROM public.accounts account
                LEFT JOIN public.clan_data clan ON clan.clan_id = account.clan_id
                LEFT JOIN public.weapon_record weapon ON weapon.player_id = account.player_id
                WHERE account.player_id = :playerId
                """)
                .param("playerId", playerId)
                .query((resultSet, rowNum) -> mapProfile(resultSet))
                .optional();

        return profile.map(row -> new GameProfileResponse(
                row.playerId(),
                row.username(),
                row.accessLevel(),
                row.nickname(),
                row.rank(),
                row.experience(),
                row.matches(),
                row.wins(),
                row.losses(),
                row.draws(),
                row.kills(),
                row.assists(),
                row.deaths(),
                row.headshots(),
                row.totalPlaytime(),
                row.matchPlaytime(),
                row.clanName(),
                row.weapons(),
                findFriends(row.playerId()),
                findClanMembers(row.clanId(), row.playerId())));
    }

    private List<PlayerSummary> findFriends(long playerId) {
        return jdbcClient.sql("""
                SELECT friend.player_id,
                       COALESCE(NULLIF(friend.player_name, ''), friend.login) AS nickname,
                       COALESCE(friend.rank, 0) AS player_rank,
                       COALESCE(friend.online, false) AS online
                FROM public.friends relationship
                JOIN public.accounts friend ON friend.player_id = relationship.friend_id
                WHERE relationship.owner_id = :playerId
                  AND COALESCE(relationship.removed, false) = false
                  AND COALESCE(relationship.state, 0) = 0
                ORDER BY friend.online DESC, LOWER(friend.player_name), friend.player_id
                LIMIT 100
                """)
                .param("playerId", playerId)
                .query((resultSet, rowNum) -> mapPlayerSummary(resultSet))
                .list();
    }

    private List<PlayerSummary> findClanMembers(int clanId, long playerId) {
        if (clanId <= 0) return List.of();

        return jdbcClient.sql("""
                SELECT member.player_id,
                       COALESCE(NULLIF(member.player_name, ''), member.login) AS nickname,
                       COALESCE(member.rank, 0) AS player_rank,
                       COALESCE(member.online, false) AS online
                FROM public.accounts member
                WHERE member.clan_id = :clanId
                  AND member.player_id <> :playerId
                ORDER BY member.online DESC, member.exp DESC, member.player_id
                LIMIT 100
                """)
                .param("clanId", clanId)
                .param("playerId", playerId)
                .query((resultSet, rowNum) -> mapPlayerSummary(resultSet))
                .list();
    }

    private ProfileRow mapProfile(ResultSet resultSet) throws SQLException {
        return new ProfileRow(
                resultSet.getLong("player_id"),
                resultSet.getString("username"),
                resultSet.getInt("access_level"),
                resultSet.getString("nickname"),
                resultSet.getInt("player_rank"),
                resultSet.getLong("experience"),
                resultSet.getInt("matches"),
                resultSet.getInt("wins"),
                resultSet.getInt("losses"),
                resultSet.getInt("draws"),
                resultSet.getInt("kills"),
                resultSet.getInt("assists"),
                resultSet.getInt("deaths"),
                resultSet.getInt("headshots"),
                resultSet.getLong("total_playtime"),
                resultSet.getLong("match_playtime"),
                resultSet.getInt("clan_id"),
                resultSet.getString("clan_name"),
                new WeaponStatistics(
                        resultSet.getInt("assault_kills"),
                        resultSet.getInt("assault_deaths"),
                        resultSet.getInt("smg_kills"),
                        resultSet.getInt("smg_deaths"),
                        resultSet.getInt("sniper_kills"),
                        resultSet.getInt("sniper_deaths"),
                        resultSet.getInt("mg_kills"),
                        resultSet.getInt("mg_deaths"),
                        resultSet.getInt("shotgun_kills"),
                        resultSet.getInt("shotgun_deaths")));
    }

    private PlayerSummary mapPlayerSummary(ResultSet resultSet) throws SQLException {
        return new PlayerSummary(
                resultSet.getLong("player_id"),
                resultSet.getString("nickname"),
                resultSet.getInt("player_rank"),
                resultSet.getBoolean("online"));
    }

    private record ProfileRow(
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
            int clanId,
            String clanName,
            WeaponStatistics weapons) {
    }
}
