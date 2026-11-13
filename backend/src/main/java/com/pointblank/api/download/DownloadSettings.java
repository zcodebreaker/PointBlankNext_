package com.pointblank.api.download;

import java.time.OffsetDateTime;

public record DownloadSettings(
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
}
