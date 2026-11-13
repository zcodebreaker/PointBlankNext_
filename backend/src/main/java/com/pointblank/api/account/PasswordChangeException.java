package com.pointblank.api.account;

public class PasswordChangeException extends RuntimeException {

    private final String field;

    /**
     * Cria um erro de alteração de senha associado a um campo do formulário.
     *
     * @param field campo inválido
     * @param message descrição do erro
     */
    public PasswordChangeException(String field, String message) {
        super(message);
        this.field = field;
    }

    /**
     * Retorna o campo que causou o erro.
     *
     * @return nome do campo inválido
     */
    public String field() {
        return field;
    }
}
