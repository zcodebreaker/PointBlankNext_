"use client";

import { useState, type MouseEvent } from "react";

export default function QuickMenu() {
  const [isOpen, setIsOpen] = useState(false);

  function toggleQuickMenu(event: MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    setIsOpen((open) => !open);
  }

  return (
    <>
      <aside
        id="quick-menu"
        className="quick_area"
        aria-label="Acessos rápidos"
        style={isOpen ? { display: "block" } : undefined}
      >
        <h3>quick<br />menu</h3>
        <div className="list">
          <div className="dst">
            <div className="dstc">
              <ul className="q_gnb">
                <li className="down"><a href="/game/download"><span>Download</span></a></li>
                <li className="cheat"><a href="/ticket/form?mode=cheat"><span>Denunciar<br />trapaça</span></a></li>
                <li className="selector">
                  <div className="desc">
                    <span />
                    <div className="list">
                      <a href="/mode/mapselector" className="map">Seletor de Mapa</a>
                      <a href="/mode/modeselector" className="mode">Seletor de Modo</a>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </aside>
      <a
        href="#"
        className={`go_quick${isOpen ? " open" : ""}`}
        onClick={toggleQuickMenu}
        aria-controls="quick-menu"
        aria-expanded={isOpen}
      >
        quick<br />menu
      </a>
    </>
  );
}
