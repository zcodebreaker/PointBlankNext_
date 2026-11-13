ALTER TABLE public.news_articles
    ADD COLUMN carousel_url VARCHAR(500) NULL,
    ADD COLUMN carousel_image_data BYTEA NULL,
    ADD COLUMN carousel_image_content_type VARCHAR(32) NULL,
    ADD COLUMN carousel_image_width INTEGER NULL,
    ADD COLUMN carousel_image_height INTEGER NULL;

UPDATE public.news_articles
SET carousel_url = banner_url;

ALTER TABLE public.news_articles
    ALTER COLUMN carousel_url SET NOT NULL,
    ADD CONSTRAINT ck_news_articles_carousel_metadata
        CHECK (
            (carousel_image_data IS NULL
                AND carousel_image_content_type IS NULL
                AND carousel_image_width IS NULL
                AND carousel_image_height IS NULL)
            OR
            (carousel_image_data IS NOT NULL
                AND carousel_image_content_type IS NOT NULL
                AND carousel_image_width > 0
                AND carousel_image_height > 0)
        ),
    ADD CONSTRAINT ck_news_articles_carousel_size
        CHECK (carousel_image_data IS NULL OR octet_length(carousel_image_data) <= 2097152),
    ADD CONSTRAINT ck_news_articles_carousel_type
        CHECK (
            carousel_image_content_type IS NULL
            OR carousel_image_content_type IN ('image/jpeg', 'image/png')
        );
