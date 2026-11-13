ALTER TABLE public.webstore_purchases
    ADD COLUMN IF NOT EXISTS delivered_object_id BIGINT,
    ADD COLUMN IF NOT EXISTS recovery_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS recovered_at TIMESTAMPTZ;

ALTER TABLE public.webstore_purchases
    DROP CONSTRAINT IF EXISTS chk_webstore_purchases_recovery_count;

ALTER TABLE public.webstore_purchases
    ADD CONSTRAINT chk_webstore_purchases_recovery_count
    CHECK (recovery_count BETWEEN 0 AND 1);

CREATE UNIQUE INDEX IF NOT EXISTS uq_webstore_purchases_delivered_object
    ON public.webstore_purchases (delivered_object_id)
    WHERE delivered_object_id IS NOT NULL;
