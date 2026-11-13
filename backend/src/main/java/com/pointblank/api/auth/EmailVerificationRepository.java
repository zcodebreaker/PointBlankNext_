package com.pointblank.api.auth;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EmailVerificationRepository {

    private final JdbcClient jdbcClient;

    public EmailVerificationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void createPending(long playerId, String tokenHash, Instant expiresAt) {
        jdbcClient.sql("""
                INSERT INTO public.account_email_verifications
                    (player_id, token_hash, expires_at)
                VALUES (:playerId, :tokenHash, :expiresAt)
                """)
                .param("playerId", playerId)
                .param("tokenHash", tokenHash)
                .param("expiresAt", toDatabaseTimestamp(expiresAt))
                .update();
    }

    public boolean requiresVerification(long playerId) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                    FROM public.account_email_verifications
                    WHERE player_id = :playerId
                      AND verified_at IS NULL
                )
                """)
                .param("playerId", playerId)
                .query(Boolean.class)
                .single();
    }

    public Optional<VerificationRecord> findByTokenHash(String tokenHash) {
        return jdbcClient.sql("""
                SELECT player_id, expires_at, verified_at
                FROM public.account_email_verifications
                WHERE token_hash = :tokenHash
                """)
                .param("tokenHash", tokenHash)
                .query((resultSet, rowNumber) -> {
                    OffsetDateTime expiresAt = resultSet.getObject("expires_at", OffsetDateTime.class);
                    OffsetDateTime verifiedAt = resultSet.getObject("verified_at", OffsetDateTime.class);
                    return new VerificationRecord(
                            resultSet.getLong("player_id"),
                            expiresAt.toInstant(),
                            verifiedAt == null ? null : verifiedAt.toInstant());
                })
                .optional();
    }

    public boolean markVerified(String tokenHash, Instant verifiedAt) {
        return jdbcClient.sql("""
                UPDATE public.account_email_verifications
                SET verified_at = :verifiedAt,
                    token_hash = NULL
                WHERE token_hash = :tokenHash
                  AND verified_at IS NULL
                  AND expires_at >= :verifiedAt
                """)
                .param("tokenHash", tokenHash)
                .param("verifiedAt", toDatabaseTimestamp(verifiedAt))
                .update() == 1;
    }

    private static OffsetDateTime toDatabaseTimestamp(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    public record VerificationRecord(long playerId, Instant expiresAt, Instant verifiedAt) {
        public boolean verified() {
            return verifiedAt != null;
        }
    }
}
