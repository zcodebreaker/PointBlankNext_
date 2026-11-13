package com.pointblank.api.ticket;

public record TicketAttachmentInfo(
        long attachmentId,
        String fileName,
        String contentType,
        int fileSize) {
}

