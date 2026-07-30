import type { ApiErrorResponse, SessionIdentity } from '@/src/types/auth';

const apiUrl = (process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1').replace(
  /\/$/,
  '',
);

export class ApiClientError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'ApiClientError';
  }
}

export async function postJson<TResponse, TBody>(path: string, body: TBody): Promise<TResponse> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);

  try {
    const response = await fetch(`${apiUrl}${path}`, {
      body: JSON.stringify(body),
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
      signal: controller.signal,
    });
    const payload = (await response.json().catch(() => null)) as TResponse | ApiErrorResponse | null;

    if (!response.ok) {
      const message =
        (payload as ApiErrorResponse | null)?.message ??
        'We could not complete the request. Please try again.';
      throw new ApiClientError(message, response.status);
    }

    return payload as TResponse;
  } catch (error) {
    if (error instanceof ApiClientError) throw error;
    if (error instanceof Error && error.name === 'AbortError') {
      throw new ApiClientError('The server took too long to respond.', 408);
    }
    throw new ApiClientError('Unable to connect to Daily Reminder.', 0);
  } finally {
    clearTimeout(timeout);
  }
}

export async function getSessionIdentity(accessToken: string): Promise<SessionIdentity> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 15000);

  try {
    const response = await fetch(`${apiUrl}/session`, {
      headers: {
        Accept: 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      signal: controller.signal,
    });
    const payload = (await response.json().catch(() => null)) as SessionIdentity | ApiErrorResponse | null;

    if (!response.ok) {
      throw new ApiClientError(
        (payload as ApiErrorResponse | null)?.message ?? 'Unable to validate your session.',
        response.status,
      );
    }

    return payload as SessionIdentity;
  } catch (error) {
    if (error instanceof ApiClientError) throw error;
    if (error instanceof Error && error.name === 'AbortError') {
      throw new ApiClientError('The server took too long to respond.', 408);
    }
    throw new ApiClientError('Unable to connect to Daily Reminder.', 0);
  } finally {
    clearTimeout(timeout);
  }
}
