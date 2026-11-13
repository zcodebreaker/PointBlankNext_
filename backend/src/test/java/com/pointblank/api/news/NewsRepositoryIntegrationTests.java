package com.pointblank.api.news;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.pointblank.api.account.AccountRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class NewsRepositoryIntegrationTests {
    @Autowired private NewsRepository news;
    @Autowired private AccountRepository accounts;

    @ParameterizedTest
    @EnumSource(NewsCategory.class)
    void createsUpdatesDeletesAndRecreatesEveryCategory(NewsCategory category) {
        String username = "news_" + UUID.randomUUID().toString().substring(0, 8);
        long authorId = accounts.create(username, "test-only", username + "@example.invalid").playerId();
        var initial = new CreateNewsRequest(NewsCategory.PATCH, "Teste temporário", "Resumo", "Texto", "/teste.jpg", false);
        var request = new CreateNewsRequest(category, "Teste temporário", "Resumo", "Texto", "/teste.jpg", true);
        long id = news.create(authorId, initial);
        assertThat(news.update(id, request)).isTrue();
        assertThat(news.findById(id).orElseThrow().category()).isEqualTo(category);
        assertThat(news.deleteById(id)).isTrue();
        assertThat(news.findById(id)).isEmpty();
        assertThat(news.deleteById(id)).isFalse();
        long replacement = news.create(authorId, request);
        assertThat(news.findById(replacement).orElseThrow().category()).isEqualTo(category);
    }
}
