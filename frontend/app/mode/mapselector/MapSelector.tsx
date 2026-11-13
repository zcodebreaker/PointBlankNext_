"use client";

import Image from "next/image";
import Link from "next/link";
import { type CSSProperties, useEffect, useRef, useState } from "react";
import styles from "./mapselector.module.css";

type MapInfo = {
  id: number;
  name: string;
  imageName: string;
};

const maps: MapInfo[] = [
  { id: 18, name: "Fallucity", imageName: "fallucity" },
  { id: 1, name: "New Midtown", imageName: "newmidtown" },
  { id: 2, name: "Luxvile", imageName: "luxville" },
  { id: 3, name: "Downtown", imageName: "downtown" },
  { id: 4, name: "Blowcity", imageName: "blowcity" },
  { id: 5, name: "Stromtube", imageName: "stormtube" },
  { id: 6, name: "Sandstorm", imageName: "sandstorm" },
  { id: 7, name: "Midtown", imageName: "midtown" },
  { id: 8, name: "Provence", imageName: "provence" },
  { id: 9, name: "Safehouse", imageName: "safehouse" },
  { id: 10, name: "SentryBase", imageName: "sentrybase" },
  { id: 11, name: "Secret Mansion", imageName: "secretmansion" },
  { id: 12, name: "Outpost", imageName: "outpost" },
  { id: 13, name: "Hospital", imageName: "hospital" },
  { id: 14, name: "Airport", imageName: "airport" },
  { id: 15, name: "Cemitério", imageName: "cemetery" },
  { id: 16, name: "Broken Alley", imageName: "brokenalley" },
  { id: 17, name: "Villa", imageName: "villa" },
];

const wheelOrder = [...maps.slice(1), maps[0]];
const allMapIds = maps.map((map) => map.id);
const spinDurationMs = 10_000;

