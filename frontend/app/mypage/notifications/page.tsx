import type { Metadata } from "next";
import CookieConsent from "@/components/CookieConsent";
import NotificationList, { type AccountNotification } from "@/components/NotificationList";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { requireCurrentAccount } from "@/lib/auth-session";
import { getMyTickets } from "@/lib/tickets";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Acompanhe as notificações da sua conta Point Blank.",
};

function formatRelativeDate(value: string) {
  const elapsedSeconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
  const units = [
    { name: "YEAR", seconds: 31_536_000 },
    { name: "MONTH", seconds: 2_592_000 },
    { name: "DAY", seconds: 86_400 },
    { name: "HOUR", seconds: 3_600 },
    { name: "MINUTE", seconds: 60 },
  ] as const;

  for (const unit of units) {
    if (elapsedSeconds >= unit.seconds) {
      const amount = Math.floor(elapsedSeconds / unit.seconds);
      return `${amount} ${unit.name}${amount === 1 ? "" : "S"} AGO`;
    }
  }

  return "JUST NOW";
}

export default async function AccountNotificationsPage() {
  const [account, tickets] = await Promise.all([
    requireCurrentAccount(),
    getMyTickets(),
  ]);
  const notifications = tickets
    .filter((ticket) => ticket.adminReply && ticket.repliedAt)
    .sort((left, right) => new Date(right.repliedAt!).getTime() - new Date(left.repliedAt!).getTime())
    .map<AccountNotification>((ticket) => ({
      id: `${ticket.ticketId}:${ticket.repliedAt}`,
      ticketId: ticket.ticketId,
      relativeDate: formatRelativeDate(ticket.repliedAt!),
    }));

  return (
    <div id="all_wrap">
      <SiteHeader username={account.username} accessLevel={account.accessLevel} />
      <QuickMenu />

      <div className="pb_content">
        <div className="pb_sub">
          <div className="pb_account">
            <div className="pb_top">
              <div className="typing_wrap">
                <div className="typing-txt">NOTIFICATIONS</div>
                <div className="typing">NOTIFICATIONS</div>
              </div>
            </div>

            <div className="pb_sub_con">
              <div className="inner">
                <div className="notifi_wrap">
                  <NotificationList notifications={notifications} playerId={account.playerId} />
                </div>
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
