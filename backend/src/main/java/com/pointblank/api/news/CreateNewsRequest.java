package com.pointblank.api.news;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNewsRequest(
        @NotNull(message = "Selecione uma categoria")
        NewsCategory category,

        @NotBlank(message = "Informe o título")
        @Size(max = 160, message = "O título deve possuir no máximo 160 caracteres")
        String title,

        @Size(max = 300, message = "O resumo deve possuir no máximo 300 caracteres")
        String summary,

        @NotBlank(message = "Escreva o conteúdo da notícia")
        @Size(max = 30000, message = "O conteúdo deve possuir no máximo 30000 caracteres")
        String content,

        @NotBlank(message = "Informe o link do banner interno")
        @Size(max = 500, message = "O link do banner deve possuir no máximo 500 caracteres")
        String bannerUrl,

        boolean published) {
}
