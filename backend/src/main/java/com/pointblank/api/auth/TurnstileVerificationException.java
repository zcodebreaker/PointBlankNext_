package com.pointblank.api.auth;

public class TurnstileVerificationException extends RuntimeException {

    public TurnstileVerificationException() {
        super("Não foi possível validar o CAPTCHA. Conclua a verificação novamente.");
    }
}
