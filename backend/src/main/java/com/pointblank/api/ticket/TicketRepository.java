package com.pointblank.api.ticket;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRepository {

    private static final String TICKET_COLUMNS = """
            t.ticket_id, t.player_id, player.login AS username,
            t.main_category, t.sub_category, t.subject, t.message, t.status,
            t.admin_reply_subject, t.admin_reply, responder.login AS replied_by_username,
            t.replied_at, t.created_at, t.updated_at
            """;

    private final JdbcClient jdbcClient;

    /**
     * Cria o repositório usando o cliente JDBC configurado pela aplicação.
     *
     * @param jdbcClient cliente usado para executar comandos SQL
     */
    public TicketRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Persiste um novo ticket de suporte.
     *
     * @param playerId identificador do autor
     * @param mainCategory categoria principal
     * @param subCategory subcategoria
     * @param subject assunto
     * @param message mensagem do usuário
     * @return identificador do ticket criado
     */
    public long create(
            long playerId,
            String mainCategory,
            String subCategory,
            String subject,
            String message) {
        return jdbcClient.sql("""
                INSERT INTO public.support_tickets
                    (player_id, main_category, sub_category, subject, message)
                VALUES
                    (:playerId, :mainCategory, :subCategory, :subject, :message)
                RETURNING ticket_id
                """)
                .param("playerId", playerId)
                .param("mainCategory", mainCategory)
                .param("subCategory", subCategory)
                .param("subject", subject)
                .param("message", message)
                .query(Long.class)
                .single();
    }

    /**
     * Persiste um arquivo anexado a um ticket.
     *
     * @param ticketId identificador do ticket
     * @param fileName nome seguro do arquivo
     * @param contentType tipo MIME validado
     * @param content conteúdo binário
     */
    public void addAttachment(
            long ticketId,
            String fileName,
            String contentType,
            byte[] content) {
        jdbcClient.sql("""
                INSERT INTO public.support_ticket_attachments
                    (ticket_id, file_name, content_type, file_size, content)
                VALUES
                    (:ticketId, :fileName, :contentType, :fileSize, :content)
                """)
                .param("ticketId", ticketId)
                .param("fileName", fileName)
                .param("contentType", contentType)
                .param("fileSize", content.length)
                .param("content", content)
                .update();
    }

    /**
     * Lista os tickets de um jogador do mais recente para o mais antigo.
     *
     * @param playerId identificador do jogador
     * @return tickets pertencentes à conta
     */
    public List<SupportTicket> findByPlayerId(long playerId) {
        return baseQuery("""
                WHERE t.player_id = :playerId
                ORDER BY t.created_at DESC
                """)
                .param("playerId", playerId)
                .query(this::mapTicket)
                .list();
    }

    /**
     * Busca um ticket pelo identificador.
     *
     * @param ticketId identificador do ticket
     * @return ticket encontrado, se existir
     */
    public Optional<SupportTicket> findById(long ticketId) {
        return baseQuery("WHERE t.ticket_id = :ticketId")
                .param("ticketId", ticketId)
                .query(this::mapTicket)
                .optional();
    }

    /**
     * Lista tickets com filtros e paginação para o painel administrativo.
     *
     * @param search filtro textual
     * @param status filtro de status
     * @param page índice da página
     * @param size quantidade máxima de itens
     * @return tickets da página solicitada
     */
    public List<SupportTicket> findAll(
            String search,
            String status,
            int page,
            int size) {
        String normalizedSearch = normalize(search);
        String normalizedStatus = normalizeStatus(status);

        return baseQuery("""
                WHERE (:search = ''
                    OR LOWER(player.login) LIKE :searchPattern
                    OR LOWER(t.subject) LIKE :searchPattern
                    OR CAST(t.ticket_id AS VARCHAR) = :search)
                  AND (:status = '' OR t.status = :status)
                ORDER BY
                    CASE WHEN t.status = 'WAITING' THEN 0 ELSE 1 END,
                    t.created_at DESC
                LIMIT :size OFFSET :offset
                """)
                .param("search", normalizedSearch)
                .param("searchPattern", "%" + normalizedSearch + "%")
                .param("status", normalizedStatus)
                .param("size", size)
                .param("offset", (long) page * size)
                .query(this::mapTicket)
                .list();
    }

    /**
     * Conta os tickets que atendem aos filtros informados.
     *
     * @param search filtro textual
     * @param status filtro de status
     * @return total de tickets encontrados
     */
    public long countAll(String search, String status) {
        String normalizedSearch = normalize(search);
        String normalizedStatus = normalizeStatus(status);

        return jdbcClient.sql("""
                SELECT COUNT(*)
                FROM public.support_tickets t
                JOIN public.accounts player ON player.player_id = t.player_id
                WHERE (:search = ''
                    OR LOWER(player.login) LIKE :searchPattern
                    OR LOWER(t.subject) LIKE :searchPattern
                    OR CAST(t.ticket_id AS VARCHAR) = :search)
                  AND (:status = '' OR t.status = :status)
                """)
                .param("search", normalizedSearch)
                .param("searchPattern", "%" + normalizedSearch + "%")
                .param("status", normalizedStatus)
                .query(Long.class)
                .single();
    }

    /**
     * Lista os metadados dos anexos de um ticket sem carregar seus conteúdos.
     *
     * @param ticketId identificador do ticket
     * @return metadados dos anexos
     */
    public List<TicketAttachmentInfo> findAttachmentInfo(long ticketId) {
        return jdbcClient.sql("""
                SELECT attachment_id, file_name, content_type, file_size
                FROM public.support_ticket_attachments
                WHERE ticket_id = :ticketId
                ORDER BY attachment_id
                """)
                .param("ticketId", ticketId)
                .query((resultSet, rowNum) -> new TicketAttachmentInfo(
                        resultSet.getLong("attachment_id"),
                        resultSet.getString("file_name"),
                        resultSet.getString("content_type"),
                        resultSet.getInt("file_size")))
                .list();
    }

    /**
     * Busca o conteúdo completo de um anexo relacionado a um ticket.
     *
     * @param ticketId identificador do ticket
     * @param attachmentId identificador do anexo
     * @return anexo encontrado, se existir
     */
    public Optional<TicketAttachmentContent> findAttachment(long ticketId, long attachmentId) {
        return jdbcClient.sql("""
                SELECT attachment_id, file_name, content_type, content
                FROM public.support_ticket_attachments
                WHERE ticket_id = :ticketId AND attachment_id = :attachmentId
                """)
                .param("ticketId", ticketId)
                .param("attachmentId", attachmentId)
                .query((resultSet, rowNum) -> new TicketAttachmentContent(
                        resultSet.getLong("attachment_id"),
                        resultSet.getString("file_name"),
                        resultSet.getString("content_type"),
                        resultSet.getBytes("content")))
                .optional();
    }

    /**
     * Registra a resposta administrativa quando o ticket ainda não foi encerrado.
     *
     * @param ticketId identificador do ticket
     * @param administratorId identificador de quem respondeu
     * @param subject assunto da resposta
     * @param message conteúdo da resposta
     * @param close indica se o ticket deve ser encerrado
     * @return {@code true} quando o ticket foi atualizado
     */
    public boolean reply(
            long ticketId,
            long administratorId,
            String subject,
            String message,
            boolean close) {
        return jdbcClient.sql("""
                UPDATE public.support_tickets
                SET admin_reply_subject = :subject,
                    admin_reply = :message,
                    replied_by = :administratorId,
                    replied_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP,
                    status = :status
                WHERE ticket_id = :ticketId
                  AND status <> 'CLOSED'
                """)
                .param("subject", subject)
                .param("message", message)
                .param("administratorId", administratorId)
                .param("status", close ? TicketStatus.CLOSED.name() : TicketStatus.ANSWERED.name())
                .param("ticketId", ticketId)
                .update() == 1;
    }

    /**
     * Monta a consulta-base de tickets com os relacionamentos necessários.
     *
     * @param conditions cláusulas adicionais de filtro, ordenação e paginação
     * @return especificação JDBC pronta para receber parâmetros
     */
    private JdbcClient.StatementSpec baseQuery(String conditions) {
        return jdbcClient.sql("""
                SELECT %s
                FROM public.support_tickets t
                JOIN public.accounts player ON player.player_id = t.player_id
                LEFT JOIN public.accounts responder ON responder.player_id = t.replied_by
                %s
                """.formatted(TICKET_COLUMNS, conditions));
    }

    /**
     * Converte uma linha do resultado SQL em um ticket.
     *
     * @param resultSet resultado posicionado na linha a converter
     * @param rowNum número da linha informado pelo JDBC
     * @return ticket mapeado
     * @throws SQLException quando uma coluna não puder ser lida
     */
    private SupportTicket mapTicket(ResultSet resultSet, int rowNum) throws SQLException {
        return new SupportTicket(
                resultSet.getLong("ticket_id"),
                resultSet.getLong("player_id"),
                resultSet.getString("username"),
                resultSet.getString("main_category"),
                resultSet.getString("sub_category"),
                resultSet.getString("subject"),
                resultSet.getString("message"),
                TicketStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("admin_reply_subject"),
                resultSet.getString("admin_reply"),
                resultSet.getString("replied_by_username"),
                resultSet.getObject("replied_at", OffsetDateTime.class),
                resultSet.getObject("created_at", OffsetDateTime.class),
                resultSet.getObject("updated_at", OffsetDateTime.class));
    }

    /**
     * Normaliza texto livre para uma pesquisa sem diferença de caixa.
     *
     * @param value valor recebido no filtro
     * @return texto aparado em minúsculas ou vazio
     */
    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    /**
     * Normaliza o status para o formato persistido no banco.
     *
     * @param value valor recebido no filtro
     * @return status aparado em maiúsculas ou vazio
     */
    private static String normalizeStatus(String value) {
        return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
