CREATE INDEX ix_support_tickets_player_created
    ON public.support_tickets (player_id, created_at DESC);

CREATE INDEX ix_support_tickets_status_created
    ON public.support_tickets (status, created_at DESC);

CREATE INDEX ix_support_ticket_attachments_ticket
    ON public.support_ticket_attachments (ticket_id);

