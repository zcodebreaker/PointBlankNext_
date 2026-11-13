import type { Metadata } from "next";
import Link from "next/link";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { requireCurrentAccount } from "@/lib/auth-session";
import { getMyTicket, type TicketStatus } from "@/lib/tickets";

export const metadata: Metadata = {
  title: "POINT BLANK",
};

const statusInfo: Record<TicketStatus, { className: string; label: string }> = {
  WAITING: { className: "wait", label: "Aguardando" },
  ANSWERED: { className: "closed", label: "Completo" },
  CLOSED: { className: "closed", label: "Completo" },
};

const monthLabels = ["Jan.", "Fev.", "Mar.", "Abr.", "Mai.", "Jun.", "Jul.", "Ago.", "Set.", "Out.", "Nov.", "Dez."];

function formatTicketDate(value: string) {
  const parts = new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "numeric",
    year: "numeric",
    timeZone: "America/Sao_Paulo",
  }).formatToParts(new Date(value));
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find((item) => item.type === type)?.value ?? "";

  return `${part("day")}. ${monthLabels[Number(part("month")) - 1]} ${part("year")}`;
}

type TicketDetailPageProps = {
  params: Promise<{ ticketId: string }>;
};

export default async function TicketDetailPage({ params }: TicketDetailPageProps) {
  const { ticketId } = await params;
  const numericTicketId = Number(ticketId);
  const [account, ticket] = await Promise.all([
    requireCurrentAccount(),
    getMyTicket(Number.isSafeInteger(numericTicketId) ? numericTicketId : 0),
  ]);
  const status = statusInfo[ticket.status];

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
              <div className="ticket_wrap v2">
                <div className="tv">
                  <div className={`tv_top ${status.className}`}>
                    <div className="inner">
                      <div className="sort"><span>{ticket.mainCategory}</span> {ticket.subCategory}</div>
                      <div className="tit">{ticket.subject}</div>
                      <div className={`state ${status.className}`}><span>{status.label}</span></div>
                    </div>
                  </div>

                  <div className="inner">
                    <div className="tv_date">{formatTicketDate(ticket.createdAt)}</div>
                    <div className="tv_con">
                      <div className="text ticket-preserve-lines">{ticket.message}</div>
                      {ticket.attachments.length > 0 ? (
                        <ul className="attach">
                          {ticket.attachments.map((attachment) => (
                            <li key={attachment.attachmentId}>
                              <a href={`/api/tickets/${ticket.ticketId}/attachments/${attachment.attachmentId}`} target="_blank" rel="noreferrer">
                                {attachment.fileName}
                              </a>
                            </li>
                          ))}
                        </ul>
                      ) : null}
                    </div>

                    {ticket.adminReply ? (
                      <div className="reply">
                        <div className="writer admin">ADMINISTRADOR</div>
                        {ticket.repliedAt ? <div className="date">{formatTicketDate(ticket.repliedAt)}</div> : null}
                        {ticket.adminReplySubject ? <div className="ticket-reply-subject">{ticket.adminReplySubject}</div> : null}
                        <div className="txt ticket-preserve-lines">{ticket.adminReply}</div>
                      </div>
                    ) : null}

                    <div className="tl_bottom">
                      <Link href="/ticket/list" className="btn_line_b02">Lista</Link>
                    </div>
                  </div>
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
