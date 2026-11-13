import type { ReactNode } from "react";
import Link from "next/link";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/SiteHeader";
import { getHeaderSession } from "@/lib/auth-session";

type AuthPageLayoutProps = {
  title: string;
  description: string;
  active: "login" | "cadastro";
  children: ReactNode;
};

export default async function AuthPageLayout({ title, description, active, children }: AuthPageLayoutProps) {
  const session = await getHeaderSession();

  return (
    <div id="all_wrap" className="auth-site">
      <SiteHeader username={session?.username} accessLevel={session?.accessLevel} />
      <QuickMenu />
      <main className="pb_sub auth-page">
        <div className="pb_account">
          <header className="pb_top">
            <div className="typing_wrap"><h1 className="typing">{title}</h1></div>
            <p className="txt">{description}</p>
            <nav className="sub_depth2" aria-label="Área da conta">
              <ul>
                <li className={active === "login" ? "on" : ""}><Link href="/login/form">Login</Link></li>
                <li className={active === "cadastro" ? "on" : ""}><Link href="/member/signup">Cadastro</Link></li>
                <li><Link href="/">Voltar ao início</Link></li>
              </ul>
            </nav>
          </header>
          <div className="pb_sub_con">
            <div className="inner">{children}</div>
          </div>
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
