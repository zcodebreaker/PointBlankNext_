ALTER TABLE public.webstore_catalog
    ADD COLUMN IF NOT EXISTS display_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cover_filename VARCHAR(255),
    ADD COLUMN IF NOT EXISTS cover_content_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS cover_data BYTEA;

