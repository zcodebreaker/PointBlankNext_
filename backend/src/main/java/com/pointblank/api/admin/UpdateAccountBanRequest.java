package com.pointblank.api.admin;

import jakarta.validation.constraints.NotNull;

public record UpdateAccountBanRequest(
        @NotNull(message = "Informe o estado do banimento")
        Boolean banned) {
}
