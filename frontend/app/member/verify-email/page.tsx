import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import TopButton from "@/components/TopButton";
import { API_URL } from "@/lib/auth-session";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Confirme seu e-mail para ativar sua conta.",
};

type VerificationPageProps = {
  searchParams: Promise<{ token?: string; sent?: string }>;
};

type ApiProblem = { detail?: string };
type VerificationResult = { verified: boolean; message: string };

async function verifyToken(token: string): Promise<VerificationResult> {
  try {
    const response = await fetch(
      `${API_URL}/api/v1/auth/verify-email?token=${encodeURIComponent(token)}`,
      { cache: "no-store" },
    );
    const body = (await response.json().catch(() => ({}))) as VerificationResult & ApiProblem;

    if (!response.ok) {
      return {
        verified: false,
        message: body.detail ?? "Não foi possível confirmar este link de ativação.",
      };
    }

    return body;
  } catch {
    return {
      verified: false,
      message: "A API está indisponível. Tente abrir o link novamente em instantes.",
    };
  }
}

export default async function VerifyEmailPage({ searchParams }: VerificationPageProps) {
  const { token, sent } = await searchParams;
  const result = token?.trim()
    ? await verifyToken(token.trim())
    : sent === "1"
      ? {
          verified: false,
          message: "Enviamos o link de ativação. Verifique sua caixa de entrada e também a pasta de spam.",
        }
      : {
          verified: false,
          message: "Abra o link enviado ao seu e-mail para ativar a conta.",
        };

  return (
    <div id="verification-page-top" className="official-register-page">
      <Link className="official-register-background" href="/" aria-label="Voltar para a página inicial">
        <Image
          className="official-register-background-desktop"
          src="/images/background/background.jpg"
          alt=""
          width={2520}
          height={1828}
          priority
          unoptimized
        />
        <Image
          className="official-register-background-mobile"
          src="/images/background/background-mobile.jpg"
          alt=""
          width={1536}
          height={1232}
          priority
          unoptimized
        />
      </Link>

      <main className="official-register-account email-verification-account">
        <div className="official-register-content">
          <h1 className="official-register-logo">
            <Link href="/" aria-label="Point Blank — início">
              <Image
                src="/images/common/logo_bi_wide.png"
                alt="Point Blank Beyond Limits"
                width={440}
                height={134}
                priority
                unoptimized
              />
            </Link>
          </h1>
        </div>

        <section className="official-register-panel email-verification-panel">
          <p className={`email-verification-status ${result.verified ? "success" : "pending"}`}>
            {result.verified ? "CONTA ATIVADA" : "CONFIRMAÇÃO DE E-MAIL"}
          </p>
          <h2>{result.verified ? "E-mail confirmado" : "Confira seu e-mail"}</h2>
          <p>{result.message}</p>
          <Link className="official-register-submit email-verification-action" href="/login/form">
            {result.verified ? "Entrar na conta" : "Ir para o login"}
          </Link>
        </section>
      </main>

      <footer className="official-register-footer">
        <div className="official-register-footer-inner">
          <span className="official-register-footer-logo">
            <Image src="/images/common/logo_kryonix_ntw.png" alt="Kryonix Network" width={160} height={58} />
          </span>
          <div className="official-register-contact">Se houver dúvidas, entre em contato com <Link href="/#rodape">Ticket</Link></div>
          <div className="official-register-copyright">Copyright Kryonix Network. Todos direitos reservados</div>
          <div className="official-register-age">
            <Image src="/images/br/age18.png" alt="Classificação indicativa: 18 anos" width={502} height={288} />
          </div>
        </div>
      </footer>

      <TopButton className="official-register-top" />
    </div>
  );
}
