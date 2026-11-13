package com.pointblank.api.auth;

public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(Throwable cause) {
        super("Não foi possível enviar o e-mail de ativação. Tente novamente em instantes.", cause);
    }
}
