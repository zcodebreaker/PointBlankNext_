package com.pointblank.api.admin;

import java.util.List;

public record AdminAccountPageResponse(
        List<AdminAccountResponse> accounts,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
