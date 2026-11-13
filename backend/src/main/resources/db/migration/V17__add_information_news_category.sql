ALTER TABLE public.news_articles
    DROP CONSTRAINT IF EXISTS news_articles_category_check;

ALTER TABLE public.news_articles
    ADD CONSTRAINT news_articles_category_check
    CHECK (category IN ('PROMO', 'EVENT', 'PATCH', 'ESPORTS', 'INFO'));
