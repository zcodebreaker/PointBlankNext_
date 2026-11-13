package com.pointblank.api.auth;

public class BannedAccountException extends RuntimeException {

    /**
     * Cria o erro retornado quando uma conta banida tenta acessar a API.
     */
    public BannedAccountException() {
        super("Esta conta está banida");
    }
}
