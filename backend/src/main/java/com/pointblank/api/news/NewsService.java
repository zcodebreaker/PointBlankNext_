package com.pointblank.api.news;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

@Service
public class NewsService {

    private static final int ADMIN_ACCESS_LEVEL = 5;
    private static final int COVER_SIZE = 1080;
    private static final int CAROUSEL_WIDTH = 1116;
    private static final int CAROUSEL_HEIGHT = 628;
    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private final NewsRepository newsRepository;
    private final AccountRepository accountRepository;

    /**
     * Cria o serviço de notícias com acesso às publicações e contas.
     *
     * @param newsRepository repositório de notícias
     * @param accountRepository repositório de contas
     */
    public NewsService(NewsRepository newsRepository, AccountRepository accountRepository) {
        this.newsRepository = newsRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Lista notícias disponíveis para visitantes.
     *
     * @param category categoria opcional
     * @param limit quantidade máxima solicitada
     * @return notícias públicas
     */
    @Transactional(readOnly = true)
    public List<NewsArticle> findPublished(String category, int limit) {
        return newsRepository.findPublished(normalizeCategory(category), limit);
    }

    /**
     * Busca uma notícia pública ou responde que ela não existe.
     *
     * @param newsId identificador da notícia
     * @return notícia encontrada
     */
    @Transactional(readOnly = true)
    public NewsArticle findPublishedById(long newsId) {
        return newsRepository.findPublishedById(newsId)
                .orElseThrow(() -> new NewsException(HttpStatus.NOT_FOUND, "Notícia não encontrada"));
    }

    /**
     * Lista notícias e rascunhos após validar o acesso administrativo.
     *
     * @param administratorId identificador do administrador
     * @param limit quantidade máxima solicitada
     * @return publicações administrativas
     */
    @Transactional(readOnly = true)
    public List<NewsArticle> findAll(long administratorId, int limit) {
        requireAdministrator(administratorId);
        return newsRepository.findAll(limit);
    }

    /**
     * Cria uma publicação em nome do administrador autenticado.
     *
     * @param administratorId identificador do autor
     * @param request dados da notícia
     * @param coverFile capa quadrada usada nos cards
     * @param carouselFile imagem horizontal usada no carrossel
     * @return notícia persistida
     */
    @Transactional
    public NewsArticle create(
            long administratorId,
            CreateNewsRequest request,
            MultipartFile coverFile,
            MultipartFile carouselFile) {
        requireAdministrator(administratorId);
        NewsCover cover = validateCover(coverFile);
        NewsCover carousel = validateCarousel(carouselFile);
        long newsId = newsRepository.create(administratorId, request);
        newsRepository.saveCover(newsId, cover);
        newsRepository.saveCarousel(newsId, carousel);
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsException(HttpStatus.INTERNAL_SERVER_ERROR, "A notícia não pôde ser carregada"));
    }

