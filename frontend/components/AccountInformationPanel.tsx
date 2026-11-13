"use client";

import { type FormEvent, useEffect, useState, useTransition } from "react";
import { changePassword } from "@/app/actions/account";
import type { AccountProfile } from "@/lib/auth-session";

type PasswordMessage = {
  type: "error" | "success";
  text: string;
  errors?: Record<string, string>;
} | null;

const numberFormatter = new Intl.NumberFormat("pt-BR");

function maskEmail(email: string) {
  if (!email) return "Não informado";

  const [local, domain] = email.split("@");
  if (!local || !domain) return email;

  const visibleLength = Math.min(Math.max(2, Math.ceil(local.length / 2)), 8);
  return `${local.slice(0, visibleLength)}${"*".repeat(Math.max(4, local.length - visibleLength))}@${domain}`;
}

function PasswordField({
  id,
  name,
  label,
  autoComplete,
  error,
}: {
  id: string;
  name: string;
  label: string;
  autoComplete: "current-password" | "new-password";
  error?: string;
}) {
  const [visible, setVisible] = useState(false);

  return (
    <>
      <li>
        <label className="tit" htmlFor={id}>{label}</label>
        <div className="inp">
          <input
            id={id}
            name={name}
            type={visible ? "text" : "password"}
            minLength={8}
            maxLength={16}
            autoComplete={autoComplete}
            placeholder="Digite sua senha contendo de 8 a 16 caracteres."
            aria-describedby={error ? `${id}-error` : undefined}
            required
          />
          <button
            type="button"
            className="pw_show"
            aria-label={visible ? "Ocultar senha" : "Mostrar senha"}
            aria-pressed={visible}
            onClick={() => setVisible((current) => !current)}
          />
        </div>
      </li>
      <li id={`${id}-error`} className="error" hidden={!error}>{error ?? "error message"}</li>
    </>
  );
}

export default function AccountInformationPanel({ account }: { account: AccountProfile }) {
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState<PasswordMessage>(null);
  const [emailNotice, setEmailNotice] = useState(false);
  const [isPending, startTransition] = useTransition();

  useEffect(() => {
    if (!passwordOpen) return;

    const previousOverflow = document.body.style.overflow;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setPasswordOpen(false);
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", closeOnEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [passwordOpen]);

  function closePasswordModal() {
    if (isPending) return;
    setPasswordMessage(null);
    setPasswordOpen(false);
  }

  function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    const newPassword = String(formData.get("newPassword") ?? "");
    const confirmation = String(formData.get("newPasswordConfirmation") ?? "");

    if (newPassword !== confirmation) {
      setPasswordMessage({
        type: "error",
        text: "As novas senhas informadas não são iguais.",
        errors: { newPasswordConfirmation: "Repita exatamente a nova senha." },
      });
      return;
    }

    setPasswordMessage(null);
    startTransition(async () => {
      const result = await changePassword(formData);
      setPasswordMessage({
        type: result.success ? "success" : "error",
        text: result.message,
        errors: result.success ? undefined : result.errors,
      });
      if (result.success) form.reset();
    });
  }

  const accessLabel = account.accessLevel > 0 ? "Administrador" : "Jogador";

  return (
    <>
      <div className="my_wrap">
        <div className="my_con">
          <div className="ps_detail">
            <div className="title">Detalhes pessoais</div>

            <div className="line">
              <div className="tit">ID</div>
              <div className="txt">{account.username}</div>
            </div>

            <div className="line">
              <div className="tit">Senha</div>
              <div className="txt">****</div>
              <button type="button" className="btn_edit btn_pw" onClick={() => setPasswordOpen(true)}>Edit</button>
            </div>

            <div className="line">
              <div className="tit">E-mail</div>
              <div className="txt">{maskEmail(account.email)}</div>
              <button type="button" className="btn_edit btn_email" onClick={() => setEmailNotice((visible) => !visible)}>Edit</button>
              {emailNotice ? <div className="require">Alteração de e-mail ainda não disponível.</div> : null}
            </div>

            <div className="line in_person">
              <div className="tit">Personal Data</div>
              <div className="txt">
                <ul className="p_data">
                  <li><span>Player ID</span>{account.playerId}</li>
                  <li className="half"><span>Tipo de conta</span>{accessLabel}</li>
                  <li className="half"><span>Patente</span>Rank {account.rank}</li>
                  <li><span>Experiência</span>{numberFormatter.format(account.experience)}<br />pontos acumulados</li>
                  <li className="half"><span>Gold</span>{numberFormatter.format(account.gold)}</li>
                  <li className="half"><span>Cash</span>{numberFormatter.format(account.cash)}</li>
                </ul>
              </div>
            </div>

            <div className="line">
              <div className="tit">Serviço de e-mail</div>
              <div className="txt">
                <div className="rd_controls">
                  <input id="erd_y" name="email_rd" type="radio" value="1" />
                  <label htmlFor="erd_y">Concordar</label>
                  <input id="erd_n" name="email_rd" type="radio" value="0" defaultChecked />
                  <label htmlFor="erd_n">Discordar</label>
                </div>
              </div>
            </div>

            <div className="line">
              <div className="tit">Mostrar amigos</div>
              <div className="txt">
                <div className="rd_controls">
                  <input id="frd_y" name="friend_rd" type="radio" value="1" defaultChecked />
                  <label htmlFor="frd_y">Concordar</label>
                  <input id="frd_n" name="friend_rd" type="radio" value="0" />
                  <label htmlFor="frd_n">Discordar</label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {passwordOpen ? (
        <div className="layer_pw pop_if profile-password-layer">
          <div className="dimmed" aria-hidden="true" onClick={closePasswordModal} />
          <div className="pop-up pop-my pop-pw pop_in" role="dialog" aria-modal="true" aria-labelledby="change-password-title" style={{ display: "block" }}>
            <div className="pop-layout">
              <h2 id="change-password-title">
                Reiniciar sua senha
                <button type="button" className="pop-close" aria-label="Fechar" onClick={closePasswordModal}>Close</button>
              </h2>
              <form className="pop-con" onSubmit={handlePasswordSubmit}>
                <div className="pop_my_con">
                  <div className="info">Uso um mínimo 8 caracteres e máximo de 16 caracteres, misturando letras e números.</div>
                  <div className="my_edit">
                    <ul className="list">
                      <PasswordField id="current-password" name="currentPassword" label="Senha" autoComplete="current-password" error={passwordMessage?.errors?.currentPassword} />
                      <PasswordField id="new-password" name="newPassword" label="Nova senha" autoComplete="new-password" error={passwordMessage?.errors?.newPassword} />
                      <PasswordField id="new-password-confirmation" name="newPasswordConfirmation" label="Confirmar nova senha" autoComplete="new-password" error={passwordMessage?.errors?.newPasswordConfirmation} />
                    </ul>
                    {passwordMessage ? <div className={`profile-password-message ${passwordMessage.type}`} role="status" aria-live="polite">{passwordMessage.text}</div> : null}
                  </div>
                </div>
                <div className="pop-btns">
                  <button type="button" className="btn_line_r" disabled={isPending} onClick={closePasswordModal}>Cancelar</button>
                  <button type="submit" className="btn_line_r02" disabled={isPending}>{isPending ? "Alterando..." : "OK"}</button>
                </div>
              </form>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
