import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "O endereço solicitado não existe ou não está mais disponível.",
};

const leftLines = ["line01", "line02", "line03", "line04", "line05"] as const;
const rightLines = ["line06", "line09", "line08", "line07", "line10", "line11"] as const;

export default function NotFound() {
  return (
    <main className="error_wrap">
      <h1>
        <Link href="/" aria-label="Voltar para a página inicial da Kryonix Network">
          <Image
            src="/images/common/logo_kryonix_ntw.png"
            alt="Kryonix Network"
            width={160}
            height={58}
            priority
          />
        </Link>
      </h1>

      <section className="motion" aria-labelledby="not-found-title">
        <div className="left" aria-hidden="true">
          {leftLines.map((line) => (
            <Image
              key={line}
              src={`/images/common/error/${line}.png`}
              alt=""
              width={295}
              height={308}
            />
          ))}
        </div>

        <div className="center" aria-hidden="true">
          <div className="pic">
            <Image
              src="/images/common/error/error.gif"
              alt=""
              width={350}
              height={350}
              unoptimized
              priority
            />
          </div>
        </div>

        <div className="right" aria-hidden="true">
          {rightLines.map((line) => (
            <Image
              key={line}
              src={`/images/common/error/${line}.png`}
              alt=""
              width={327}
              height={335}
            />
          ))}
        </div>

        <div className="info">
          <div className="tit" id="not-found-title">
            <span className="not-found-title-accent">O</span>ps!
          </div>
          <p className="txt">404 página não encontrada!</p>
          <Link className="not-found-home-link" href="/">
            Voltar para o início
          </Link>
        </div>
      </section>
    </main>
  );
}
