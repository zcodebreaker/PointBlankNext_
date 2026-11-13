CREATE TABLE public.web_admin_profiles (
    player_id BIGINT PRIMARY KEY,
    display_name VARCHAR(32) NOT NULL DEFAULT '',
    avatar_data BYTEA,
    avatar_content_type VARCHAR(32),
    avatar_width INTEGER,
    avatar_height INTEGER,
    avatar_version VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_admin_profile_avatar CHECK (
        (avatar_data IS NULL AND avatar_content_type IS NULL AND avatar_width IS NULL
            AND avatar_height IS NULL AND avatar_version IS NULL)
        OR (avatar_data IS NOT NULL AND avatar_content_type IN ('image/jpeg', 'image/png')
            AND avatar_width > 0 AND avatar_height > 0 AND avatar_version IS NOT NULL
            AND octet_length(avatar_data) BETWEEN 1 AND 10485760)
    )
);
