package com.pointblank.api.auth;

public record EmailAvailabilityResponse(
        String email,
        boolean available) {
}
