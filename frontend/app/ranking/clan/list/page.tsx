import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import ClanLogo from "@/components/ClanLogo";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { getHeaderSession } from "@/lib/auth-session";
import { getClanRanking } from "@/lib/clan-ranking";
import { getClanRank } from "@/lib/clan-ranks";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Ranking geral dos clãs do Piercing Blow BR.",
};

const RANKING_PATH = "/ranking/clan/list";
const PAGE_SIZE = 20;

type ClanRankingPageProps = {
  searchParams: Promise<{ page?: string; termtype?: string }>;
};

function clanRankName(rank: number) {
  if (rank >= 50) return "BlackOps Brigade";
  if (rank >= 40) return "BlackOps Regiment";
  if (rank >= 30) return "BlackOps Battalion";
  if (rank >= 20) return "BlackOps Company";
  if (rank >= 10) return "BlackOps Platoon";
  return "BlackOps Squad";
}

function rankingClass(position: number) {
  return position >= 1 && position <= 3 ? `no0${position}` : "";
}

function pageNumbers(currentPage: number, totalPages: number) {
  const firstPage = Math.floor((currentPage - 1) / 10) * 10 + 1;
  const lastPage = Math.min(totalPages, firstPage + 9);
  return Array.from({ length: Math.max(0, lastPage - firstPage + 1) }, (_, index) => firstPage + index);
}

export default async function ClanRankingPage({ searchParams }: ClanRankingPageProps) {
  const sessionPromise = getHeaderSession();
  const parameters = await searchParams;
  const requestedPage = Math.max(1, Number.parseInt(parameters.page ?? "1", 10) || 1);
  const termType = ["1", "2", "3"].includes(parameters.termtype ?? "") ? parameters.termtype! : "3";
  const [session, ranking] = await Promise.all([
    sessionPromise,
    getClanRanking(requestedPage - 1, PAGE_SIZE),
  ]);
  const currentPage = ranking.page + 1;
  const totalPages = Math.max(1, ranking.totalPages);
  const visiblePages = pageNumbers(currentPage, totalPages);

  return (
    <div id="all_wrap">
      <SiteHeader username={session?.username} accessLevel={session?.accessLevel} />
      <QuickMenu />
      <main className="pb_content">
        <div className="pb_sub">
          <div className="pb_rank">
            <div className="pb_top">
              <div className="typing_wrap">
                <div className="typing-txt">Rank de Clãs</div>
                <div className="typing">Rank de Clãs</div>
              </div>
            </div>

            <div className="pb_sub_con">
              <div className="nrank_wrap">
                <nav className="rk_depth2 nsb_depth2" aria-label="Período do ranking">
                  <span className="nw_tab">{termType === "1" ? "diário" : termType === "2" ? "semanal" : "geral"}</span>
                  <ul>
                    <li className={termType === "3" ? "on" : undefined}><Link href={`${RANKING_PATH}?termtype=3`}>geral</Link></li>
                    <li className={termType === "1" ? "on" : undefined}><Link href={`${RANKING_PATH}?termtype=1`}>diário</Link></li>
                    <li className={termType === "2" ? "on" : undefined}><Link href={`${RANKING_PATH}?termtype=2`}>semanal</Link></li>
                    <li><Link href={`${RANKING_PATH}?termtype=3#ranking-information`}>TEMPORADA CLAN MATCH</Link></li>
                  </ul>
                </nav>

                <div className="inner">
                  <div className="nrank_table nrank_table02">
                    {ranking.clans.length === 0 ? (
                      <div className="nodata_n">
                        <Image src="/images/common/mypage/icon_nodata.png" alt="" width={27} height={34} unoptimized />
                        <span>sem dados</span>
                      </div>
                    ) : ranking.clans.map((clan) => {
                      const rank = getClanRank(clan.experience);

                      return (
                        <div className={`r_tr ${rankingClass(clan.position)}`} id={`clan-${clan.clanId}`} key={clan.clanId}>
                          <div className="r_td win">{clan.position}<span className="state" /></div>
                          <div className="r_td name">
                            <ClanLogo value={clan.logo} />
                            <span className="clan-ranking-name">{clan.name}</span>
                            <span className="rank">
                              <Image src={rank.image} alt="Rank do clã" width={20} height={20} unoptimized />
                              {clanRankName(rank.level)}
                            </span>
                          </div>
                          <div className="r_td leader ml">
                            <span className="tit">LÍDER DO CLÃ</span>
                            <div className="txt">{clan.leader || "-"}</div>
                          </div>
                          <div className="r_td member ml">
                            <span className="tit">MEMBROS DO CLÃ</span>
                            <div className="txt">{clan.memberCount}</div>
                          </div>
                          <div className="r_td exp ml">
                            <span className="tit">EXP</span>
                            <div className="txt">{clan.experience.toLocaleString("pt-BR")}</div>
                          </div>
                          <div className="r_td odd ml">
                            <span className="tit">Win/Lose</span>
                            <div className="txt">
                              {clan.wins.toLocaleString("pt-BR")}
                              <span className="clan-ranking-losses">/{clan.losses.toLocaleString("pt-BR")}</span>{" "}
                              <span className="per">[({clan.winRate}%)]</span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {ranking.totalPages > 0 ? (
                    <div className="nrk_b">
                      <nav className="paging_wrap" aria-label="Paginação do ranking de clãs">
                        <ul>
                          <li className="first"><Link href={`${RANKING_PATH}?page=1&termtype=${termType}`} className="first" aria-label="Primeira página" /></li>
                          <li className="prev"><Link href={`${RANKING_PATH}?page=${Math.max(1, currentPage - 1)}&termtype=${termType}`} className="prev" aria-label="Página anterior" /></li>
                          {visiblePages.map((page) => (
                            <li className={page === currentPage ? "on" : undefined} key={page}>
                              <Link href={`${RANKING_PATH}?page=${page}&termtype=${termType}`} aria-current={page === currentPage ? "page" : undefined}>{page}</Link>
                            </li>
                          ))}
                          <li className="next"><Link href={`${RANKING_PATH}?page=${Math.min(totalPages, currentPage + 1)}&termtype=${termType}`} className="next" aria-label="Próxima página" /></li>
                          <li className="last"><Link href={`${RANKING_PATH}?page=${totalPages}&termtype=${termType}`} className="last" aria-label="Última página" /></li>
                        </ul>
                      </nav>
                    </div>
                  ) : null}

                  <div className="nrk_explan" id="ranking-information">
                    <ul className="red_dot">
                      <li>O Ranking será atualizado às <span>04:00</span></li>
                      <li>Os dados exibidos são obtidos diretamente do servidor do jogo.</li>
                      <li>O ranking geral é ordenado pela experiência acumulada do clã.</li>
                      <li>A taxa de vitória considera somente partidas com vitória ou derrota.</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
      <CookieConsent />
      <SiteFooter />
      <TopButton className="go_top" />
    </div>
  );
}
