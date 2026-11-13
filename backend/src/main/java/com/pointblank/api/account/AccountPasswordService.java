package com.pointblank.api.account;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.pointblank.api.auth.BannedAccountException;

@Service
public class AccountPasswordService {

    private final AccountRepository accountRepository;

    /**
     * Cria o serviço de senha com acesso às contas persistidas.
     *
     * @param accountRepository repositório de contas
     */
    public AccountPasswordService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Valida e substitui a senha de uma conta.
     *
     * @param playerId identificador da conta
     * @param request dados necessários para confirmar e trocar a senha
     */
    @Transactional
    public void changePassword(long playerId, ChangePasswordRequest request) {
        if (!request.newPassword().equals(request.newPasswordConfirmation())) {
            throw new PasswordChangeException(
                    "newPasswordConfirmation",
                    "As novas senhas informadas não são iguais");
        }

        if (accountRepository.isBanned(playerId)) {
            throw new BannedAccountException();
        }

        if (!accountRepository.passwordMatches(playerId, request.currentPassword())) {
            throw new PasswordChangeException("currentPassword", "A senha atual está incorreta");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new PasswordChangeException(
                    "newPassword",
                    "A nova senha deve ser diferente da senha atual");
        }

        if (!accountRepository.updatePassword(playerId, request.newPassword())) {
            throw accountNotFound();
        }
    }

    /**
     * Cria a exceção padronizada para uma conta inexistente.
     *
     * @return exceção HTTP com status 404
     */
    private static ResponseStatusException accountNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada");
    }
}
