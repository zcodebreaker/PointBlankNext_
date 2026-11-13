package com.pointblank.api.ticket;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

@Service
public class AdminTicketService {

    private static final int ADMIN_ACCESS_LEVEL = 5;

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final AccountRepository accountRepository;

    /**
     * Cria o serviço administrativo de tickets com suas dependências.
     *
     * @param ticketRepository repositório de tickets
     * @param ticketService serviço com as regras compartilhadas de tickets
     * @param accountRepository repositório usado para autorizar administradores
     */
    public AdminTicketService(
            TicketRepository ticketRepository,
            TicketService ticketService,
            AccountRepository accountRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
        this.accountRepository = accountRepository;
    }

    /**
     * Autoriza o administrador e lista tickets com filtros e paginação.
     *
     * @param administratorId identificador do administrador
     * @param search filtro textual
     * @param status filtro de status
     * @param page índice da página
     * @param size quantidade máxima de itens
     * @return página de tickets
     */
    @Transactional(readOnly = true)
    public TicketPageResponse findAll(
            long administratorId,
            String search,
            String status,
            int page,
            int size) {
        requireAdministrator(administratorId);
        validateStatus(status);

        long totalElements = ticketRepository.countAll(search, status);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        List<TicketResponse> tickets = ticketRepository.findAll(search, status, page, size).stream()
                .map(ticketService::toResponse)
                .toList();
        return new TicketPageResponse(tickets, page, size, totalElements, totalPages);
    }

    /**
     * Busca um ticket para um administrador autorizado.
     *
     * @param administratorId identificador do administrador
     * @param ticketId identificador do ticket
     * @return ticket solicitado
     */
    @Transactional(readOnly = true)
    public TicketResponse findById(long administratorId, long ticketId) {
        requireAdministrator(administratorId);
        return ticketService.toResponse(ticketService.requireTicket(ticketId));
    }

    /**
     * Responde um ticket aberto e opcionalmente o encerra.
     *
     * @param administratorId identificador do administrador
     * @param ticketId identificador do ticket
     * @param request dados da resposta
     * @return ticket atualizado
     */
    @Transactional
    public TicketResponse reply(
            long administratorId,
            long ticketId,
            AdminTicketReplyRequest request) {
        requireAdministrator(administratorId);
        SupportTicket ticket = ticketService.requireTicket(ticketId);

        if (ticket.status() == TicketStatus.CLOSED) {
            throw closedTicket();
        }

        if (!ticketRepository.reply(
                ticketId,
                administratorId,
                request.subject().trim(),
                request.message().trim(),
                request.close())) {
            throw closedTicket();
        }
        return ticketService.toResponse(ticketService.requireTicket(ticketId));
    }

    /**
     * Busca um anexo após validar o acesso administrativo e o ticket.
     *
     * @param administratorId identificador do administrador
     * @param ticketId identificador do ticket
     * @param attachmentId identificador do anexo
     * @return conteúdo do anexo
     */
    @Transactional(readOnly = true)
    public TicketAttachmentContent findAttachment(
            long administratorId,
            long ticketId,
            long attachmentId) {
        requireAdministrator(administratorId);
        ticketService.requireTicket(ticketId);
        return ticketService.requireAttachment(ticketId, attachmentId);
    }

    /**
     * Exige uma conta com nível administrativo.
     *
     * @param playerId identificador da conta autenticada
     * @return conta administrativa validada
     */
    private Account requireAdministrator(long playerId) {
        Account account = accountRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new TicketException(HttpStatus.UNAUTHORIZED, "Conta não encontrada"));
        if (account.accessLevel() < ADMIN_ACCESS_LEVEL) {
            throw new TicketException(HttpStatus.FORBIDDEN, "Acesso administrativo necessário");
        }
        return account;
    }

    /**
     * Valida um filtro de status quando ele foi informado.
     *
     * @param status valor recebido no filtro
     */
    private static void validateStatus(String status) {
        if (status == null || status.isBlank()) return;
        try {
            TicketStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new TicketException(HttpStatus.BAD_REQUEST, "Status de ticket inválido");
        }
    }

    /**
     * Cria o erro usado ao tentar responder um ticket encerrado.
     *
     * @return exceção de conflito de ticket
     */
    private static TicketException closedTicket() {
        return new TicketException(HttpStatus.CONFLICT, "Ticket encerrado não aceita novas respostas");
    }
}
