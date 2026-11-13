import type { CSSProperties, ReactNode } from "react";

type RichTextMark = {
  type?: string;
  attrs?: Record<string, unknown>;
};

type RichTextNode = {
  type?: string;
  attrs?: Record<string, unknown>;
  content?: RichTextNode[];
  marks?: RichTextMark[];
  text?: string;
};

const allowedFonts = new Set(["Arial", "Georgia", "Tahoma", "Times New Roman", "Verdana"]);
const allowedFontSizes = new Set(["12px", "14px", "16px", "18px", "24px", "32px"]);
const hexColorPattern = /^#[0-9a-f]{6}$/i;

function parseDocument(content: string): RichTextNode | null {
  const trimmed = content.trim();
  if (!trimmed.startsWith("{")) return null;

  try {
    const parsed = JSON.parse(trimmed) as RichTextNode;
    return parsed.type === "doc" && Array.isArray(parsed.content) ? parsed : null;
  } catch {
    return null;
  }
}

function safeUrl(value: unknown) {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  if (trimmed.startsWith("/") && !trimmed.startsWith("//")) return trimmed;

  try {
    const url = new URL(trimmed);
    return url.protocol === "http:" || url.protocol === "https:" ? url.toString() : null;
  } catch {
    return null;
  }
}

function textStyle(attrs: Record<string, unknown> | undefined): CSSProperties {
  const style: CSSProperties = {};
  if (typeof attrs?.color === "string" && hexColorPattern.test(attrs.color)) style.color = attrs.color;
  if (typeof attrs?.fontFamily === "string" && allowedFonts.has(attrs.fontFamily)) style.fontFamily = attrs.fontFamily;
  if (typeof attrs?.fontSize === "string" && allowedFontSizes.has(attrs.fontSize)) style.fontSize = attrs.fontSize;
  return style;
}

function renderText(node: RichTextNode, key: string): ReactNode {
  let result: ReactNode = node.text ?? "";

  for (const [index, mark] of (node.marks ?? []).entries()) {
    const markKey = `${key}-mark-${index}`;
    if (mark.type === "bold") result = <strong key={markKey}>{result}</strong>;
    else if (mark.type === "italic") result = <em key={markKey}>{result}</em>;
    else if (mark.type === "underline") result = <u key={markKey}>{result}</u>;
    else if (mark.type === "strike") result = <s key={markKey}>{result}</s>;
    else if (mark.type === "code") result = <code key={markKey}>{result}</code>;
    else if (mark.type === "textStyle") result = <span key={markKey} style={textStyle(mark.attrs)}>{result}</span>;
    else if (mark.type === "link") {
      const href = safeUrl(mark.attrs?.href);
      if (href) result = <a key={markKey} href={href} rel="noopener noreferrer">{result}</a>;
    }
  }

  return result;
}

function renderChildren(node: RichTextNode, key: string) {
  return (node.content ?? []).map((child, index) => renderNode(child, `${key}-${index}`));
}

function renderNode(node: RichTextNode, key: string): ReactNode {
  if (node.type === "text") return renderText(node, key);
  if (node.type === "hardBreak") return <br key={key} />;
  if (node.type === "horizontalRule") return <hr key={key} />;

  const children = renderChildren(node, key);
  if (node.type === "paragraph") return <p key={key}>{children.length ? children : <br />}</p>;
  if (node.type === "heading") {
    return node.attrs?.level === 3
      ? <h3 key={key}>{children}</h3>
      : <h2 key={key}>{children}</h2>;
  }
  if (node.type === "bulletList") return <ul key={key}>{children}</ul>;
  if (node.type === "orderedList") return <ol key={key} start={typeof node.attrs?.start === "number" ? node.attrs.start : undefined}>{children}</ol>;
  if (node.type === "listItem") return <li key={key}>{children}</li>;
  if (node.type === "blockquote") return <blockquote key={key}>{children}</blockquote>;
  if (node.type === "codeBlock") return <pre key={key}><code>{children}</code></pre>;
  if (node.type === "image") {
    const src = safeUrl(node.attrs?.src);
    if (!src) return null;
    const alt = typeof node.attrs?.alt === "string" ? node.attrs.alt.slice(0, 300) : "";
    return <img key={key} src={src} alt={alt} loading="lazy" decoding="async" />;
  }

  return children;
}

export default function RichNewsContent({ content }: { content: string }) {
  const document = parseDocument(content);
  if (document) {
    return <div className="news-article-text news-article-text--rich">{renderChildren(document, "document")}</div>;
  }

  const paragraphs = content.split(/\r?\n/);
  return (
    <div className="news-article-text">
      {paragraphs.map((paragraph, index) => paragraph
        ? <p key={`${index}-${paragraph}`}>{paragraph}</p>
        : <br key={`space-${index}`} />)}
    </div>
  );
}
