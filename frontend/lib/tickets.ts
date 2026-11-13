import "server-only";

import { cache } from "react";
import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";
import { AUTH_COOKIE } from "@/lib/auth-constants";
import { API_URL } from "@/lib/auth-session";

export type TicketStatus = "WAITING" | "ANSWERED" | "CLOSED";

export type TicketAttachment = {
  attachmentId: number;
  fileName: string;
  contentType: string;
  fileSize: number;
};

export type SupportTicket = {
  ticketId: number;
  playerId: number;
  username: string;
  mainCategory: string;
  subCategory: string;
  subject: string;
  message: string;
  status: TicketStatus;
  adminReplySubject: string | null;
  adminReply: string | null;
  repliedBy: string | null;
  repliedAt: string | null;
  createdAt: string;
  updatedAt: string;
  attachments: TicketAttachment[];
};

async function ticketRequest(path: string) {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;

  if (!token) redirect("/login/form");

  let response: Response;
  try {
    response = await fetch(`${API_URL}/api/v1/tickets${path}`, {
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });
  } catch (error) {
    throw new Error("A API do Point Blank está indisponível.", { cause: error });
  }

  if (response.status === 401 || response.status === 403) redirect("/login/form");
  if (response.status === 404) notFound();
  if (!response.ok) throw new Error(`Não foi possível carregar os tickets (HTTP ${response.status}).`);

  return response;
}

export const getMyTickets = cache(async function getMyTickets(): Promise<SupportTicket[]> {
  return (await ticketRequest("")).json() as Promise<SupportTicket[]>;
});

export async function getMyTicket(ticketId: number): Promise<SupportTicket> {
  return (await ticketRequest(`/${ticketId}`)).json() as Promise<SupportTicket>;
}
