package com.pointblank.api.auth;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Confirme seu e-mail antes de entrar. Verifique a caixa de entrada e a pasta de spam.");
    }
}
