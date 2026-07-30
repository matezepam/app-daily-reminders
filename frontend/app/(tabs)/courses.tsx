import { RoleBadge } from '@/src/components/RoleBadge';
import { useAuth } from '@/src/context/AuthContext';
import { colors, shadow } from '@/src/theme';
import { Ionicons } from '@expo/vector-icons';
import { router } from 'expo-router';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

type CoursePreview = {
  description: string;
  id: number;
  joinCode: string;
  memberCount: number;
  name: string;
  ownedByMe: boolean;
};

const teacherCourses: CoursePreview[] = [
  {
    description: 'Periodo actual',
    id: 1,
    joinCode: 'MAT-8K2PX',
    memberCount: 24,
    name: 'Matematicas II',
    ownedByMe: true,
  },
];

const studentCourses: CoursePreview[] = [
  {
    description: 'Actividades y examenes del semestre',
    id: 2,
    joinCode: 'FIS-82KLM',
    memberCount: 31,
    name: 'Fisica General',
    ownedByMe: false,
  },
];

export default function CoursesScreen() {
  const { session } = useAuth();
  const isTeacher = session?.roles?.some((role) => role === 'TEACHER' || role === 'ADMIN');
  const courses = isTeacher ? teacherCourses : studentCourses;

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView contentContainerStyle={styles.scroll} showsVerticalScrollIndicator={false}>
        <View style={styles.page}>
          <View style={styles.header}>
            <View style={styles.headerCopy}>
              <RoleBadge roles={session?.roles} />
              <Text style={styles.title}>Mis cursos</Text>
              <Text style={styles.subtitle}>
                {isTeacher ? 'Clases creadas por ti para compartir actividades.' : 'Clases en las que estas inscrito.'}
              </Text>
            </View>
            <Pressable
              accessibilityLabel={isTeacher ? 'Crear clase' : 'Unirme a clase'}
              onPress={() => router.push((isTeacher ? '/course/new' : '/course/join') as never)}
              style={({ pressed }) => [styles.actionButton, pressed && styles.pressed]}
            >
              <Ionicons color={colors.white} name={isTeacher ? 'add' : 'enter-outline'} size={24} />
            </Pressable>
          </View>

          {courses.length ? (
            <View style={styles.list}>
              {courses.map((course) => (
                <Pressable key={course.id} style={({ pressed }) => [styles.courseCard, pressed && styles.pressed]}>
                  <View style={styles.courseIcon}>
                    <Ionicons color={colors.blue} name={course.ownedByMe ? 'school-outline' : 'book-outline'} size={27} />
                  </View>
                  <View style={styles.courseBody}>
                    <View style={styles.courseTop}>
                      <Text numberOfLines={1} style={styles.courseName}>{course.name}</Text>
                      <View style={styles.codePill}>
                        <Text selectable style={styles.codeText}>{course.joinCode}</Text>
                      </View>
                    </View>
                    <Text numberOfLines={2} style={styles.courseDescription}>{course.description}</Text>
                    <View style={styles.metaRow}>
                      <Ionicons color={colors.muted} name="people-outline" size={16} />
                      <Text style={styles.metaText}>{course.memberCount} estudiantes</Text>
                      <View style={styles.metaDot} />
                      <Text style={styles.metaText}>{course.ownedByMe ? 'Profesor' : 'Estudiante'}</Text>
                    </View>
                  </View>
                  <Ionicons color={colors.muted} name="chevron-forward" size={20} />
                </Pressable>
              ))}
            </View>
          ) : (
            <View style={styles.emptyCard}>
              <View style={styles.emptyIcon}>
                <Ionicons color={colors.blue} name="albums-outline" size={34} />
              </View>
              <Text style={styles.emptyTitle}>Aun no hay cursos</Text>
              <Text style={styles.emptyText}>
                {isTeacher ? 'Crea tu primera clase para generar un codigo.' : 'Ingresa el codigo de tu profesor para unirte.'}
              </Text>
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { backgroundColor: colors.background, flex: 1 },
  scroll: { paddingBottom: 36 },
  page: { alignSelf: 'center', gap: 22, maxWidth: 720, padding: 22, width: '100%' },
  header: { alignItems: 'flex-start', flexDirection: 'row', gap: 14, justifyContent: 'space-between' },
  headerCopy: { flex: 1, gap: 9 },
  title: { color: colors.navy, fontSize: 29, fontWeight: '800' },
  subtitle: { color: colors.muted, fontSize: 15, lineHeight: 21 },
  actionButton: {
    alignItems: 'center',
    backgroundColor: colors.blue,
    borderRadius: 17,
    height: 52,
    justifyContent: 'center',
    width: 52,
    ...shadow,
  },
  pressed: { opacity: 0.76, transform: [{ scale: 0.995 }] },
  list: { gap: 14 },
  courseCard: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: colors.lineSoft,
    borderRadius: 22,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 13,
    padding: 16,
    ...shadow,
  },
  courseIcon: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderRadius: 17,
    height: 56,
    justifyContent: 'center',
    width: 56,
  },
  courseBody: { flex: 1, gap: 7 },
  courseTop: { alignItems: 'center', flexDirection: 'row', gap: 10 },
  courseName: { color: colors.navy, flex: 1, fontSize: 17, fontWeight: '800' },
  codePill: { backgroundColor: colors.bluePale, borderRadius: 10, paddingHorizontal: 9, paddingVertical: 5 },
  codeText: { color: colors.blue, fontSize: 12, fontWeight: '900', letterSpacing: 1 },
  courseDescription: { color: colors.muted, fontSize: 13, lineHeight: 18 },
  metaRow: { alignItems: 'center', flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  metaText: { color: colors.muted, fontSize: 12, fontWeight: '600' },
  metaDot: { backgroundColor: colors.line, borderRadius: 3, height: 5, width: 5 },
  emptyCard: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: colors.lineSoft,
    borderRadius: 24,
    borderWidth: 1,
    gap: 10,
    padding: 28,
    ...shadow,
  },
  emptyIcon: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderRadius: 25,
    height: 76,
    justifyContent: 'center',
    width: 76,
  },
  emptyTitle: { color: colors.navy, fontSize: 19, fontWeight: '800', textAlign: 'center' },
  emptyText: { color: colors.muted, fontSize: 14, lineHeight: 20, textAlign: 'center' },
});
