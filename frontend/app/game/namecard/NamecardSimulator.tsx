"use client";

import Image from "next/image";
import { useEffect, useRef, useState } from "react";
import styles from "./namecard.module.css";

type NamecardSimulatorProps = {
  initialNickname: string;
};

type PaletteMode = "text" | "outline";

const cards = [
  { id: 1, title: "Bactérias", type: "Cartão Animado", file: "215913508.gif", width: 381 },
  { id: 2, title: "Olho da Besta", type: "Cartão Animado", file: "220120582.gif", width: 381 },
  { id: 3, title: "Nuvem Negra", type: "Cartão Animado", file: "220140959.gif", width: 318 },
  { id: 4, title: "Céu Preto e Branco", type: "Cartão Estático", file: "220210461.png", width: 381 },
  { id: 5, title: "Camuflagem Digital", type: "Cartão Estático", file: "220250052.png", width: 318 },
  { id: 6, title: "Camuflagem", type: "Cartão Estático", file: "220316400.gif", width: 318 },
  { id: 7, title: "Camuflagem Verde", type: "Cartão Estático", file: "220334917.png", width: 318 },
  { id: 8, title: "Queijástico", type: "Cartão Animado", file: "220442371.gif", width: 381 },
  { id: 9, title: "Coelho Fofo", type: "Cartão Animado", file: "220512798.gif", width: 381 },
  { id: 10, title: "Vaga-Lume", type: "Cartão Animado", file: "220602596.gif", width: 320 },
  { id: 11, title: "Fogos de Artifício", type: "Cartão Animado", file: "220622960.gif", width: 381 },
  { id: 12, title: "Flor", type: "Cartão Estático", file: "220640745.gif", width: 381 },
  { id: 13, title: "Fantasma", type: "Cartão Animado", file: "220705445.gif", width: 318 },
  { id: 14, title: "Efeito de Luz", type: "Cartão Animado", file: "220737124.gif", width: 320 },
  { id: 16, title: "Vampire Rica", type: "Cartão Animado", file: "220848713.gif", width: 381 },
  { id: 15, title: "Vampire Hide", type: "Cartão Animado", file: "220835965.gif", width: 381 },
  { id: 17, title: "Lobo Solitário", type: "Cartão Animado", file: "220911052.gif", width: 381 },
] as const;

const basePalette = [
  [255, 0, 0], [253, 64, 21], [253, 128, 24], [252, 192, 28], [255, 255, 0],
  [189, 255, 30], [125, 255, 27], [61, 255, 26], [0, 255, 68], [9, 255, 129],
  [18, 255, 191], [26, 255, 254], [27, 191, 253], [28, 126, 252], [28, 61, 252],
  [0, 0, 255], [70, 0, 252], [131, 0, 252], [193, 0, 252], [255, 0, 255],
  [254, 0, 190], [253, 6, 66],
] as const;

const darkFactors = [0.3, 0.45, 0.6, 0.75, 0.9, 1];
const lightFactors = [0.15, 0.3, 0.45, 0.6, 0.75];
const grayPalette = [0, 26, 51, 77, 102, 128, 153, 179, 204, 230, 242];

function colorPalette() {
  const rows = darkFactors.map((factor, row) => [
    ...basePalette.map(([red, green, blue]) =>
      `rgb(${Math.floor(red * factor)}, ${Math.floor(green * factor)}, ${Math.floor(blue * factor)})`),
    `rgb(${grayPalette[row]}, ${grayPalette[row]}, ${grayPalette[row]})`,
  ]);

  lightFactors.forEach((factor, index) => {
    rows.push([
      ...basePalette.map(([red, green, blue]) =>
        `rgb(${Math.floor(red + (255 - red) * factor)}, ${Math.floor(green + (255 - green) * factor)}, ${Math.floor(blue + (255 - blue) * factor)})`),
      `rgb(${grayPalette[index + darkFactors.length]}, ${grayPalette[index + darkFactors.length]}, ${grayPalette[index + darkFactors.length]})`,
    ]);
  });

  const colors = rows.flat();
  const originalExceptions: Record<number, string> = {
    5: "rgb(57, 76, 9)",
    82: "rgb(24, 94, 189)",
    97: "rgb(170, 229, 26)",
    98: "rgb(119, 229, 24)",
    108: "rgb(62, 0, 226)",
    110: "rgb(173, 0, 229)",
    112: "rgb(228, 0, 170)",
    123: "rgb(0, 266, 68)",
    144: "rgb(114, 255, 61)",
    150: "rgb(65, 210, 253)",
    171: "rgb(90, 255, 210)",
    191: "rgb(149, 255, 129)",
    195: "rgb(127, 255, 255)",
    203: "rgb(254, 114, 255)",
    236: "rgb(255, 255, 198)",
  };

  Object.entries(originalExceptions).forEach(([index, color]) => {
    colors[Number(index)] = color;
  });

  return colors;
}

