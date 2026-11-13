package com.pointblank.api.auth;

import com.pointblank.api.account.AccountResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AccountResponse account) {
}

