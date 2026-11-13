import Image from "next/image";
import Link from "next/link";

export default function SiteFooter() {
  return (
    <footer id="rodape" className="footer">
      <div className="info">
        <div className="list_service">
          <ul>
            <li><a href="#rodape">Consentimento de cookies</a></li>
          </ul>
        </div>
      </div>
      <div className="inner">
        <span className="f_logo"><Image src="/images/common/logo_kryonix_ntw.png" alt="Kryonix Network" width={160} height={58} loading="eager" unoptimized /></span>
        <div className="contact">Se houver dúvidas, entre em contato com <Link href="/ticket/form" className="pcol">Ticket</Link></div>
        <div className="copyright">COPYRIGHT KRYONIX NETWORK. TODOS DIREITOS RESERVADOS</div>
        <div className="age"><Image src="/images/br/age18.png" alt="Classificação indicativa: 18 anos" width={502} height={288} loading="eager" unoptimized /></div>
      </div>
    </footer>
  );
}
