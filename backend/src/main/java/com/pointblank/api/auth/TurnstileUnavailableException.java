package com.pointblank.api.auth;

public class TurnstileUnavailableException extends RuntimeException {

    public TurnstileUnavailableException(Throwable cause) {
        super("O serviço de verificação está indisponível. Tente novamente em instantes.", cause);
    }

    public TurnstileUnavailableException() {
        super("O serviço de verificação está indisponível. Tente novamente em instantes.");
    }
}
