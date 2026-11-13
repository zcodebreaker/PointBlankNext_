import type { Metadata } from "next";
import CookieConsent from "@/components/CookieConsent";
import LogoutCompletion from "@/components/LogoutCompletion";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";

export const metadata: Metadata = {
  title: "POINT BLANK",
};

export default function LogoutPage() {
  return (
    <div id="all_wrap" className="logout-page">
      <SiteHeader username={null} />
      <QuickMenu />
      <main className="pb_content">
        <div className="pb_sub">
          <div className="pb_base">
            <div className="pb_sub_con">
              <LogoutCompletion />
            </div>
          </div>
        </div>
      </main>
      <SiteFooter />
      <CookieConsent />
      <TopButton className="go_top" />
    </div>
  );
}
