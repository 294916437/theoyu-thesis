import {NativeModules} from "react-native";
import type {AuthSession} from "./appTypes";

const {BlueSkySessionStorage} = NativeModules;

let cachedSession: AuthSession = {};

export async function loadSession(): Promise<AuthSession> {
  const values = await BlueSkySessionStorage.getSession();
  cachedSession = {
    token: values?.token || undefined,
    userId: values?.userId || undefined,
    nickname: values?.nickname || undefined,
    phone: values?.phone || undefined,
    avatar: values?.avatar || undefined,
  };
  return cachedSession;
}

export function currentSession(): AuthSession {
  return cachedSession;
}

export function currentToken(): string | undefined {
  return cachedSession.token;
}

export async function saveSession(token: string, userId: string) {
  cachedSession = {...cachedSession, token, userId};
  await BlueSkySessionStorage.saveSession(token, userId);
}

export async function saveUserProfile(profile: Pick<AuthSession, "userId" | "nickname" | "phone" | "avatar">) {
  cachedSession = {...cachedSession, ...profile};
  await BlueSkySessionStorage.saveUserProfile(profile.userId, profile.nickname ?? null, profile.phone ?? null, profile.avatar ?? null);
}

export async function clearSession() {
  cachedSession = {};
  await BlueSkySessionStorage.clearSession();
}
