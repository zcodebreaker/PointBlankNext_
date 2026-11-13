package com.pointblank.api.auth;

public class DuplicateAccountException extends RuntimeException {

    /**
     * Cria um erro de conflito para um dado de conta já cadastrado.
     *
     * @param message descrição do dado duplicado
     */
    public DuplicateAccountException(String message) {
        super(message);
    }
}
