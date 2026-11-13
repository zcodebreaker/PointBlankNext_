import type { Metadata } from "next";
import AccountInformationPanel from "@/components/AccountInformationPanel";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { requireCurrentAccount } from "@/lib/auth-session";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Consulte e gerencie as informações da sua conta Point Blank.",
};

export default async function AccountInformationPage() {
  const account = await requireCurrentAccount();

  return (
    <div id="all_wrap">
      <SiteHeader username={account.username} accessLevel={account.accessLevel} />
      <QuickMenu />

      <div className="pb_content">
        <div className="pb_sub">
          <div className="pb_account">
            <div className="pb_top">
              <div className="typing_wrap">
                <div className="typing-txt">Minhas Informações</div>
                <div className="typing">Minhas Informações</div>
              </div>
            </div>

            <div className="pb_sub_con">
              <div className="inner">
                <AccountInformationPanel account={account} />
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
