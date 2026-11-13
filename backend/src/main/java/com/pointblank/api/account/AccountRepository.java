package com.pointblank.api.account;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.pointblank.api.auth.LegacyPasswordHasher;

@Repository
public class AccountRepository {

    private static final String ACCOUNT_COLUMNS = """
            a.player_id,
            a.login AS username,
            COALESCE(a.email, '') AS email,
            COALESCE(NULLIF(a.player_name, ''), '') AS nickname,
            COALESCE(a.rank, 0) AS rank,
            COALESCE(a.exp, 0) AS experience,
            COALESCE(a.gp, 0) AS gold,
            COALESCE(a.money, 0) AS cash,
            COALESCE(a.access_level, 0) AS access_level,
            (COALESCE(a.banned, 0) <> 0) AS banned
            """;

    private final JdbcClient jdbcClient;
    private final LegacyPasswordHasher passwordHasher;

    public AccountRepository(JdbcClient jdbcClient, LegacyPasswordHasher passwordHasher) {
        this.jdbcClient = jdbcClient;
        this.passwordHasher = passwordHasher;
    }

    public boolean usernameExists(String username) {
        return existsBy("login", username);
    }

    public boolean emailExists(String email) {
        return existsBy("email", email);
    }

    private boolean existsBy(String column, String value) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.accounts
                    WHERE LOWER(%s) = LOWER(:value)
                )
                """.formatted(column))
                .param("value", value)
                .query(Boolean.class)
                .single();
    }

    public Account create(String username, String password, String email) {
        long playerId = jdbcClient.sql("""
                INSERT INTO public.accounts (login, password, email)
                VALUES (:username, :passwordHash, :email)
                RETURNING player_id
                """)
                .param("username", username)
                .param("passwordHash", passwordHasher.hash(password))
                .param("email", email)
                .query(Long.class)
                .single();

        return findByPlayerId(playerId)
                .orElseThrow(() -> new IllegalStateException("A conta criada não pôde ser carregada"));
    }

    public Optional<Account> authenticate(String username, String password) {
        return jdbcClient.sql("""
                SELECT %s
                FROM public.accounts a
                WHERE LOWER(a.login) = LOWER(:username)
                  AND a.password = :passwordHash
                """.formatted(ACCOUNT_COLUMNS))
                .param("username", username)
                .param("passwordHash", passwordHasher.hash(password))
                .query((resultSet, rowNum) -> mapAccount(resultSet))
                .optional();
    }

    public boolean passwordMatches(long playerId, String password) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.accounts
                    WHERE player_id = :playerId
                      AND password = :passwordHash
                )
                """)
                .param("playerId", playerId)
                .param("passwordHash", passwordHasher.hash(password))
                .query(Boolean.class)
                .single();
    }

    public Optional<Account> findByPlayerId(long playerId) {
        return jdbcClient.sql("""
                SELECT %s
                FROM public.accounts a
                WHERE a.player_id = :playerId
                """.formatted(ACCOUNT_COLUMNS))
                .param("playerId", playerId)
                .query((resultSet, rowNum) -> mapAccount(resultSet))
                .optional();
    }

    public boolean isBanned(long playerId) {
        return jdbcClient.sql("""
                SELECT COALESCE(banned, 0) <> 0
                FROM public.accounts
                WHERE player_id = :playerId
                """)
                .param("playerId", playerId)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    public List<Account> findAll(String search, int page, int size) {
        String normalizedSearch = normalize(search);
        String searchPattern = "%" + normalizedSearch + "%";
        long offset = (long) page * size;

        return jdbcClient.sql("""
                SELECT %s
                FROM public.accounts a
                WHERE :search = ''
                   OR LOWER(a.login) LIKE :searchPattern
                   OR LOWER(COALESCE(a.email, '')) LIKE :searchPattern
                   OR LOWER(COALESCE(a.player_name, '')) LIKE :searchPattern
                   OR CAST(a.player_id AS VARCHAR) = :search
                ORDER BY a.player_id DESC
                LIMIT :size OFFSET :offset
                """.formatted(ACCOUNT_COLUMNS))
                .param("search", normalizedSearch)
                .param("searchPattern", searchPattern)
                .param("offset", offset)
                .param("size", size)
                .query((resultSet, rowNum) -> mapAccount(resultSet))
                .list();
    }

    public long countAll(String search) {
        String normalizedSearch = normalize(search);
        String searchPattern = "%" + normalizedSearch + "%";

        return jdbcClient.sql("""
                SELECT COUNT(*)
                FROM public.accounts a
                WHERE :search = ''
                   OR LOWER(a.login) LIKE :searchPattern
                   OR LOWER(COALESCE(a.email, '')) LIKE :searchPattern
                   OR LOWER(COALESCE(a.player_name, '')) LIKE :searchPattern
                   OR CAST(a.player_id AS VARCHAR) = :search
                """)
                .param("search", normalizedSearch)
                .param("searchPattern", searchPattern)
                .query(Long.class)
                .single();
    }

    public boolean updateGold(long playerId, int gold) {
        return jdbcClient.sql("""
                UPDATE public.accounts
                SET gp = :gold
                WHERE player_id = :playerId
                """)
                .param("gold", gold)
                .param("playerId", playerId)
                .update() == 1;
    }

    public boolean updateCash(long playerId, int cash) {
        return jdbcClient.sql("""
                UPDATE public.accounts
                SET money = :cash
                WHERE player_id = :playerId
                """)
                .param("cash", cash)
                .param("playerId", playerId)
                .update() == 1;
    }

    public boolean addCurrencies(long playerId, int gold, int cash) {
        return jdbcClient.sql("""
                UPDATE public.accounts
                SET gp = COALESCE(gp, 0) + :gold,
                    money = COALESCE(money, 0) + :cash
                WHERE player_id = :playerId
                  AND COALESCE(gp, 0) <= :maxValue - :gold
                  AND COALESCE(money, 0) <= :maxValue - :cash
                """)
                .param("gold", gold)
                .param("cash", cash)
                .param("maxValue", Integer.MAX_VALUE)
                .param("playerId", playerId)
                .update() == 1;
    }

    public boolean updateBanStatus(long playerId, boolean banned) {
        return jdbcClient.sql("""
                UPDATE public.accounts
                SET banned = :banned
                WHERE player_id = :playerId
                """)
                .param("banned", banned ? 1 : 0)
                .param("playerId", playerId)
                .update() == 1;
    }

    public boolean updatePassword(long playerId, String password) {
        return jdbcClient.sql("""
                UPDATE public.accounts
                SET password = :passwordHash
                WHERE player_id = :playerId
                """)
                .param("passwordHash", passwordHasher.hash(password))
                .param("playerId", playerId)
                .update() == 1;
    }

    private Account mapAccount(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new Account(
                resultSet.getLong("player_id"),
                resultSet.getString("username"),
                resultSet.getString("email"),
                resultSet.getString("nickname"),
                resultSet.getInt("rank"),
                resultSet.getLong("experience"),
                resultSet.getInt("gold"),
                resultSet.getInt("cash"),
                resultSet.getInt("access_level"),
                resultSet.getBoolean("banned"));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
