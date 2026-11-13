package com.pointblank.api.admin;

import org.springframework.http.HttpStatus;

public class AdminAccountException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Cria um erro de operação administrativa com seu status HTTP.
     *
     * @param status status devolvido ao cliente
     * @param message descrição do erro
     */
    public AdminAccountException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Retorna o status HTTP associado ao erro.
     *
     * @return status da resposta
     */
    public HttpStatus status() {
        return status;
    }
}
