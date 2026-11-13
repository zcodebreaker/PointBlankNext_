package com.pointblank.api.ticket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/tickets")
public class AdminTicketController {

    private final AdminTicketService adminTicketService;

    /**
     * Cria o controlador administrativo de tickets.
     *
     * @param adminTicketService serviço de gerenciamento de tickets
     */
    public AdminTicketController(AdminTicketService adminTicketService) {
        this.adminTicketService = adminTicketService;
    }

    /**
     * Lista tickets com filtros e paginação para o painel administrativo.
     *
     * @param jwt token do administrador autenticado
     * @param search filtro por usuário, assunto ou identificador
     * @param status filtro opcional de status
     * @param page índice da página
     * @param size quantidade máxima de itens
     * @return página de tickets
     */
    @GetMapping
    public TicketPageResponse findAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminTicketService.findAll(playerId(jwt), search, status, page, size);
    }

    /**
     * Retorna um ticket específico para um administrador autorizado.
     *
     * @param jwt token do administrador autenticado
     * @param ticketId identificador do ticket
     * @return ticket solicitado
     */
    @GetMapping("/{ticketId}")
    public TicketResponse findById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long ticketId) {
        return adminTicketService.findById(playerId(jwt), ticketId);
    }

    /**
     * Registra a resposta administrativa de um ticket aberto.
     *
     * @param jwt token do administrador autenticado
     * @param ticketId identificador do ticket
     * @param request assunto, mensagem e estado final da resposta
     * @return ticket atualizado
     */
    @PatchMapping("/{ticketId}/reply")
    public TicketResponse reply(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long ticketId,
            @Valid @RequestBody AdminTicketReplyRequest request) {
        return adminTicketService.reply(playerId(jwt), ticketId, request);
    }

    /**
     * Disponibiliza um anexo de ticket para um administrador autorizado.
     *
     * @param jwt token do administrador autenticado
     * @param ticketId identificador do ticket
     * @param attachmentId identificador do anexo
     * @return conteúdo binário e cabeçalhos do anexo
     */
    @GetMapping("/{ticketId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long ticketId,
            @PathVariable long attachmentId) {
        return TicketController.attachmentResponse(adminTicketService.findAttachment(
                playerId(jwt), ticketId, attachmentId));
    }

    /**
     * Extrai o identificador do administrador do subject do JWT.
     *
     * @param jwt token da requisição autenticada
     * @return identificador do administrador
     */
    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new TicketException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}
