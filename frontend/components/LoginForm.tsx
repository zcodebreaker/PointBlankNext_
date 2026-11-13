"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type FormEvent, useState, useTransition } from "react";
import { loginAccount } from "@/app/actions/auth";
import PasswordInput from "@/components/PasswordInput";
import TurnstileWidget from "@/components/TurnstileWidget";

type FormMessage = { type: "error" | "info"; text: string } | null;

export default function LoginForm({ turnstileSiteKey }: { turnstileSiteKey: string }) {
  const router = useRouter();
  const [message, setMessage] = useState<FormMessage>(null);
  const [successPopup, setSuccessPopup] = useState<string | null>(null);
  const [turnstileToken, setTurnstileToken] = useState("");
  const [turnstileResetSignal, setTurnstileResetSignal] = useState(0);
  const [isPending, startTransition] = useTransition();

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    if (!turnstileToken) {
      setMessage({ type: "error", text: "Conclua a verificação de segurança." });
      return;
    }

    formData.set("turnstileToken", turnstileToken);
    setMessage(null);
    setSuccessPopup(null);

    startTransition(async () => {
      const result = await loginAccount(formData);
      if (result.success) {
        setSuccessPopup(result.message);
      } else {
        window.alert(result.message);
        setTurnstileToken("");
        setTurnstileResetSignal((current) => current + 1);
      }
    });
  }

  function continueAfterLogin() {
    setSuccessPopup(null);
    router.replace("/");
  }

  return (
    <>
      <form className="official-register-panel official-login-panel" onSubmit={handleSubmit}>
        <ul className="official-register-fields official-login-fields">
          <li>
            <label className="official-register-label" htmlFor="login-username">ID</label>
            <div className="official-register-input">
              <input
                id="login-username"
                name="username"
                type="text"
                maxLength={20}
                autoComplete="username"
                placeholder="ID"
                required
              />
            </div>
          </li>

          <li>
            <label className="official-register-label" htmlFor="login-password">Senha</label>
            <PasswordInput id="login-password" name="password" autoComplete="current-password" placeholder="Senha" />
          </li>

          <li className="official-login-captcha">
            <TurnstileWidget
              siteKey={turnstileSiteKey}
              action="login"
              resetSignal={turnstileResetSignal}
              onTokenChange={setTurnstileToken}
            />
          </li>
        </ul>

        {message ? <p className={`auth-message official-login-message ${message.type}`} role="status" aria-live="polite">{message.text}</p> : null}

        <button className="official-register-submit official-login-submit" type="submit" disabled={isPending || !turnstileToken}>
          {isPending ? "Autenticando..." : "Login"}
        </button>

        <div className="official-register-login-row official-login-actions">
          <button
            type="button"
            className="official-register-login-text official-login-forgot"
            onClick={() => setMessage({ type: "info", text: "A recuperação de senha será disponibilizada em breve." })}
          >
            Esqueceu seu Senha?
          </button>
          <Link className="official-register-login-button" href="/member/signup">Cadastro</Link>
        </div>
      </form>

      {successPopup ? (
        <div className="official-login-success-overlay">
          <div
            className="official-login-success-popup"
            role="dialog"
            aria-modal="true"
            aria-labelledby="login-success-title"
            aria-describedby="login-success-description"
            onKeyDown={(event) => {
              if (event.key === "Escape") continueAfterLogin();
              if (event.key === "Tab") event.preventDefault();
            }}
          >
            <span className="official-login-success-icon" aria-hidden="true">✓</span>
            <h2 id="login-success-title">Login realizado</h2>
            <p id="login-success-description">{successPopup}</p>
            <button type="button" autoFocus onClick={continueAfterLogin}>Continuar</button>
          </div>
        </div>
      ) : null}
    </>
  );
}
