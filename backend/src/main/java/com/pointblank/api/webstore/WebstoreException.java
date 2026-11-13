package com.pointblank.api.webstore;

import org.springframework.http.HttpStatus;

public class WebstoreException extends RuntimeException {

    private final HttpStatus status;

    public WebstoreException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
