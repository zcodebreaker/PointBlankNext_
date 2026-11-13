package com.pointblank.api.ticket;

import org.springframework.http.HttpStatus;

public class TicketException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Cria um erro de ticket com o status HTTP correspondente.
     *
     * @param status status devolvido ao cliente
     * @param message descrição do erro
     */
    public TicketException(HttpStatus status, String message) {
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
