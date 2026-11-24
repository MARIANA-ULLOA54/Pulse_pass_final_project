import { z } from "zod";
export const category = z.enum([
  "MUSIC",
  "SPORTS",
  "TECHNOLOGY",
  "EDUCATION",
  "CULTURE",
  "ENTERTAINMENT",
]);
export const eventStatus = z.enum([
  "DRAFT",
  "PUBLISHED",
  "SOLD_OUT",
  "CANCELLED",
  "FINISHED",
]);
export const ticketType = z.enum(["GENERAL", "VIP", "BACKSTAGE", "STUDENT"]);
export const ticketStatus = z.enum(["RESERVED", "PAID", "CANCELLED", "USED"]);
export const artist = z.object({
  id: z.number(),
  stageName: z.string(),
  country: z.string().nullable().optional(),
  genre: z.string().nullable().optional(),
  active: z.boolean(),
});
export const venue = z.object({
  id: z.number(),
  code: z.string(),
  name: z.string(),
  city: z.string(),
  address: z.string().nullable().optional(),
  capacity: z.number(),
  active: z.boolean(),
});
export const eventSummary = z.object({
  id: z.number(),
  eventCode: z.string(),
  name: z.string(),
  category,
  status: eventStatus,
  eventDate: z.string(),
  venueCode: z.string(),
  venueName: z.string(),
});
export const eventDetail = eventSummary.extend({
  description: z.string().nullable().optional(),
  minimumAge: z.number(),
  artists: z.array(artist),
});
export const ticket = z.object({
  id: z.number(),
  ticketCode: z.string(),
  type: ticketType,
  price: z.number(),
  status: ticketStatus,
  purchaseDate: z.string(),
  userEmail: z.string(),
  eventCode: z.string(),
  eventName: z.string(),
});
export const user = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  active: z.boolean(),
  firstName: z.string(),
  lastName: z.string(),
  phone: z.string().nullable().optional(),
  city: z.string().nullable().optional(),
  birthDate: z.string(),
});
export const identity = z.object({
  userId: z.number(),
  username: z.string(),
  email: z.string().email(),
  roles: z.array(z.string()),
});
export const authResponse = z.object({
  accessToken: z.string(),
  tokenType: z.string(),
  expiresIn: z.number().int().positive(),
});
export type EventSummary = z.infer<typeof eventSummary>;
export type EventDetail = z.infer<typeof eventDetail>;
export type Ticket = z.infer<typeof ticket>;
export type TicketStatus = z.infer<typeof ticketStatus>;
export type Artist = z.infer<typeof artist>;
export type Venue = z.infer<typeof venue>;
export type Identity = z.infer<typeof identity>;
export type Category = z.infer<typeof category>;
export type TicketType = z.infer<typeof ticketType>;
export const registerSchema = z.object({
  username: z.string().trim().min(1, "Ingresa tu usuario").max(60),
  email: z.string().trim().pipe(z.email("Ingresa un correo válido").max(150)),
  firstName: z.string().trim().min(1, "Ingresa tus nombres").max(80),
  lastName: z.string().trim().min(1, "Ingresa tus apellidos").max(80),
  birthDate: z.iso.date("Ingresa una fecha válida").refine((value) => {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60_000);
    return value <= localDate.toISOString().slice(0, 10);
  }, "La fecha de nacimiento no puede ser futura"),
  password: z.string().min(12, "Usa al menos 12 caracteres")
    .refine((value) => value.trim().length > 0, "Ingresa tu contraseña")
    .refine((value) => new TextEncoder().encode(value).length <= 72, "La contraseña supera los 72 bytes"),
});
export const loginSchema = z.object({
  identifier: z.string().trim().min(1, "Ingresa tu usuario o correo"),
  password: z.string().min(1, "Ingresa tu contraseña"),
});
export const createEventSchema = z.object({
  eventCode: z.string().trim().min(1, "Ingresa el código del evento").max(30),
  name: z.string().trim().min(1, "Ingresa el nombre del evento").max(150),
  description: z.string().max(2000, "La descripción supera los 2000 caracteres"),
  category,
  eventDate: z.string().min(1),
  minimumAge: z.number().int().min(0),
  venueCode: z.string().min(1),
});
