package com.pointblank.api.ticket;

import java.util.List;

public record TicketPageResponse(
        List<TicketResponse> tickets,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}

