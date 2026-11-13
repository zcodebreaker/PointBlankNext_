package com.pointblank.api.news;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/v1/admin/news")
public class AdminNewsController {

    private final NewsService newsService;

    /**
     * Cria o controlador administrativo de notícias.
     *
     * @param newsService serviço de notícias
     */
    public AdminNewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * Lista publicações e rascunhos para o painel.
     *
     * @param jwt token do Game Master
     * @param limit quantidade máxima de itens
     * @return notícias administrativas
     */
    @GetMapping
    public List<NewsArticle> findAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return newsService.findAll(playerId(jwt), limit);
    }

    /**
     * Publica ou salva como rascunho uma nova notícia.
     *
     * @param jwt token do Game Master
     * @param request conteúdo validado
     * @param cover capa quadrada usada nos cards
     * @param carousel imagem horizontal usada no carrossel
     * @return notícia criada
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NewsArticle create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestPart("article") CreateNewsRequest request,
            @RequestPart("cover") MultipartFile cover,
            @RequestPart("carousel") MultipartFile carousel) {
        return newsService.create(playerId(jwt), request, cover, carousel);
    }

    /**
     * Atualiza uma notícia existente e substitui suas imagens somente quando
     * novos arquivos forem enviados.
     *
     * @param jwt token do Game Master
     * @param newsId identificador da notícia
     * @param request conteúdo validado
     * @param cover nova capa opcional
     * @param carousel nova imagem do carrossel opcional
     * @return notícia atualizada
     */
    @PutMapping(path = "/{newsId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NewsArticle update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive long newsId,
            @Valid @RequestPart("article") CreateNewsRequest request,
            @RequestPart(name = "cover", required = false) MultipartFile cover,
            @RequestPart(name = "carousel", required = false) MultipartFile carousel) {
        return newsService.update(playerId(jwt), newsId, request, cover, carousel);
    }

    /**
     * Entrega a capa no painel, inclusive quando a notícia ainda é rascunho.
     *
     * @param jwt token do Game Master
     * @param newsId identificador da notícia
     * @return bytes da capa
     */
    @GetMapping("/{newsId}/cover")
    public ResponseEntity<byte[]> findCover(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive long newsId) {
        NewsCover cover = newsService.findCover(playerId(jwt), newsId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .contentLength(cover.data().length)
                .cacheControl(CacheControl.noStore())
                .body(cover.data());
    }

    /**
     * Entrega a imagem do carrossel no painel, inclusive para rascunhos.
     *
     * @param jwt token do Game Master
     * @param newsId identificador da notícia
     * @return bytes da imagem do carrossel
     */
    @GetMapping("/{newsId}/carousel")
    public ResponseEntity<byte[]> findCarousel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive long newsId) {
        NewsCover carousel = newsService.findCarousel(playerId(jwt), newsId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(carousel.contentType()))
                .contentLength(carousel.data().length)
                .cacheControl(CacheControl.noStore())
                .body(carousel.data());
    }

    /**
     * Exclui uma notícia cadastrada no painel.
     *
     * @param jwt token do Game Master
     * @param newsId identificador da notícia
     */
    @DeleteMapping("/{newsId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive long newsId) {
        newsService.delete(playerId(jwt), newsId);
    }

    /**
     * Extrai o identificador da conta do subject do JWT.
     *
     * @param jwt token autenticado
     * @return identificador do Game Master
     */
    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new NewsException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}
