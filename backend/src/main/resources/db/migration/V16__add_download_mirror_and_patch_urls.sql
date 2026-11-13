ALTER TABLE public.download_settings
    ADD COLUMN mirror_torrent_url VARCHAR(2048) NOT NULL
        DEFAULT 'https://download-br.zepetto.com/pointblank/PointBlank_SetupFull_20260209.torrent',
    ADD COLUMN mirror_google_drive_url VARCHAR(2048) NOT NULL
        DEFAULT 'https://drive.google.com/file/d/1cx2YhZuzUH74RSNJL9WZ6eYmVGmAuJA_/view?usp=sharing',
    ADD COLUMN mirror_one_drive_url VARCHAR(2048) NOT NULL
        DEFAULT 'https://1drv.ms/u/c/9cf48022fcd0db06/IQCpI3CHfrbSRbUfwpefRfhbAYt6GFc5SV6o20p5wH8d8d8?e=5zFhZR',
    ADD COLUMN patch_primary_url VARCHAR(2048) NOT NULL
        DEFAULT 'https://download-br.zepetto.com/pointblank/Patch_PointBlank_Ver261.exe',
    ADD COLUMN patch_secondary_url VARCHAR(2048) NOT NULL
        DEFAULT 'https://download-br.zepetto.com/pointblank/Patch_PointBlank_Ver260.exe';
