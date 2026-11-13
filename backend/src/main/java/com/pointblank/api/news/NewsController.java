package com.pointblank.api.news;

import java.util.List;
import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.validation.annotation.Validated;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    /**
     * Cria o controlador público de notícias.
     *
     * @param newsService serviço de notícias
     */
    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    /**
     * Lista publicações visíveis para visitantes.
     *
     * @param category categoria opcional
     * @param limit quantidade máxima de itens
     * @return notícias publicadas
     */
    @GetMapping
    public List<NewsArticle> findPublished(
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "60") @Min(1) @Max(100) int limit) {
        return newsService.findPublished(category, limit);
    }

    /**
     * Exibe uma publicação pelo identificador.
     *
     * @param newsId identificador da notícia
     * @return notícia publicada
     */
    @GetMapping("/{newsId}")
    public NewsArticle findPublishedById(@PathVariable @Positive long newsId) {
        return newsService.findPublishedById(newsId);
    }

    /**
     * Entrega a capa persistida de uma notícia publicada.
     *
     * @param newsId identificador da notícia
     * @return bytes da imagem com cache público
     */
    @GetMapping("/{newsId}/cover")
    public ResponseEntity<byte[]> findPublishedCover(@PathVariable @Positive long newsId) {
        NewsCover cover = newsService.findPublishedCover(newsId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .contentLength(cover.data().length)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(cover.data());
    }

    /**
     * Entrega a imagem horizontal do carrossel de uma notícia publicada.
     *
     * @param newsId identificador da notícia
     * @return bytes da imagem com cache público
     */
    @GetMapping("/{newsId}/carousel")
    public ResponseEntity<byte[]> findPublishedCarousel(@PathVariable @Positive long newsId) {
        NewsCover carousel = newsService.findPublishedCarousel(newsId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(carousel.contentType()))
                .contentLength(carousel.data().length)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .body(carousel.data());
    }
}
