import { Platform } from "react-native";
import Constants from "expo-constants";
import type {
  AuthSession,
  ApiErrorResponse,
  UserProfile,
} from "@/src/types/auth";

const expoHost = Constants.expoConfig?.hostUri?.split(":")[0];
const nativeApiUrl = expoHost
  ? `http://${expoHost}:9090`
  : Platform.select({
      android: "http://10.0.2.2:9090",
      default: "http://localhost:9090",
    });
export const API_URL = (
  process.env.EXPO_PUBLIC_API_URL ??
  Platform.select({ web: "/api", default: nativeApiUrl })!
).replace(/\/$/, "");
const region = process.env.EXPO_PUBLIC_COGNITO_REGION ?? "us-east-1";
const clientId =
  process.env.EXPO_PUBLIC_COGNITO_CLIENT_ID ?? "21jjggihppq5ba7dsk1532eada";
const cognitoUrl = `https://cognito-idp.${region}.amazonaws.com/`;
const cognitoMessage = (message?: string) => {
  if (!message) return "Cognito no pudo completar la solicitud.";
  if (/already exists/i.test(message))
    return "Ya existe una cuenta con este correo.";
  if (/incorrect username or password/i.test(message))
    return "Correo o contraseña incorrectos.";
  if (/not confirmed/i.test(message))
    return "Debes verificar tu correo antes de iniciar sesión.";
  if (/invalid verification code/i.test(message))
    return "El código de verificación no es válido.";
  if (/expired/i.test(message)) return "El código expiró. Solicita uno nuevo.";
  if (/password/i.test(message) && /requirements|conform/i.test(message))
    return "La contraseña no cumple los requisitos de seguridad.";
  return message;
};

export class ApiClientError extends Error {
  constructor(
    message: string,
    readonly status = 0,
  ) {
    super(message);
    this.name = "ApiClientError";
  }
}
type CognitoResult = {
  AuthenticationResult?: {
    AccessToken: string;
    IdToken: string;
    RefreshToken?: string;
    ExpiresIn: number;
  };
  UserSub?: string;
  UserConfirmed?: boolean;
  CodeDeliveryDetails?: { Destination?: string };
  message?: string;
  __type?: string;
};

async function cognitoRaw(
  target: string,
  body: unknown,
): Promise<CognitoResult> {
  const response = await fetch(cognitoUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-amz-json-1.1",
      "X-Amz-Target": `AWSCognitoIdentityProviderService.${target}`,
    },
    body: JSON.stringify(body),
  });
  const data = (await response.json()) as CognitoResult;
  if (!response.ok)
    throw new ApiClientError(cognitoMessage(data.message), response.status);
  return data;
}

async function cognito(target: string, body: unknown) {
  const data = await cognitoRaw(target, body);
  if (!data.AuthenticationResult)
    throw new ApiClientError("Cognito no devolvió una sesión válida.");
  return data;
}

export async function authenticate(username: string, password: string) {
  const data = await cognito("InitiateAuth", {
    AuthFlow: "USER_PASSWORD_AUTH",
    ClientId: clientId,
    AuthParameters: { USERNAME: username, PASSWORD: password },
  });
  return data.AuthenticationResult!;
}

export async function refreshTokens(refreshToken: string) {
  const data = await cognito("InitiateAuth", {
    AuthFlow: "REFRESH_TOKEN_AUTH",
    ClientId: clientId,
    AuthParameters: { REFRESH_TOKEN: refreshToken },
  });
  return data.AuthenticationResult!;
}

export async function signUp(
  email: string,
  password: string,
  fullName: string,
) {
  return cognitoRaw("SignUp", {
    ClientId: clientId,
    Username: email,
    Password: password,
    UserAttributes: [
      { Name: "email", Value: email },
      { Name: "name", Value: fullName },
    ],
  });
}
export const confirmSignUp = (email: string, code: string) =>
  cognitoRaw("ConfirmSignUp", {
    ClientId: clientId,
    Username: email,
    ConfirmationCode: code,
  });
export const resendConfirmation = (email: string) =>
  cognitoRaw("ResendConfirmationCode", { ClientId: clientId, Username: email });
export const forgotPassword = (email: string) =>
  cognitoRaw("ForgotPassword", { ClientId: clientId, Username: email });
export const confirmForgotPassword = (
  email: string,
  code: string,
  password: string,
) =>
  cognitoRaw("ConfirmForgotPassword", {
    ClientId: clientId,
    Username: email,
    ConfirmationCode: code,
    Password: password,
  });

async function parseError(response: Response) {
  const payload = (await response
    .json()
    .catch(() => null)) as ApiErrorResponse | null;
  const details = payload?.fieldErrors
    ? Object.values(payload.fieldErrors).join(". ")
    : "";
  return (
    details || payload?.message || payload?.error || `Error ${response.status}`
  );
}

export async function apiRequest<T>(
  session: AuthSession,
  path: string,
  options: { method?: string; body?: unknown } = {},
): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    method: options.method ?? "GET",
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
      Authorization: `Bearer ${session.accessToken}`,
    },
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  if (!response.ok)
    throw new ApiClientError(await parseError(response), response.status);
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

export const getMyProfile = (accessToken: string) =>
  apiRequest<UserProfile>(
    {
      accessToken,
      idToken: "",
      refreshToken: null,
      expiresAt: 0,
      profile: {} as UserProfile,
    },
    "/users/me",
  );
