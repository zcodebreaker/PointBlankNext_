package com.pointblank.api.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AddAccountCurrenciesRequest(
        @NotNull(message = "Informe a quantidade de Gold")
        @PositiveOrZero(message = "Gold não pode ser negativo")
        Integer gold,

        @NotNull(message = "Informe a quantidade de Cash")
        @PositiveOrZero(message = "Cash não pode ser negativo")
        Integer cash) {
}
