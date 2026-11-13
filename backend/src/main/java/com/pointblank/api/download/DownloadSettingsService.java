package com.pointblank.api.download;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

@Service
public class DownloadSettingsService {

    private static final int ADMIN_ACCESS_LEVEL = 5;
    private final DownloadSettingsRepository repository;
    private final DownloadFileMetadataClient metadataClient;
    private final AccountRepository accountRepository;

    public DownloadSettingsService(
            DownloadSettingsRepository repository,
            DownloadFileMetadataClient metadataClient,
            AccountRepository accountRepository) {
        this.repository = repository;
        this.metadataClient = metadataClient;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public DownloadSettings findPublicSettings() {
        return requireSettings();
    }

    @Transactional(readOnly = true)
    public DownloadSettings findAdminSettings(long administratorId) {
        requireAdministrator(administratorId);
        return requireSettings();
    }

    public DownloadSettings update(long administratorId, UpdateDownloadSettingsRequest request) {
        requireAdministrator(administratorId);
        String fileUrl = normalizeHttpUrl(request.fileUrl(), "o arquivo do cliente");
        String clientVersion = request.clientVersion().trim();
        String mirrorTorrentUrl = normalizeHttpUrl(request.mirrorTorrentUrl(), "o Torrent");
        String mirrorGoogleDriveUrl = normalizeHttpUrl(request.mirrorGoogleDriveUrl(), "o Google Drive");
        String mirrorOneDriveUrl = normalizeHttpUrl(request.mirrorOneDriveUrl(), "o OneDrive");
        String patchPrimaryUrl = normalizeHttpUrl(request.patchPrimaryUrl(), "o primeiro patch");
        String patchSecondaryUrl = normalizeHttpUrl(request.patchSecondaryUrl(), "o segundo patch");
        DownloadFileMetadata metadata = metadataClient.inspect(fileUrl);
        OffsetDateTime checkedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return repository.save(
                fileUrl,
                clientVersion,
                mirrorTorrentUrl,
                mirrorGoogleDriveUrl,
                mirrorOneDriveUrl,
                patchPrimaryUrl,
                patchSecondaryUrl,
                metadata.sizeBytes(),
                metadata.updatedAt(),
                checkedAt);
    }

    private String normalizeHttpUrl(String rawValue, String destination) {
        String value = rawValue.trim();
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException();
            }
            return value;
        } catch (IllegalArgumentException exception) {
            throw new DownloadSettingsException(
                    HttpStatus.BAD_REQUEST,
                    "Informe uma URL HTTP ou HTTPS válida para " + destination);
        }
    }

    private DownloadSettings requireSettings() {
        return repository.find().orElseThrow(() -> new DownloadSettingsException(
                HttpStatus.NOT_FOUND,
                "A configuração de download ainda não foi cadastrada"));
    }

    private void requireAdministrator(long administratorId) {
        Account account = accountRepository.findByPlayerId(administratorId)
                .orElseThrow(() -> new DownloadSettingsException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (account.accessLevel() < ADMIN_ACCESS_LEVEL) {
            throw new DownloadSettingsException(HttpStatus.FORBIDDEN, "Acesso restrito a Game Masters");
        }
    }
}
