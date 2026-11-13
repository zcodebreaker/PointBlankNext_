CREATE TABLE public.download_settings (
    settings_id SMALLINT PRIMARY KEY CHECK (settings_id = 1),
    file_url VARCHAR(2048) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes >= 0),
    file_updated_at TIMESTAMPTZ NOT NULL,
    metadata_checked_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO public.download_settings (
    settings_id,
    file_url,
    file_size_bytes,
    file_updated_at,
    metadata_checked_at
)
VALUES (
    1,
    'https://download-br.zepetto.com/pointblank/ZepettoDownloader_v1.3.exe',
    16192026706,
    TIMESTAMPTZ '2026-03-25 00:00:00+00',
    CURRENT_TIMESTAMP
);
