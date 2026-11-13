package com.pointblank.api.ticket;

import java.time.OffsetDateTime;
import java.util.List;

public record TicketResponse(
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
        OffsetDateTime updatedAt,
        List<TicketAttachmentInfo> attachments) {

    /**
     * Converte um ticket persistido no formato retornado pela API.
     *
     * @param ticket ticket de origem
     * @param attachments metadados dos anexos relacionados
     * @return resposta completa do ticket
     */
    public static TicketResponse from(SupportTicket ticket, List<TicketAttachmentInfo> attachments) {
        return new TicketResponse(
                ticket.ticketId(),
                ticket.playerId(),
                ticket.username(),
                ticket.mainCategory(),
                ticket.subCategory(),
                ticket.subject(),
                ticket.message(),
                ticket.status(),
                ticket.adminReplySubject(),
                ticket.adminReply(),
                ticket.repliedBy(),
                ticket.repliedAt(),
                ticket.createdAt(),
                ticket.updatedAt(),
                attachments);
    }
}
