"use client";

import Image from "next/image";
import { useRouter } from "next/navigation";
import { useMemo, useState, type ChangeEvent, type FormEvent } from "react";

const categories = {
  "Verificação de Idade": ["Verificação de identidade", "Restrição de idade"],
  "Recuperação de Conta": ["Esqueci minha senha", "Conta invadida", "Alteração de e-mail"],
  "Bad User": ["Denúncia de trapaça", "Comportamento inadequado"],
  "Problemas Técnicos": ["Erro ao iniciar", "Desconexão", "Desempenho"],
  Loja: ["Cash não creditado", "Erro na compra", "Item não recebido"],
  "Atendimento Geral": ["Dúvida", "Sugestão", "Outros"],
} as const;

const MAX_TOTAL_SIZE = 2 * 1024 * 1024;
const ATTACHMENT_COUNT = 5;

type TicketFormProps = {
  reportCheat?: boolean;
};

type ApiProblem = {
  detail?: string;
};

function formatSize(size: number) {
  if (size === 0) return "0kb";
  return `${Math.max(1, Math.round(size / 1024))}kb`;
}

export default function TicketForm({ reportCheat = false }: TicketFormProps) {
  const router = useRouter();
  const initialCategory = reportCheat ? "Bad User" : "";
  const [mainCategory, setMainCategory] = useState(initialCategory);
  const [subCategory, setSubCategory] = useState(reportCheat ? "Denúncia de trapaça" : "");
  const [files, setFiles] = useState<Array<File | null>>(() => Array(ATTACHMENT_COUNT).fill(null));
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const subCategories = mainCategory
    ? categories[mainCategory as keyof typeof categories] ?? []
    : [];
  const totalSize = useMemo(() => files.reduce((total, file) => total + (file?.size ?? 0), 0), [files]);

  function changeFile(index: number, event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0] ?? null;
    setFiles((current) => current.map((item, itemIndex) => (itemIndex === index ? file : item)));
  }

  async function submitTicket(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");

    if (totalSize > MAX_TOTAL_SIZE) {
      setMessage("Os anexos devem possuir no máximo 2 MB no total.");
      return;
    }

    setSubmitting(true);
    try {
      const response = await fetch("/api/tickets", {
        method: "POST",
        body: new FormData(event.currentTarget),
      });
      const result = (await response.json().catch(() => ({}))) as ApiProblem & { ticketId?: number };

      if (response.status === 401 || response.status === 403) {
        router.push("/login/form");
        return;
      }
      if (!response.ok) {
        setMessage(result.detail ?? "Não foi possível enviar o ticket.");
        return;
      }

      router.push(`/ticket/list?created=${result.ticketId ?? "1"}`);
      router.refresh();
    } catch {
      setMessage("Não foi possível se comunicar com o servidor.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={submitTicket} encType="multipart/form-data">
      <div className="ticket_wrap v2">
        <div className="tw">
          <div className="tw_form">
            <ul>
              <li>
                <label className="tit" htmlFor="mainCategory">Main Category <span className="ticket-required">*</span></label>
                <div className="tw_sel ticket-native-select">
                  <select
                    id="mainCategory"
                    name="mainCategory"
                    required
                    value={mainCategory}
                    onChange={(event) => {
                      setMainCategory(event.target.value);
                      setSubCategory("");
                    }}
                  >
                    <option value="">Please select a Main category.</option>
                    {Object.keys(categories).map((category) => <option key={category}>{category}</option>)}
                  </select>
                </div>
              </li>

              <li>
                <label className="tit" htmlFor="subCategory">Sub Category <span className="ticket-required">*</span></label>
                <div className="tw_sel ticket-native-select">
                  <select id="subCategory" name="subCategory" required value={subCategory} onChange={(event) => setSubCategory(event.target.value)}>
                    <option value="">Please select a Sub category.</option>
                    {subCategories.map((category) => <option key={category}>{category}</option>)}
                  </select>
                </div>
              </li>

              <li>
                <label className="tit" htmlFor="ticket-subject">Assunto <span className="ticket-required">*</span></label>
                <div className="inp">
                  <input id="ticket-subject" type="text" name="subject" minLength={6} maxLength={128} required placeholder="Por favor, preencha o assunto." />
                </div>
              </li>

              <li>
                <label className="tit" htmlFor="ticket-message">Pergunta / Reclamação <span className="ticket-required">*</span></label>
                <div className="inp">
                  <textarea id="ticket-message" name="message" maxLength={1500} required placeholder="Por favor, preencha sua dúvida." />
                </div>
              </li>

              <li>
                <span className="tit">
                  Anexos
                  <span className="type">(GIF, JPG, PNG)</span>
                  <span className="size_wrap">
                    <span className="txt">size <span className="ticket-muted">/</span> Max</span>
                    <span className={`size${totalSize > MAX_TOTAL_SIZE ? " over" : ""}`}>{formatSize(totalSize)} / <span className="ticket-highlight">2MB</span></span>
                  </span>
                </span>
                <ul className="atc_list">
                  {files.map((file, index) => (
                    <li key={index}>
                      <div className="input file_input">
                        <label className="btn_attach">
                          <Image src="/images/common/mypage/icon_comment.png" alt="" width={12} height={12} />
                          Anexar arquivo
                          <input type="file" name="attachments" accept=".jpg,.jpeg,.png,.gif,image/jpeg,image/png,image/gif" onChange={(event) => changeFile(index, event)} />
                        </label>
                        <input type="text" readOnly value={file?.name ?? ""} placeholder="Anexe o arquivo" title={`Arquivo ${index + 1}`} />
                      </div>
                      <span className="size">{formatSize(file?.size ?? 0)}</span>
                    </li>
                  ))}
                </ul>
              </li>
            </ul>
          </div>

          {message ? <div className="ticket-form-message" role="alert">{message}</div> : null}
          <div className="tl_bottom tl_bottom02">
            <button type="submit" className="btn_line_b02" disabled={submitting || totalSize > MAX_TOTAL_SIZE}>
              {submitting ? "ENVIANDO..." : "ENVIAR"}
            </button>
          </div>
        </div>
      </div>
    </form>
  );
}
