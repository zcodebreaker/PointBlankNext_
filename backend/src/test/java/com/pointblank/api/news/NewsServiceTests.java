package com.pointblank.api.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import org.mockito.ArgumentCaptor;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

class NewsServiceTests {

    private NewsRepository newsRepository;
    private AccountRepository accountRepository;
    private NewsService newsService;

    @BeforeEach
    void setUp() {
        newsRepository = mock(NewsRepository.class);
        accountRepository = mock(AccountRepository.class);
        newsService = new NewsService(newsRepository, accountRepository);
    }

    @Test
    void deletesNewsForAdministrator() {
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(newsRepository.deleteById(12L)).thenReturn(true);

        newsService.delete(1L, 12L);

        verify(newsRepository).deleteById(12L);
    }

    @Test
    void reportsMissingNewsDuringDeletion() {
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(newsRepository.deleteById(99L)).thenReturn(false);

        assertThatThrownBy(() -> newsService.delete(1L, 99L))
                .isInstanceOfSatisfying(NewsException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("Notícia não encontrada");
                });
    }

    @Test
    void rejectsNewsDeletionFromRegularPlayer() {
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(player()));

        assertThatThrownBy(() -> newsService.delete(35L, 12L))
                .isInstanceOfSatisfying(NewsException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(newsRepository, never()).deleteById(12L);
    }

    @Test
    void createsNewsWithValidatedSquareCover() throws Exception {
        CreateNewsRequest request = new CreateNewsRequest(
                NewsCategory.PATCH,
                "Notas de atualização",
                "Resumo",
                "Conteúdo completo",
                "/upload/image/banner.jpg",
                true);
        NewsArticle created = article(21L);

        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(newsRepository.create(1L, request)).thenReturn(21L);
        when(newsRepository.findById(21L)).thenReturn(Optional.of(created));

        NewsArticle result = newsService.create(
                1L, request, image("cover", 1080, 1080), image("carousel", 1116, 628));

        ArgumentCaptor<NewsCover> coverCaptor = ArgumentCaptor.forClass(NewsCover.class);
        ArgumentCaptor<NewsCover> carouselCaptor = ArgumentCaptor.forClass(NewsCover.class);
        verify(newsRepository).saveCover(org.mockito.ArgumentMatchers.eq(21L), coverCaptor.capture());
        verify(newsRepository).saveCarousel(org.mockito.ArgumentMatchers.eq(21L), carouselCaptor.capture());
        assertThat(coverCaptor.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(coverCaptor.getValue().width()).isEqualTo(1080);
        assertThat(coverCaptor.getValue().height()).isEqualTo(1080);
        assertThat(carouselCaptor.getValue().width()).isEqualTo(1116);
        assertThat(carouselCaptor.getValue().height()).isEqualTo(628);
        assertThat(result).isEqualTo(created);
    }

    @Test
    void rejectsCoverWithDifferentDimensionsBeforeWritingNews() throws Exception {
        CreateNewsRequest request = new CreateNewsRequest(
                NewsCategory.EVENT,
                "Evento",
                "Resumo",
                "Conteúdo completo",
                "/upload/image/banner.jpg",
                true);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));

        assertThatThrownBy(() -> newsService.create(
                1L, request, image("cover", 1080, 720), image("carousel", 1116, 628)))
                .isInstanceOfSatisfying(NewsException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).contains("1080 × 1080");
                });

        verify(newsRepository, never()).create(1L, request);
    }

    @Test
    void rejectsCarouselWithDifferentDimensionsBeforeWritingNews() throws Exception {
        CreateNewsRequest request = request(true);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));

        assertThatThrownBy(() -> newsService.create(
                1L, request, image("cover", 1080, 1080), image("carousel", 1116, 500)))
                .isInstanceOfSatisfying(NewsException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).contains("1116 × 628");
                });

        verify(newsRepository, never()).create(1L, request);
    }

    @Test
    void updatesNewsWithoutReplacingItsCover() {
        CreateNewsRequest request = request(false);
        NewsArticle updated = article(21L);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(newsRepository.update(21L, request)).thenReturn(true);
        when(newsRepository.findById(21L)).thenReturn(Optional.of(updated));

        NewsArticle result = newsService.update(1L, 21L, request, null, null);

        verify(newsRepository, never()).saveCover(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.any(NewsCover.class));
        verify(newsRepository, never()).saveCarousel(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.any(NewsCover.class));
        assertThat(result).isEqualTo(updated);
    }

    @Test
    void updatesNewsAndReplacesItsCover() throws Exception {
        CreateNewsRequest request = request(true);
        NewsArticle updated = article(21L);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(newsRepository.update(21L, request)).thenReturn(true);
        when(newsRepository.findById(21L)).thenReturn(Optional.of(updated));

        newsService.update(1L, 21L, request, image("cover", 1080, 1080), null);

        verify(newsRepository).saveCover(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.any(NewsCover.class));
    }

    @Test
    void updatesNewsAndReplacesItsCarouselImage() throws Exception {
        CreateNewsRequest request = request(true);
        NewsArticle updated = article(21L);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(newsRepository.update(21L, request)).thenReturn(true);
        when(newsRepository.findById(21L)).thenReturn(Optional.of(updated));

        newsService.update(1L, 21L, request, null, image("carousel", 1116, 628));

        verify(newsRepository).saveCarousel(
                org.mockito.ArgumentMatchers.eq(21L),
                org.mockito.ArgumentMatchers.any(NewsCover.class));
    }

    @Test
    void reportsMissingNewsDuringUpdate() {
        CreateNewsRequest request = request(true);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(newsRepository.update(99L, request)).thenReturn(false);

        assertThatThrownBy(() -> newsService.update(1L, 99L, request, null, null))
                .isInstanceOfSatisfying(NewsException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getMessage()).isEqualTo("Notícia não encontrada");
                });
    }

    private CreateNewsRequest request(boolean published) {
        return new CreateNewsRequest(
                NewsCategory.PATCH,
                "Notas de atualização",
                "Resumo",
                "Conteúdo completo",
                "/upload/image/banner.jpg",
                published);
    }

    private MockMultipartFile image(String fieldName, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", output);
        return new MockMultipartFile(fieldName, fieldName + ".jpg", "image/jpeg", output.toByteArray());
    }

    private NewsArticle article(long newsId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new NewsArticle(
                newsId,
                1L,
                "admin",
                NewsCategory.PATCH,
                "Notas de atualização",
                "Resumo",
                "Conteúdo completo",
                "/api/v1/news/" + newsId + "/cover",
                "/api/v1/news/" + newsId + "/carousel",
                "/upload/image/banner.jpg",
                true,
                now,
                now,
                now);
    }

    private Account administrator() {
        return new Account(1L, "admin", "admin@example.com", "Admin", 0, 0, 0, 0, 5, false);
    }

    private Account player() {
        return new Account(35L, "player", "player@example.com", "Player", 0, 0, 0, 0, 0, false);
    }
}
