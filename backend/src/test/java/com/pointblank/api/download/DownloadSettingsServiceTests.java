package com.pointblank.api.download;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

class DownloadSettingsServiceTests {

    private DownloadSettingsRepository repository;
    private DownloadFileMetadataClient metadataClient;
    private AccountRepository accountRepository;
    private DownloadSettingsService service;

    @BeforeEach
    void setUp() {
        repository = mock(DownloadSettingsRepository.class);
        metadataClient = mock(DownloadFileMetadataClient.class);
        accountRepository = mock(AccountRepository.class);
        service = new DownloadSettingsService(repository, metadataClient, accountRepository);
    }

    @Test
    void calculatesAndPersistsRemoteMetadata() {
        OffsetDateTime fileUpdatedAt = OffsetDateTime.parse("2026-10-20T18:30:00Z");
        DownloadSettings saved = new DownloadSettings(
                "https://cdn.example.com/client.exe",
                "0.3.0",
                "https://cdn.example.com/client.torrent",
                "https://drive.google.com/client",
                "https://1drv.ms/client",
                "https://cdn.example.com/patch-261.exe",
                "https://cdn.example.com/patch-260.exe",
                4_294_967_296L,
                fileUpdatedAt,
                OffsetDateTime.parse("2026-10-21T12:00:00Z"));

        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(metadataClient.inspect("https://cdn.example.com/client.exe"))
                .thenReturn(new DownloadFileMetadata(4_294_967_296L, fileUpdatedAt));
        when(repository.save(
                eq("https://cdn.example.com/client.exe"),
                eq("0.3.0"),
                eq("https://cdn.example.com/client.torrent"),
                eq("https://drive.google.com/client"),
                eq("https://1drv.ms/client"),
                eq("https://cdn.example.com/patch-261.exe"),
                eq("https://cdn.example.com/patch-260.exe"),
                eq(4_294_967_296L),
                eq(fileUpdatedAt),
                any(OffsetDateTime.class)))
                .thenReturn(saved);

        DownloadSettings result = service.update(
                1L,
                new UpdateDownloadSettingsRequest(
                        " https://cdn.example.com/client.exe ",
                        " 0.3.0 ",
                        " https://cdn.example.com/client.torrent ",
                        " https://drive.google.com/client ",
                        " https://1drv.ms/client ",
                        " https://cdn.example.com/patch-261.exe ",
                        " https://cdn.example.com/patch-260.exe "));

        assertThat(result).isEqualTo(saved);
        verify(metadataClient).inspect("https://cdn.example.com/client.exe");
    }

    @Test
    void rejectsRegularPlayersBeforeConsultingTheRemoteFile() {
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(player()));

        assertThatThrownBy(() -> service.update(
                35L,
                new UpdateDownloadSettingsRequest(
                        "https://cdn.example.com/client.exe",
                        "0.3.0",
                        "https://cdn.example.com/client.torrent",
                        "https://drive.google.com/client",
                        "https://1drv.ms/client",
                        "https://cdn.example.com/patch-261.exe",
                        "https://cdn.example.com/patch-260.exe")))
                .isInstanceOfSatisfying(DownloadSettingsException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(metadataClient, never()).inspect(any());
    }

    @Test
    void rejectsInvalidMirrorUrlBeforeConsultingTheRemoteFile() {
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));

        assertThatThrownBy(() -> service.update(
                1L,
                new UpdateDownloadSettingsRequest(
                        "https://cdn.example.com/client.exe",
                        "0.3.0",
                        "javascript:alert(1)",
                        "https://drive.google.com/client",
                        "https://1drv.ms/client",
                        "https://cdn.example.com/patch-261.exe",
                        "https://cdn.example.com/patch-260.exe")))
                .isInstanceOfSatisfying(DownloadSettingsException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(metadataClient, never()).inspect(any());
    }

    private Account administrator() {
        return new Account(1L, "admin", "admin@example.com", "Admin", 0, 0, 0, 0, 5, false);
    }

    private Account player() {
        return new Account(35L, "player", "player@example.com", "Player", 0, 0, 0, 0, 0, false);
    }
}
