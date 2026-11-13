ALTER TABLE public.news_articles
    ADD COLUMN image_data BYTEA NULL,
    ADD COLUMN image_content_type VARCHAR(32) NULL,
    ADD COLUMN image_width INTEGER NULL,
    ADD COLUMN image_height INTEGER NULL;

ALTER TABLE public.news_articles
    ADD CONSTRAINT ck_news_articles_cover_metadata
        CHECK (
            (image_data IS NULL AND image_content_type IS NULL AND image_width IS NULL AND image_height IS NULL)
            OR
            (image_data IS NOT NULL AND image_content_type IS NOT NULL AND image_width > 0 AND image_height > 0)
        ),
    ADD CONSTRAINT ck_news_articles_cover_size
        CHECK (image_data IS NULL OR octet_length(image_data) <= 2097152),
    ADD CONSTRAINT ck_news_articles_cover_type
        CHECK (image_content_type IS NULL OR image_content_type IN ('image/jpeg', 'image/png'));
