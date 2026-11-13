package com.pointblank.api.news;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NewsRepository {

    private static final String COLUMNS = """
            n.news_id, n.author_id, author.login AS author_username,
            n.category, n.title, n.summary, n.content, n.cover_url, n.carousel_url, n.banner_url,
            n.published, n.published_at, n.created_at, n.updated_at
            """;

    private final JdbcClient jdbcClient;

    /**
     * Cria o repositório de notícias com o cliente JDBC da aplicação.
     *
     * @param jdbcClient cliente usado nas consultas SQL
     */
    public NewsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Lista as notícias publicadas, com filtro opcional de categoria.
     *
     * @param category categoria normalizada ou vazio para todas
     * @param limit quantidade máxima de resultados
     * @return notícias públicas mais recentes
     */
    public List<NewsArticle> findPublished(String category, int limit) {
        return baseQuery("""
                WHERE n.published
                  AND (:category = '' OR n.category = :category)
                ORDER BY n.published_at DESC, n.news_id DESC
                LIMIT :limit
                """)
                .param("category", category)
                .param("limit", limit)
                .query(this::mapArticle)
                .list();
    }

    /**
     * Lista publicações e rascunhos para o painel administrativo.
     *
     * @param limit quantidade máxima de resultados
     * @return notícias mais recentes
     */
    public List<NewsArticle> findAll(int limit) {
        return baseQuery("""
                ORDER BY n.created_at DESC, n.news_id DESC
                LIMIT :limit
                """)
                .param("limit", limit)
                .query(this::mapArticle)
                .list();
    }

    /**
     * Busca uma notícia publicada pelo identificador.
     *
     * @param newsId identificador da notícia
     * @return notícia pública, quando encontrada
     */
    public Optional<NewsArticle> findPublishedById(long newsId) {
        return baseQuery("WHERE n.news_id = :newsId AND n.published")
                .param("newsId", newsId)
                .query(this::mapArticle)
                .optional();
    }

    /**
     * Persiste uma nova notícia e devolve seu identificador.
     *
     * @param authorId identificador do Game Master
     * @param request dados validados da publicação
     * @return identificador gerado
     */
    public long create(long authorId, CreateNewsRequest request) {
        return jdbcClient.sql("""
                INSERT INTO public.news_articles
                    (author_id, category, title, summary, content, cover_url, carousel_url,
                     banner_url, published, published_at)
                VALUES
                    (:authorId, :category, :title, :summary, :content, '', '', :bannerUrl,
                     :published, CASE WHEN :published THEN CURRENT_TIMESTAMP ELSE NULL END)
                RETURNING news_id
                """)
                .param("authorId", authorId)
                .param("category", request.category().name())
                .param("title", request.title().trim())
                .param("summary", request.summary() == null ? "" : request.summary().trim())
                .param("content", request.content().trim())
                .param("bannerUrl", request.bannerUrl().trim())
                .param("published", request.published())
                .query(Long.class)
                .single();
    }

    /**
     * Atualiza os campos editoriais de uma notícia sem alterar sua capa.
     *
     * @param newsId identificador da notícia
     * @param request novos dados da publicação
     * @return {@code true} quando a notícia foi encontrada
     */
    public boolean update(long newsId, CreateNewsRequest request) {
        return jdbcClient.sql("""
                UPDATE public.news_articles
                SET category = :category,
                    title = :title,
                    summary = :summary,
                    content = :content,
                    banner_url = :bannerUrl,
                    published_at = CASE
                        WHEN :published AND NOT published THEN CURRENT_TIMESTAMP
                        WHEN :published THEN published_at
                        ELSE NULL
                    END,
                    published = :published,
                    updated_at = CURRENT_TIMESTAMP
                WHERE news_id = :newsId
                """)
                .param("category", request.category().name())
                .param("title", request.title().trim())
                .param("summary", request.summary() == null ? "" : request.summary().trim())
                .param("content", request.content().trim())
                .param("bannerUrl", request.bannerUrl().trim())
                .param("published", request.published())
                .param("newsId", newsId)
                .update() == 1;
    }

    /**
     * Salva a capa da notícia e publica a URL estável usada pelo frontend.
     *
     * @param newsId identificador da notícia
     * @param cover imagem validada
     */
    public void saveCover(long newsId, NewsCover cover) {
        jdbcClient.sql("""
                UPDATE public.news_articles
                SET cover_url = :coverUrl,
                    image_data = :imageData,
                    image_content_type = :contentType,
                    image_width = :width,
                    image_height = :height,
                    updated_at = CURRENT_TIMESTAMP
                WHERE news_id = :newsId
                """)
                .param("coverUrl", "/api/v1/news/" + newsId + "/cover")
                .param("imageData", cover.data())
                .param("contentType", cover.contentType())
                .param("width", cover.width())
                .param("height", cover.height())
                .param("newsId", newsId)
                .update();
    }

    /**
     * Salva a imagem horizontal exclusiva do carrossel da página inicial.
     *
     * @param newsId identificador da notícia
     * @param carousel imagem validada
     */
    public void saveCarousel(long newsId, NewsCover carousel) {
        jdbcClient.sql("""
                UPDATE public.news_articles
                SET carousel_url = :carouselUrl,
                    carousel_image_data = :imageData,
                    carousel_image_content_type = :contentType,
                    carousel_image_width = :width,
                    carousel_image_height = :height,
                    updated_at = CURRENT_TIMESTAMP
                WHERE news_id = :newsId
                """)
                .param("carouselUrl", "/api/v1/news/" + newsId + "/carousel")
                .param("imageData", carousel.data())
                .param("contentType", carousel.contentType())
                .param("width", carousel.width())
                .param("height", carousel.height())
                .param("newsId", newsId)
                .update();
    }

    /**
     * Carrega somente os bytes da capa de uma notícia publicada.
     *
     * @param newsId identificador da notícia
     * @return capa, quando cadastrada e pública
     */
    public Optional<NewsCover> findPublishedCover(long newsId) {
        return jdbcClient.sql("""
                SELECT image_data, image_content_type, image_width, image_height
                FROM public.news_articles
                WHERE news_id = :newsId
                  AND published
                  AND image_data IS NOT NULL
                """)
                .param("newsId", newsId)
                .query((resultSet, rowNum) -> new NewsCover(
                        resultSet.getBytes("image_data"),
                        resultSet.getString("image_content_type"),
                        resultSet.getInt("image_width"),
                        resultSet.getInt("image_height")))
                .optional();
    }

    public Optional<NewsCover> findPublishedCarousel(long newsId) {
        return jdbcClient.sql("""
                SELECT carousel_image_data, carousel_image_content_type,
                       carousel_image_width, carousel_image_height
                FROM public.news_articles
                WHERE news_id = :newsId
                  AND published
                  AND carousel_image_data IS NOT NULL
                """)
                .param("newsId", newsId)
                .query((resultSet, rowNum) -> new NewsCover(
                        resultSet.getBytes("carousel_image_data"),
                        resultSet.getString("carousel_image_content_type"),
                        resultSet.getInt("carousel_image_width"),
                        resultSet.getInt("carousel_image_height")))
                .optional();
    }

    /**
     * Carrega a capa para o painel, incluindo notícias ainda não publicadas.
     *
     * @param newsId identificador da notícia
     * @return capa cadastrada
     */
    public Optional<NewsCover> findCover(long newsId) {
        return jdbcClient.sql("""
                SELECT image_data, image_content_type, image_width, image_height
                FROM public.news_articles
                WHERE news_id = :newsId
                  AND image_data IS NOT NULL
                """)
                .param("newsId", newsId)
                .query((resultSet, rowNum) -> new NewsCover(
                        resultSet.getBytes("image_data"),
                        resultSet.getString("image_content_type"),
                        resultSet.getInt("image_width"),
                        resultSet.getInt("image_height")))
                .optional();
    }

    public Optional<NewsCover> findCarousel(long newsId) {
        return jdbcClient.sql("""
                SELECT carousel_image_data, carousel_image_content_type,
                       carousel_image_width, carousel_image_height
                FROM public.news_articles
                WHERE news_id = :newsId
                  AND carousel_image_data IS NOT NULL
                """)
                .param("newsId", newsId)
                .query((resultSet, rowNum) -> new NewsCover(
                        resultSet.getBytes("carousel_image_data"),
                        resultSet.getString("carousel_image_content_type"),
                        resultSet.getInt("carousel_image_width"),
                        resultSet.getInt("carousel_image_height")))
                .optional();
    }

    /**
     * Busca uma notícia pelo identificador, incluindo rascunhos.
     *
     * @param newsId identificador da notícia
     * @return notícia encontrada
     */
    public Optional<NewsArticle> findById(long newsId) {
        return baseQuery("WHERE n.news_id = :newsId")
                .param("newsId", newsId)
                .query(this::mapArticle)
                .optional();
    }

    /**
     * Remove uma notícia pelo identificador.
     *
     * @param newsId identificador da notícia
     * @return {@code true} quando uma notícia foi removida
     */
    public boolean deleteById(long newsId) {
        return jdbcClient.sql("DELETE FROM public.news_articles WHERE news_id = :newsId")
                .param("newsId", newsId)
                .update() == 1;
    }

    /**
     * Monta a consulta-base com os dados do autor.
     *
     * @param conditions filtros e ordenação adicionais
     * @return consulta pronta para receber parâmetros
     */
    private JdbcClient.StatementSpec baseQuery(String conditions) {
        return jdbcClient.sql("""
                SELECT %s
                FROM public.news_articles n
                JOIN public.accounts author ON author.player_id = n.author_id
                %s
                """.formatted(COLUMNS, conditions));
    }

    /**
     * Converte uma linha SQL no modelo de notícia.
     *
     * @param resultSet resultado posicionado
     * @param rowNum número da linha
     * @return notícia mapeada
     * @throws SQLException quando uma coluna não puder ser lida
     */
    private NewsArticle mapArticle(ResultSet resultSet, int rowNum) throws SQLException {
        return new NewsArticle(
                resultSet.getLong("news_id"),
                resultSet.getLong("author_id"),
                resultSet.getString("author_username"),
                NewsCategory.valueOf(resultSet.getString("category")),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                resultSet.getString("content"),
                resultSet.getString("cover_url"),
                resultSet.getString("carousel_url"),
                resultSet.getString("banner_url"),
                resultSet.getBoolean("published"),
                resultSet.getObject("published_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }
}
