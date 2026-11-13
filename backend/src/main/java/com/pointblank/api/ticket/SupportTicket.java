package com.pointblank.api.ticket;

import java.time.OffsetDateTime;

public record SupportTicket(
        long ticketId,
        long playerId,
        String username,
        String mainCategory,
        String subCategory,
        String subject,
        String message,
        TicketStatus status,
        String adminReplySubject,
        String adminReply,
        String repliedBy,
        OffsetDateTime repliedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
