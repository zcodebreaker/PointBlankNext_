ALTER TABLE public.accounts
    ADD COLUMN email VARCHAR(255) NOT NULL DEFAULT '';

CREATE UNIQUE INDEX ux_accounts_login_lower
    ON public.accounts (LOWER(login));

CREATE UNIQUE INDEX ux_accounts_email_lower
    ON public.accounts (LOWER(email))
    WHERE email <> '';
