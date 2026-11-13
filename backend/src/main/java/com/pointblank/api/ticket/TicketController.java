package com.pointblank.api.ticket;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;

    /**
     * Cria o controlador de tickets do usuário.
     *
     * @param ticketService serviço de tickets
     */
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * Lista todos os tickets pertencentes à conta autenticada.
     *
     * @param jwt token do usuário autenticado
     * @return tickets da conta
     */
    @GetMapping
    public List<TicketResponse> findMine(@AuthenticationPrincipal Jwt jwt) {
        return ticketService.findMine(playerId(jwt));
    }

    /**
     * Busca um ticket específico pertencente à conta autenticada.
     *
     * @param jwt token do usuário autenticado
     * @param ticketId identificador do ticket
     * @return ticket solicitado
     */
    @GetMapping("/{ticketId}")
    public TicketResponse findMineById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long ticketId) {
        return ticketService.findMineById(playerId(jwt), ticketId);
    }

    /**
     * Abre um ticket de suporte com anexos opcionais.
     *
     * @param jwt token do usuário autenticado
     * @param mainCategory categoria principal
     * @param subCategory subcategoria
     * @param subject assunto do ticket
     * @param message dúvida enviada pelo usuário
     * @param attachments imagens anexadas opcionalmente
     * @return ticket criado
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String mainCategory,
            @RequestParam String subCategory,
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(name = "attachments", required = false) List<MultipartFile> attachments) {
        return ticketService.create(
                playerId(jwt),
                mainCategory,
                subCategory,
                subject,
                message,
                attachments);
    }

    /**
     * Disponibiliza um anexo pertencente a um ticket do usuário autenticado.
     *
     * @param jwt token do usuário autenticado
     * @param ticketId identificador do ticket
     * @param attachmentId identificador do anexo
     * @return conteúdo binário e cabeçalhos do anexo
     */
    @GetMapping("/{ticketId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable long ticketId,
            @PathVariable long attachmentId) {
        return attachmentResponse(ticketService.findMineAttachment(
                playerId(jwt), ticketId, attachmentId));
    }

    /**
     * Monta uma resposta HTTP segura para exibir um anexo no navegador.
     *
     * @param attachment conteúdo e metadados do anexo
     * @return resposta binária com tipo e disposição do arquivo
     */
    static ResponseEntity<byte[]> attachmentResponse(TicketAttachmentContent attachment) {
        MediaType mediaType = MediaType.parseMediaType(attachment.contentType());
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(attachment.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(attachment.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(attachment.content());
    }

    /**
     * Extrai o identificador do usuário do subject do JWT.
     *
     * @param jwt token da requisição autenticada
     * @return identificador do jogador
     */
    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new TicketException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}