export default function MapSelector() {
  const [enabled, setEnabled] = useState<number[]>(allMapIds);
  const [rotation, setRotation] = useState(0);
  const [spinning, setSpinning] = useState(false);
  const [result, setResult] = useState<MapInfo | null>(null);
  const [notice, setNotice] = useState("");
  const spinTimer = useRef<number | null>(null);

  useEffect(() => () => {
    if (spinTimer.current !== null) window.clearTimeout(spinTimer.current);
  }, []);

  useEffect(() => {
    if (!result) return;
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") closeResult();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [result]);

  function toggleMap(mapId: number) {
    if (spinning || result) return;
    setEnabled((current) => current.includes(mapId) ? current.filter((id) => id !== mapId) : [...current, mapId].sort((left, right) => left - right));
    setNotice("");
  }

  function resetMaps() {
    if (spinning || result) return;
    setEnabled(allMapIds);
    setRotation(0);
    setNotice("");
  }

  function spin() {
    if (spinning || result) return;
    if (enabled.length === 0) {
      setNotice("Nenhum mapa selecionado, por favor selecione um mapa.");
      return;
    }

    const selectedId = enabled[Math.floor(Math.random() * enabled.length)];
    const selected = maps.find((map) => map.id === selectedId) ?? maps[0];
    const segmentIndex = wheelOrder.findIndex((map) => map.id === selected.id);
    const targetAngle = 360 - (segmentIndex * 20 + 10);
    const normalizedRotation = ((rotation % 360) + 360) % 360;
    const alignment = (targetAngle - normalizedRotation + 360) % 360;

    setNotice("");
    setSpinning(true);
    setRotation((current) => current + 6480 + alignment);
    spinTimer.current = window.setTimeout(() => {
      setResult(selected);
      setSpinning(false);
      spinTimer.current = null;
    }, spinDurationMs);
  }

  function closeResult() {
    if (result) {
      setEnabled((current) => current.filter((id) => id !== result.id));
    }
    setRotation(0);
    setResult(null);
  }

  return (
    <main className={styles.root}>
      <div className={styles.allWrap}>
        <Image className={`${styles.character} ${styles.characterLeft}`} src="/images/mapselector/cha01.png" alt="" width={540} height={1014} priority />
        <Image className={`${styles.character} ${styles.characterRight}`} src="/images/mapselector/cha02.png" alt="" width={480} height={1014} priority />

        <header className={styles.header}>
          <Link className={styles.gameLogo} href="/" aria-label="Point Blank — página inicial">
            <Image src="/images/common/logo_bi_wide_b.png" alt="Point Blank" width={440} height={134} preload />
          </Link>
        </header>

        <section className={styles.container} aria-label="Seletor de mapas">
          <div className={`${styles.line} ${styles.lineLeft}`} aria-hidden="true" />
          <div className={`${styles.line} ${styles.lineRight}`} aria-hidden="true" />

          <div className={styles.mapSelector}>
            <div className={styles.wheel}>
              <div
                className={styles.wheelSpin}
                style={{ transform: `rotate(${rotation}deg)`, transitionDuration: spinning ? `${spinDurationMs}ms` : "0ms" }}
                aria-hidden="true"
              >
                {wheelOrder.map((map, index) => (
                  <span className={styles.slice} key={map.id} style={{ transform: `translateX(-50%) rotate(${index * 20 + 10}deg)` }}>
                    <Image src={`/images/mapselector/br/${map.id}${enabled.includes(map.id) ? "" : "_dis"}.png`} alt="" width={126} height={360} unoptimized />
                  </span>
                ))}
              </div>
              <span className={styles.frame} aria-hidden="true" />
              <span className={styles.arrow} aria-hidden="true"><Image src="/images/mapselector/arrow.png" alt="" width={60} height={60} unoptimized /></span>
              <button
                className={styles.startButton}
                type="button"
                onClick={spin}
                disabled={spinning || Boolean(result)}
                aria-label={spinning ? "Sorteando um mapa" : "Iniciar seleção aleatória de mapa"}
              >
                {spinning ? "Sorteando" : "Início"}
              </button>
            </div>
          </div>

          <button className={styles.resetButton} type="button" onClick={resetMaps} disabled={spinning || Boolean(result)}><span>Reset</span></button>

          <ul className={styles.mapList} aria-label="Mapas disponíveis">
            {maps.map((map, index) => {
              const isEnabled = enabled.includes(map.id);
              const isLeft = index < 9;
              const row = isLeft ? index : index - 9;

              return (
                <li
                  className={!isEnabled ? styles.disabled : undefined}
                  key={map.id}
                  style={{ "--map-row": row, [isLeft ? "left" : "right"]: 0 } as CSSProperties}
                >
                  <input id={`map-${map.id}`} type="checkbox" checked={isEnabled} disabled={spinning || Boolean(result)} onChange={() => toggleMap(map.id)} />
                  <label htmlFor={`map-${map.id}`} style={{ backgroundImage: `url(/images/mapselector/common/check_map${map.id}.png)` }}><span>{map.name}</span></label>
                </li>
              );
            })}
          </ul>

          <p className={styles.notice} role="status" aria-live="polite">{spinning ? "Sorteando um mapa..." : notice}</p>
        </section>

        <footer className={styles.footer}>
          COPYRIGHT KRYONIX NETWORK. TODOS DIREITOS RESERVADOS.
        </footer>
      </div>

      {result ? (
        <div className={styles.popup} role="dialog" aria-modal="true" aria-label={`Mapa selecionado: ${result.name}`}>
          <button className={styles.dimmed} type="button" onClick={closeResult} aria-label="Fechar resultado" />
          <button className={styles.closeButton} type="button" onClick={closeResult} aria-label="Fechar resultado" autoFocus>Fechar</button>
          <div className={styles.popupMap}>
            <div className={styles.mapPreview}>
              <Image src={`/images/mapselector/popup/img_${result.imageName}.png`} alt={result.name} fill sizes="(max-width: 768px) 82vw, 568px" unoptimized />
            </div>
            <h2>{result.name}</h2>
          </div>
        </div>
      ) : null}
    </main>
  );
}
