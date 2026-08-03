import axios, {AxiosError} from "axios";
import {currentToken} from "./session";

export const API_BASE_URL = "http://10.0.2.2:8000/";

export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
});

api.interceptors.request.use(config => {
  const token = currentToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export async function request<T = unknown>(call: Promise<{data: T}>): Promise<T> {
  try {
    const response = await call;
    return response.data;
  } catch (error) {
    const axiosError = error as AxiosError<{message?: string; notification?: string}>;
    const message =
      axiosError.response?.data?.notification ||
      axiosError.response?.data?.message ||
      axiosError.message ||
      "网络请求失败";
    throw new Error(message);
  }
}

export const authApi = {
  getVerificationCode: (phone: string) => request(api.post("auth/verification/code/send", {phone})),
  login: (payload: Record<string, unknown>) => request(api.post("auth/login", payload)),
  logout: () => request(api.post("auth/logout")),
};

export const userApi = {
  getUserProfile: (userId: string) => request(api.post("user/user/profile", {userId})),
  updateUserProfile: (form: FormData) => request(api.post("user/user/update", form, {headers: {"Content-Type": "multipart/form-data"}})),
  getUserOnlineStatus: (userId: string) => request(api.post("user/user/online/check", {userId})),
  setUserOnlineStatus: (userId: string) => request(api.post("user/user/online/set", {userId})),
  setUserOfflineStatus: (userId: string) => request(api.post("user/user/offline/set", {userId})),
};

export const roomApi = {
  validateMeetingNo: (meetingNo: string) => request(api.post("media/room/validate-no", {meetingNo: meetingNo.trim()})),
  fetchMeetingInfo: (roomId: string) => request(api.get(`media/room/info/${roomId}`)),
  fetchMeetingDetail: (roomIdOrNo: string) => request(api.get(`media/room/detail/${roomIdOrNo}`)),
  closeMeeting: (roomId: string) => request(api.post("media/room/close", {roomId})),
  createMeeting: (payload: Record<string, unknown>) => request(api.post("media/room/create", payload)),
  joinMeeting: (payload: Record<string, unknown>) => request(api.post("media/room/join", payload)),
  fetchUpcomingMeetings: (page = 1, size = 5) => request(api.get("media/room/upcoming", {params: {page, size}})),
  fetchRecentMeetings: (page = 1, size = 10) => request(api.get("media/room/recent", {params: {page, size}})),
  fetchParticipantsList: (roomId: string, status = "1", page = 1, size = 100) =>
    request(api.get("media/room/participants", {params: {roomId, status, page, size}})),
};

export function responseDataObject(value: any): any | undefined {
  return value?.data && typeof value.data === "object" && !Array.isArray(value.data) ? value.data : undefined;
}

export function firstString(body: any, ...names: string[]): string | undefined {
  if (!body || typeof body !== "object") return undefined;
  for (const name of names) {
    const value = body[name];
    if (value !== undefined && value !== null && String(value).trim()) {
      return String(value);
    }
  }
  return undefined;
}

export function asArray(value: any): any[] {
  if (Array.isArray(value)) return value;
  const body = value && typeof value === "object" ? value : {};
  const direct = [body.data, body.list, body.records, body.rows, body.items].find(Array.isArray);
  if (direct) return direct;
  const data = body.data && typeof body.data === "object" ? body.data : {};
  return [data.list, data.records, data.rows, data.items].find(Array.isArray) || [];
}
