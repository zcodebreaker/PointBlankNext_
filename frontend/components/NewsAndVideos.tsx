import Image from "next/image";
import Link from "next/link";
import { newsDate } from "@/components/NewsCard";
import type { NewsArticle } from "@/lib/news";

const newsLabels = {
  PROMO: "PROMOÇÃO",
  EVENT: "EVENTO",
  PATCH: "PATCH",
  ESPORTS: "E-SPORTS",
  INFO: "INFORMAÇÕES",
} as const;

const newsClasses = {
  PROMO: "promo",
  EVENT: "event",
  PATCH: "patch",
  ESPORTS: "esports",
  INFO: "info",
} as const;

export default function NewsAndVideos({ news }: { news: NewsArticle[] }) {
  return (
    <section className="mid_section">
      <div id="noticias" className="news_area">
        <div className="inner">
          <div className="title">Notícias <Link href="/news/list" className="go_read">Leia Mais</Link></div>
          <ul className="news_list">
            {news.map((item, index) => (
              <li className={`${newsClasses[item.category]}${index === 0 ? " first" : ""}`} key={item.newsId}>
                <div className="con">
                  <div className="thum_img"><span>
                    <Image
                      className="news-card-image"
                      src={item.coverUrl}
                      alt={`Capa da notícia ${item.title}`}
                      width={1080}
                      height={1080}
                      sizes="(max-width: 640px) 258px, (max-width: 1280px) 224px, 20vw"
                      unoptimized={!item.coverUrl.startsWith("/api/v1/news/")}
                    />
                  </span></div>
                  <div className="desc">
                    <div className="tit"><span>{item.title}</span></div>
                    <span className="label">{newsLabels[item.category]}</span>
                    <div className="txt">{item.summary}</div>
                    <div className="date">{newsDate(item.publishedAt ?? item.createdAt)}</div>
                  </div>
                  <Link href={`/noticias/view?idx=${item.newsId}&page=1`} className="s_link" aria-label={`Ler ${item.title}`}>Ler notícia</Link>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>

    </section>
  );
}
