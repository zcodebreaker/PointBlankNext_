CREATE TABLE IF NOT EXISTS public.webstore_catalog (
    good_id INTEGER PRIMARY KEY,
    image_path VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS public.webstore_purchases (
    purchase_id BIGSERIAL PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,
    player_id BIGINT NOT NULL,
    good_id INTEGER NOT NULL,
    item_id INTEGER NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    price_cash INTEGER NOT NULL CHECK (price_cash >= 0),
    item_count BIGINT NOT NULL CHECK (item_count > 0),
    cash_after INTEGER NOT NULL CHECK (cash_after >= 0),
    purchased_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webstore_purchases_player_date
    ON public.webstore_purchases (player_id, purchased_at DESC);

INSERT INTO public.webstore_catalog (good_id, image_path, sort_order, active) VALUES
    (10352312, '/images/webstore/1432112191001.png', 10, TRUE),
    (10343312, '/images/webstore/1432250361001.png', 20, TRUE),
    (30126512, '/images/webstore/1427350941001.png', 30, TRUE),
    (10535512, '/images/webstore/1128078071001.png', 40, TRUE),
    (10535712, '/images/webstore/1126540831001.png', 50, TRUE),
    (10456612, '/images/webstore/1428458661001.png', 60, TRUE),
    (20214012, '/images/webstore/1427186241001.png', 70, TRUE),
    (30126812, '/images/webstore/1428107471001.png', 80, TRUE)
ON CONFLICT (good_id) DO UPDATE SET
    image_path = EXCLUDED.image_path,
    sort_order = EXCLUDED.sort_order;
