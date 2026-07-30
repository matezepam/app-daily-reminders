import { ApiClientError, postAuthenticatedJson } from '@/src/api/client';
import { useAuth } from '@/src/context/AuthContext';
import { colors, shadow } from '@/src/theme';
import type { Course } from '@/src/types/auth';
import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function JoinCourseScreen() {
  const { session } = useAuth();
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [joinedCourse, setJoinedCourse] = useState<Course | null>(null);
  const canJoin = session?.roles?.includes('STUDENT');

  function normalizeCode(value: string) {
    const clean = value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 8);
    return clean.length > 3 ? `${clean.slice(0, 3)}-${clean.slice(3)}` : clean;
  }

  async function submit() {
    if (code.length !== 9 || !session || !canJoin) return;

    setBusy(true);
    setError('');
    try {
      const course = await postAuthenticatedJson<Course, { code: string }>(
        '/courses/join',
        { code },
        session.accessToken,
      );
      setJoinedCourse(course);
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : 'No se pudo completar la inscripción.',
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <SafeAreaView style={styles.safeArea}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.flex}>
        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <View style={styles.page}>
            <View style={styles.header}>
              <Pressable accessibilityLabel="Volver" onPress={() => router.back()} style={styles.backButton}>
                <Ionicons color={colors.blue} name="chevron-back" size={24} />
              </Pressable>
              <View style={styles.headerCopy}>
                <Text style={styles.title}>Unirme a una clase</Text>
                <Text style={styles.subtitle}>Ingresa el código que compartió tu profesor</Text>
              </View>
            </View>

            {!canJoin ? (
              <View style={styles.deniedCard}>
                <Ionicons color={colors.danger} name="lock-closed-outline" size={34} />
                <Text style={styles.deniedTitle}>Acceso exclusivo para estudiantes</Text>
                <Text style={styles.deniedText}>Las cuentas de profesor no pueden inscribirse en una clase.</Text>
              </View>
            ) : joinedCourse ? (
              <View style={styles.successCard}>
                <View style={styles.successIcon}>
                  <Ionicons color={colors.success} name="checkmark-circle-outline" size={44} />
                </View>
                <Text style={styles.successTitle}>Inscripción completa</Text>
                <Text style={styles.successText}>{joinedCourse.name}</Text>
                <View style={styles.joinedCodeBox}>
                  <Ionicons color={colors.blue} name="key-outline" size={22} />
                  <Text selectable style={styles.joinedCode}>{joinedCourse.joinCode}</Text>
                </View>
                <Pressable onPress={() => router.replace('/(tabs)' as never)} style={styles.submitButton}>
                  <Ionicons color={colors.white} name="home-outline" size={22} />
                  <Text style={styles.submitText}>Volver al inicio</Text>
                </Pressable>
              </View>
            ) : (
              <View style={styles.card}>
                <View style={styles.illustration}>
                  <Ionicons color={colors.blue} name="people-outline" size={42} />
                </View>
                <View style={styles.intro}>
                  <Text style={styles.cardTitle}>Código de invitación</Text>
                  <Text style={styles.cardText}>
                    Lo encontrarás en el detalle de la clase del profesor. Cada clase tiene un código diferente.
                  </Text>
                </View>

                <View style={styles.fieldGroup}>
                  <Text style={styles.label}>Código de la clase</Text>
                  <View style={styles.codeField}>
                    <Ionicons color={colors.blue} name="key-outline" size={24} />
                    <TextInput
                      autoCapitalize="characters"
                      autoCorrect={false}
                      maxLength={9}
                      onChangeText={(value) => setCode(normalizeCode(value))}
                      placeholder="MAT-8K2PX"
                      placeholderTextColor={colors.placeholder}
                      returnKeyType="done"
                      style={styles.codeInput}
                      value={code}
                    />
                  </View>
                  <Text style={styles.help}>Formato: tres letras, guion y cinco caracteres.</Text>
                </View>

                <View style={styles.infoRow}>
                  <Ionicons color={colors.blue} name="information-circle-outline" size={24} />
                  <Text style={styles.infoText}>
                    Al unirte podrás consultar todas las actividades publicadas en la clase.
                  </Text>
                </View>

                {error ? (
                  <View accessibilityRole="alert" style={styles.errorBox}>
                    <Ionicons color={colors.danger} name="alert-circle-outline" size={21} />
                    <Text style={styles.errorText}>{error}</Text>
                  </View>
                ) : null}

                <Pressable
                  disabled={code.length !== 9 || busy}
                  onPress={() => void submit()}
                  style={({ pressed }) => [
                    styles.submitButton,
                    (code.length !== 9 || busy) && styles.submitDisabled,
                    pressed && styles.submitPressed,
                  ]}
                >
                  {busy ? (
                    <ActivityIndicator color={colors.white} />
                  ) : (
                    <>
                      <Ionicons color={colors.white} name="enter-outline" size={23} />
                      <Text style={styles.submitText}>Unirme a la clase</Text>
                    </>
                  )}
                </Pressable>
              </View>
            )}
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { backgroundColor: colors.background, flex: 1 },
  flex: { flex: 1 },
  scroll: { flexGrow: 1, paddingBottom: 36 },
  page: { alignSelf: 'center', gap: 22, maxWidth: 620, padding: 22, width: '100%' },
  header: { alignItems: 'center', flexDirection: 'row', gap: 13 },
  backButton: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: colors.lineSoft,
    borderRadius: 14,
    borderWidth: 1,
    height: 46,
    justifyContent: 'center',
    width: 46,
  },
  headerCopy: { flex: 1, gap: 3 },
  title: { color: colors.navy, fontSize: 27, fontWeight: '800' },
  subtitle: { color: colors.muted, fontSize: 14, lineHeight: 20 },
  card: {
    backgroundColor: colors.white,
    borderColor: colors.lineSoft,
    borderRadius: 26,
    borderWidth: 1,
    gap: 20,
    padding: 22,
    ...shadow,
  },
  illustration: {
    alignItems: 'center',
    alignSelf: 'center',
    backgroundColor: colors.bluePale,
    borderRadius: 25,
    height: 82,
    justifyContent: 'center',
    width: 82,
  },
  intro: { alignItems: 'center', gap: 6 },
  cardTitle: { color: colors.navy, fontSize: 20, fontWeight: '800' },
  cardText: { color: colors.muted, fontSize: 14, lineHeight: 21, textAlign: 'center' },
  fieldGroup: { gap: 8 },
  label: { color: colors.text, fontSize: 15, fontWeight: '700' },
  codeField: {
    alignItems: 'center',
    backgroundColor: '#FBFDFF',
    borderColor: colors.line,
    borderRadius: 15,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 12,
    minHeight: 62,
    paddingHorizontal: 16,
  },
  codeInput: {
    color: colors.navy,
    flex: 1,
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: 2,
    paddingVertical: 14,
  },
  help: { color: colors.muted, fontSize: 12 },
  infoRow: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderRadius: 14,
    flexDirection: 'row',
    gap: 11,
    padding: 14,
  },
  infoText: { color: colors.text, flex: 1, fontSize: 13, lineHeight: 19 },
  submitButton: {
    alignItems: 'center',
    backgroundColor: colors.blue,
    borderRadius: 14,
    flexDirection: 'row',
    gap: 9,
    justifyContent: 'center',
    minHeight: 56,
  },
  submitDisabled: { opacity: 0.45 },
  submitPressed: { opacity: 0.78, transform: [{ scale: 0.995 }] },
  submitText: { color: colors.white, fontSize: 17, fontWeight: '800' },
  deniedCard: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: '#F6C4BE',
    borderRadius: 24,
    borderWidth: 1,
    gap: 10,
    padding: 28,
    ...shadow,
  },
  deniedTitle: { color: colors.navy, fontSize: 18, fontWeight: '800', textAlign: 'center' },
  deniedText: { color: colors.muted, fontSize: 14, textAlign: 'center' },
  errorBox: {
    alignItems: 'center',
    backgroundColor: '#FFF0EE',
    borderRadius: 13,
    flexDirection: 'row',
    gap: 9,
    padding: 12,
  },
  errorText: { color: colors.danger, flex: 1, fontSize: 13, lineHeight: 18 },
  successCard: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: colors.lineSoft,
    borderRadius: 26,
    borderWidth: 1,
    gap: 14,
    padding: 26,
    ...shadow,
  },
  successIcon: {
    alignItems: 'center',
    backgroundColor: colors.successPale,
    borderRadius: 32,
    height: 64,
    justifyContent: 'center',
    width: 64,
  },
  successTitle: { color: colors.navy, fontSize: 24, fontWeight: '800', textAlign: 'center' },
  successText: { color: colors.muted, fontSize: 16, textAlign: 'center' },
  joinedCodeBox: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderColor: colors.blueSoft,
    borderRadius: 16,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 10,
    justifyContent: 'center',
    paddingHorizontal: 18,
    paddingVertical: 14,
    width: '100%',
  },
  joinedCode: { color: colors.blue, fontSize: 22, fontWeight: '900', letterSpacing: 2 },
});
