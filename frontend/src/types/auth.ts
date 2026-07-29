export type AuthenticationResponse = {
  accessToken: string;
  idToken: string;
  refreshToken: string | null;
  expiresIn: number;
  tokenType: string;
};

export type AuthSession = AuthenticationResponse & {
  email: string;
  expiresAt: number;
};

export type ApiErrorResponse = {
  message?: string;
};
