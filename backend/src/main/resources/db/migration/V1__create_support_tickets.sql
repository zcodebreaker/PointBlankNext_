CREATE TABLE public.support_tickets (
    ticket_id BIGSERIAL NOT NULL,
    player_id BIGINT NOT NULL,
    main_category VARCHAR(50) NOT NULL,
    sub_category VARCHAR(80) NOT NULL,
    subject VARCHAR(128) NOT NULL,
    message VARCHAR(1500) NOT NULL,
    status VARCHAR(20) NOT NULL
        CONSTRAINT df_support_tickets_status DEFAULT 'WAITING',
    admin_reply_subject VARCHAR(128) NULL,
    admin_reply VARCHAR(1500) NULL,
    replied_by BIGINT NULL,
    replied_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL
        CONSTRAINT df_support_tickets_created_at DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL
        CONSTRAINT df_support_tickets_updated_at DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_support_tickets PRIMARY KEY (ticket_id),
    CONSTRAINT ck_support_tickets_status
        CHECK (status IN ('WAITING', 'ANSWERED', 'CLOSED')),
    CONSTRAINT ck_support_tickets_subject_length
        CHECK (CHAR_LENGTH(subject) BETWEEN 6 AND 128),
    CONSTRAINT ck_support_tickets_message_length
        CHECK (CHAR_LENGTH(message) BETWEEN 1 AND 1500),
    CONSTRAINT ck_support_tickets_admin_reply CHECK (
        (admin_reply IS NULL AND admin_reply_subject IS NULL)
        OR (
            admin_reply IS NOT NULL
            AND admin_reply_subject IS NOT NULL
            AND CHAR_LENGTH(admin_reply_subject) BETWEEN 6 AND 128
        )
    )
);

CREATE TABLE public.support_ticket_attachments (
    attachment_id BIGSERIAL NOT NULL,
    ticket_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    file_size INT NOT NULL,
    content BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
        CONSTRAINT df_support_ticket_attachments_created_at DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_support_ticket_attachments PRIMARY KEY (attachment_id),
    CONSTRAINT fk_support_ticket_attachments_ticket
        FOREIGN KEY (ticket_id) REFERENCES public.support_tickets(ticket_id) ON DELETE CASCADE,
    CONSTRAINT ck_support_ticket_attachment_size
        CHECK (file_size > 0 AND file_size <= 2097152)
);

