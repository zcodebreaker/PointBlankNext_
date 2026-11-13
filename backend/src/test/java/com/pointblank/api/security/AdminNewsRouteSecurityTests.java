package com.pointblank.api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.pointblank.api.news.AdminNewsController;
import com.pointblank.api.news.CreateNewsRequest;
import com.pointblank.api.news.NewsService;

@WebMvcTest(
        controllers = AdminNewsController.class,
        properties = {
                "app.jwt.secret=01234567890123456789012345678901",
                "app.jwt.issuer=pointblank-api",
                "app.jwt.access-token-ttl=PT2H",
                "app.jwt.remember-token-ttl=P30D",
                "app.cors.allowed-origins=http://localhost:3000"
        })
@Import(SecurityConfig.class)
class AdminNewsRouteSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NewsService newsService;

    @Test
    void rejectsAnonymousDeletion() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/news/12"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletesNewsForAuthorizedToken() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/news/12")
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isNoContent());

        verify(newsService).delete(1L, 12L);
    }

    @Test
    void acceptsMultipartNewsWithCoverForAuthorizedToken() throws Exception {
        MockMultipartFile article = new MockMultipartFile(
                "article",
                "article.json",
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "category":"PATCH",
                          "title":"Notas de atualização",
                          "summary":"Resumo",
                          "content":"Conteúdo completo",
                          "bannerUrl":"/upload/image/banner.jpg",
                          "published":true
                        }
                        """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockMultipartFile cover = new MockMultipartFile(
                "cover",
                "cover.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] {1, 2, 3});
        MockMultipartFile carousel = new MockMultipartFile(
                "carousel",
                "carousel.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] {4, 5, 6});

        mockMvc.perform(multipart("/api/v1/admin/news")
                        .file(article)
                        .file(cover)
                        .file(carousel)
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk());

        verify(newsService).create(
                eq(1L),
                argThat((CreateNewsRequest request) ->
                        request.title().equals("Notas de atualização")
                                && request.bannerUrl().equals("/upload/image/banner.jpg")
                                && request.published()),
                any(MultipartFile.class),
                any(MultipartFile.class));
    }

    @Test
    void acceptsMultipartNewsUpdateWithoutNewCover() throws Exception {
        MockMultipartFile article = new MockMultipartFile(
                "article",
                "article.json",
                MediaType.APPLICATION_JSON_VALUE,
                """
                        {
                          "category":"EVENT",
                          "title":"Evento atualizado",
                          "summary":"Resumo",
                          "content":"Conteúdo atualizado",
                          "bannerUrl":"/upload/image/event-banner.jpg",
                          "published":false
                        }
                        """.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/admin/news/12")
                        .file(article)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .with(jwt()
                                .jwt(token -> token.subject("1"))
                                .authorities(new SimpleGrantedAuthority("SCOPE_player"))))
                .andExpect(status().isOk());

        verify(newsService).update(
                eq(1L),
                eq(12L),
                argThat((CreateNewsRequest request) ->
                        request.title().equals("Evento atualizado") && !request.published()),
                isNull(),
                isNull());
    }
}
