



import type { Metadata } from "next";
import Image from "next/image";
import CookieConsent from "@/components/CookieConsent";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import SiteHeader from "@/components/ConnectedSiteHeader";
import TopButton from "@/components/TopButton";
import { getHeaderSession } from "@/lib/auth-session";
import { formatDownloadDate, formatDownloadSize, getDownloadSettings } from "@/lib/download-settings";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Baixe o cliente oficial do Point Blank e consulte os requisitos do sistema.",
};

const dx9Requirements = [
  ["Sistema operacional", "Windows 7 Service Pack 1 (64-bit) ou mais atual"],
  ["Processador (CPU)", "Intel Pentium E5700 @ 3.0GHz ou AMD Athlon II X2 245 @2.9Hz ou superior"],
  ["Memória (RAM)", "4GB RAM ou mais"],
  ["Placa de vídeo", "NVIDIA GeForce 8600 GT / AMD Radeon HD 2600 XT ou melhor (compatível com DirectX 9)"],
  ["Memória gráfica (VRAM)", "2256MB VRAM ou mais (pode ser exibida como 241MB de acordo com o sistema)"],
  ["Armazenamento", "15GB de espaço disponível"],
  ["Versão do DirectX", "DirectX 9.0c"],
  ["Taxa de atualização", "60Hz ou superior"],
] as const;

const dx11Requirements = [
  { label: "Sistema operacional", minimum: "Windows 10 (64-bit) ou mais recente" },
  { label: "Processador (CPU)", minimum: "Intel Core i5-3550 @ 3.30Hz (4 CPUs) / AMD FX-6300 @ 3.5GHz ou superior" },
  { label: "Memória (RAM)", minimum: "4GB RAM ou mais", recommended: "8GB RAM ou mais" },
  {
    label: "Placa de vídeo",
    minimum: <>NVIDIA GeForce GTX 760 /<br />AMD Radeon R9 270X ou superior<br />(Compatível com DirectX 11)</>,
    recommended: <>NVIDIA GeForce GTX 1060 /<br />AMD Radeon RX 580 ou superior<br />(Compatível com DirectX 11)</>,
  },
  { label: "Memória gráfica (VRAM)", minimum: "VRAM 2GB dedicada ou superior" },
  { label: "Shader model", minimum: "Placa de Vídeo com suporte a Shader Model 5.0 ou superior" },
  { label: "Armazenamento", minimum: <>15GB de espaço disponível<br />Um SSD é recomendado para carregamento mais rápido</> },
  { label: "Versão do DirectX", minimum: "DirectX 11" },
  { label: "Taxa de atualização", minimum: "60Hz ou superior" },
] as const;

const drivers = [
  { image: "dv01.png", alt: "AMD", href: "https://support.amd.com/pt-br/download", name: "Graphics Card" },
  { image: "dv02.png", alt: "NVIDIA", href: "https://www.nvidia.com/pt-br/drivers/", name: "Graphics Card" },
  { image: "dv04.png", alt: "DirectX", href: "https://support.microsoft.com/pt-br/kb/179113", name: "Direct X" },
] as const;

