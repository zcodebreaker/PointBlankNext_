package com.pointblank.api.download;

import org.springframework.http.HttpStatus;

public class DownloadSettingsException extends RuntimeException {

    private final HttpStatus status;

    public DownloadSettingsException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
