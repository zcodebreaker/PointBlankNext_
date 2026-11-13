"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { type ChangeEvent, type FormEvent, useRef, useState, useTransition } from "react";
import {
  type AvailabilityResult,
  checkEmailAvailability,
  checkUsernameAvailability,
  registerAccount,
} from "@/app/actions/auth";
import PasswordInput from "@/components/PasswordInput";
import TurnstileWidget from "@/components/TurnstileWidget";

const USERNAME_PATTERN = /^[A-Za-z0-9_]{4,16}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+$/;

type FormMessage = { type: "error" | "info" | "success"; text: string } | null;
type AvailabilityStatus = "idle" | "checking" | "available" | "unavailable" | "error";
type Availability = { status: AvailabilityStatus; message?: string };
type AvailabilityChecker = (candidate: string) => Promise<AvailabilityResult>;

type AvailabilityConfig = {
  valid: (candidate: string) => boolean;
  check: AvailabilityChecker;
  availableMessage: string;
  unavailableMessage: string;
};

const usernameConfig: AvailabilityConfig = {
  valid: (candidate) => USERNAME_PATTERN.test(candidate),
  check: checkUsernameAvailability,
  availableMessage: "Nome de usuário disponível.",
  unavailableMessage: "Este nome de usuário já está em uso.",
};

const emailConfig: AvailabilityConfig = {
  valid: (candidate) => candidate.length <= 255 && EMAIL_PATTERN.test(candidate),
  check: checkEmailAvailability,
  availableMessage: "E-mail disponível.",
  unavailableMessage: "Este e-mail já está em uso.",
};

function useAvailability(value: string, config: AvailabilityConfig) {
  const [availability, setAvailability] = useState<Availability>({ status: "idle" });
  const sequence = useRef(0);

  const applyResult = (result: AvailabilityResult, currentSequence: number) => {
    if (currentSequence !== sequence.current) return;
    if (!result.success) {
      setAvailability({ status: "error", message: result.message });
      return;
    }
    setAvailability(result.available
      ? { status: "available", message: config.availableMessage }
      : { status: "unavailable", message: config.unavailableMessage });
  };

  const reset = () => {
    sequence.current += 1;
    setAvailability({ status: "idle" });
  };

  const checkNow = async () => {
    const candidate = value.trim();
    const currentSequence = ++sequence.current;

    if (!config.valid(candidate)) {
      setAvailability({ status: "error", message: "Confira o formato e a quantidade de caracteres." });
      return;
    }

    setAvailability({ status: "checking", message: "Verificando disponibilidade..." });
    applyResult(await config.check(candidate), currentSequence);
  };

  return { availability, reset, checkNow };
}

function AvailabilityMessage({ id, availability }: { id: string; availability: Availability }) {
  if (availability.status === "idle") return null;
  return <small id={id} className={`field-availability ${availability.status}`} role="status" aria-live="polite">{availability.message}</small>;
}

