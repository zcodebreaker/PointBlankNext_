import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import type { GameProfilePlayer } from "@/lib/game-profile";
import { requireGameProfile } from "@/lib/game-profile";
import { getPlayerRank } from "@/lib/player-ranks";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Consulte as estatísticas da sua conta Point Blank.",
};

const numberFormatter = new Intl.NumberFormat("pt-BR");
const percentageFormatter = new Intl.NumberFormat("pt-BR", {
  minimumFractionDigits: 0,
  maximumFractionDigits: 1,
});

type WeaponMetric = {
  key: "assault" | "smg" | "sniper" | "mg" | "shotgun";
  label: string;
  kills: number;
  deaths: number;
};

function percentage(value: number, total: number) {
  if (value <= 0 || total <= 0) return 0;
  return Math.min(100, Math.round((value / total) * 1000) / 10);
}

function PlayerList({ players }: { players: GameProfilePlayer[] }) {
  if (players.length === 0) {
    return <p className="txt"><span>Sem dados</span></p>;
  }

  return (
    <ul>
      {players.map((player) => {
        const rank = getPlayerRank(player.rank, "profile");
        return (
          <li key={player.playerId}>
            <Link href={`/ranking/individual/list?keyword=${encodeURIComponent(player.nickname)}`}>
              <Image src={rank.image} alt={rank.name} width={20} height={20} unoptimized />
              {player.nickname}
              <span className={`profile-player-status${player.online ? " online" : ""}`}>
                {player.online ? "Online" : "Offline"}
              </span>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

function WeaponSection({
  kind,
  title,
  icon,
  weapons,
}: {
  kind: "kill" | "death";
  title: string;
  icon: string;
  weapons: WeaponMetric[];
}) {
  const total = weapons.reduce((sum, weapon) => sum + (kind === "kill" ? weapon.kills : weapon.deaths), 0);

  return (
    <div className={kind}>
      <h3 className="title">
        <Image src={icon} alt="" width={180} height={180} unoptimized />
        <br />MÉDIA DE <span className="profile-accent">{title}</span> POR ARMA
      </h3>
      <ul className="list">
        {weapons.map((weapon) => {
          const value = kind === "kill" ? weapon.kills : weapon.deaths;
          const ratio = percentage(value, total);
          return (
            <li className={weapon.key} key={weapon.key}>
              <h4>{weapon.label}</h4>
              <div className="graph">
                <div className="progress">
                  <div
                    className="bar reveal active"
                    role="progressbar"
                    aria-label={`${title.toLowerCase()} com ${weapon.label}`}
                    aria-valuenow={ratio}
                    aria-valuemin={0}
                    aria-valuemax={100}
                    style={{ height: `${ratio}%` }}
                  >
                    &nbsp;
                  </div>
                </div>
              </div>
              <p className="ratio">{percentageFormatter.format(ratio)}%</p>
              <p className="value">{numberFormatter.format(value)}</p>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

export default async function GameProfilePage() {
  const profile = await requireGameProfile();
  const rank = getPlayerRank(profile.rank, "profile");
  const winRate = percentage(profile.wins, profile.matches);
  const killDeathRate = percentage(profile.kills, profile.kills + profile.deaths);
  const headshotRate = percentage(profile.headshots, profile.kills);
  const rankProgress = Math.min(100, Math.max(2, Math.round(((profile.rank + 1) / 52) * 1000) / 10));
  const weapons: WeaponMetric[] = [
    { key: "assault", label: "ASSAULT", kills: profile.weapons.assaultKills, deaths: profile.weapons.assaultDeaths },
    { key: "smg", label: "SMG", kills: profile.weapons.smgKills, deaths: profile.weapons.smgDeaths },
    { key: "sniper", label: "SNIPER", kills: profile.weapons.sniperKills, deaths: profile.weapons.sniperDeaths },
    { key: "mg", label: "MG", kills: profile.weapons.mgKills, deaths: profile.weapons.mgDeaths },
    { key: "shotgun", label: "SHOTGUN", kills: profile.weapons.shotgunKills, deaths: profile.weapons.shotgunDeaths },
  ];

  return (
    <div id="all_wrap">
      <SiteHeader username={profile.username} accessLevel={profile.accessLevel} />
      <QuickMenu />

      <main className="pb_content">
        <div className="pb_sub">
          <div className="profile_wrap">
            <section className="profile_user" aria-labelledby="game-profile-title">
              <div className="info_user info_user5">
                <div className="info_img" aria-hidden="true">
                  <Image src="/images/common/profile/user_bg5.jpg" className="flag" alt="" width={1884} height={492} priority unoptimized />
                </div>
                <div className="inner">
                  <div className="my_level">
                    <Image src={rank.image} alt={rank.name} width={96} height={96} priority unoptimized />
                  </div>
                  <h1 id="game-profile-title" className="nick">{profile.nickname}</h1>
                  <div className="exp">
                    <p>TOTAL EXP</p>
                    <p>{numberFormatter.format(profile.experience)}</p>
                  </div>
                  <ul className="playtime" aria-label="Tempo de jogo">
                    <li>
                      <h5>TEMPO <span className="profile-accent">DE JOGO</span></h5>
                      <p>{numberFormatter.format(profile.totalPlaytime)}<span className="profile-unit">M</span></p>
                    </li>
                    <li>
                      <h5>TEMPO <span className="profile-accent">DE PARTIDAS</span></h5>
                      <p>{numberFormatter.format(profile.matchPlaytime)}<span className="profile-unit">M</span></p>
                    </li>
                  </ul>
                  <div className="rank">
                    <p className="level">{rank.name}</p>
                    <p className="nextlevel">PATENTE {profile.rank + 1} <span className="profile-accent">/ 52</span></p>
                    <div className="progress">
                      <div
                        className="bar reveal active"
                        role="progressbar"
                        aria-label="Progresso de patente"
                        aria-valuenow={rankProgress}
                        aria-valuemin={0}
                        aria-valuemax={100}
                        style={{ width: `${rankProgress}%` }}
                      >
                        &nbsp;
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="info_ratio">
                <div className="inner">
                  <div className="battle">
                    <div className="title">
                      <h3>PARTIDAS TOTAIS</h3>
                      <p>{numberFormatter.format(profile.matches)}</p>
                    </div>
                    <div className="match">
                      <div className="stat">
                        <ul>
                          <li><h4>WIN</h4><p><span>{numberFormatter.format(profile.wins)}</span></p></li>
                          <li><h4>LOSE</h4><p><span>{numberFormatter.format(profile.losses)}</span></p></li>
                          <li><h4>EMPATES</h4><p><span>{numberFormatter.format(profile.draws)}</span></p></li>
                        </ul>
                      </div>
                      <div className="ratio">
                        <h4>taxa <br />de partidas</h4>
                        <ul className="graph">
                          <li className="me">
                            <h5>MINHA CONTA</h5>
                            <div className="progress"><div className="bar reveal active" style={{ height: `${winRate}%` }}><span>{percentageFormatter.format(winRate)}%</span></div></div>
                          </li>
                          <li>
                            <h5>AMIGOS (MÉDIA)</h5>
                            <div className="progress"><div className="bar reveal active" style={{ height: "0%" }}><span>0%</span></div></div>
                          </li>
                          <li>
                            <h5>AMIGOS (TOP 30%)</h5>
                            <div className="progress"><div className="bar reveal active" style={{ height: "0%" }}><span>0%</span></div></div>
                          </li>
                        </ul>
                      </div>
                    </div>

                    <div className="skill">
                      <div className="stat">
                        <ul>
                          <li className="kill reveal active"><h4>KILL</h4><p>{numberFormatter.format(profile.kills)}</p></li>
                          <li className="assist reveal active"><h4>ASSISTÊNCIAS</h4><p>{numberFormatter.format(profile.assists)}</p></li>
                          <li className="death reveal active"><h4>DEATH</h4><p>{numberFormatter.format(profile.deaths)}</p></li>
                          <li className="headshot reveal active"><h4>HEADSHOT</h4><p>{numberFormatter.format(profile.headshots)}</p></li>
                        </ul>
                      </div>
                      <div className="ratio">
                        <div className="kda">
                          <h4>TAXA DE KILL/DEATH</h4>
                          <ul className="graph">
                            <li className="me"><div className="progress"><div className="bar reveal active" style={{ width: `${killDeathRate}%` }}><span>{percentageFormatter.format(killDeathRate)}%</span></div></div></li>
                            <li><div className="progress"><div className="bar reveal active" style={{ width: "0%" }}><span>0%</span></div></div></li>
                            <li><div className="progress"><div className="bar reveal active" style={{ width: "0%" }}><span>0%</span></div></div></li>
                          </ul>
                        </div>
                        <div className="txt">
                          <ul>
                            <li className="me"><h5>MINHA CONTA</h5></li>
                            <li><h5>AMIGOS (MÉDIA)</h5></li>
                            <li><h5>AMIGOS (TOP 30%)</h5></li>
                          </ul>
                        </div>
                        <div className="headshot">
                          <h4>TAXA DE HEADSHOT</h4>
                          <ul className="graph">
                            <li className="me"><div className="progress"><div className="bar reveal active" style={{ width: `${headshotRate}%` }}><span>{percentageFormatter.format(headshotRate)}%</span></div></div></li>
                            <li><div className="progress"><div className="bar reveal active" style={{ width: "0%" }}><span>0%</span></div></div></li>
                            <li><div className="progress"><div className="bar reveal active" style={{ width: "0%" }}><span>0%</span></div></div></li>
                          </ul>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="weapon">
                  <div className="inner">
                    <WeaponSection kind="kill" title="KILLS" icon="/images/common/profile/icon_kill.png" weapons={weapons} />
                    <WeaponSection kind="death" title="MORTE" icon="/images/common/profile/icon_death.png" weapons={weapons} />
                  </div>
                </div>
              </div>

              <div className="members">
                <div className="inner">
                  <div className="friends reveal active">
                    <div className="title"><div className="tit"><h3>LISTA DE AMIGOS</h3><p>{profile.friends.length} membros</p></div></div>
                    <div className="list"><PlayerList players={profile.friends} /></div>
                  </div>
                  <div className="clan reveal active">
                    <div className="title"><div className="tit"><h3>CLÃ</h3><p className="clanname">{profile.clanName}</p></div></div>
                    <div className="list"><PlayerList players={profile.clanMembers} /></div>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </div>
      </main>

      <CookieConsent />
      <SiteFooter />
      <TopButton className="go_top" />
    </div>
  );
}
