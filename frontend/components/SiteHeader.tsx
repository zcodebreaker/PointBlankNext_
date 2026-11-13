"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { logoutAccount } from "@/app/actions/auth";
import NotificationBadge from "@/components/NotificationBadge";

type MenuItem = {
  label: string;
  href: string;
  children?: Array<{ label: string; href: string }>;
};

const menuItems: MenuItem[] = [
  { label: "Notícias", href: "/news/list" },
  {
    label: "Jogo",
    href: "/game/download",
    children: [
      { label: "Simulador de cartão de nick", href: "/game/namecard" },
      { label: "Download", href: "/game/download" },
    ],
  },
  {
    label: "Estatísticas",
    href: "/game/profile",
    children: [
      { label: "Perfil de jogo", href: "/game/profile" },
    ],
  },
  {
    label: "Rank",
    href: "/ranking/individual/list",
    children: [
      { label: "Ranking de jogadores", href: "/ranking/individual/list" },
      { label: "Rank de clãs", href: "/ranking/clan/list" },
    ],
  },
  { label: "Loja Premium", href: "/webstore/index" },
];

function Brand() {
  return (
    <h1>
      <Link href="/" aria-label="Point Blank — página inicial">
        <Image className="site-main-brand-logo" src="/images/common/site-main-logo-v3.png" alt="Point Blank" width={187} height={28} priority />
      </Link>
    </h1>
  );
}

type SiteHeaderProps = {
  username?: string | null;
  accessLevel?: number | null;
  notificationIds?: string[];
  notificationPlayerId?: number | null;
};

export default function SiteHeader({
  username = null,
  notificationIds = [],
  notificationPlayerId = null,
}: SiteHeaderProps) {
  const [mobile, setMobile] = useState(false);
  const [panel, setPanel] = useState<"menu" | "account" | null>(null);
  const [expanded, setExpanded] = useState<string | null>(null);
  const [accountMenuOpen, setAccountMenuOpen] = useState(false);

  useEffect(() => {
    const query = window.matchMedia("(max-width: 1280px)");
    const sync = () => setMobile(query.matches);
    sync();
    query.addEventListener("change", sync);
    return () => query.removeEventListener("change", sync);
  }, []);

  useEffect(() => {
    document.body.style.overflow = panel ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [panel]);

  const closePanel = () => {
    setPanel(null);
    setAccountMenuOpen(false);
  };

  return (
    <header id="head" className={mobile ? "mp" : "pc"}>
      <div className="inn clr">
        <Brand />
        <button className="m_menu icon-button" type="button" aria-label="Abrir menu" onClick={() => setPanel("menu")}>
          <Image src="/images/common/btn_menu.png" alt="" width={48} height={28} />
        </button>
        <button className="m_user icon-button" type="button" aria-label="Abrir área do usuário" onClick={() => setPanel("account")}>
          <Image src="/images/common/btn_user.png" alt="" width={48} height={48} />
        </button>

        <nav id="gnb" className={panel === "menu" ? "active" : ""} aria-label="Menu principal">
          {mobile && (
            <>
              <Brand />
              <button className="m_close icon-button" type="button" aria-label="Fechar menu" onClick={closePanel} />
            </>
          )}
          <ul>
            {menuItems.map((item) => (
              <li className={`dpt1${item.children ? " list_gnb" : ""}${expanded === item.label ? " expanded" : ""}`} key={item.label}>
                <Link
                  href={item.href}
                  onClick={(event) => {
                    if (mobile && item.children && item.href.startsWith("#") && expanded !== item.label) {
                      event.preventDefault();
                      setExpanded(item.label);
                      return;
                    }
                    closePanel();
                  }}
                >
                  {item.label}
                </Link>
                {item.children && (
                  <ul className="dpt2">
                    {item.children.map((child) => (
                      <li key={child.label}>
                        <Link href={child.href} onClick={closePanel}>{child.label}</Link>
                      </li>
                    ))}
                  </ul>
                )}
              </li>
            ))}
          </ul>
        </nav>

        <div className="gnb_etc">
          <div className={`login_sec${panel === "account" ? " active" : ""}`}>
            {mobile && (
              <>
                <Brand />
                <button className="m_close icon-button" type="button" aria-label="Fechar área do usuário" onClick={closePanel} />
              </>
            )}
            {username ? (
              <ul>
                <li className="my">
                  <div
                    className="my_wrap"
                    onMouseEnter={() => {
                      if (!mobile) setAccountMenuOpen(true);
                    }}
                    onMouseLeave={() => {
                      if (!mobile) setAccountMenuOpen(false);
                    }}
                    onFocus={() => {
                      if (!mobile) setAccountMenuOpen(true);
                    }}
                    onBlur={(event) => {
                      const nextTarget = event.relatedTarget;
                      if (!mobile && (!(nextTarget instanceof Node) || !event.currentTarget.contains(nextTarget))) {
                        setAccountMenuOpen(false);
                      }
                    }}
                  >
                    <NotificationBadge
                      notificationIds={notificationIds}
                      playerId={notificationPlayerId}
                    />
                    <button
                      type="button"
                      className="my_btn"
                      aria-expanded={accountMenuOpen}
                      aria-controls="header-account-menu"
                      onClick={() => setAccountMenuOpen((open) => !open)}
                      title={`Usuário: ${username}`}
                    >
                      <span className="nation">
                        <Image src="/images/common/icon_regi_on.png" alt="" width={12} height={12} />
                      </span>
                      {username}
                    </button>
                    <ul id="header-account-menu" className={`my_list${accountMenuOpen ? " is-open" : ""}`}>
                      <li><Link href="/mypage/info" onClick={closePanel}>MINHAS INFORMAÇÕES</Link></li>
                      <li><Link href="/ticket/list" onClick={closePanel}>TICKET</Link></li>
                      <li><Link href="/mypage/notifications" onClick={closePanel}>NOTIFICAÇÕES</Link></li>
                    </ul>
                  </div>
                </li>
                <li className="log_on log_off">
                  <form action={logoutAccount}>
                    <button type="submit" className="header-logout-button" onClick={closePanel}>Sair</button>
                  </form>
                </li>
              </ul>
            ) : (
              <ul className="logout">
                <li className="join"><Link href="/member/signup" onClick={closePanel}>Cadastro</Link></li>
                <li className="log_on"><Link href="/login/form" onClick={closePanel}>Login</Link></li>
              </ul>
            )}
          </div>
        </div>
      </div>
      <div className="nation_wrap login" />
    </header>
  );
}
