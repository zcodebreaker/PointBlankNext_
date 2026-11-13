import SiteHeader from "@/components/SiteHeader";
import { getMyTickets } from "@/lib/tickets";

type ConnectedSiteHeaderProps = {
  username?: string | null;
  accessLevel?: number | null;
};

export default async function ConnectedSiteHeader({
  username = null,
  accessLevel = null,
}: ConnectedSiteHeaderProps) {
  let notificationIds: string[] = [];
  let notificationPlayerId: number | null = null;

  if (username) {
    try {
      const tickets = await getMyTickets();
      const repliedTickets = tickets.filter((ticket) => ticket.adminReply && ticket.repliedAt);

      notificationIds = repliedTickets.map((ticket) => `${ticket.ticketId}:${ticket.repliedAt}`);
      notificationPlayerId = repliedTickets[0]?.playerId ?? null;
    } catch {
      // Notifications do not prevent the rest of the site from loading if the API is unavailable.
    }
  }

  return (
    <SiteHeader
      username={username}
      accessLevel={accessLevel}
      notificationIds={notificationIds}
      notificationPlayerId={notificationPlayerId}
    />
  );
}
