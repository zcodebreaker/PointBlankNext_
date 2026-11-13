CREATE TABLE IF NOT EXISTS public.account_email_verifications (
    player_id BIGINT PRIMARY KEY REFERENCES public.accounts(player_id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_account_email_verifications_pending
    ON public.account_email_verifications (expires_at)
    WHERE verified_at IS NULL;
