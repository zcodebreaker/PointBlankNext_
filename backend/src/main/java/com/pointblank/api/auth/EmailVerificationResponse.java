package com.pointblank.api.auth;

public record EmailVerificationResponse(boolean verified, String message) {
}
