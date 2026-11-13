package com.pointblank.api.download;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDownloadSettingsRequest(
        @NotBlank(message = "Informe a URL do arquivo")
        @Size(max = 2048, message = "A URL deve possuir no máximo 2048 caracteres")
        String fileUrl,
        @NotBlank(message = "Informe a versão do cliente")
        @Size(max = 64, message = "A versão deve possuir no máximo 64 caracteres")
        String clientVersion,
        @NotBlank(message = "Informe a URL do Torrent")
        @Size(max = 2048, message = "A URL do Torrent deve possuir no máximo 2048 caracteres")
        String mirrorTorrentUrl,
        @NotBlank(message = "Informe a URL do Google Drive")
        @Size(max = 2048, message = "A URL do Google Drive deve possuir no máximo 2048 caracteres")
        String mirrorGoogleDriveUrl,
        @NotBlank(message = "Informe a URL do OneDrive")
        @Size(max = 2048, message = "A URL do OneDrive deve possuir no máximo 2048 caracteres")
        String mirrorOneDriveUrl,
        @NotBlank(message = "Informe a URL do primeiro patch")
        @Size(max = 2048, message = "A URL do primeiro patch deve possuir no máximo 2048 caracteres")
        String patchPrimaryUrl,
        @NotBlank(message = "Informe a URL do segundo patch")
        @Size(max = 2048, message = "A URL do segundo patch deve possuir no máximo 2048 caracteres")
        String patchSecondaryUrl) {
}
