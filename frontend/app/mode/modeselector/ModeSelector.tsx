"use client";

import Image from "next/image";
import Link from "next/link";
import { type CSSProperties, useEffect, useRef, useState } from "react";
import styles from "./modeselector.module.css";

type ModeInfo = {
  id: number;
  name: string;
  slug: string;
  cardImage: string;
};

const modes: ModeInfo[] = [
  { id: 1, name: "Partida mortal", slug: "deathmatch", cardImage: "check_deathmatch" },
  { id: 2, name: "Bomba", slug: "bomb", cardImage: "check_bomb" },
  { id: 3, name: "Vidro", slug: "glass", cardImage: "check_glass" },
  { id: 4, name: "Eliminar", slug: "eliminate", cardImage: "check_eliminate" },
  { id: 5, name: "Ás", slug: "ace", cardImage: "check_ace" },
  { id: 6, name: "Destruir", slug: "destroy", cardImage: "check_destory" },
  { id: 7, name: "Grátis para todos", slug: "freeforall", cardImage: "check_freeforall" },
  { id: 8, name: "Caçador de cabeças", slug: "headhunter", cardImage: "check_headhunter" },
  { id: 9, name: "Caos", slug: "chaos", cardImage: "check_chaos" },
  { id: 10, name: "Escapar", slug: "escape", cardImage: "check_escape" },
];

const allModeIds = modes.map((mode) => mode.id);
const spinDurationMs = 6500;

type ModePositionStyle = CSSProperties & { "--mode-row": number };
type ResultPanelStyle = CSSProperties & { "--glitch-image": string };

export default function ModeSelector() {
  const [enabled, setEnabled] = useState<number[]>(allModeIds);
  const [rotation, setRotation] = useState(0);
  const [spinning, setSpinning] = useState(false);
  const [result, setResult] = useState<ModeInfo | null>(null);
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

  function toggleMode(modeId: number) {
    if (spinning || result) return;
    setEnabled((current) => current.includes(modeId)
      ? current.filter((id) => id !== modeId)
      : [...current, modeId].sort((left, right) => left - right));
    setNotice("");
  }

  function resetModes() {
    if (spinning || result) return;
    setEnabled(allModeIds);
    setNotice("");
  }

  function spin() {
    if (spinning || result) return;
    if (enabled.length === 0) {
      setNotice("Nenhum modo selecionado. Selecione pelo menos um modo.");
      return;
    }

    const selectedId = enabled[Math.floor(Math.random() * enabled.length)];
    const selected = modes.find((mode) => mode.id === selectedId) ?? modes[0];
    const segmentIndex = modes.findIndex((mode) => mode.id === selected.id);
    const normalizedRotation = ((rotation % 360) + 360) % 360;
    const targetRotation = (360 - segmentIndex * 36) % 360;
    const alignment = (targetRotation - normalizedRotation + 360) % 360;

    setNotice("");
    setSpinning(true);
    setRotation((current) => current + 2160 + alignment);
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
    setResult(null);
  }

  return (
    <main className={styles.root}>
      <div className={styles.backgrounds} aria-hidden="true">
        <span className={`${styles.background} ${styles.backgroundOne}`} />
        <span className={`${styles.background} ${styles.backgroundTwo}`} />
        <span className={`${styles.background} ${styles.backgroundThree}`} />
        <span className={`${styles.background} ${styles.backgroundFour}`} />
      </div>
      <div className={styles.scanEffect} aria-hidden="true" />

      <header className={styles.header}>
        <Link href="/" aria-label="Voltar para a página inicial">
          <Image
            src="/images/common/logo_kryonix_ntw.png"
            alt="Kryonix Network"
            width={160}
            height={58}
            preload
          />
        </Link>
      </header>

      <div className={styles.content}>
        <h1 className={styles.title}>
          <Image
            src="/images/modeselector/title.png"
            alt="Seletor de Modo"
            width={974}
            height={62}
            sizes="(max-width: 1024px) calc(100vw - 48px), 974px"
          />
        </h1>

        <section className={styles.selector} aria-label="Seletor aleatório de modos">
          <div className={styles.wheelArea}>
            <div className={styles.wheel}>
              <div
                className={styles.wheelSpin}
                style={{ transform: `rotate(${rotation}deg)`, transitionDuration: `${spinDurationMs}ms` }}
                aria-hidden="true"
              >
                {modes.map((mode, index) => (
                  <span
                    className={styles.segment}
                    key={mode.id}
                    style={{ transform: `translateX(-50%) rotate(${index * 36}deg)` }}
                  >
                    <Image
                      src={`/images/modeselector/br/${mode.id}${enabled.includes(mode.id) ? "" : "_dis"}.png`}
                      alt=""
                      width={208}
                      height={330}
                      unoptimized
                    />
                  </span>
                ))}
              </div>

              <span className={styles.arrow} aria-hidden="true">
                <Image src="/images/modeselector/btn_arrow.png" alt="" width={74} height={36} unoptimized />
              </span>
              <span className={styles.startFrame} aria-hidden="true" />
              <button
                className={styles.startButton}
                type="button"
                onClick={spin}
                disabled={spinning}
                aria-label={spinning ? "Sorteando um modo" : "Iniciar sorteio de modo"}
              >
                {spinning ? "Sorteando" : "Início"}
              </button>
            </div>
          </div>

          <ul className={styles.modeList} aria-label="Modos disponíveis">
            {modes.map((mode, index) => {
              const isEnabled = enabled.includes(mode.id);
              const isLeft = index < 5;
              const row = isLeft ? index : index - 5;
              const positionStyle = {
                "--mode-row": row,
                [isLeft ? "left" : "right"]: 0,
                backgroundImage: `url(/images/modeselector/common/${mode.cardImage}.png)`,
              } as ModePositionStyle;

              return (
                <li className={isEnabled ? styles.enabled : styles.disabled} key={mode.id} style={positionStyle}>
                  <input
                    id={`mode-${mode.id}`}
                    type="checkbox"
                    checked={isEnabled}
                    disabled={spinning}
                    onChange={() => toggleMode(mode.id)}
                  />
                  <label htmlFor={`mode-${mode.id}`}><span>{mode.name}</span></label>
                </li>
              );
            })}
          </ul>

          <p className={styles.notice} role="status" aria-live="polite">
            {spinning ? "Sorteando um modo..." : notice}
          </p>
        </section>

        <button className={styles.resetButton} type="button" onClick={resetModes} disabled={spinning || Boolean(result)}>
          <span>Reset</span>
        </button>
      </div>

      <footer className={styles.footer}>COPYRIGHT KRYONIX NETWORK. TODOS DIREITOS RESERVADOS.</footer>

      {result ? (
        <div className={styles.popup} role="dialog" aria-modal="true" aria-labelledby="selected-mode-title">
          <button className={styles.backdrop} type="button" onClick={closeResult} aria-label="Fechar resultado" />
          <div
            className={styles.resultPanel}
            style={{ "--glitch-image": `url(/images/modeselector/common/img_${result.slug}_glitch.png)` } as ResultPanelStyle}
          >
            <button className={styles.closeButton} type="button" onClick={closeResult} aria-label="Fechar resultado" autoFocus>
              Fechar
            </button>
            <div className={styles.resultIcon}>
              <Image
                src={`/images/modeselector/common/img_${result.slug}.png`}
                alt=""
                width={240}
                height={240}
                unoptimized
              />
            </div>
            <h2 id="selected-mode-title">{result.name}</h2>
          </div>
        </div>
      ) : null}
    </main>
  );
}
