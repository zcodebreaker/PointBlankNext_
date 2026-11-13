import type { Metadata } from "next";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TicketForm from "@/components/TicketForm";
import TopButton from "@/components/TopButton";
import { requireCurrentAccount } from "@/lib/auth-session";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Envie uma solicitação ao suporte do Point Blank.",
};

type TicketFormPageProps = {
  searchParams: Promise<{ mode?: string | string[] }>;
};

export default async function TicketFormPage({ searchParams }: TicketFormPageProps) {
  const [account, query] = await Promise.all([requireCurrentAccount(), searchParams]);

  return (
    <div id="all_wrap">
      <SiteHeader username={account.username} accessLevel={account.accessLevel} />
      <QuickMenu />

      <div className="pb_content">
        <div className="pb_sub">
          <div className="pb_account">
            <div className="pb_top">
              <div className="typing_wrap">
                <div className="typing-txt">ticket</div>
                <div className="typing">ticket</div>
              </div>
            </div>

            <div className="pb_sub_con">
              <div className="top_info">
                <div className="inner">
                  A categoria certa nos ajuda a responder imediatamente à sua reclamação.<br />
                  A categoria errada ou incorreta pode afetar a resposta.
                </div>
              </div>
              <div className="inner">
                <TicketForm reportCheat={query.mode === "cheat"} />
              </div>
            </div>
          </div>
        </div>
      </div>

      <CookieConsent />
      <SiteFooter />
      <TopButton className="go_top" />
    </div>
  );
}
