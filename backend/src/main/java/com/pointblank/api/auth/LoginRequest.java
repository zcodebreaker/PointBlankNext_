package com.pointblank.api.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Informe o nome de usuário")
        @Size(max = 16, message = "O usuário deve ter no máximo 16 caracteres")
        String username,

        @NotBlank(message = "Informe a senha")
        @Size(max = 16, message = "A senha deve ter no máximo 16 caracteres")
        String password,

        @NotBlank(message = "Conclua a verificação de segurança")
        @Size(max = 2048, message = "A verificação de segurança é inválida")
        String turnstileToken,

        boolean remember) {
}
