package com.pointblank.api.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminTicketReplyRequest(
        @NotBlank(message = "Informe o assunto da resposta")
        @Size(min = 6, max = 128, message = "O assunto deve possuir entre 6 e 128 caracteres")
        String subject,
        @NotBlank(message = "Escreva uma resposta")
        @Size(max = 1500, message = "A resposta deve possuir no máximo 1500 caracteres")
        String message,
        boolean close) {
}
