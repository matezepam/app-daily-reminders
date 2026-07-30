import { Brand } from '@/src/components/Brand';
import { RoleBadge } from '@/src/components/RoleBadge';
import { useAuth } from '@/src/context/AuthContext';
import { colors, shadow } from '@/src/theme';
import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function HomeScreen() {
  const { session } = useAuth();
  const isTeacher = session?.roles?.some((role) => role === 'TEACHER' || role === 'ADMIN');

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.page}>
        <Brand compact />
        <View style={styles.hero}>
          <RoleBadge roles={session?.roles} />
          <Text style={styles.title}>Tu espacio académico</Text>
          <Text style={styles.subtitle}>
            {isTeacher
              ? 'Administra tus clases y publica actividades para tus estudiantes.'
              : 'Organiza tus recordatorios y participa en tus clases.'}
          </Text>
        </View>
        <Pressable
          onPress={() => router.push((isTeacher ? '/course/new' : '/course/join') as never)}
          style={({ pressed }) => [styles.card, pressed && styles.cardPressed]}
        >
          <View style={styles.icon}>
            <Ionicons color={colors.blue} name={isTeacher ? 'add-circle-outline' : 'enter-outline'} size={30} />
          </View>
          <View style={styles.copy}>
            <Text style={styles.cardTitle}>{isTeacher ? 'Crear una clase' : 'Unirse a una clase'}</Text>
            <Text style={styles.cardText}>
              {isTeacher
                ? 'La opción para generar un código único estará disponible en Cursos.'
                : 'La opción para ingresar el código del profesor estará disponible en Cursos.'}
            </Text>
          </View>
          <Ionicons color={colors.muted} name="chevron-forward" size={22} />
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { backgroundColor: colors.background, flex: 1 },
  page: { alignSelf: 'center', gap: 24, maxWidth: 620, padding: 22, width: '100%' },
  hero: { gap: 10, paddingTop: 12 },
  title: { color: colors.navy, fontSize: 30, fontWeight: '800' },
  subtitle: { color: colors.muted, fontSize: 16, lineHeight: 23 },
  card: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: colors.lineSoft,
    borderRadius: 22,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 14,
    padding: 18,
    ...shadow,
  },
  cardPressed: { opacity: 0.76, transform: [{ scale: 0.995 }] },
  icon: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderRadius: 18,
    height: 58,
    justifyContent: 'center',
    width: 58,
  },
  copy: { flex: 1, gap: 4 },
  cardTitle: { color: colors.navy, fontSize: 17, fontWeight: '800' },
  cardText: { color: colors.muted, fontSize: 14, lineHeight: 20 },
});
