package com.pointblank.api.account;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pointblank.api.auth.BannedAccountException;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountPasswordService accountPasswordService;

    /**
     * Cria o controlador de contas com as dependências necessárias.
     *
     * @param accountRepository repositório usado para consultar contas
     * @param accountPasswordService serviço responsável pela troca de senha
     */
    public AccountController(
            AccountRepository accountRepository,
            AccountPasswordService accountPasswordService) {
        this.accountRepository = accountRepository;
        this.accountPasswordService = accountPasswordService;
    }

    /**
     * Retorna os dados públicos da conta autenticada.
     *
     * @param jwt token JWT validado pelo Spring Security
     * @return dados da conta vinculada ao token
     */
    @GetMapping("/me")
    public AccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        long playerId = playerId(jwt);

        return accountRepository.findByPlayerId(playerId)
                .map(account -> {
                    if (account.banned()) {
                        throw new BannedAccountException();
                    }
                    return AccountResponse.from(account);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));
    }

    /**
     * Altera a senha da conta autenticada após validar a senha atual.
     *
     * @param jwt token JWT validado pelo Spring Security
     * @param request senhas atual, nova e confirmação
     */
    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        accountPasswordService.changePassword(playerId(jwt), request);
    }

    /**
     * Extrai o identificador numérico do jogador do subject do JWT.
     *
     * @param jwt token da requisição autenticada
     * @return identificador do jogador
     */
    private long playerId(Jwt jwt) {
        long playerId;
        try {
            playerId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido", exception);
        }

        return playerId;
    }
}
