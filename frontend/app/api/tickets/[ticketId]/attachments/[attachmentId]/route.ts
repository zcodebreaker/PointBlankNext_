import { cookies } from "next/headers";
import { AUTH_COOKIE } from "@/lib/auth-constants";
import { API_URL } from "@/lib/auth-session";

type AttachmentRouteContext = {
  params: Promise<{ ticketId: string; attachmentId: string }>;
};

export async function GET(_request: Request, context: AttachmentRouteContext) {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;
  if (!token) return new Response(null, { status: 401 });

  const { ticketId, attachmentId } = await context.params;
  try {
    const response = await fetch(
      `${API_URL}/api/v1/tickets/${encodeURIComponent(ticketId)}/attachments/${encodeURIComponent(attachmentId)}`,
      {
        headers: { Authorization: `Bearer ${token}` },
        cache: "no-store",
      },
    );
    const body = await response.arrayBuffer();
    const headers = new Headers();

    for (const name of ["content-type", "content-length", "content-disposition", "x-content-type-options"]) {
      const value = response.headers.get(name);
      if (value) headers.set(name, value);
    }

    return new Response(body, { status: response.status, headers });
  } catch {
    return new Response(null, { status: 503 });
  }
}