    /**
     * Atualiza uma notícia existente, preservando as imagens atuais quando o
     * painel não envia substitutas.
     *
     * @param administratorId identificador do administrador
     * @param newsId identificador da notícia
     * @param request novos dados da notícia
     * @param coverFile nova capa opcional
     * @param carouselFile nova imagem do carrossel opcional
     * @return notícia atualizada
     */
    @Transactional
    public NewsArticle update(
            long administratorId,
            long newsId,
            CreateNewsRequest request,
            MultipartFile coverFile,
            MultipartFile carouselFile) {
        requireAdministrator(administratorId);
        NewsCover cover = coverFile == null || coverFile.isEmpty() ? null : validateCover(coverFile);
        NewsCover carousel = carouselFile == null || carouselFile.isEmpty()
                ? null
                : validateCarousel(carouselFile);
        if (!newsRepository.update(newsId, request)) {
            throw new NewsException(HttpStatus.NOT_FOUND, "Notícia não encontrada");
        }
        if (cover != null) {
            newsRepository.saveCover(newsId, cover);
        }
        if (carousel != null) {
            newsRepository.saveCarousel(newsId, carousel);
        }
        return newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsException(HttpStatus.INTERNAL_SERVER_ERROR, "A notícia não pôde ser carregada"));
    }

    /**
     * Busca a capa de uma notícia publicada.
     *
     * @param newsId identificador da notícia
     * @return imagem armazenada no banco
     */
    @Transactional(readOnly = true)
    public NewsCover findPublishedCover(long newsId) {
        return newsRepository.findPublishedCover(newsId)
                .orElseThrow(() -> new NewsException(HttpStatus.NOT_FOUND, "Capa da notícia não encontrada"));
    }

    @Transactional(readOnly = true)
    public NewsCover findPublishedCarousel(long newsId) {
        return newsRepository.findPublishedCarousel(newsId)
                .orElseThrow(() -> new NewsException(HttpStatus.NOT_FOUND, "Imagem do carrossel não encontrada"));
    }

    /**
     * Carrega uma capa para o painel após validar o administrador.
     *
     * @param administratorId identificador do administrador
     * @param newsId identificador da notícia
     * @return imagem armazenada no banco
     */
    @Transactional(readOnly = true)
    public NewsCover findCover(long administratorId, long newsId) {
        requireAdministrator(administratorId);
        return newsRepository.findCover(newsId)
                .orElseThrow(() -> new NewsException(HttpStatus.NOT_FOUND, "Capa da notícia não encontrada"));
    }

    @Transactional(readOnly = true)
    public NewsCover findCarousel(long administratorId, long newsId) {
        requireAdministrator(administratorId);
        return newsRepository.findCarousel(newsId)
                .orElseThrow(() -> new NewsException(HttpStatus.NOT_FOUND, "Imagem do carrossel não encontrada"));
    }

    /**
     * Exclui uma notícia após validar o acesso administrativo.
     *
     * @param administratorId identificador do Game Master autenticado
     * @param newsId identificador da notícia
     */
    @Transactional
    public void delete(long administratorId, long newsId) {
        requireAdministrator(administratorId);
        if (!newsRepository.deleteById(newsId)) {
            throw new NewsException(HttpStatus.NOT_FOUND, "Notícia não encontrada");
        }
    }

    /**
     * Exige nível de Game Master para operações administrativas.
     *
     * @param administratorId identificador da conta autenticada
     */
    private void requireAdministrator(long administratorId) {
        Account account = accountRepository.findByPlayerId(administratorId)
                .orElseThrow(() -> new NewsException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (account.accessLevel() < ADMIN_ACCESS_LEVEL) {
            throw new NewsException(HttpStatus.FORBIDDEN, "Acesso administrativo necessário");
        }
    }

    private NewsCover validateCover(MultipartFile coverFile) {
        return validateImage(coverFile, "capa", COVER_SIZE, COVER_SIZE);
    }

    private NewsCover validateCarousel(MultipartFile carouselFile) {
        return validateImage(carouselFile, "imagem do carrossel", CAROUSEL_WIDTH, CAROUSEL_HEIGHT);
    }

    private NewsCover validateImage(MultipartFile file, String label, int expectedWidth, int expectedHeight) {
        String capitalizedLabel = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        if (file == null || file.isEmpty()) {
            throw new NewsException(HttpStatus.BAD_REQUEST, "Selecione uma " + label + " para a notícia");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new NewsException(HttpStatus.BAD_REQUEST, capitalizedLabel + " deve possuir no máximo 2 MB");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new NewsException(HttpStatus.BAD_REQUEST, capitalizedLabel + " deve ser uma imagem JPG ou PNG");
        }

        try {
            byte[] data = file.getBytes();
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new NewsException(HttpStatus.BAD_REQUEST, "O arquivo enviado não é uma imagem válida");
            }
            if (image.getWidth() != expectedWidth || image.getHeight() != expectedHeight) {
                throw new NewsException(HttpStatus.BAD_REQUEST, capitalizedLabel + " deve possuir exatamente "
                        + expectedWidth + " × " + expectedHeight + " pixels");
            }
            return new NewsCover(data, contentType, image.getWidth(), image.getHeight());
        } catch (IOException exception) {
            throw new NewsException(HttpStatus.BAD_REQUEST, "Não foi possível ler a " + label + " enviada");
        }
    }

    /**
     * Valida e normaliza a categoria recebida por parâmetro.
     *
     * @param category categoria textual
     * @return nome persistido ou vazio
     */
    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return "";
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        try {
            return NewsCategory.valueOf(normalized).name();
        } catch (IllegalArgumentException exception) {
            throw new NewsException(HttpStatus.BAD_REQUEST, "Categoria de notícia inválida");
        }
    }
}
