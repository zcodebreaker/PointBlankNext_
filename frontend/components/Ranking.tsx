import Image from "next/image";
import Link from "next/link";
import ClanRankingCards from "@/components/ClanRankingCards";
import type { ClanRankingEntry } from "@/lib/clan-ranking";
import type { PlayerRankingEntry } from "@/lib/player-ranking";
import { getPlayerRank } from "@/lib/player-ranks";

type RankingProps = {
  clans: ClanRankingEntry[];
  players: PlayerRankingEntry[];
};

export default function Ranking({ clans, players }: RankingProps) {
  return (
    <section id="rank" className="rank_section">
      <div className="inner">
        <div className="title">Rank <Link href="/ranking/clan/list" className="go_read">Leia Mais</Link></div>
        <div className="indi_area">
          <div className="title">Rank Individual</div>
          <ul className="list">
            {players.map((player) => {
              const rank = getPlayerRank(player.rank, "profile");

              return (
              <li className={`win0${player.position}`} key={`${player.nickname}-${player.position}`}>
                <div className="con">
                  <div className="win">{player.position}</div>
                  <div className="rank">
                    <Image
                      src={rank.image}
                      alt={rank.name}
                      width={96}
                      height={96}
                      style={{ height: "auto" }}
                      unoptimized
                    />
                  </div>
                  <div className="user"><Link href="/ranking/individual/list">{player.nickname}</Link></div>
                  <div className="exp"><span>exp</span>{player.experience.toLocaleString("pt-BR")}</div>
                </div>
              </li>
              );
            })}
          </ul>
        </div>
        <div className="clan_area">
          <div className="title">Rank de Clãs</div>
          <ClanRankingCards clans={clans} />
        </div>
      </div>
    </section>
  );
}
