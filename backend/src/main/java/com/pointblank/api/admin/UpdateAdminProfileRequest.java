package com.pointblank.api.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAdminProfileRequest(
        @NotNull @Size(max = 32) @Pattern(regexp = "[^\\p{Cntrl}]*", message = "O apelido contém caracteres inválidos") String displayName,
        boolean removeAvatar) {
}
