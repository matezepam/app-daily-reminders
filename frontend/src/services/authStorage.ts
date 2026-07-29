import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

import type { AuthSession } from '@/src/types/auth';

const sessionKey = 'daily-reminder.auth-session';
const accessTokenKey = `${sessionKey}.access-token`;
const idTokenKey = `${sessionKey}.id-token`;
const refreshTokenKey = `${sessionKey}.refresh-token`;

type SessionMetadata = Omit<AuthSession, 'accessToken' | 'idToken' | 'refreshToken'>;

export async function saveSession(session: AuthSession) {
  if (Platform.OS === 'web') {
    await AsyncStorage.setItem(sessionKey, JSON.stringify(session));
    return;
  }

  const { accessToken, idToken, refreshToken, ...metadata } = session;
  await Promise.all([
    SecureStore.setItemAsync(sessionKey, JSON.stringify(metadata)),
    SecureStore.setItemAsync(accessTokenKey, accessToken),
    SecureStore.setItemAsync(idTokenKey, idToken),
    refreshToken
      ? SecureStore.setItemAsync(refreshTokenKey, refreshToken)
      : SecureStore.deleteItemAsync(refreshTokenKey),
  ]);
}

export async function loadSession(): Promise<AuthSession | null> {
  try {
    let session: AuthSession;

    if (Platform.OS === 'web') {
      const serializedSession = await AsyncStorage.getItem(sessionKey);
      if (!serializedSession) return null;
      session = JSON.parse(serializedSession) as AuthSession;
    } else {
      const [serializedMetadata, accessToken, idToken, refreshToken] = await Promise.all([
        SecureStore.getItemAsync(sessionKey),
        SecureStore.getItemAsync(accessTokenKey),
        SecureStore.getItemAsync(idTokenKey),
        SecureStore.getItemAsync(refreshTokenKey),
      ]);
      if (!serializedMetadata || !accessToken || !idToken) return null;
      session = {
        ...(JSON.parse(serializedMetadata) as SessionMetadata),
        accessToken,
        idToken,
        refreshToken,
      };
    }

    if (session.expiresAt <= Date.now()) {
      await clearSession();
      return null;
    }
    return session;
  } catch {
    await clearSession();
    return null;
  }
}

export async function clearSession() {
  if (Platform.OS === 'web') {
    await AsyncStorage.removeItem(sessionKey);
    return;
  }

  await Promise.all([
    SecureStore.deleteItemAsync(sessionKey),
    SecureStore.deleteItemAsync(accessTokenKey),
    SecureStore.deleteItemAsync(idTokenKey),
    SecureStore.deleteItemAsync(refreshTokenKey),
  ]);
}
