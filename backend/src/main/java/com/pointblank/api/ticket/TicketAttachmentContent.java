package com.pointblank.api.ticket;

public record TicketAttachmentContent(
        long attachmentId,
        String fileName,
        String contentType,
        byte[] content) {
}

