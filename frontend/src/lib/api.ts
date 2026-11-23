import { z } from "zod";
import { http, json, patch } from "./http";
import {
  artist,
  authResponse,
  category,
  createEventSchema,
  eventDetail,
  eventSummary,
  identity,
  loginSchema,
  registerSchema,
  ticket,
  ticketType,
  user,
  venue,
} from "./schemas";
const enc = encodeURIComponent;
export const api = {
  events: () => http("/api/events/published", z.array(eventSummary)),
  event: (code: string) => http(`/api/events/${enc(code)}`, eventDetail),
  byArtist: (name: string) =>
    http(`/api/events/by-artist?stageName=${enc(name)}`, z.array(eventSummary)),
  artists: () => http("/api/artists/active", z.array(artist)),
  artist: (id: string) => http(`/api/artists/${enc(id)}`, artist),
  artistByName: (name: string) =>
    http(`/api/artists/by-stage-name?stageName=${enc(name)}`, artist),
  venues: () => http("/api/venues/active", z.array(venue)),
  venue: (code: string) => http(`/api/venues/${enc(code)}`, venue),
  login: (data: z.infer<typeof loginSchema>) =>
    http("/api/auth/login", authResponse, json(data)),
  me: () => http("/api/auth/me", identity),
  register: (data: z.infer<typeof registerSchema>) =>
    http("/api/users", user, json(data)),
  userByEmail: (email: string) =>
    http(`/api/users/by-email?email=${enc(email)}`, user),
  userByUsername: (username: string) =>
    http(`/api/users/by-username?username=${enc(username)}`, user),
  tickets: (email: string) =>
    http(`/api/tickets/by-user?email=${enc(email)}`, z.array(ticket)),
  ticket: (code: string) => http(`/api/tickets/${enc(code)}`, ticket),
  buy: (data: {
    userEmail: string;
    eventCode: string;
    type: z.infer<typeof ticketType>;
  }) => http("/api/tickets", ticket, json(data)),
  cancel: (code: string) =>
    http(`/api/tickets/${enc(code)}/cancel`, ticket, patch),
  use: (code: string) => http(`/api/tickets/${enc(code)}/use`, ticket, patch),
  paid: (code: string) =>
    http(`/api/events/${enc(code)}/tickets/paid`, z.array(ticket)),
  createEvent: (data: z.input<typeof createEventSchema>) =>
    http("/api/events", eventDetail, json(data)),
  publish: (code: string) =>
    http(`/api/events/${enc(code)}/publish`, eventDetail, patch),
  addArtist: (code: string, id: number) =>
    http(`/api/events/${enc(code)}/artists/${id}`, eventDetail, {
      method: "POST",
    }),
};
export { category };
