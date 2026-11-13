import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { redirect } from "next/navigation";
import LoginForm from "@/components/LoginForm";
import TopButton from "@/components/TopButton";
import { hasAuthSession } from "@/lib/auth-session";
import { getTurnstileSiteKey } from "@/lib/turnstile-config";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Acesse sua conta do Point Blank.",
};

export default async function LoginPage() {
  if (await hasAuthSession()) redirect("/");

  return (
    <div id="login-page-top" className="official-register-page official-login-page">
      <Link
        className="official-register-background"
        href="/"
        target="_blank"
        aria-label="Abrir a página inicial do Point Blank em uma nova aba"
      >
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

      <main className="official-register-account official-login-account">
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
        <LoginForm turnstileSiteKey={getTurnstileSiteKey()} />
      </main>

      <footer className="official-register-footer official-login-footer">
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
