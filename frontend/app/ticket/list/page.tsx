import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { requireCurrentAccount } from "@/lib/auth-session";
import { getMyTickets, type TicketStatus } from "@/lib/tickets";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Acompanhe suas solicitações de suporte.",
};

const statusInfo: Record<TicketStatus, { className: string; label: string }> = {
  WAITING: { className: "wait", label: "Aguardando" },
  ANSWERED: { className: "closed", label: "Completo" },
  CLOSED: { className: "closed", label: "Completo" },
};

const pageSize = 10;
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

function getPageNumbers(currentPage: number, totalPages: number) {
  const firstPage = Math.max(1, Math.min(currentPage - 2, totalPages - 4));
  const lastPage = Math.min(totalPages, firstPage + 4);

  return Array.from({ length: lastPage - firstPage + 1 }, (_, index) => firstPage + index);
}

type TicketListPageProps = {
  searchParams: Promise<{
    created?: string | string[];
    page?: string | string[];
  }>;
};

export default async function TicketListPage({ searchParams }: TicketListPageProps) {
  const [account, tickets, query] = await Promise.all([
    requireCurrentAccount(),
    getMyTickets(),
    searchParams,
  ]);
  const requestedPage = Number.parseInt(Array.isArray(query.page) ? query.page[0] : query.page ?? "1", 10);
  const totalPages = Math.max(1, Math.ceil(tickets.length / pageSize));
  const currentPage = Math.min(Math.max(Number.isFinite(requestedPage) ? requestedPage : 1, 1), totalPages);
  const visibleTickets = tickets.slice((currentPage - 1) * pageSize, currentPage * pageSize);
  const pageNumbers = getPageNumbers(currentPage, totalPages);

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
                  <div className="ticket_con">
                    Que ajuda você precisa?
                    <span><span className="ticket-highlight">se você tiver alguma dúvida, basta nos enviar um ticket.</span></span>
                    <Link href="/ticket/form" className="btn_read btn_line_y">Enviar ticket</Link>
                  </div>
                </div>
              </div>

              <div className="inner">
                <div className={tickets.length === 0 ? "notifi_wrap" : "ticket_wrap v2"}>
                  {query.created ? <div className="ticket-success-message">Seu ticket foi enviado com sucesso.</div> : null}
                  {tickets.length === 0 ? (
                    <ul className="list">
                      <div className="nodata_n">
                        <Image
                          src="/images/common/mypage/icon_nodata.png"
                          alt=""
                          width={27}
                          height={34}
                          unoptimized
                        />
                        <span>sem dados</span>
                      </div>
                    </ul>
                  ) : (
                    <>
                      <ul className="tl">
                        {visibleTickets.map((ticket) => {
                          const status = statusInfo[ticket.status];

                          return (
                            <li key={ticket.ticketId}>
                              <div className="con">
                                <div className="num">{ticket.ticketId}</div>
                                <div className="desc">
                                  <div className="sort"><span>{ticket.mainCategory}</span> {ticket.subCategory}</div>
                                  <div className="tit"><span>{ticket.subject}</span></div>
                                </div>
                                <div className="date">{formatTicketDate(ticket.createdAt)}</div>
                                <div className={`state ${status.className}`}><span>{status.label}</span></div>
                              </div>
                              <Link href={`/ticket/${ticket.ticketId}`} className="s_link" aria-label={`Abrir ticket ${ticket.ticketId}`}>
                                Abrir ticket
                              </Link>
                            </li>
                          );
                        })}
                      </ul>

                      <nav className="paging_wrap" aria-label="Paginação dos tickets">
                        <ul>
                          <li className="first">
                            <Link href="?page=1" className="first" aria-label="Primeira página" aria-disabled={currentPage === 1} />
                          </li>
                          <li className="prev">
                            <Link href={`?page=${Math.max(1, currentPage - 1)}`} className="prev" aria-label="Página anterior" aria-disabled={currentPage === 1} />
                          </li>
                          {pageNumbers.map((pageNumber) => (
                            <li key={pageNumber} className={pageNumber === currentPage ? "on" : undefined}>
                              <Link href={`?page=${pageNumber}`} aria-current={pageNumber === currentPage ? "page" : undefined}>
                                {pageNumber}
                              </Link>
                            </li>
                          ))}
                          <li className="next">
                            <Link href={`?page=${Math.min(totalPages, currentPage + 1)}`} className="next" aria-label="Próxima página" aria-disabled={currentPage === totalPages} />
                          </li>
                          <li className="last">
                            <Link href={`?page=${totalPages}`} className="last" aria-label="Última página" aria-disabled={currentPage === totalPages} />
                          </li>
                        </ul>
                      </nav>
                    </>
                  )}
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