const palette = colorPalette();
const PAGE_TITLE = "SIMULADOR DE CARTÃO DE NICK";

function outlineShadow(color: string) {
  return `-2px -2px 0 ${color}, 0 -2px 0 ${color}, 2px -2px 0 ${color}, 2px 0 0 ${color}, 2px 2px 0 ${color}, 0 2px 0 ${color}, -2px 2px 0 ${color}, -2px 0 0 ${color}`;
}

function Particles() {
  return (
    <span className={styles.particles} aria-hidden="true">
      {Array.from({ length: 18 }, (_, index) => (
        <span className={styles[`hexa${index + 1}`]} key={index} />
      ))}
    </span>
  );
}

export default function NamecardSimulator({ initialNickname }: NamecardSimulatorProps) {
  const rootRef = useRef<HTMLDivElement>(null);
  const [nickname, setNickname] = useState("");
  const [textColor, setTextColor] = useState("");
  const [outlineColor, setOutlineColor] = useState("");
  const [previewCard, setPreviewCard] = useState<(typeof cards)[number] | null>(null);
  const [selectedCardId, setSelectedCardId] = useState<number | null>(null);
  const [paletteMode, setPaletteMode] = useState<PaletteMode | null>(null);
  const [paletteTop, setPaletteTop] = useState(0);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [visibleTitle, setVisibleTitle] = useState("");
  const nicknamePreview = nickname.trim() || initialNickname || "NICK NAME";
  const textShadow = outlineColor ? outlineShadow(outlineColor) : undefined;

  useEffect(() => {
    let character = 0;
    const typingTimer = window.setInterval(() => {
      character += 1;
      setVisibleTitle(PAGE_TITLE.slice(0, character));
      if (character >= PAGE_TITLE.length) window.clearInterval(typingTimer);
    }, 100);

    return () => window.clearInterval(typingTimer);
  }, []);

  const openPalette = (mode: PaletteMode, element: HTMLButtonElement) => {
    const rootTop = rootRef.current?.getBoundingClientRect().top ?? 0;
    setPaletteTop(element.getBoundingClientRect().top - rootTop - 20);
    setPaletteMode(mode);
  };

  const selectColor = (color: string) => {
    if (paletteMode === "text") setTextColor(color);
    if (paletteMode === "outline") setOutlineColor(color);
  };

  const sharePreview = () => {
    const url = encodeURIComponent(window.location.href);
    window.open(
      `http://www.facebook.com/sharer/sharer.php?u=${url}`,
      "share",
      "width=640,height=335,toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=no,resizable=no,copyhistory=no",
    );
  };

  return (
    <div className={styles.root} ref={rootRef}>
      <div className={styles.container} id="namecard-container">
        <section className={`${styles.section} ${styles.s1}`} id="namecard-top">
          <div className={styles.inner}>
            <div className={styles.titleWrap}>
              <h1 aria-label={PAGE_TITLE}>{visibleTitle || "\u00a0"}</h1>
            </div>
          </div>
          <Particles />
          <div className={styles.namePanel}>
            <div className={styles.inner}>
              <div className={styles.nameInfo}>
                <div className={styles.nameInfoPicture}>
                  <Image src="/images/common/namecard/chage_name.png" alt="Item para alterar a cor do nickname" width={104} height={79} unoptimized />
                  <div className={styles.nameInfoLabel}>Este item altera a cor do seu nickname.<br />Não pode ser usado durante partidas.</div>
                </div>
                <p>Teste o simulador de Cartão de Nick para encontrar sua combinação preferida<br /> antes de comprar o item de Alterar Cor do Nickname</p>
              </div>
            </div>
          </div>
        </section>

        <section className={`${styles.section} ${styles.s2}`} id="namecard-nickname">
          <div className={styles.inner}>
            <div className={styles.step}>PASSO 1</div>
            <div className={styles.contentBox}>
              <h2>DIGITE O <span>NICKNAME</span></h2>
              <div className={styles.nicknameInput}>
                <input
                  type="text"
                  value={nickname}
                  maxLength={32}
                  placeholder="NICK NAME"
                  aria-label="Nickname"
                  onChange={(event) => setNickname(event.target.value)}
                />
              </div>
            </div>
          </div>
        </section>

        <section className={`${styles.section} ${styles.s3}`} id="namecard-text-color">
          <div className={styles.inner}>
            <div className={styles.step}>PASSO 2</div>
            <div className={styles.contentBox}>
              <h2><span>ESCOLHA A COR</span> DO TEXTO</h2>
              <button
                className={styles.colorButton}
                type="button"
                style={textColor ? { color: textColor } : undefined}
                aria-label="Escolher a cor do texto"
                onClick={(event) => openPalette("text", event.currentTarget)}
              >Aa</button>
            </div>
          </div>
        </section>

        <section className={`${styles.section} ${styles.s4}`} id="namecard-outline-color">
          <div className={styles.inner}>
            <div className={styles.step}>PASSO 3</div>
            <div className={styles.contentBox}>
              <h2><span>ESCOLHA A COR DA</span> BORDA DO TEXTO</h2>
              <button
                className={`${styles.colorButton} ${styles.outlineColorButton}`}
                type="button"
                style={textShadow ? { textShadow } : undefined}
                aria-label="Escolher a cor da borda do texto"
                onClick={(event) => openPalette("outline", event.currentTarget)}
              >Aa</button>
            </div>
          </div>
        </section>

        <section className={`${styles.section} ${styles.s5}`} id="namecard-background">
          <div className={styles.inner}>
            <div className={styles.step}>PASSO 4</div>
            <div className={styles.contentBox}>
              <h2><span>ESCOLHA O PLANO DE</span> FUNDO DO CARTÃO</h2>
              <div className={styles.cardFrame}>
                <div className={styles.cardScroller}>
                  <ul className={styles.cardList}>
                    {cards.map((card) => (
                      <li className={selectedCardId === card.id ? styles.selected : undefined} key={card.id}>
                        <button
                          type="button"
                          onClick={() => {
                            setPreviewCard(card);
                            setSelectedCardId((selectedId) => selectedId === card.id ? null : card.id);
                          }}
                          aria-pressed={selectedCardId === card.id}
                        >
                          <span className={styles.cardTitle}>
                            <strong>{card.title}</strong>
                            <em>{card.type}</em>
                          </span>
                          <span className={styles.cardImage}>
                            <Image
                              src={`/images/common/namecard/cards/${card.file}`}
                              alt={`Cartão ${card.title}`}
                              width={card.width}
                              height={49}
                              unoptimized
                            />
                          </span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </section>

        <div className={styles.preview}>
          <button
            className={styles.previewOpen}
            type="button"
            aria-expanded={previewOpen}
            onClick={() => setPreviewOpen((open) => !open)}
          >PREVIEW</button>
          <div className={`${styles.previewArea} ${previewOpen ? styles.visible : ""}`} aria-hidden={!previewOpen}>
            <div className={styles.previewInner}>
              <div
                className={styles.previewBox}
                style={{
                  color: textColor || undefined,
                  textShadow,
                  backgroundImage: previewCard ? `url('/images/common/namecard/cards/${previewCard.file}')` : undefined,
                }}
              >{nicknamePreview}</div>
              <p>*Os tons das cores exibidas no simulador podem diferir das do jogo.</p>
              <button className={styles.facebook} type="button" aria-label="Compartilhar no Facebook" onClick={sharePreview} />
            </div>
          </div>
        </div>
      </div>

      {paletteMode ? (
        <div
          className={styles.palette}
          style={{ top: paletteTop }}
          role="dialog"
          aria-label="Paleta de cores do cartão de nick"
        >
          <button
            className={`${styles.colorButton} ${paletteMode === "outline" ? styles.outlineColorButton : ""}`}
            type="button"
            style={paletteMode === "text"
              ? (textColor ? { color: textColor } : undefined)
              : (textShadow ? { textShadow } : undefined)}
            aria-label="Fechar paleta"
            onClick={() => setPaletteMode(null)}
          >Aa</button>
          <div className={styles.paletteTitle}>Escolha uma cor para usar no CARTÃO DE NICK.</div>
          <ul className={styles.paletteGrid}>
            {palette.map((color) => {
              const selectedColor = paletteMode === "text" ? textColor : outlineColor;
              return (
                <li key={color}>
                  <button
                    type="button"
                    className={selectedColor === color ? styles.checked : undefined}
                    style={{ backgroundColor: color }}
                    aria-label={`Selecionar ${color}`}
                    onClick={() => selectColor(color)}
                  />
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}
    </div>
  );
}
