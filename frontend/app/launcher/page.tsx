import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { getPublishedNews, type NewsArticle, type NewsCategory } from "@/lib/news";
import styles from "./launcher.module.css";
import fallbackBanner from "./upload/image/20260904/192246739.png";
import storeBanner from "./upload/image/20250702/110235824.gif";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Notícias e acessos rápidos do launcher Point Blank.",
};

export const dynamic = "force-dynamic";

const categoryLabels: Record<NewsCategory, string> = {
  PROMO: "PROMOÇÃO",
  EVENT: "EVENTO",
  PATCH: "PATCH",
  ESPORTS: "E-SPORTS",
  INFO: "INFORMAÇÕES",
};

const categoryClasses: Record<NewsCategory, string> = {
  PROMO: styles.tagPromo,
  EVENT: styles.tagEvent,
  PATCH: styles.tagPatch,
  ESPORTS: styles.tagEsports,
  INFO: styles.tagInfo,
};

const monthNames = ["Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"];

function launcherNewsHref(newsId: number) {
  return `/noticias/view?idx=${newsId}&page=1`;
}

function launcherDate(article: NewsArticle) {
  const value = article.publishedAt ?? article.createdAt;
  const date = new Date(value);
  if (Number.isNaN(date.valueOf())) return "";
  return `${String(date.getUTCDate()).padStart(2, "0")}. ${monthNames[date.getUTCMonth()]}. ${date.getUTCFullYear()}`;
}

export default async function LauncherPage() {
  const news = await getPublishedNews();
  const visibleNews = news.slice(0, 5);

  return (
    <main className={styles.launcherPage}>
      <section className={styles.launcher} aria-label="Web launcher Point Blank">
        <div className={styles.webArea}>
          <header className={styles.top}>
            <nav aria-label="Redes sociais">
              <ul className={styles.socialList}>
                <li><span className={`${styles.socialLink} ${styles.discord}`} role="img" aria-label="Discord" /></li>
                <li><span className={`${styles.socialLink} ${styles.facebook}`} role="img" aria-label="Facebook" /></li>
                <li><span className={`${styles.socialLink} ${styles.instagram}`} role="img" aria-label="Instagram" /></li>
                <li><span className={`${styles.socialLink} ${styles.youtube}`} role="img" aria-label="YouTube" /></li>
              </ul>
            </nav>

            <nav aria-label="Acessos rápidos">
              <ul className={styles.groupButtons}>
                <li><Link className={`${styles.groupLink} ${styles.home}`} href="/" target="_blank" aria-label="Página inicial" /></li>
                <li><Link className={`${styles.groupLink} ${styles.user}`} href="/mypage/info" target="_blank" aria-label="Minha conta" /></li>
                <li><span className={`${styles.groupLink} ${styles.cash}`} role="img" aria-label="Cash" /></li>
              </ul>
            </nav>
          </header>

          <div className={styles.banner}>
            <div className={styles.hero}>
              <Link className={styles.heroCard} href="/news/list" target="_blank" aria-label="Ver todas as notícias">
                <Image className={styles.heroImage} src={fallbackBanner} alt="Point Blank" priority unoptimized />
              </Link>
            </div>

            <aside className={styles.notice} aria-label="Últimas notícias">
              {visibleNews.length > 0 ? (
                <ul className={styles.noticeList}>
                  {visibleNews.map((article) => (
                    <li className={styles.noticeItem} key={article.newsId}>
                      <p className={styles.noticeTitle}>
                        <a href={launcherNewsHref(article.newsId)} target="_blank" rel="noreferrer" title={article.title}>
                          <span className={`${styles.tag} ${categoryClasses[article.category]}`}>{categoryLabels[article.category]}</span>
                          {article.title}
                        </a>
                      </p>
                      <time className={styles.noticeDate} dateTime={article.publishedAt ?? article.createdAt}>{launcherDate(article)}</time>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className={styles.emptyNews}>Nenhuma notícia publicada.</p>
              )}

              <Link className={styles.storeBanner} href="/webstore/index" target="_blank" rel="noreferrer" aria-label="Abrir a loja Point Blank">
                <Image src={storeBanner} alt="Loja Point Blank" width={254} height={75} unoptimized />
              </Link>
            </aside>
          </div>
        </div>
      </section>
    </main>
  );
}
