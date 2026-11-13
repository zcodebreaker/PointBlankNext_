import Link from "next/link";
import { notFound } from "next/navigation";
import CookieConsent from "@/components/CookieConsent";
import SiteHeader from "@/components/ConnectedSiteHeader";
import NewsArticleActions from "@/components/NewsArticleActions";
import { newsDate } from "@/components/NewsCard";
import QuickMenu from "@/components/QuickMenu";
import RichNewsContent from "@/components/RichNewsContent";
import SiteFooter from "@/components/SiteFooter";
import TopButton from "@/components/TopButton";
import { getHeaderSession } from "@/lib/auth-session";
import { getPublishedArticle, getPublishedNews } from "@/lib/news";

const labels = { PROMO: "promoção", EVENT: "evento", PATCH: "patch", ESPORTS: "e-sports", INFO: "informações" } as const;

function highResolutionBannerUrl(bannerUrl: string) {
  try {
    const url = new URL(bannerUrl);

    if (url.hostname === "i.imgur.com") {
      url.pathname = url.pathname.replace(/_d(?=\.[a-z0-9]+$)/i, "");
      url.search = "";
    }

    return url.toString();
  } catch {
    return bannerUrl;
  }
}

export default async function NewsArticleView({ newsId, page }: { newsId: number; page: number }) {
  if (!Number.isSafeInteger(newsId) || newsId <= 0) notFound();
  const [session, fetchedArticle, publishedNews] = await Promise.all([
    getHeaderSession(),
    getPublishedArticle(newsId),
    getPublishedNews(),
  ]);
  if (!fetchedArticle) notFound();
  const articleIndex = publishedNews.findIndex((item) => item.newsId === fetchedArticle.newsId);
  const previousArticle = articleIndex > 0 ? publishedNews[articleIndex - 1] : null;
  const nextArticle = articleIndex >= 0 && articleIndex + 1 < publishedNews.length ? publishedNews[articleIndex + 1] : null;
  return (
    <div id="all_wrap">
      <SiteHeader username={session?.username} accessLevel={session?.accessLevel} />
      <QuickMenu />
      <div className="pb_content"><div className="pb_sub"><div className="pb_news">
        <div className="pb_top"><div className="typing_wrap"><div className="typing-txt">NOTÍCIAS</div><div className="typing">NOTÍCIAS</div></div></div>
        <div className="pb_sub_con"><div className="nw_wrap"><div className="nw_view">
          <div className="nw_v_tit"><div className="inner">
            <span className={`label label_${fetchedArticle.category.toLowerCase()}`}>{labels[fetchedArticle.category]}</span>
            <div className="tit">{fetchedArticle.title}</div>
            <div className="date">{newsDate(fetchedArticle.publishedAt ?? fetchedArticle.createdAt)}</div>
          </div></div>
          <div className="inner">
            <div className="nw_v_con">
              <img
                className="news-article-banner"
                src={highResolutionBannerUrl(fetchedArticle.bannerUrl)}
                alt=""
                decoding="async"
              />
              <div className="news-article-copy">
                <div className="news-article-date">Data: {new Intl.DateTimeFormat("pt-BR", { timeZone: "UTC" }).format(new Date(fetchedArticle.publishedAt ?? fetchedArticle.createdAt))}</div>
                <h1>{fetchedArticle.title}</h1>
                <hr />
                {fetchedArticle.summary ? <p className="news-article-summary">{fetchedArticle.summary}</p> : null}
                <RichNewsContent content={fetchedArticle.content} />
              </div>
            </div>
            <NewsArticleActions />
            {previousArticle || nextArticle ? (
              <div className="nw_paging"><ul>
                <li className="left">{previousArticle ? <Link href={`/noticias/view?idx=${previousArticle.newsId}&page=${page}`}>{previousArticle.title}</Link> : null}</li>
                <li className="right">{nextArticle ? <Link href={`/noticias/view?idx=${nextArticle.newsId}&page=${page}`}>{nextArticle.title}</Link> : null}</li>
              </ul></div>
            ) : null}
          </div>
        </div></div></div>
      </div></div></div>
      <CookieConsent /><SiteFooter /><TopButton className="go_top" />
    </div>
  );
}