export default function RegisterForm({ turnstileSiteKey }: { turnstileSiteKey: string }) {
  const router = useRouter();
  const [message, setMessage] = useState<FormMessage>(null);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [turnstileToken, setTurnstileToken] = useState("");
  const [turnstileResetSignal, setTurnstileResetSignal] = useState(0);
  const [isPending, startTransition] = useTransition();
  const usernameState = useAvailability(username, usernameConfig);
  const emailState = useAvailability(email.toLowerCase(), emailConfig);
  const usernameInput = useRef<HTMLInputElement>(null);
  const emailInput = useRef<HTMLInputElement>(null);

  const availabilityBlocksSubmit = [usernameState, emailState]
    .some(({ availability }) => availability.status === "checking" || availability.status === "unavailable");

  function updateField(
    event: ChangeEvent<HTMLInputElement>,
    setter: (value: string) => void,
    reset: () => void,
  ) {
    reset();
    setMessage(null);
    setter(event.target.value);
  }

  function validateAvailability() {
    const fields = [
      {
        state: usernameState.availability,
        idle: "Clique em Verificar para validar o nome de usuário.",
        checking: "Aguarde a verificação do nome de usuário.",
        unavailable: "Escolha outro nome de usuário.",
        error: "Não foi possível verificar o nome de usuário. Tente novamente.",
        ref: usernameInput,
      },
      {
        state: emailState.availability,
        idle: "Clique em Verificar para validar o e-mail.",
        checking: "Aguarde a verificação do e-mail.",
        unavailable: "Informe outro e-mail.",
        error: "Não foi possível verificar o e-mail. Tente novamente.",
        ref: emailInput,
      },
    ];

    const blocked = fields.find(({ state }) => state.status !== "available");
    if (!blocked) return true;

    const status = blocked.state.status;
    const text = status === "idle"
      ? blocked.idle
      : status === "checking"
        ? blocked.checking
        : status === "unavailable"
          ? blocked.unavailable
          : blocked.error;
    setMessage({
      type: status === "checking" || status === "idle" ? "info" : "error",
      text,
    });
    blocked.ref.current?.focus();
    return false;
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);

    if (!validateAvailability()) return;
    if (formData.get("password") !== formData.get("passwordConfirmation")) {
      setMessage({ type: "error", text: "As senhas informadas não são iguais." });
      return;
    }
    if (!turnstileToken) {
      setMessage({ type: "error", text: "Conclua a verificação de segurança." });
      return;
    }

    formData.set("turnstileToken", turnstileToken);
    setMessage(null);
    startTransition(async () => {
      const result = await registerAccount(formData);
      setMessage({ type: result.success ? "success" : "error", text: result.message });
      if (result.success) {
        router.replace("/member/verify-email?sent=1");
      } else {
        setTurnstileResetSignal((current) => current + 1);
      }
    });
  }

  return (
    <form className="official-register-panel" onSubmit={handleSubmit}>
      <ul className="official-register-fields">
        <li>
          <label className="official-register-label" htmlFor="register-username">ID</label>
          <div className={`official-register-input has-check is-${usernameState.availability.status}`}>
            <input ref={usernameInput} id="register-username" name="username" type="text" minLength={4} maxLength={16} pattern="[A-Za-z0-9_]+" autoComplete="username" placeholder="Preencha seu ID contendo entre 4 e 16 caracteres." value={username} onChange={(event) => updateField(event, setUsername, usernameState.reset)} aria-invalid={usernameState.availability.status === "unavailable"} aria-describedby="username-availability" required autoFocus />
            <button type="button" className="official-register-check" disabled={usernameState.availability.status === "checking"} onClick={() => void usernameState.checkNow()}>Verificar</button>
          </div>
          <AvailabilityMessage id="username-availability" availability={usernameState.availability} />
        </li>

        <li>
          <label className="official-register-label" htmlFor="register-password">Senha</label>
          <PasswordInput id="register-password" name="password" autoComplete="new-password" placeholder="Digite sua senha contendo de 8 a 16 caracteres." minLength={8} />
        </li>

        <li>
          <label className="official-register-label" htmlFor="register-password-confirmation">Confirme sua senha</label>
          <PasswordInput id="register-password-confirmation" name="passwordConfirmation" autoComplete="new-password" placeholder="Digite sua senha contendo de 8 a 16 caracteres." minLength={8} />
        </li>

        <li>
          <label className="official-register-label" htmlFor="register-email">E-mail</label>
          <div className={`official-register-input has-check is-${emailState.availability.status}`}>
            <input ref={emailInput} id="register-email" name="email" type="email" maxLength={255} autoComplete="email" placeholder="Por favor, insira o seu e-mail." value={email} onChange={(event) => updateField(event, setEmail, emailState.reset)} aria-invalid={emailState.availability.status === "unavailable"} aria-describedby="email-availability" required />
            <button type="button" className="official-register-check" disabled={emailState.availability.status === "checking"} onClick={() => void emailState.checkNow()}>Verificar</button>
          </div>
          <AvailabilityMessage id="email-availability" availability={emailState.availability} />
        </li>
      </ul>

      <div className="official-register-accept">
        <input id="register-terms" name="terms" type="checkbox" required />
        <label htmlFor="register-terms">
          Li e aceito os Termos de Uso e a Política de Privacidade.
        </label>
      </div>

      <div className="official-register-submit-area">
        <div className="official-register-security-space">
          <TurnstileWidget
            siteKey={turnstileSiteKey}
            action="register"
            resetSignal={turnstileResetSignal}
            onTokenChange={setTurnstileToken}
          />
        </div>
        {message ? <p className={`auth-message official-register-message ${message.type}`} role="status" aria-live="polite">{message.text}</p> : null}
        <button className="official-register-submit" type="submit" disabled={isPending || availabilityBlocksSubmit || !turnstileToken}>
          {isPending ? "Criando conta..." : "Cadastro"}
        </button>
      </div>

      <div className="official-register-login-row">
        <Link className="official-register-login-text" href="/login/form">Já possui uma conta?</Link>
        <Link className="official-register-login-button" href="/login/form">Login</Link>
      </div>
    </form>
  );
}
