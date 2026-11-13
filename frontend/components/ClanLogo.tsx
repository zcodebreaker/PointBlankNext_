import Image from "next/image";

type ClanLogoProps = {
  value: number;
};

const CLAN_MARK_PATH = "/images/common/clan/clanmark";

function clanMarkClasses(value: number) {
  const logo = Number.isFinite(value) ? Math.trunc(value) >>> 0 : 0;
  const cm = logo & 0xff;
  const cf = (logo >>> 8) & 0xff;
  const tl = (logo >>> 16) & 0xff;
  const bp = (logo >>> 24) & 0xff;

  const valid = cm >= 1 && cm <= 10
    && cf >= 1 && cf <= 10
    && tl >= 1 && tl <= 181
    && bp >= 1 && bp <= 27;

  if (!valid) return null;

  return `cm${String(cm).padStart(2, "0")} cf${String(cf).padStart(2, "0")} tl${String(tl).padStart(2, "0")} bp${String(bp).padStart(2, "0")}`;
}

export default function ClanLogo({ value }: ClanLogoProps) {
  const classes = clanMarkClasses(value);

  if (!classes) {
    return (
      <div className="logo clan-logo-default" aria-label="Emblema padrão do clã" role="img">
        <Image
          className="img_logo"
          src={`${CLAN_MARK_PATH}/2b37949c-d895-491d-970c-31d9406ae18e-0.png`}
          alt=""
          width={64}
          height={64}
          unoptimized
        />
      </div>
    );
  }

  return (
    <div className={`logo ${classes}`} aria-label="Emblema do clã" role="img">
      <Image src={`${CLAN_MARK_PATH}/clanmark_bg_filled_x2.png`} className="c_bg" alt="" width={952} height={316} unoptimized />
      <Image src={`${CLAN_MARK_PATH}/clanmark_bg_outline_x2.png`} className="c_bg_line" alt="" width={952} height={316} unoptimized />
      <Image src={`${CLAN_MARK_PATH}/clanmark_symbol_filled_x2.png`} className="c_img" alt="" width={952} height={2224} unoptimized />
      <Image src={`${CLAN_MARK_PATH}/clanmark_symbol_outline_x2.png`} className="c_img_line" alt="" width={952} height={2224} unoptimized />
    </div>
  );
}