export default async function DownloadPage() {
  const [session, downloadSettings] = await Promise.all([
    getHeaderSession(),
    getDownloadSettings(),
  ]);

  return (
    <div id="all_wrap">
      <SiteHeader username={session?.username} accessLevel={session?.accessLevel} />
      <QuickMenu />

      <div className="pb_content">
        <div className="pb_sub">
          <div className="pb_game">
            <div className="pb_top">
              <div className="typing_wrap">
                <div className="typing-txt">Download</div>
                <div className="typing">Download</div>
              </div>
              <div className="txt">
                Para jogar Point Blank, você precisa baixar e instalar o CLIENT do jogo.<br />
                Escolha uma das opções abaixo e divirta-se!
              </div>
            </div>

            <div className="pb_sub_con">
              <div className="global">
                <div className="inner">
                  <div className="full_client">
                    <div className="tit">Download do jogo</div>
                    <div className="btns_down">
                      <div className="btn_quick">
                        <div className="name">Cliente completo</div>
                        <div className="txt">Download do jogo</div>
                        <a href={downloadSettings.fileUrl} className="go_down" target="_blank" rel="noreferrer">Download</a>
                      </div>
                    </div>
                    <div className="desc">
                      <div className="info">
                        <ul>
                          <li><span>Last update</span>{formatDownloadDate(downloadSettings.fileUpdatedAt)}</li>
                          <li><span>Client size</span>{formatDownloadSize(downloadSettings.fileSizeBytes)}</li>
                          <li><span>Client version</span>{downloadSettings.clientVersion}</li>
                        </ul>
                      </div>
                    </div>
                  </div>

                  <div className="mid_half">
                    <div className="mirror_site">
                      <div className="tit">Mirror Sites</div>
                      <div className="txt">Você também pode fazer o download do Point Blank pelos links abaixo.</div>
                      <div className="btns">
                        <a href={downloadSettings.mirrorTorrentUrl} target="_blank" rel="noreferrer" className="btn">
                          <Image src="/images/common/game/icon_torrent.png" alt="Torrent" width={80} height={80} style={{ height: "auto" }} unoptimized />
                          <span>Torrent</span>
                        </a>
                        <a href={downloadSettings.mirrorGoogleDriveUrl} target="_blank" rel="noreferrer" className="btn">
                          <Image src="/images/common/game/icon_ggdrive.png" alt="Google Drive" width={79} height={80} style={{ height: "auto" }} unoptimized />
                          <span>Google Drive</span>
                        </a>
                        <a href={downloadSettings.mirrorOneDriveUrl} target="_blank" rel="noreferrer" className="btn">
                          <Image src="/images/common/game/icon_onedrive.png" alt="OneDrive" width={97} height={80} style={{ height: "auto" }} unoptimized />
                          <span>One Drive</span>
                        </a>
                      </div>
                    </div>
                  </div>

                  <div className="system_dv">
                    <div className="wrap">
                      <div className="down_patch">
                        <div className="r_tit">
                          <div className="t1">Download patches</div>
                          <div className="t2">Não conseguiu fazer o patch automático? Baixe a atualização aqui.</div>
                        </div>
                        <div className="btn_patch">
                          <ul>
                            <li><a href={downloadSettings.patchPrimaryUrl}><span>Manual</span> Ver.261.0</a></li>
                            <li><a href={downloadSettings.patchSecondaryUrl}><span>Manual</span> Ver.260.0</a></li>
                          </ul>
                        </div>
                      </div>

                      <div className="r_tit">
                        <div className="t1">Requisitos de sistema <span>/ Configuração recomendada</span></div>
                      </div>

                      <div className="dx_table">
                        <ul>
                          <li className="title">
                            <div className="tit" />
                            <div className="txt"><span>DX9</span> Recomendado</div>
                          </li>
                          {dx9Requirements.map(([label, value]) => (
                            <li key={label}>
                              <div className="tit">{label}</div>
                              <div className="txt">
                                {value}
                                {label === "Memória (RAM)" ? <div className="b_txt"><span>OBS</span>Sistemas 32-bit possuem limitação de aproximadamente 3.5GB</div> : null}
                                {label === "Armazenamento" ? <> <br />Um SSD é recomendado para carregamento mais rápido</> : null}
                              </div>
                            </li>
                          ))}
                        </ul>
                      </div>

                      <div className="dx_table dx_table02">
                        <ul>
                          <li className="title in_txts">
                            <div className="tit" />
                            <div className="txt"><span>DX11</span> Mínimo</div>
                            <div className="txt"><span>DX11</span> Recomendado</div>
                          </li>
                          {dx11Requirements.map((requirement) => (
                            <li className={"recommended" in requirement ? "in_txts" : undefined} key={requirement.label}>
                              <div className="tit">{requirement.label}</div>
                              <div className="txt">{requirement.minimum}</div>
                              {"recommended" in requirement ? <div className="txt">{requirement.recommended}</div> : null}
                            </li>
                          ))}
                        </ul>
                      </div>

                      <div className="dx_info">
                        <ul>
                          <li>Certifique-se que o DirectX 11 runtime, atualizações do Windows e drivers gráficos estejam atualizados.<br />Caso contrário, poderá haver instabilidade no jogo ou problemas de execução.</li>
                          <li>Para performance otimizada, certifique-se que seu computador atenda ou exceda as configurações recomendadas.<br />Note que a performance do jogo pode variar de acordo com seu hardware, aplicativos executados em background e versão dos drivers gráficos.</li>
                        </ul>
                      </div>

                      <div className="driver">
                        <div className="r_tit">
                          <div className="t1">Drivers</div>
                          <div className="t2">
                            <ul>
                              <li>Atualize o driver da sua placa de vídeo.</li>
                              <li>Baixe o pacote DirectX para que você possa rodar o Point Blank</li>
                            </ul>
                          </div>
                        </div>
                        <ul>
                          {drivers.map((driver) => (
                            <li key={driver.image}>
                              <div className="con">
                                <div className="logo"><Image src={`/images/common/game/${driver.image}`} alt={driver.alt} width={130} height={80} style={{ height: "auto" }} loading="eager" unoptimized /></div>
                                <div className="info">
                                  <div className="name">{driver.name}</div>
                                  <div className="txt">Driver</div>
                                </div>
                                <a href={driver.href} target="_blank" rel="noreferrer" className="go_down">Download</a>
                              </div>
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  </div>

                  <a href="#download-installation" className="btn_read">See here for how to install</a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <CookieConsent />
      <SiteFooter />
      <TopButton className="go_top" />
    </div>
  );
}
