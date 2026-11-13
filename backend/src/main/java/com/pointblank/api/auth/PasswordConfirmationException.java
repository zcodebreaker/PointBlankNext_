package com.pointblank.api.auth;

public class PasswordConfirmationException extends RuntimeException {

    /**
     * Cria o erro usado quando senha e confirmação não coincidem.
     */
    public PasswordConfirmationException() {
        super("As senhas informadas não são iguais");
    }
}
