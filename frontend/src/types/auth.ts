export type AuthenticationResponse = {
  accessToken: string;
  idToken: string;
  refreshToken: string | null;
  expiresIn: number;
  tokenType: string;
};

export type SessionRole = 'ADMIN' | 'STUDENT' | 'TEACHER';

export type SessionIdentity = {
  roles: SessionRole[];
  userId: string;
  username: string;
};

export type AuthSession = AuthenticationResponse & {
  email: string;
  expiresAt: number;
  roles?: SessionRole[];
  userId?: string;
  username?: string;
};

export type ApiErrorResponse = {
  message?: string;
};

export type Course = {
  createdAt: string;
  description?: string | null;
  id: number;
  joinCode: string;
  name: string;
  ownerUserId: string;
};
