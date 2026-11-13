package com.pointblank.api.webstore;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WebstorePurchaseRequest(
        @Positive(message = "Selecione um produto válido") int goodId,
        @NotNull(message = "Identifique a solicitação de compra") UUID requestId) {
}
