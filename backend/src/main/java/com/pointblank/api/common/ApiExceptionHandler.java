package com.pointblank.api.common;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pointblank.api.auth.DuplicateAccountException;
import com.pointblank.api.auth.PasswordConfirmationException;
import com.pointblank.api.auth.TurnstileUnavailableException;
import com.pointblank.api.auth.TurnstileVerificationException;
import com.pointblank.api.account.PasswordChangeException;
import com.pointblank.api.admin.AdminAccountException;
import com.pointblank.api.auth.BannedAccountException;
import com.pointblank.api.auth.EmailDeliveryException;
import com.pointblank.api.auth.EmailNotVerifiedException;
import com.pointblank.api.auth.EmailVerificationException;
import com.pointblank.api.ticket.TicketException;
import com.pointblank.api.news.NewsException;
import com.pointblank.api.webstore.WebstoreException;
import com.pointblank.api.download.DownloadSettingsException;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Converte falhas de validação dos DTOs em erros associados aos campos.
     *
     * @param exception erro produzido pela validação da requisição
     * @return resposta padronizada com os campos inválidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                "Revise os campos enviados e tente novamente.",
                "validation-error");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Trata divergências entre a senha e sua confirmação no cadastro.
     *
     * @param exception erro de confirmação de senha
     * @return resposta padronizada de dados inválidos
     */
    @ExceptionHandler(PasswordConfirmationException.class)
    ResponseEntity<ProblemDetail> handlePasswordConfirmation(PasswordConfirmationException exception) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                exception.getMessage(),
                "password-confirmation-error");
        problem.setProperty("errors", Map.of("passwordConfirmation", exception.getMessage()));
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Trata falhas de validação durante a troca de senha.
     *
     * @param exception erro com o campo responsável pela falha
     * @return resposta padronizada de alteração de senha
     */
    @ExceptionHandler(PasswordChangeException.class)
    ResponseEntity<ProblemDetail> handlePasswordChange(PasswordChangeException exception) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Não foi possível alterar a senha",
                exception.getMessage(),
                "password-change-error");
        problem.setProperty("errors", Map.of(exception.field(), exception.getMessage()));
        return ResponseEntity.badRequest().body(problem);
    }

    /**
     * Trata conflitos causados por dados únicos já cadastrados.
     *
     * @param exception conflito entre os dados de contas
     * @return resposta HTTP 409 padronizada
     */
    @ExceptionHandler(DuplicateAccountException.class)
    ResponseEntity<ProblemDetail> handleConflict(DuplicateAccountException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem(
                HttpStatus.CONFLICT,
                "Conta já existente",
                exception.getMessage(),
                "account-conflict"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem(
                HttpStatus.CONFLICT,
                "Conflito nos dados",
                "Não foi possível concluir a operação porque os dados conflitam com registros ou regras do banco de dados.",
                "data-integrity-conflict"));
    }

    /**
     * Trata tentativas de login com credenciais inválidas.
     *
     * @param exception falha de autenticação
     * @return resposta HTTP 401 padronizada
     */
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem(
                HttpStatus.UNAUTHORIZED,
                "Falha na autenticação",
                exception.getMessage(),
                "invalid-credentials"));
    }

    @ExceptionHandler(TurnstileVerificationException.class)
    ResponseEntity<ProblemDetail> handleTurnstileVerification(TurnstileVerificationException exception) {
        return ResponseEntity.badRequest().body(problem(
                HttpStatus.BAD_REQUEST,
                "Verificação de segurança recusada",
                exception.getMessage(),
                "turnstile-verification"));
    }

    @ExceptionHandler(TurnstileUnavailableException.class)
    ResponseEntity<ProblemDetail> handleTurnstileUnavailable(TurnstileUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Verificação de segurança indisponível",
                exception.getMessage(),
                "turnstile-unavailable"));
    }

    /**
     * Trata operações realizadas por uma conta banida.
     *
     * @param exception erro de conta banida
     * @return resposta HTTP 403 padronizada
     */
    @ExceptionHandler(BannedAccountException.class)
    ResponseEntity<ProblemDetail> handleBannedAccount(BannedAccountException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem(
                HttpStatus.FORBIDDEN,
                "Conta banida",
                exception.getMessage(),
                "account-banned"));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    ResponseEntity<ProblemDetail> handleEmailNotVerified(EmailNotVerifiedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem(
                HttpStatus.FORBIDDEN,
                "E-mail ainda não confirmado",
                exception.getMessage(),
                "email-not-verified"));
    }

    @ExceptionHandler(EmailVerificationException.class)
    ResponseEntity<ProblemDetail> handleEmailVerification(EmailVerificationException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem(
                HttpStatus.NOT_FOUND,
                "Link de ativação indisponível",
                exception.getMessage(),
                "email-verification-token-not-found"));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    ResponseEntity<ProblemDetail> handleEmailDelivery(EmailDeliveryException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "E-mail indisponível",
                exception.getMessage(),
                "email-delivery"));
    }

    /**
     * Trata erros gerados pelo gerenciamento administrativo de contas.
     *
     * @param exception erro com status definido pelo domínio
     * @return resposta administrativa padronizada
     */
    @ExceptionHandler(AdminAccountException.class)
    ResponseEntity<ProblemDetail> handleAdminAccount(AdminAccountException exception) {
        return ResponseEntity.status(exception.status()).body(problem(
                exception.status(),
                "Operação administrativa recusada",
                exception.getMessage(),
                "admin-account-operation"));
    }

    /**
     * Trata erros produzidos pelas operações de suporte.
     *
     * @param exception erro de ticket com status definido pelo domínio
     * @return resposta de ticket padronizada
     */
    @ExceptionHandler(TicketException.class)
    ResponseEntity<ProblemDetail> handleTicket(TicketException exception) {
        return ResponseEntity.status(exception.status()).body(problem(
                exception.status(),
                "Operação de ticket recusada",
                exception.getMessage(),
                "ticket-operation"));
    }

    /**
     * Trata erros produzidos pela publicação e consulta de notícias.
     *
     * @param exception erro de notícia com status definido pelo domínio
     * @return resposta de notícia padronizada
     */
    @ExceptionHandler(NewsException.class)
    ResponseEntity<ProblemDetail> handleNews(NewsException exception) {
        return ResponseEntity.status(exception.status()).body(problem(
                exception.status(),
                "Operação de notícia recusada",
                exception.getMessage(),
                "news-operation"));
    }

    @ExceptionHandler(WebstoreException.class)
    ResponseEntity<ProblemDetail> handleWebstore(WebstoreException exception) {
        return ResponseEntity.status(exception.status()).body(problem(
                exception.status(),
                "Operação da loja recusada",
                exception.getMessage(),
                "webstore-operation"));
    }

    @ExceptionHandler(DownloadSettingsException.class)
    ResponseEntity<ProblemDetail> handleDownloadSettings(DownloadSettingsException exception) {
        return ResponseEntity.status(exception.status()).body(problem(
                exception.status(),
                "Configuração de download recusada",
                exception.getMessage(),
                "download-settings-operation"));
    }

    /**
     * Monta o corpo comum usado nas respostas de erro da API.
     *
     * @param status status HTTP do problema
     * @param title título legível do problema
     * @param detail descrição específica do erro
     * @param type identificador anexado à URI do tipo do problema
     * @return corpo no formato Problem Details
     */
    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://pointblank.local/problems/" + type));
        return problem;
    }
}
