package com.pointblank.api.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Informe a senha atual")
        @Size(max = 16, message = "A senha atual deve ter no máximo 16 caracteres")
        String currentPassword,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 16, message = "A nova senha deve ter de 8 a 16 caracteres")
        String newPassword,

        @NotBlank(message = "Repita a nova senha")
        @Size(min = 8, max = 16, message = "A confirmação deve ter de 8 a 16 caracteres")
        String newPasswordConfirmation) {
}
