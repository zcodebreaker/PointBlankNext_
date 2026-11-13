CREATE TABLE public.news_articles (
    news_id BIGSERIAL NOT NULL,
    author_id BIGINT NOT NULL,
    category VARCHAR(16) NOT NULL,
    title VARCHAR(160) NOT NULL,
    summary VARCHAR(300) NOT NULL
        CONSTRAINT df_news_articles_summary DEFAULT '',
    content TEXT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    published BOOLEAN NOT NULL
        CONSTRAINT df_news_articles_published DEFAULT TRUE,
    published_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL
        CONSTRAINT df_news_articles_created_at DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL
        CONSTRAINT df_news_articles_updated_at DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_news_articles PRIMARY KEY (news_id),
    CONSTRAINT ck_news_articles_category
        CHECK (category IN ('PROMO', 'EVENT', 'PATCH', 'ESPORTS')),
    CONSTRAINT ck_news_articles_published_at
        CHECK (NOT published OR published_at IS NOT NULL)
);

CREATE INDEX ix_news_articles_publication
    ON public.news_articles (published, published_at DESC, news_id DESC);

CREATE INDEX ix_news_articles_category
    ON public.news_articles (category, published_at DESC);

