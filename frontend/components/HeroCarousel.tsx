"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";

type HeroSlide = {
  newsId: number;
  title: string;
  imageUrl: string;
};

export default function HeroCarousel({ slides }: { slides: HeroSlide[] }) {
  const [active, setActive] = useState(0);

  if (slides.length === 0) return null;

  const activeSlide = active % slides.length;

  const move = (direction: number) => {
    setActive((current) => (current + direction + slides.length) % slides.length);
  };

  return (
    <section id="inicio" className="main_slide_wrap" aria-label="Destaques">
      <Image src="/upload/image/20260827/161858442.jpg" alt="" width={2400} height={700} className="pc hero-backdrop" loading="eager" unoptimized />
      <Image src="/upload/image/20260827/161858480.jpg" alt="" width={640} height={720} className="mobile hero-backdrop" loading="eager" unoptimized />
      <div className="main_slide">
        <div className="hero-slider">
          <div className="hero-viewport">
            <ul className="hero-slides">
              {slides.map((slide, index) => (
                <li className={activeSlide === index ? "is-active" : ""} key={slide.newsId} aria-hidden={activeSlide !== index}>
                  <Link href={`/noticias/view?idx=${slide.newsId}&page=1`} tabIndex={activeSlide === index ? 0 : -1}>
                    <Image
                      className="hero-news-image"
                      src={slide.imageUrl}
                      alt={slide.title}
                      width={1116}
                      height={628}
                      loading={index === 0 ? "eager" : "lazy"}
                      unoptimized
                    />
                  </Link>
                </li>
              ))}
            </ul>
          </div>
          <div className="hero-controls">
            <button type="button" className="slick-arrow slick-prev" aria-label="Banner anterior" onClick={() => move(-1)}>Anterior</button>
            <ul className="slick-dots" aria-label="Selecionar banner">
              {slides.map((slide, index) => (
                <li className={activeSlide === index ? "slick-active" : ""} key={slide.newsId}>
                  <button type="button" aria-label={`Mostrar banner ${index + 1}`} onClick={() => setActive(index)}>{index + 1}</button>
                </li>
              ))}
            </ul>
            <button type="button" className="slick-arrow slick-next" aria-label="Próximo banner" onClick={() => move(1)}>Próximo</button>
          </div>
        </div>
      </div>
    </section>
  );
}
