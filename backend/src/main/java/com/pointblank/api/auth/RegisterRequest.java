package com.pointblank.api.auth;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe o nome de usuário")
        @Pattern(
                regexp = "^[A-Za-z0-9_]{4,16}$",
                message = "O usuário deve ter de 4 a 16 caracteres e usar apenas letras, números ou _")
        String username,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
        String email,

        @NotBlank(message = "Informe a senha")
        @Size(min = 8, max = 16, message = "A senha deve ter de 8 a 16 caracteres")
        String password,

        @NotBlank(message = "Confirme a senha")
        @Size(min = 8, max = 16, message = "A confirmação deve ter de 8 a 16 caracteres")
        String passwordConfirmation,

        @NotBlank(message = "Conclua a verificação de segurança")
        @Size(max = 2048, message = "A verificação de segurança é inválida")
        String turnstileToken,

        @AssertTrue(message = "Você precisa aceitar os termos de uso")
        boolean acceptedTerms) {
}
