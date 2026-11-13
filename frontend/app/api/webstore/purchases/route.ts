import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { API_URL } from "@/lib/auth-session";
import { AUTH_COOKIE } from "@/lib/auth-constants";

export async function POST(request: Request) {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;

  if (!token) {
    return NextResponse.json(
      { title: "Login necessário", detail: "Entre na sua conta para comprar na Premium Shop." },
      { status: 401 },
    );
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return NextResponse.json(
      { title: "Dados inválidos", detail: "A solicitação de compra está incompleta." },
      { status: 400 },
    );
  }

  try {
    const response = await fetch(`${API_URL}/api/v1/webstore/purchases`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
      cache: "no-store",
    });

    const responseBody = await response.text();
    return new Response(responseBody, {
      status: response.status,
      headers: { "Content-Type": response.headers.get("Content-Type") ?? "application/json" },
    });
  } catch {
    return NextResponse.json(
      { title: "Loja indisponível", detail: "Não foi possível comunicar com a API da Premium Shop." },
      { status: 503 },
    );
  }
}
