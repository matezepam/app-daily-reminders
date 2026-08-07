export type SessionRole = "ADMIN" | "PROFESSOR" | "USER";

export type UserProfile = {
  id: number;
  cognitoSub: string;
  email: string;
  fullName: string;
  role: SessionRole;
  createdAt: string;
  updatedAt: string;
};

export type AuthSession = {
  accessToken: string;
  idToken: string;
  refreshToken: string | null;
  expiresAt: number;
  profile: UserProfile;
};

export type ApiErrorResponse = {
  message?: string;
  error?: string;
  fieldErrors?: Record<string, string>;
};
