import type { Metadata } from "next";
import { Chakra_Petch, Roboto } from "next/font/google";
import type { ReactNode } from "react";
import "./globals.css";

const chakraPetch = Chakra_Petch({
  variable: "--font-chakra-petch",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  display: "swap",
});

const roboto = Roboto({
  variable: "--font-roboto",
  subsets: ["latin"],
  weight: ["300", "400", "500", "700"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Site oficial do Piercing Blow BR.",
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html
      lang="pt-BR"
      className={`${chakraPetch.variable} ${roboto.variable}`}
      data-scroll-behavior="smooth"
    >
      <head>
        <link rel="stylesheet" href="/css/br/commona693.css" />
        <link rel="stylesheet" href="/css/br/suba693.css" />
        <link rel="stylesheet" href="/css/br/maina693.css" />
        <link rel="stylesheet" href="/css/br/clan.css" />
      </head>
      <body>{children}</body>
    </html>
  );
}
