import Image from "next/image";
import Link from "next/link";
import type { NewsArticle } from "@/lib/news";

const labels = {
  PROMO: "promoção",
  EVENT: "evento",
  PATCH: "patch",
  ESPORTS: "e-sports",
  INFO: "informações",
} as const;

const classes = {
  PROMO: "promo",
  EVENT: "event",
  PATCH: "patch",
  ESPORTS: "esports",
  INFO: "info",
} as const;

const months = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];

export function newsDate(value: string | null) {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.valueOf())) return "";
  return `${String(date.getUTCDate()).padStart(2, "0")}.${months[date.getUTCMonth()]}.${date.getUTCFullYear()}`;
}

export default function NewsCard({ article }: { article: NewsArticle }) {
  const href = `/noticias/view?idx=${article.newsId}&page=1`;
  const isStoredCover = article.coverUrl.startsWith("/api/v1/news/");

  return (
    <li className={classes[article.category]}>
      <div className="con">
        <div className="thum_img">
          <span>
            <Image
              className="news-card-image"
              src={article.coverUrl}
              alt={`Capa da notícia ${article.title}`}
              width={1080}
              height={1080}
              sizes="(max-width: 640px) 258px, (max-width: 1280px) 224px, 30vw"
              unoptimized={!isStoredCover}
            />
          </span>
        </div>
        <div className="desc">
          <span className="label">{labels[article.category]}</span>
          <div className="tit">{article.title}</div>
          <div className="txt">{article.summary}</div>
          <div className="date">{newsDate(article.publishedAt ?? article.createdAt)}</div>
        </div>
        <Link href={href} className="s_link">link</Link>
      </div>
    </li>
  );
}
