package com.pointblank.api.news;

import org.springframework.http.HttpStatus;

public class NewsException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Cria um erro de domínio relacionado às notícias.
     *
     * @param status status HTTP a devolver
     * @param message descrição legível do problema
     */
    public NewsException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Informa o status HTTP associado ao erro.
     *
     * @return status configurado
     */
    public HttpStatus status() {
        return status;
    }
}
