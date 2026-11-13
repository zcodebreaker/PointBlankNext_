-- V3 named the original constraint ck_news_articles_category. V17 replaced only
-- news_articles_category_check, leaving the old restriction active on upgraded databases.
ALTER TABLE public.news_articles
    DROP CONSTRAINT IF EXISTS ck_news_articles_category,
    DROP CONSTRAINT IF EXISTS news_articles_category_check;

ALTER TABLE public.news_articles
    ADD CONSTRAINT ck_news_articles_category
    CHECK (category IN ('PROMO', 'EVENT', 'PATCH', 'ESPORTS', 'INFO'));
