ALTER TABLE public.news_articles
    RENAME COLUMN image_url TO banner_url;

ALTER TABLE public.news_articles
    ADD COLUMN cover_url VARCHAR(500) NULL;

UPDATE public.news_articles
SET cover_url = CASE
    WHEN image_data IS NOT NULL THEN '/api/v1/news/' || news_id || '/cover'
    ELSE banner_url
END;

ALTER TABLE public.news_articles
    ALTER COLUMN cover_url SET NOT NULL;
