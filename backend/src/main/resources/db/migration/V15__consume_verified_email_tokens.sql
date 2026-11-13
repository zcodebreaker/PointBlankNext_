ALTER TABLE public.account_email_verifications
    ALTER COLUMN token_hash DROP NOT NULL;

UPDATE public.account_email_verifications
SET token_hash = NULL
WHERE verified_at IS NOT NULL;

ALTER TABLE public.account_email_verifications
    ADD CONSTRAINT ck_account_email_verification_token_lifecycle
    CHECK (
        (verified_at IS NULL AND token_hash IS NOT NULL)
        OR (verified_at IS NOT NULL AND token_hash IS NULL)
    );
