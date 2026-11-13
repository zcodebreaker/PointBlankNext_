import type { Metadata } from "next";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { getHeaderSession } from "@/lib/auth-session";
import NamecardSimulator from "./NamecardSimulator";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Simule cores e cartões para o seu nickname no Point Blank.",
};

export default async function NamecardPage() {
  const session = await getHeaderSession();

  return (
    <div id="all_wrap">
      <SiteHeader username={session?.username} accessLevel={session?.accessLevel} />
      <QuickMenu />
      <main className="pb_content">
        <div className="pb_sub">
          <NamecardSimulator initialNickname={session?.username ?? ""} />
        </div>
      </main>
      <CookieConsent />
      <SiteFooter />
      <TopButton className="go_top" />
    </div>
  );
}
