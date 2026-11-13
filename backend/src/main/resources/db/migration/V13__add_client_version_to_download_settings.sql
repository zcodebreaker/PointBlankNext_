ALTER TABLE public.download_settings
    ADD COLUMN client_version VARCHAR(64) NOT NULL DEFAULT '0.2.2';
