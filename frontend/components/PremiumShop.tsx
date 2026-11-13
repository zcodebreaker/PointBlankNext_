"use client";

import Image from "next/image";
import { useEffect, useRef } from "react";

type PremiumShopItem = {
  goodId: number;
  name: string;
  priceCash: number;
  countLabel: string;
  categoryLabel: string;
  tag: number;
  tagLabel: string;
  imagePath: string;
};

type PremiumShopProps = {
  items: PremiumShopItem[];
};

const numberFormatter = new Intl.NumberFormat("pt-BR");

function tagClassName(tag: number) {
  if (tag === 2) return "hot";
  if (tag === 3) return "limit";
  if (tag === 4) return "random";
  return "new";
}

function getSlidesToShow(viewportWidth: number) {
  if (viewportWidth < 670) return 2;
  if (viewportWidth < 1400) return 3;
  if (viewportWidth < 1800) return 4;
  return 5;
}

export default function PremiumShop({ items }: PremiumShopProps) {
  const listRef = useRef<HTMLUListElement>(null);

  useEffect(() => {
    const alignInitialSlide = () => {
      const list = listRef.current;
      if (!list) return;

      list.scrollLeft = window.innerWidth < 1800 ? list.clientWidth / getSlidesToShow(window.innerWidth) : 0;
    };

    const animationFrame = window.requestAnimationFrame(alignInitialSlide);
    window.addEventListener("resize", alignInitialSlide);

    return () => {
      window.cancelAnimationFrame(animationFrame);
      window.removeEventListener("resize", alignInitialSlide);
    };
  }, []);

  const scroll = (direction: number) => {
    const list = listRef.current;
    if (!list) return;
    list.scrollBy({ left: direction * (list.clientWidth / getSlidesToShow(window.innerWidth)), behavior: "smooth" });
  };

  return (
    <section id="loja" className="shop_area">
      <div className="title">Loja Premium <a href="/webstore/index" className="go_read">Leia Mais</a></div>
      {items.length > 0 ? (
        <>
          <button type="button" className="btn_shop_arr btn_shop_l" aria-label="Produtos anteriores" onClick={() => scroll(-1)}>Anterior</button>
          <button type="button" className="btn_shop_arr btn_shop_r" aria-label="Próximos produtos" onClick={() => scroll(1)}>Próximo</button>
        </>
      ) : null}
      <div className="item_wrap">
        <ul className="item_list" ref={listRef}>
          {items.map((item) => (
            <li className="slick-slide" key={item.goodId}>
              <div className="con">
                <div className="item">
                  <div className="img">
                    <Image src={item.imagePath} alt={item.name} width={312} height={312} />
                  </div>
                </div>
                <div className="desc">
                  <div className="name">{item.name}</div>
                  <div className="state">
                    <span>{item.categoryLabel}</span>
                    <span className="limit">{item.countLabel}</span>
                    {item.tagLabel ? <span className={tagClassName(item.tag)}>{item.tagLabel}</span> : null}
                  </div>
                  <div className="price">
                    <Image src="/images/common/webshop/coin_pb.png" alt="PB Cash" width={24} height={24} />
                    <span className="current">{numberFormatter.format(item.priceCash)}</span>
                  </div>
                </div>
                <a href="/webstore/index" className="s_link" aria-label={`Ver ${item.name}`}>Ver produto</a>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
