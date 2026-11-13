package com.pointblank.api.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateAccountCurrenciesRequest(
        @NotNull(message = "Informe a quantidade de Gold")
        @PositiveOrZero(message = "Gold não pode ser negativo")
        @Max(value = Integer.MAX_VALUE, message = "Gold excede o limite permitido")
        Integer gold,

        @NotNull(message = "Informe a quantidade de Cash")
        @PositiveOrZero(message = "Cash não pode ser negativo")
        @Max(value = Integer.MAX_VALUE, message = "Cash excede o limite permitido")
        Integer cash) {
}
