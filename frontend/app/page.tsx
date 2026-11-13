import CookieConsent from "@/components/CookieConsent";
import HeroCarousel from "@/components/HeroCarousel";
import NewsAndVideos from "@/components/NewsAndVideos";
import PremiumShop from "@/components/PremiumShop";
import QuickMenu from "@/components/QuickMenu";
import Ranking from "@/components/Ranking";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { getHeaderSession } from "@/lib/auth-session";
import { getClanRanking } from "@/lib/clan-ranking";
import { getPublishedNews } from "@/lib/news";
import { getPlayerRanking } from "@/lib/player-ranking";
import { getWebstoreItems } from "@/lib/webstore";

export default async function Home() {
  const [session, publishedNews, clanRanking, playerRanking, webstoreItems] = await Promise.all([
    getHeaderSession(),
    getPublishedNews(),
    getClanRanking(0, 3),
    getPlayerRanking(0, 3),
    getWebstoreItems().catch(() => []),
  ]);
  const heroSlides = publishedNews.slice(0, 5).map(({ newsId, title, carouselUrl }) => ({
    newsId,
    title,
    imageUrl: carouselUrl,
  }));
  const premiumItems = webstoreItems.map(({
    goodId,
    name,
    priceCash,
    countLabel,
    categoryLabel,
    tag,
    tagLabel,
    imagePath,
  }) => ({ goodId, name, priceCash, countLabel, categoryLabel, tag, tagLabel, imagePath }));

  return (
    <div id="all_wrap">
      <SiteHeader username={session?.username} accessLevel={session?.accessLevel} />
      <QuickMenu />
      <main className="pb_content">
        <div className="main_section new">
          <HeroCarousel slides={heroSlides} />
          <PremiumShop items={premiumItems} />
          <NewsAndVideos news={publishedNews.slice(0, 5)} />
          <Ranking clans={clanRanking.clans} players={playerRanking.players} />
        </div>
      </main>
      <SiteFooter />
      <CookieConsent />
      <TopButton className="go_top" />
    </div>
  );
}
