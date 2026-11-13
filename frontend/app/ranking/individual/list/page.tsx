import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { getHeaderSession } from "@/lib/auth-session";
import { getCurrentPlayerRanking, getPlayerRanking } from "@/lib/player-ranking";
import { getPlayerRank } from "@/lib/player-ranks";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Ranking geral dos jogadores do Piercing Blow BR.",
};

const RANKING_PATH = "/ranking/individual/list";
const PAGE_SIZE = 20;

type PlayerRankingPageProps = {
  searchParams: Promise<{ page?: string; termtype?: string; keyword?: string }>;
};

function rankingClass(position: number) {
  return position >= 1 && position <= 3 ? `no0${position}` : "";
}

function pageNumbers(currentPage: number, totalPages: number) {
  const firstPage = Math.floor((currentPage - 1) / 10) * 10 + 1;
  const lastPage = Math.min(totalPages, firstPage + 9);
  return Array.from({ length: Math.max(0, lastPage - firstPage + 1) }, (_, index) => firstPage + index);
}

function rankingPageHref(page: number, termType: string, keyword: string) {
  const parameters = new URLSearchParams({ page: String(page), termtype: termType });
  if (keyword) parameters.set("keyword", keyword);
  return `${RANKING_PATH}?${parameters}`;
}

export default async function PlayerRankingPage({ searchParams }: PlayerRankingPageProps) {
  const sessionPromise = getHeaderSession();
  const parameters = await searchParams;
  const requestedPage = Math.max(1, Number.parseInt(parameters.page ?? "1", 10) || 1);
  const rankTerm = parameters.termtype === "1" || parameters.termtype === "2" ? parameters.termtype : "3";
  const keyword = (parameters.keyword ?? "").trim().slice(0, 32);
  const [session, ranking, currentPlayer] = await Promise.all([
    sessionPromise,
    getPlayerRanking(requestedPage - 1, PAGE_SIZE, keyword),
    getCurrentPlayerRanking(),
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
                <div className="typing-txt">RANKING DE JOGADORES</div>
                <div className="typing">RANKING DE JOGADORES</div>
              </div>
            </div>

            <div className="pb_sub_con">
              <div className="nrank_wrap">
                {currentPlayer ? (() => {
                  const currentRank = getPlayerRank(currentPlayer.rank);

                  return (
                    <div className="nrank_top my_rank">
                      <div className="inner">
                        <div className="nrank_table">
                          <div className="r_tr">
                            <div className="r_td win">
                              <Image src="/images/common/rank/my_rank.png" alt="" width={56} height={44} unoptimized />
                              <span>meu <em className="my-rank-label">rank</em></span>
                            </div>
                            <div className="r_td name">
                              <button type="button" className="player-ranking-name">{currentPlayer.nickname}</button>
                              <div className="num">{currentPlayer.position}<span className="state" /></div>
                            </div>
                            <div className="r_td rank ml">
                              <span className="tit">rank</span>
                              <div className="txt">
                                <span className="rank_img">
                                  <Image src={currentRank.image} alt="" width={24} height={24} unoptimized />
                                </span>
                                {currentRank.name}
                              </div>
                            </div>
                            <div className="r_td exp ml">
                              <span className="tit">exp</span>
                              <div className="txt">{currentPlayer.experience.toLocaleString("pt-BR")}</div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  );
                })() : !session ? (
                  <div className="nrank_top">
                    <div className="inner">
                      <Image src="/images/common/rank/icon_info.png" alt="" width={56} height={56} unoptimized />
                      Você pode ver seu RANK após o Login.
                    </div>
                  </div>
                ) : null}
                <nav className="rk_depth2 nsb_depth2" aria-label="Categorias de ranking">
                  <Link className="nw_tab" href={`${RANKING_PATH}?page=1&termtype=3`}>geral</Link>
                  <ul>
                    <li className={rankTerm === "3" ? "on" : undefined}><Link href={`${RANKING_PATH}?termtype=3`}>geral</Link></li>
                    <li className={rankTerm === "1" ? "on" : undefined}><Link href={`${RANKING_PATH}?termtype=1`}>diário</Link></li>
                    <li className={rankTerm === "2" ? "on" : undefined}><Link href={`${RANKING_PATH}?termtype=2`}>semanal</Link></li>
                  </ul>
                </nav>

                <div className="inner">
                  <div className="nrank_table player-ranking-table">
                    {ranking.players.length === 0 ? (
                      <div className="nodata_n">
                        <Image src="/images/common/mypage/icon_nodata.png" alt="" width={27} height={34} unoptimized />
                        <span>sem dados</span>
                      </div>
                    ) : ranking.players.map((player) => {
                      const playerRank = getPlayerRank(player.rank);

                      return (
                        <div className={`r_tr ${rankingClass(player.position)}`} key={`${player.nickname}-${player.position}`}>
                          <div className="r_td win">{player.position}</div>
                          <div className="r_td name">
                            <button type="button" className="player-ranking-name">{player.nickname}</button>
                            <div className="num">{player.position}<span className="state" /></div>
                          </div>
                          <div className="r_td rank ml">
                            <span className="tit">rank</span>
                            <div className="txt">
                              <span className="rank_img">
                                <Image src={playerRank.image} alt="" width={24} height={24} unoptimized />
                              </span>
                              {playerRank.name}
                            </div>
                          </div>
                          <div className="r_td exp ml">
                            <span className="tit">exp</span>
                            <div className="txt">{player.experience.toLocaleString("pt-BR")}</div>
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {ranking.totalPages > 0 ? (
                    <div className="nrk_b">
                      <nav className="paging_wrap" aria-label="Paginação do ranking de jogadores">
                        <ul>
                          <li className="first"><Link href={rankingPageHref(1, rankTerm, keyword)} className="first" aria-label="Primeira página" /></li>
                          <li className="prev"><Link href={rankingPageHref(Math.max(1, currentPage - 1), rankTerm, keyword)} className="prev" aria-label="Página anterior" /></li>
                          {visiblePages.map((page) => (
                            <li className={page === currentPage ? "on" : undefined} key={page}>
                              <Link href={rankingPageHref(page, rankTerm, keyword)} aria-current={page === currentPage ? "page" : undefined}>{page}</Link>
                            </li>
                          ))}
                          <li className="next"><Link href={rankingPageHref(Math.min(totalPages, currentPage + 1), rankTerm, keyword)} className="next" aria-label="Próxima página" /></li>
                          <li className="last"><Link href={rankingPageHref(totalPages, rankTerm, keyword)} className="last" aria-label="Última página" /></li>
                        </ul>
                      </nav>
                      <form className="new_search_wrap" action={RANKING_PATH}>
                        <input type="hidden" name="termtype" value={rankTerm} />
                        <div className="search_area">
                          <input type="text" name="keyword" defaultValue={keyword} placeholder="nickname" maxLength={32} />
                          <button type="submit">search</button>
                        </div>
                      </form>
                    </div>
                  ) : null}

                  <div className="nrk_explan">
                    <ul className="red_dot">
                      <li>O Ranking será atualizado às <span>04:00</span></li>
                      <li>Dados do Ranking Diário registram de 00:00 ~ 23:59</li>
                      <li>Se o jogador começar a jogar às 23:59 e não realizar logout até às 00:00, o jogador não será registrado no ranking diário daquele dia.</li>
                      <li>Dados do Ranking Semanal atualizam toda Segunda-feira às 05:00.</li>
                      <li>Os cálculos do sistema são realizados com base na taxa de vitória.</li>
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
