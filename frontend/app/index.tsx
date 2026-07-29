import { Brand } from '@/src/components/Brand';
import { AuthField } from '@/src/components/AuthField';
import { colors, shadow } from '@/src/theme';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { useState } from 'react';
import {
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
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [emailError, setEmailError] = useState('');
  const [passwordError, setPasswordError] = useState('');

  function validateForm() {
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

    if (!nextEmailError && !nextPasswordError) {
      Keyboard.dismiss();
    }
  }

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
                }}
                onToggleSecure={() => setPasswordVisible((current) => !current)}
                placeholder="Enter your password"
                secureTextEntry={!passwordVisible}
                value={password}
              />

              <Pressable accessibilityRole="button" style={styles.forgotButton}>
                <Text style={styles.forgotText}>Forgot your password?</Text>
              </Pressable>

              <Pressable
                accessibilityRole="button"
                onPress={validateForm}
                style={({ pressed }) => [styles.loginButton, pressed && styles.loginButtonPressed]}
              >
                <LinearGradient
                  colors={[colors.blue, colors.blueDark]}
                  end={{ x: 1, y: 1 }}
                  start={{ x: 0, y: 0 }}
                  style={styles.loginGradient}
                >
                  <Ionicons color={colors.white} name="log-in-outline" size={24} />
                  <Text style={styles.loginText}>Sign In</Text>
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
