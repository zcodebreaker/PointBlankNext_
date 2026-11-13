CREATE TABLE IF NOT EXISTS public.webstore_purchase_entitlements (
    player_id BIGINT NOT NULL,
    item_id INTEGER NOT NULL,
    purchase_id BIGINT UNIQUE REFERENCES public.webstore_purchases (purchase_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_id, item_id)
);

INSERT INTO public.webstore_purchase_entitlements (player_id, item_id, purchase_id, created_at)
SELECT DISTINCT ON (player_id, item_id)
       player_id, item_id, purchase_id, purchased_at
FROM public.webstore_purchases
ORDER BY player_id, item_id, purchased_at, purchase_id
ON CONFLICT (player_id, item_id) DO NOTHING;
