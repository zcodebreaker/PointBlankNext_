package com.pointblank.api.auth;

import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;
import com.pointblank.api.account.AccountResponse;
import com.pointblank.api.config.AppProperties;
import com.pointblank.api.security.JwtService;
import com.pointblank.api.security.JwtService.IssuedToken;
import com.pointblank.api.auth.EmailVerificationRepository.VerificationRecord;
import com.pointblank.api.auth.EmailVerificationTokenService.IssuedVerificationToken;

@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,16}$");

    private final AccountRepository accountRepository;
    private final JwtService jwtService;
    private final TurnstileService turnstileService;
    private final EmailVerificationRepository verificationRepository;
    private final EmailVerificationTokenService verificationTokenService;
    private final VerificationEmailService verificationEmailService;
    private final AppProperties properties;

    /**
     * Cria o serviço de autenticação com suas dependências.
     *
     * @param accountRepository repositório de contas
     * @param jwtService serviço emissor de tokens
     */
    public AuthService(
            AccountRepository accountRepository,
            JwtService jwtService,
            TurnstileService turnstileService,
            EmailVerificationRepository verificationRepository,
            EmailVerificationTokenService verificationTokenService,
            VerificationEmailService verificationEmailService,
            AppProperties properties) {
        this.accountRepository = accountRepository;
        this.jwtService = jwtService;
        this.turnstileService = turnstileService;
        this.verificationRepository = verificationRepository;
        this.verificationTokenService = verificationTokenService;
        this.verificationEmailService = verificationEmailService;
        this.properties = properties;
    }

    /**
     * Valida os dados, cria a conta pendente e envia o link de ativação.
     *
     * @param request dados de cadastro
     * @return instrução para confirmar o cadastro por e-mail
     */
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        turnstileService.verify(request.turnstileToken(), "register");

        if (!request.password().equals(request.passwordConfirmation())) {
            throw new PasswordConfirmationException();
        }

        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);

        if (accountRepository.usernameExists(username)) {
            throw new DuplicateAccountException("Este nome de usuário já está em uso");
        }
        if (accountRepository.emailExists(email)) {
            throw new DuplicateAccountException("Este e-mail já está em uso");
        }
        Account account = accountRepository.create(username, request.password(), email);
        IssuedVerificationToken token = verificationTokenService.issue();
        Instant expiresAt = Instant.now().plus(properties.emailVerification().tokenTtl());
        verificationRepository.createPending(account.playerId(), token.hash(), expiresAt);
        verificationEmailService.sendActivation(email, username, token.value());

        return new RegistrationResponse(
                "Conta criada. Enviamos um link de ativação para o e-mail informado.");
    }

    /**
     * Valida as credenciais e emite um token para uma conta ativa.
     *
     * @param request credenciais de acesso
     * @return resposta autenticada
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        turnstileService.verify(request.turnstileToken(), "login");

        Account account = accountRepository
                .authenticate(request.username().trim(), request.password())
                .orElseThrow(AuthService::badCredentials);

        if (account.banned()) {
            throw new BannedAccountException();
        }
        if (verificationRepository.requiresVerification(account.playerId())) {
            throw new EmailNotVerifiedException();
        }

        return createResponse(account, request.remember());
    }

    /**
     * Confirma o endereço de e-mail associado a uma nova conta.
     *
     * @param token token recebido no link de ativação
     * @return resultado da ativação
     */
    @Transactional
    public EmailVerificationResponse verifyEmail(String token) {
        String candidate = token == null ? "" : token.trim();
        if (candidate.isEmpty() || candidate.length() > 512) {
            throw invalidVerificationLink();
        }

        String tokenHash = verificationTokenService.hash(candidate);
        VerificationRecord verification = verificationRepository.findByTokenHash(tokenHash)
                .orElseThrow(AuthService::invalidVerificationLink);

        Instant now = Instant.now();
        if (verification.verified() || verification.expiresAt().isBefore(now)) {
            throw invalidVerificationLink();
        }

        if (!verificationRepository.markVerified(tokenHash, now)) {
            throw invalidVerificationLink();
        }

        return verifiedResponse();
    }

    /**
     * Valida o formato e consulta a disponibilidade de um nome de usuário.
     *
     * @param candidate nome de usuário candidato
     * @return resultado da consulta de disponibilidade
     */
    @Transactional(readOnly = true)
    public UsernameAvailabilityResponse usernameAvailability(String candidate) {
        String username = candidate.trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O usuário deve ter de 4 a 16 caracteres e usar apenas letras, números ou _");
        }

        return new UsernameAvailabilityResponse(username, !accountRepository.usernameExists(username));
    }

    /**
     * Valida o formato e consulta a disponibilidade de um e-mail.
     *
     * @param candidate e-mail candidato
     * @return resultado da consulta de disponibilidade
     */
    @Transactional(readOnly = true)
    public EmailAvailabilityResponse emailAvailability(String candidate) {
        String email = candidate.trim().toLowerCase(java.util.Locale.ROOT);
        if (!isValidEmailCandidate(email)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe um e-mail válido com no máximo 255 caracteres");
        }

        return new EmailAvailabilityResponse(email, !accountRepository.emailExists(email));
    }

    /**
     * Monta a resposta de autenticação e define a duração do token.
     *
     * @param account conta autenticada
     * @param remember indica se a sessão deve durar mais tempo
     * @return token e dados públicos da conta
     */
    private AuthResponse createResponse(Account account, boolean remember) {
        IssuedToken token = jwtService.issue(account, remember);
        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresIn(),
                AccountResponse.from(account));
    }

    /**
     * Cria a exceção padronizada para credenciais inválidas.
     *
     * @return exceção de autenticação
     */
    private static BadCredentialsException badCredentials() {
        return new BadCredentialsException("Usuário ou senha inválidos");
    }

    private static EmailVerificationException invalidVerificationLink() {
        return new EmailVerificationException("O link de ativação é inválido ou já não está disponível.");
    }

    private static EmailVerificationResponse verifiedResponse() {
        return new EmailVerificationResponse(
                true,
                "E-mail confirmado. Sua conta está ativa e você já pode entrar.");
    }

    /**
     * Faz uma validação leve do e-mail usado na consulta de disponibilidade.
     *
     * @param email e-mail normalizado
     * @return {@code true} quando o formato básico é válido
     */
    private static boolean isValidEmailCandidate(String email) {
        int atIndex = email.indexOf('@');
        return email.length() <= 255
                && atIndex > 0
                && atIndex == email.lastIndexOf('@')
                && atIndex < email.length() - 1
                && email.chars().noneMatch(Character::isWhitespace);
    }
}
