package com.pointblank.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;
import com.pointblank.api.auth.EmailVerificationRepository.VerificationRecord;
import com.pointblank.api.auth.EmailVerificationTokenService.IssuedVerificationToken;
import com.pointblank.api.config.AppProperties;
import com.pointblank.api.security.JwtService;
import com.pointblank.api.security.JwtService.IssuedToken;

class AuthServiceTests {

    private AccountRepository accountRepository;
    private JwtService jwtService;
    private TurnstileService turnstileService;
    private EmailVerificationRepository verificationRepository;
    private EmailVerificationTokenService verificationTokenService;
    private VerificationEmailService verificationEmailService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        jwtService = mock(JwtService.class);
        turnstileService = mock(TurnstileService.class);
        verificationRepository = mock(EmailVerificationRepository.class);
        verificationTokenService = mock(EmailVerificationTokenService.class);
        verificationEmailService = mock(VerificationEmailService.class);
        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("01234567890123456789012345678901", "pointblank-api", Duration.ofHours(2), Duration.ofDays(30)),
                new AppProperties.Cors("http://localhost:3000"),
                new AppProperties.EmailVerification("http://localhost:3000", "noreply@kryonixnetwork.com", Duration.ofHours(24)));
        authService = new AuthService(
                accountRepository,
                jwtService,
                turnstileService,
                verificationRepository,
                verificationTokenService,
                verificationEmailService,
                properties);
    }

    @Test
    void registersPendingAccountAndSendsActivationEmail() {
        Account account = account();
        RegisterRequest request = new RegisterRequest(
                "player_1", "PLAYER@EXAMPLE.COM",
                "senha-segura", "senha-segura", "captcha-token", true);

        when(accountRepository.create("player_1", "senha-segura", "player@example.com"))
                .thenReturn(account);
        when(verificationTokenService.issue())
                .thenReturn(new IssuedVerificationToken("raw-token", "token-hash"));

        RegistrationResponse response = authService.register(request);

        assertThat(response.message()).contains("link de ativação");
        verify(accountRepository).create("player_1", "senha-segura", "player@example.com");
        verify(verificationRepository).createPending(
                org.mockito.ArgumentMatchers.eq(35L),
                org.mockito.ArgumentMatchers.eq("token-hash"),
                org.mockito.ArgumentMatchers.any(Instant.class));
        verify(verificationEmailService).sendActivation("player@example.com", "player_1", "raw-token");
        verify(turnstileService).verify("captcha-token", "register");
    }

    @Test
    void rejectsDifferentPasswordConfirmation() {
        RegisterRequest request = new RegisterRequest(
                "player_1", "player@example.com",
                "senha-segura", "senha-diferente", "captcha-token", true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(PasswordConfirmationException.class);
    }

    @Test
    void rejectsInvalidCredentialsWithoutRevealingWhichFieldFailed() {
        when(accountRepository.authenticate("player_1", "errada"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("player_1", "errada", "captcha-token", false)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Usuário ou senha inválidos");
    }

    @Test
    void rejectsLoginForBannedAccountAfterValidatingPassword() {
        Account bannedAccount = new Account(
                36L, "banned_player", "banned@example.com", "Banido",
                0, 0, 1000, 500, 0, true);
        when(accountRepository.authenticate("banned_player", "senha-correta"))
                .thenReturn(java.util.Optional.of(bannedAccount));

        assertThatThrownBy(() -> authService.login(new LoginRequest("banned_player", "senha-correta", "captcha-token", false)))
                .isInstanceOf(BannedAccountException.class)
                .hasMessage("Esta conta está banida");
    }

    @Test
    void rejectsLoginUntilEmailIsVerified() {
        Account account = account();
        when(accountRepository.authenticate("player_1", "senha-correta"))
                .thenReturn(java.util.Optional.of(account));
        when(verificationRepository.requiresVerification(account.playerId())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("player_1", "senha-correta", "captcha-token", false)))
                .isInstanceOf(EmailNotVerifiedException.class)
                .hasMessageContaining("Confirme seu e-mail");
    }

    @Test
    void verifiesEmailUsingHashedToken() {
        Instant future = Instant.now().plusSeconds(3600);
        when(verificationTokenService.hash("raw-token")).thenReturn("token-hash");
        when(verificationRepository.findByTokenHash("token-hash"))
                .thenReturn(java.util.Optional.of(new VerificationRecord(35L, future, null)));
        when(verificationRepository.markVerified(
                org.mockito.ArgumentMatchers.eq("token-hash"),
                org.mockito.ArgumentMatchers.any(Instant.class)))
                .thenReturn(true);

        EmailVerificationResponse response = authService.verifyEmail("raw-token");

        assertThat(response.verified()).isTrue();
        assertThat(response.message()).contains("conta está ativa");
    }

    @Test
    void rejectsExpiredEmailVerificationTokenAsUnavailable() {
        Instant expired = Instant.now().minusSeconds(1);
        when(verificationTokenService.hash("expired-token")).thenReturn("expired-token-hash");
        when(verificationRepository.findByTokenHash("expired-token-hash"))
                .thenReturn(java.util.Optional.of(new VerificationRecord(35L, expired, null)));

        assertThatThrownBy(() -> authService.verifyEmail("expired-token"))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessage("O link de ativação é inválido ou já não está disponível.");

        verify(verificationRepository, org.mockito.Mockito.never())
                .markVerified(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(Instant.class));
    }

    @Test
    void rejectsAlreadyUsedEmailVerificationTokenAsUnavailable() {
        when(verificationTokenService.hash("used-token")).thenReturn("used-token-hash");
        when(verificationRepository.findByTokenHash("used-token-hash"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.verifyEmail("used-token"))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessage("O link de ativação é inválido ou já não está disponível.");
    }

    @Test
    void rejectsLegacyVerifiedEmailTokenAsUnavailable() {
        Instant future = Instant.now().plusSeconds(3600);
        Instant verifiedAt = Instant.now().minusSeconds(10);
        when(verificationTokenService.hash("legacy-used-token")).thenReturn("legacy-used-token-hash");
        when(verificationRepository.findByTokenHash("legacy-used-token-hash"))
                .thenReturn(java.util.Optional.of(new VerificationRecord(35L, future, verifiedAt)));

        assertThatThrownBy(() -> authService.verifyEmail("legacy-used-token"))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessage("O link de ativação é inválido ou já não está disponível.");
    }

    @Test
    void reportsAvailableUsernameAfterNormalizingIt() {
        when(accountRepository.usernameExists("new_player")).thenReturn(false);

        UsernameAvailabilityResponse response = authService.usernameAvailability("  new_player  ");

        assertThat(response.username()).isEqualTo("new_player");
        assertThat(response.available()).isTrue();
        verify(accountRepository).usernameExists("new_player");
    }

    @Test
    void reportsUnavailableUsernameIgnoringCaseInTheRepository() {
        when(accountRepository.usernameExists("PLAYER_1")).thenReturn(true);

        UsernameAvailabilityResponse response = authService.usernameAvailability("PLAYER_1");

        assertThat(response.available()).isFalse();
        verify(accountRepository).usernameExists("PLAYER_1");
    }

    @Test
    void reportsAvailableEmailAfterNormalizingIt() {
        when(accountRepository.emailExists("new.player@example.com")).thenReturn(false);

        EmailAvailabilityResponse response = authService.emailAvailability("  New.Player@Example.COM  ");

        assertThat(response.email()).isEqualTo("new.player@example.com");
        assertThat(response.available()).isTrue();
        verify(accountRepository).emailExists("new.player@example.com");
    }

    @Test
    void reportsUnavailableEmail() {
        when(accountRepository.emailExists("player@example.com")).thenReturn(true);

        EmailAvailabilityResponse response = authService.emailAvailability("player@example.com");

        assertThat(response.available()).isFalse();
        verify(accountRepository).emailExists("player@example.com");
    }

    private Account account() {
        return new Account(35L, "player_1", "player@example.com", "Soldado1", 0, 0, 100000, 80000, 0, false);
    }
}
