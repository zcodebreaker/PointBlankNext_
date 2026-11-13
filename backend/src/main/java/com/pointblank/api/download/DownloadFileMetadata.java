package com.pointblank.api.download;

import java.time.OffsetDateTime;

record DownloadFileMetadata(long sizeBytes, OffsetDateTime updatedAt) {
}
