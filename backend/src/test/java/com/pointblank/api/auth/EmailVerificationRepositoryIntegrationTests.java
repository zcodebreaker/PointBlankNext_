package com.pointblank.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class EmailVerificationRepositoryIntegrationTests {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EmailVerificationRepository verificationRepository;

    @Test
    void persistsReadsAndConfirmsPostgresTimestamps() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Account account = accountRepository.create(
                "verify_" + suffix,
                "senha-segura",
                "verify-" + suffix + "@example.invalid");
        String tokenHash = "a".repeat(64);
        Instant expiresAt = Instant.now().plusSeconds(3600).truncatedTo(ChronoUnit.MICROS);

        verificationRepository.createPending(account.playerId(), tokenHash, expiresAt);

        EmailVerificationRepository.VerificationRecord saved = verificationRepository
                .findByTokenHash(tokenHash)
                .orElseThrow();
        assertThat(saved.expiresAt()).isEqualTo(expiresAt);
        assertThat(verificationRepository.requiresVerification(account.playerId())).isTrue();

        assertThat(verificationRepository.markVerified(tokenHash, Instant.now())).isTrue();
        assertThat(verificationRepository.requiresVerification(account.playerId())).isFalse();
        assertThat(verificationRepository.findByTokenHash(tokenHash)).isEmpty();
    }
}
