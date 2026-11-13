package com.pointblank.api.common;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import com.pointblank.api.auth.DuplicateAccountException;

class ApiExceptionHandlerTests {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void databaseConstraintErrorsAreNotReportedAsDuplicateAccounts() {
        var response = handler.handleDataIntegrity(new DataIntegrityViolationException(
                "check constraint ck_news_articles_category: internal SQL details"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getType().getPath()).endsWith("/data-integrity-conflict");
        assertThat(response.getBody().getTitle()).isEqualTo("Conflito nos dados");
        assertThat(response.getBody().getDetail()).doesNotContain("conta", "ck_news", "SQL");
    }

    @Test
    void duplicateAccountsKeepTheirSpecificMessage() {
        var response = handler.handleConflict(new DuplicateAccountException("Usuário já cadastrado"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getType().getPath()).endsWith("/account-conflict");
        assertThat(response.getBody().getDetail()).isEqualTo("Usuário já cadastrado");
    }
}
