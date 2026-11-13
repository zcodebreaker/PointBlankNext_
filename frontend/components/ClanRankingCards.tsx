"use client";

import Image from "next/image";
import Link from "next/link";
import { useCallback, useEffect, useId, useRef, useState } from "react";
import ClanLogo from "@/components/ClanLogo";
import type { ClanRankingEntry } from "@/lib/clan-ranking";
import { getClanRank } from "@/lib/clan-ranks";

type ClanRankingCardsProps = {
  clans: ClanRankingEntry[];
};

const CLAN_MEMBER_LIMIT = 250;

function clanDivisionName(rank: number) {
  if (rank >= 50) return "Brigada";
  if (rank >= 40) return "Regimento";
  if (rank >= 30) return "Batalhão";
  if (rank >= 20) return "Companhia";
  if (rank >= 10) return "Pelotão";
  return "Esquadra";
}

function ClanInformationPopup({
  clan,
  titleId,
  onClose,
}: {
  clan: ClanRankingEntry;
  titleId: string;
  onClose: () => void;
}) {
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const previousOverflow = document.body.style.overflow;

    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", closeOnEscape);
    closeButtonRef.current?.focus();

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [onClose]);

  const rank = getClanRank(clan.experience);
  const division = clanDivisionName(rank.level);
  const experience = clan.experience.toLocaleString("pt-BR");

  return (
    <div className="clan-popup-layer">
      <button type="button" className="dimmed" aria-label="Fechar informações do clã" onClick={onClose} />
      <div className="pop-up pop720 pop-up-clan clan-information-popup" role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <div className="pop-layout">
          <h2 id={titleId}>
            <span>{clan.name}</span>
            <button ref={closeButtonRef} type="button" className="pop-close" aria-label="Fechar" onClick={onClose}>Fechar</button>
          </h2>

          <div className="pop-con">
            <div className="pop-in">
              <div className="state">
                <ClanLogo value={clan.logo} />
                <ul className="info">
                  <li><span>CLÃ Master</span><div>{clan.leader || "-"}</div></li>
                  <li>
                    <span>CLÃ members</span>
                    <div>{clan.memberCount}<strong className="clan-popup-member-limit">/{CLAN_MEMBER_LIMIT}</strong></div>
                  </li>
                  <li><span>Vitórias / Derrotas</span><div>{clan.wins.toLocaleString("pt-BR")} / {clan.losses.toLocaleString("pt-BR")}</div></li>
                </ul>
              </div>

              <div className="rank">
                <div className="lv">
                  <Image src={rank.image} alt="" width={20} height={20} unoptimized />
                  {division}
                </div>
                <div className="per"><span>{experience} EXP</span> · {clan.winRate}% vitórias</div>
                <div className="bar"><span className="bar_on" style={{ width: `${Math.max(0, Math.min(100, clan.winRate))}%` }} /></div>
              </div>

              <div className="pop-btns">
                <Link className="btn-detail" href={`/ranking/clan/list#clan-${clan.clanId}`} onClick={onClose}>VER DETALHES</Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function ClanRankingCards({ clans }: ClanRankingCardsProps) {
  const [selectedClan, setSelectedClan] = useState<ClanRankingEntry | null>(null);
  const titleId = useId();
  const closePopup = useCallback(() => setSelectedClan(null), []);

  return (
    <>
      <ul className="list">
        {clans.map((clan) => {
          const rank = getClanRank(clan.experience);

          return (
            <li className={`win0${clan.position}`} key={clan.clanId}>
              <div className="con">
                <div className="win">{clan.position}</div>
                <ClanLogo value={clan.logo} />
                <div className="user">
                  <button type="button" className="clan-popup-trigger" aria-haspopup="dialog" onClick={() => setSelectedClan(clan)}>
                    <span className="name">{clan.name}</span>{" "}
                    <span className="clan_rank">
                      <Image src={rank.image} alt="" width={22} height={22} unoptimized />
                      {clanDivisionName(rank.level)}
                    </span>
                  </button>
                </div>
                <div className="exp"><span>exp</span>{clan.experience.toLocaleString("pt-BR")}</div>
              </div>
            </li>
          );
        })}
      </ul>

      {selectedClan ? (
        <ClanInformationPopup clan={selectedClan} titleId={titleId} onClose={closePopup} />
      ) : null}
    </>
  );
}
