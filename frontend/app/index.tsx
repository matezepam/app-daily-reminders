import { Brand } from '@/src/components/Brand';
import { AuthField } from '@/src/components/AuthField';
import { useAuth } from '@/src/context/AuthContext';
import { colors, shadow } from '@/src/theme';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { Redirect } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  Keyboard,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function LoginScreen() {
  const { busy, clearError, error, initializing, session, signIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');

  async function submitForm() {
    const normalizedEmail = email.trim();
    const nextEmailError = !normalizedEmail
      ? 'Email is required'
      : !/^\S+@\S+\.\S+$/.test(normalizedEmail)
        ? 'Enter a valid email address'
        : '';
    const nextPasswordError = !password
      ? 'Password is required'
      : password.length < 8
        ? 'Password must contain at least 8 characters'
        : '';

    setEmailError(nextEmailError);
    setPasswordError(nextPasswordError);

    if (nextEmailError || nextPasswordError) return;

    Keyboard.dismiss();
    await signIn(normalizedEmail, password).catch(() => undefined);
  }

  if (initializing) {
    return (
      <SafeAreaView style={styles.loadingPage}>
        <ActivityIndicator color={colors.blue} size="large" />
        <Text style={styles.loadingText}>Restoring your session...</Text>
      </SafeAreaView>
    );
  }

  if (session) return <Redirect href="/(tabs)" />;

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.keyboardView}
      >
        <ScrollView
          contentContainerStyle={styles.scrollContent}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <View style={styles.page}>
            <View style={styles.header}>
              <Brand />
              <View style={styles.illustration}>
                <Ionicons color={colors.blueSoft} name="library-outline" size={116} />
              </View>
              <Text style={styles.welcome}>Welcome</Text>
              <Text style={styles.subtitle}>Sign in to organize your academic life</Text>
            </View>

            <View style={styles.card}>
              <AuthField
                autoCapitalize="none"
                autoComplete="email"
                error={emailError}
                icon="mail-outline"
                keyboardType="email-address"
                label="Email address"
                onChangeText={(value) => {
                  setEmail(value);
                  if (emailError) setEmailError('');
                  if (error) clearError();
                }}
                placeholder="student@university.edu"
                value={email}
              />

              <AuthField
                autoCapitalize="none"
                autoComplete="password"
                error={passwordError}
                icon="lock-closed-outline"
                label="Password"
                onChangeText={(value) => {
                  setPassword(value);
                  if (passwordError) setPasswordError('');
                  if (error) clearError();
                }}
                onToggleSecure={() => setPasswordVisible((current) => !current)}
                placeholder="Enter your password"
                secureTextEntry={!passwordVisible}
                value={password}
              />

              <Pressable accessibilityRole="button" style={styles.forgotButton}>
                <Text style={styles.forgotText}>Forgot your password?</Text>
              </Pressable>

              {error ? (
                <View accessibilityRole="alert" style={styles.apiError}>
                  <Ionicons color={colors.danger} name="alert-circle-outline" size={21} />
                  <Text style={styles.apiErrorText}>{error}</Text>
                </View>
              ) : null}

              <Pressable
                accessibilityRole="button"
                disabled={busy}
                onPress={() => void submitForm()}
                style={({ pressed }) => [
                  styles.loginButton,
                  pressed && styles.loginButtonPressed,
                  busy && styles.disabledButton,
                ]}
              >
                <LinearGradient
                  colors={[colors.blue, colors.blueDark]}
                  end={{ x: 1, y: 1 }}
                  start={{ x: 0, y: 0 }}
                  style={styles.loginGradient}
                >
                  {busy ? (
                    <ActivityIndicator color={colors.white} />
                  ) : (
                    <>
                      <Ionicons color={colors.white} name="log-in-outline" size={24} />
                      <Text style={styles.loginText}>Sign In</Text>
                    </>
                  )}
                </LinearGradient>
              </Pressable>

              <View style={styles.securityRow}>
                <View style={styles.securityIcon}>
                  <Ionicons color={colors.blue} name="shield-checkmark-outline" size={25} />
                </View>
                <View style={styles.securityCopy}>
                  <Text style={styles.securityTitle}>Secure and protected access</Text>
                  <Text style={styles.securityText}>Your information is protected by Cognito</Text>
                </View>
              </View>
            </View>
          </View>
        </ScrollView>
        <View pointerEvents="none" style={styles.waveLight} />
        <View pointerEvents="none" style={styles.waveDark} />
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    backgroundColor: colors.background,
    flex: 1,
  },
  keyboardView: {
    flex: 1,
    overflow: 'hidden',
  },
  loadingPage: {
    alignItems: 'center',
    backgroundColor: colors.background,
    flex: 1,
    gap: 14,
    justifyContent: 'center',
  },
  loadingText: {
    color: colors.muted,
    fontSize: 15,
  },
  scrollContent: {
    flexGrow: 1,
    paddingBottom: 96,
  },
  page: {
    alignSelf: 'center',
    maxWidth: 520,
    paddingHorizontal: 22,
    paddingTop: 22,
    width: '100%',
  },
  header: {
    alignItems: 'center',
    minHeight: 292,
  },
  illustration: {
    opacity: 0.55,
    position: 'absolute',
    right: -18,
    top: 80,
  },
  welcome: {
    color: colors.navy,
    fontSize: 34,
    fontWeight: '800',
    letterSpacing: -0.8,
    marginTop: 34,
  },
  subtitle: {
    color: colors.muted,
    fontSize: 16,
    lineHeight: 23,
    marginTop: 8,
    textAlign: 'center',
  },
  card: {
    backgroundColor: colors.white,
    borderColor: colors.lineSoft,
    borderRadius: 26,
    borderWidth: 1,
    gap: 18,
    padding: 24,
    ...shadow,
  },
  apiError: {
    alignItems: 'center',
    backgroundColor: '#FFF1F0',
    borderRadius: 12,
    flexDirection: 'row',
    gap: 9,
    marginTop: -4,
    padding: 12,
  },
  apiErrorText: {
    color: colors.danger,
    flex: 1,
    fontSize: 13,
    lineHeight: 18,
  },
  authenticatedCard: {
    alignItems: 'center',
    paddingVertical: 34,
  },
  authenticatedIcon: {
    alignItems: 'center',
    backgroundColor: colors.successPale,
    borderRadius: 34,
    height: 68,
    justifyContent: 'center',
    width: 68,
  },
  authenticatedTitle: {
    color: colors.navy,
    fontSize: 22,
    fontWeight: '800',
    textAlign: 'center',
  },
  authenticatedEmail: {
    color: colors.blue,
    fontSize: 15,
    fontWeight: '700',
  },
  authenticatedText: {
    color: colors.muted,
    fontSize: 14,
    lineHeight: 21,
    maxWidth: 310,
    textAlign: 'center',
  },
  logoutButton: {
    alignItems: 'center',
    borderColor: colors.blue,
    borderRadius: 14,
    borderWidth: 1.5,
    flexDirection: 'row',
    gap: 9,
    justifyContent: 'center',
    marginTop: 10,
    minHeight: 52,
    width: '100%',
  },
  logoutText: {
    color: colors.blue,
    fontSize: 16,
    fontWeight: '700',
  },
  forgotButton: {
    alignSelf: 'flex-end',
    marginTop: -8,
    paddingVertical: 2,
  },
  forgotText: {
    color: colors.blue,
    fontSize: 14,
    fontWeight: '600',
  },
  loginButton: {
    borderRadius: 14,
    marginTop: 2,
    overflow: 'hidden',
  },
  loginButtonPressed: {
    opacity: 0.86,
    transform: [{ scale: 0.995 }],
  },
  disabledButton: {
    opacity: 0.7,
  },
  loginGradient: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 10,
    justifyContent: 'center',
    minHeight: 56,
    paddingHorizontal: 20,
  },
  loginText: {
    color: colors.white,
    fontSize: 18,
    fontWeight: '700',
  },
  securityRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 12,
    justifyContent: 'center',
    marginTop: 8,
  },
  securityIcon: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderRadius: 22,
    height: 44,
    justifyContent: 'center',
    width: 44,
  },
  securityCopy: {
    flexShrink: 1,
  },
  securityTitle: {
    color: colors.text,
    fontSize: 14,
    fontWeight: '700',
  },
  securityText: {
    color: colors.muted,
    fontSize: 12,
    marginTop: 2,
  },
  waveLight: {
    backgroundColor: colors.waveLight,
    borderRadius: 180,
    bottom: -88,
    height: 145,
    left: -50,
    position: 'absolute',
    transform: [{ rotate: '4deg' }],
    width: '120%',
  },
  waveDark: {
    backgroundColor: colors.blue,
    borderRadius: 180,
    bottom: -108,
    height: 160,
    position: 'absolute',
    right: -36,
    transform: [{ rotate: '-4deg' }],
    width: '112%',
  },
});
