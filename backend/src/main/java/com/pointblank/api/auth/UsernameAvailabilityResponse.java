package com.pointblank.api.auth;

public record UsernameAvailabilityResponse(
        String username,
        boolean available) {
}
