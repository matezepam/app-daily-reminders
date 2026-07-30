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

export default function CreateCourseScreen() {
  const { session } = useAuth();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [createdCourse, setCreatedCourse] = useState<Course | null>(null);
  const canCreate = session?.roles?.some((role) => role === 'TEACHER' || role === 'ADMIN');

  async function submit() {
    const normalizedName = name.trim();
    if (!normalizedName || !session || !canCreate) return;

    setBusy(true);
    setError('');
    try {
      const course = await postAuthenticatedJson<
        Course,
        { description?: string; name: string }
      >(
        '/courses',
        {
          name: normalizedName,
          ...(description.trim() ? { description: description.trim() } : {}),
        },
        session.accessToken,
      );
      setCreatedCourse(course);
    } catch (requestError) {
      setError(
        requestError instanceof ApiClientError
          ? requestError.message
          : 'No se pudo crear la clase. Inténtalo nuevamente.',
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
                <Text style={styles.title}>Crear clase</Text>
                <Text style={styles.subtitle}>Comparte un código único con tus estudiantes</Text>
              </View>
            </View>

            {!canCreate ? (
              <View style={styles.deniedCard}>
                <Ionicons color={colors.danger} name="lock-closed-outline" size={34} />
                <Text style={styles.deniedTitle}>Acceso exclusivo para profesores</Text>
                <Text style={styles.deniedText}>Tu cuenta no tiene permiso para crear clases.</Text>
              </View>
            ) : createdCourse ? (
              <View style={styles.successCard}>
                <View style={styles.successIcon}>
                  <Ionicons color={colors.success} name="checkmark-circle-outline" size={44} />
                </View>
                <Text style={styles.successTitle}>¡Clase creada!</Text>
                <Text style={styles.successText}>{createdCourse.name}</Text>
                <Text style={styles.codeLabel}>Código para estudiantes</Text>
                <View style={styles.codeBox}>
                  <Ionicons color={colors.blue} name="key-outline" size={24} />
                  <Text selectable style={styles.code}>{createdCourse.joinCode}</Text>
                </View>
                <Text style={styles.codeHelp}>Comparte este código para que tus estudiantes puedan unirse.</Text>
                <Pressable onPress={() => router.replace('/(tabs)' as never)} style={styles.submitButton}>
                  <Ionicons color={colors.white} name="home-outline" size={22} />
                  <Text style={styles.submitText}>Volver al inicio</Text>
                </Pressable>
              </View>
            ) : (
              <View style={styles.card}>
                <View style={styles.illustration}>
                  <Ionicons color={colors.blue} name="school-outline" size={38} />
                </View>

                <View style={styles.fieldGroup}>
                  <Text style={styles.label}>Nombre de la clase</Text>
                  <View style={styles.field}>
                    <Ionicons color={colors.blue} name="book-outline" size={21} />
                    <TextInput
                      autoCapitalize="sentences"
                      maxLength={100}
                      onChangeText={setName}
                      placeholder="Ej. Matemáticas II"
                      placeholderTextColor={colors.placeholder}
                      style={styles.input}
                      value={name}
                    />
                  </View>
                </View>

                <View style={styles.fieldGroup}>
                  <Text style={styles.label}>Descripción (opcional)</Text>
                  <View style={[styles.field, styles.multilineField]}>
                    <Ionicons color={colors.blue} name="document-text-outline" size={21} style={styles.multilineIcon} />
                    <TextInput
                      maxLength={255}
                      multiline
                      onChangeText={setDescription}
                      placeholder="Agrega información para tus estudiantes"
                      placeholderTextColor={colors.placeholder}
                      style={[styles.input, styles.multilineInput]}
                      textAlignVertical="top"
                      value={description}
                    />
                  </View>
                  <Text style={styles.counter}>{description.length}/255</Text>
                </View>

                <View style={styles.infoRow}>
                  <Ionicons color={colors.blue} name="key-outline" size={23} />
                  <Text style={styles.infoText}>
                    Al crear la clase recibirás un código irrepetible para compartir.
                  </Text>
                </View>

                {error ? (
                  <View accessibilityRole="alert" style={styles.errorBox}>
                    <Ionicons color={colors.danger} name="alert-circle-outline" size={21} />
                    <Text style={styles.errorText}>{error}</Text>
                  </View>
                ) : null}

                <Pressable
                  disabled={!name.trim() || busy}
                  onPress={() => void submit()}
                  style={({ pressed }) => [
                    styles.submitButton,
                    (!name.trim() || busy) && styles.submitDisabled,
                    pressed && styles.submitPressed,
                  ]}
                >
                  {busy ? (
                    <ActivityIndicator color={colors.white} />
                  ) : (
                    <>
                      <Ionicons color={colors.white} name="add-circle-outline" size={23} />
                      <Text style={styles.submitText}>Crear clase</Text>
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
    borderRadius: 24,
    height: 78,
    justifyContent: 'center',
    width: 78,
  },
  fieldGroup: { gap: 8 },
  label: { color: colors.text, fontSize: 15, fontWeight: '700' },
  field: {
    alignItems: 'center',
    backgroundColor: '#FBFDFF',
    borderColor: colors.line,
    borderRadius: 14,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 10,
    minHeight: 54,
    paddingHorizontal: 14,
  },
  input: { color: colors.text, flex: 1, fontSize: 16, paddingVertical: 13 },
  multilineField: { alignItems: 'flex-start', minHeight: 112 },
  multilineIcon: { marginTop: 16 },
  multilineInput: { minHeight: 96 },
  counter: { color: colors.muted, fontSize: 12, textAlign: 'right' },
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
    gap: 13,
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
  successTitle: { color: colors.navy, fontSize: 24, fontWeight: '800' },
  successText: { color: colors.muted, fontSize: 16, textAlign: 'center' },
  codeLabel: { color: colors.text, fontSize: 13, fontWeight: '700', marginTop: 8 },
  codeBox: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderColor: colors.blueSoft,
    borderRadius: 16,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 11,
    justifyContent: 'center',
    paddingHorizontal: 20,
    paddingVertical: 16,
    width: '100%',
  },
  code: { color: colors.blue, fontSize: 25, fontWeight: '900', letterSpacing: 2 },
  codeHelp: { color: colors.muted, fontSize: 13, lineHeight: 19, marginBottom: 5, textAlign: 'center' },
});
