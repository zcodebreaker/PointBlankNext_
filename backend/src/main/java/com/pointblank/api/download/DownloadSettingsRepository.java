package com.pointblank.api.download;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class DownloadSettingsRepository {

    private final JdbcClient jdbcClient;

    public DownloadSettingsRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<DownloadSettings> find() {
        return jdbcClient.sql("""
                SELECT file_url, client_version,
                       mirror_torrent_url, mirror_google_drive_url, mirror_one_drive_url,
                       patch_primary_url, patch_secondary_url,
                       file_size_bytes, file_updated_at, metadata_checked_at
                FROM public.download_settings
                WHERE settings_id = 1
                """)
                .query((resultSet, rowNum) -> new DownloadSettings(
                        resultSet.getString("file_url"),
                        resultSet.getString("client_version"),
                        resultSet.getString("mirror_torrent_url"),
                        resultSet.getString("mirror_google_drive_url"),
                        resultSet.getString("mirror_one_drive_url"),
                        resultSet.getString("patch_primary_url"),
                        resultSet.getString("patch_secondary_url"),
                        resultSet.getLong("file_size_bytes"),
                        resultSet.getObject("file_updated_at", OffsetDateTime.class),
                        resultSet.getObject("metadata_checked_at", OffsetDateTime.class)))
                .optional();
    }

    public DownloadSettings save(
            String fileUrl,
            String clientVersion,
            String mirrorTorrentUrl,
            String mirrorGoogleDriveUrl,
            String mirrorOneDriveUrl,
            String patchPrimaryUrl,
            String patchSecondaryUrl,
            long fileSizeBytes,
            OffsetDateTime fileUpdatedAt,
            OffsetDateTime metadataCheckedAt) {
        jdbcClient.sql("""
                INSERT INTO public.download_settings (
                    settings_id, file_url, client_version,
                    mirror_torrent_url, mirror_google_drive_url, mirror_one_drive_url,
                    patch_primary_url, patch_secondary_url,
                    file_size_bytes, file_updated_at, metadata_checked_at, updated_at
                )
                VALUES (
                    1, :fileUrl, :clientVersion,
                    :mirrorTorrentUrl, :mirrorGoogleDriveUrl, :mirrorOneDriveUrl,
                    :patchPrimaryUrl, :patchSecondaryUrl,
                    :fileSizeBytes, :fileUpdatedAt, :metadataCheckedAt, CURRENT_TIMESTAMP
                )
                ON CONFLICT (settings_id) DO UPDATE SET
                    file_url = EXCLUDED.file_url,
                    client_version = EXCLUDED.client_version,
                    mirror_torrent_url = EXCLUDED.mirror_torrent_url,
                    mirror_google_drive_url = EXCLUDED.mirror_google_drive_url,
                    mirror_one_drive_url = EXCLUDED.mirror_one_drive_url,
                    patch_primary_url = EXCLUDED.patch_primary_url,
                    patch_secondary_url = EXCLUDED.patch_secondary_url,
                    file_size_bytes = EXCLUDED.file_size_bytes,
                    file_updated_at = EXCLUDED.file_updated_at,
                    metadata_checked_at = EXCLUDED.metadata_checked_at,
                    updated_at = CURRENT_TIMESTAMP
                """)
                .param("fileUrl", fileUrl)
                .param("clientVersion", clientVersion)
                .param("mirrorTorrentUrl", mirrorTorrentUrl)
                .param("mirrorGoogleDriveUrl", mirrorGoogleDriveUrl)
                .param("mirrorOneDriveUrl", mirrorOneDriveUrl)
                .param("patchPrimaryUrl", patchPrimaryUrl)
                .param("patchSecondaryUrl", patchSecondaryUrl)
                .param("fileSizeBytes", fileSizeBytes)
                .param("fileUpdatedAt", fileUpdatedAt)
                .param("metadataCheckedAt", metadataCheckedAt)
                .update();
        return find().orElseThrow();
    }
}
