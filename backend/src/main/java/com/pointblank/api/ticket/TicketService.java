package com.pointblank.api.ticket;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TicketService {

    private static final int MAX_ATTACHMENTS = 5;
    private static final long MAX_TOTAL_ATTACHMENT_SIZE = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/gif", "image/jpeg", "image/png");
    private static final Map<String, List<String>> CATEGORIES = Map.of(
            "Verificação de Idade", List.of("Verificação de identidade", "Restrição de idade"),
            "Recuperação de Conta", List.of("Esqueci minha senha", "Conta invadida", "Alteração de e-mail"),
            "Bad User", List.of("Denúncia de trapaça", "Comportamento inadequado"),
            "Problemas Técnicos", List.of("Erro ao iniciar", "Desconexão", "Desempenho"),
            "Loja", List.of("Cash não creditado", "Erro na compra", "Item não recebido"),
            "Atendimento Geral", List.of("Dúvida", "Sugestão", "Outros"));

    private final TicketRepository ticketRepository;

    /**
     * Cria o serviço de tickets.
     *
     * @param ticketRepository repositório de tickets e anexos
     */
    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * Lista os tickets pertencentes a um jogador.
     *
     * @param playerId identificador do jogador
     * @return tickets da conta
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> findMine(long playerId) {
        return ticketRepository.findByPlayerId(playerId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Busca um ticket somente quando ele pertence ao jogador.
     *
     * @param playerId identificador do jogador
     * @param ticketId identificador do ticket
     * @return ticket solicitado
     */
    @Transactional(readOnly = true)
    public TicketResponse findMineById(long playerId, long ticketId) {
        return toResponse(requireOwnedTicket(playerId, ticketId));
    }

    /**
     * Valida os dados e anexos antes de criar um ticket.
     *
     * @param playerId identificador do autor
     * @param mainCategory categoria principal
     * @param subCategory subcategoria
     * @param subject assunto
     * @param message mensagem do usuário
     * @param attachments anexos opcionais
     * @return ticket criado
     */
    @Transactional
    public TicketResponse create(
            long playerId,
            String mainCategory,
            String subCategory,
            String subject,
            String message,
            List<MultipartFile> attachments) {
        String normalizedMainCategory = required(mainCategory, "Selecione uma categoria principal");
        String normalizedSubCategory = required(subCategory, "Selecione uma subcategoria");
        String normalizedSubject = required(subject, "Preencha o assunto");
        String normalizedMessage = required(message, "Preencha sua dúvida");

        validateCategory(normalizedMainCategory, normalizedSubCategory);
        if (normalizedSubject.length() < 6 || normalizedSubject.length() > 128) {
            throw badRequest("O assunto deve possuir entre 6 e 128 caracteres");
        }
        if (normalizedMessage.length() > 1500) {
            throw badRequest("A pergunta deve possuir no máximo 1500 caracteres");
        }

        List<MultipartFile> selectedAttachments = attachments == null
                ? List.of()
                : attachments.stream().filter(file -> !file.isEmpty()).toList();
        validateAttachments(selectedAttachments);

        long ticketId = ticketRepository.create(
                playerId,
                normalizedMainCategory,
                normalizedSubCategory,
                normalizedSubject,
                normalizedMessage);

        selectedAttachments.forEach(file -> saveAttachment(ticketId, file));
        return toResponse(requireTicket(ticketId));
    }

    /**
     * Busca um anexo somente quando o ticket pertence ao jogador.
     *
     * @param playerId identificador do jogador
     * @param ticketId identificador do ticket
     * @param attachmentId identificador do anexo
     * @return conteúdo do anexo
     */
    @Transactional(readOnly = true)
    public TicketAttachmentContent findMineAttachment(
            long playerId,
            long ticketId,
            long attachmentId) {
        requireOwnedTicket(playerId, ticketId);
        return requireAttachment(ticketId, attachmentId);
    }

    /**
     * Busca um anexo ou lança um erro quando ele não existe no ticket.
     *
     * @param ticketId identificador do ticket
     * @param attachmentId identificador do anexo
     * @return conteúdo do anexo
     */
    TicketAttachmentContent requireAttachment(long ticketId, long attachmentId) {
        return ticketRepository.findAttachment(ticketId, attachmentId)
                .orElseThrow(() -> new TicketException(HttpStatus.NOT_FOUND, "Anexo não encontrado"));
    }

    /**
     * Acrescenta os metadados dos anexos ao formato de resposta do ticket.
     *
     * @param ticket ticket persistido
     * @return resposta completa do ticket
     */
    TicketResponse toResponse(SupportTicket ticket) {
        return TicketResponse.from(
                ticket,
                ticketRepository.findAttachmentInfo(ticket.ticketId()));
    }

    /**
     * Busca um ticket ou lança um erro quando ele não existe.
     *
     * @param ticketId identificador do ticket
     * @return ticket encontrado
     */
    SupportTicket requireTicket(long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketException(HttpStatus.NOT_FOUND, "Ticket não encontrado"));
    }

    /**
     * Garante que o ticket solicitado pertence ao jogador informado.
     *
     * @param playerId identificador do jogador
     * @param ticketId identificador do ticket
     * @return ticket validado
     */
    private SupportTicket requireOwnedTicket(long playerId, long ticketId) {
        SupportTicket ticket = requireTicket(ticketId);
        if (ticket.playerId() != playerId) {
            throw new TicketException(HttpStatus.NOT_FOUND, "Ticket não encontrado");
        }
        return ticket;
    }

    /**
     * Confirma que a subcategoria pertence à categoria selecionada.
     *
     * @param mainCategory categoria principal
     * @param subCategory subcategoria
     */
    private void validateCategory(String mainCategory, String subCategory) {
        List<String> subCategories = CATEGORIES.get(mainCategory);
        if (subCategories == null || !subCategories.contains(subCategory)) {
            throw badRequest("A categoria ou subcategoria selecionada é inválida");
        }
    }

    /**
     * Valida quantidade, tamanho total e tipo dos anexos.
     *
     * @param attachments arquivos enviados pelo usuário
     */
    private void validateAttachments(List<MultipartFile> attachments) {
        if (attachments.size() > MAX_ATTACHMENTS) {
            throw badRequest("Envie no máximo 5 anexos");
        }

        long totalSize = 0;
        for (MultipartFile attachment : attachments) {
            totalSize += attachment.getSize();
            String contentType = attachment.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
                throw badRequest("Somente arquivos GIF, JPG e PNG são permitidos");
            }
        }

        if (totalSize > MAX_TOTAL_ATTACHMENT_SIZE) {
            throw badRequest("Os anexos devem possuir no máximo 2 MB no total");
        }
    }

    /**
     * Lê e persiste um anexo previamente validado.
     *
     * @param ticketId identificador do ticket
     * @param attachment arquivo enviado
     */
    private void saveAttachment(long ticketId, MultipartFile attachment) {
        try {
            ticketRepository.addAttachment(
                    ticketId,
                    safeFileName(attachment.getOriginalFilename()),
                    attachment.getContentType().toLowerCase(Locale.ROOT),
                    attachment.getBytes());
        } catch (IOException exception) {
            throw new TicketException(HttpStatus.BAD_REQUEST, "Não foi possível ler um dos anexos");
        }
    }

    /**
     * Normaliza um texto obrigatório e rejeita valores vazios.
     *
     * @param value valor recebido
     * @param message mensagem usada em caso de ausência
     * @return texto aparado
     */
    private static String required(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw badRequest(message);
        }
        return normalized;
    }

    /**
     * Remove diretórios e caracteres inseguros do nome de um anexo.
     *
     * @param fileName nome original do arquivo
     * @return nome seguro limitado a 255 caracteres
     */
    private static String safeFileName(String fileName) {
        String normalized = fileName == null ? "anexo" : fileName.replace('\\', '/');
        int separator = normalized.lastIndexOf('/');
        String baseName = separator >= 0 ? normalized.substring(separator + 1) : normalized;
        String safeName = baseName.replaceAll("[^A-Za-z0-9._ -]", "_").trim();
        return safeName.isEmpty() ? "anexo" : safeName.substring(0, Math.min(safeName.length(), 255));
    }

    /**
     * Cria um erro padronizado para dados inválidos de ticket.
     *
     * @param message descrição da validação que falhou
     * @return exceção de ticket com status 400
     */
    private static TicketException badRequest(String message) {
        return new TicketException(HttpStatus.BAD_REQUEST, message);
    }
}
