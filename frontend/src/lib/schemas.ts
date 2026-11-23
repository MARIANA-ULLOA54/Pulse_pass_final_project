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
  expiresIn: z.number(),
});
export type EventSummary = z.infer<typeof eventSummary>;
export type EventDetail = z.infer<typeof eventDetail>;
export type Ticket = z.infer<typeof ticket>;
export type Artist = z.infer<typeof artist>;
export type Venue = z.infer<typeof venue>;
export type Identity = z.infer<typeof identity>;
export type Category = z.infer<typeof category>;
export type TicketType = z.infer<typeof ticketType>;
export const registerSchema = z.object({
  username: z.string().min(3),
  email: z.email(),
  firstName: z.string().min(1),
  lastName: z.string().min(1),
  birthDate: z.string().min(1),
  password: z.string().min(12),
});
export const loginSchema = z.object({
  identifier: z.string().min(1),
  password: z.string().min(1),
});
export const createEventSchema = z.object({
  eventCode: z.string().min(1).max(30),
  name: z.string().min(1).max(150),
  description: z.string(),
  category,
  eventDate: z.string().min(1),
  minimumAge: z.number().int().min(0),
  venueCode: z.string().min(1),
});
