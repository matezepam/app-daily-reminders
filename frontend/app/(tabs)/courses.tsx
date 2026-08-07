import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Button, Empty, Header, Screen } from "@/src/components/Ui";
import { useAuth } from "@/src/context/AuthContext";
import { useData } from "@/src/context/DataContext";
import { colors, shadow } from "@/src/theme";
export default function Courses() {
  const { session } = useAuth();
  const { courses, loading, refresh } = useData();
  const professor = session?.profile.role !== "USER";
  return (
    <Screen refreshing={loading} onRefresh={refresh}>
      <Header
        title={professor ? "Mis clases" : "Mis cursos"}
        subtitle={
          professor
            ? "Crea espacios y publica actividades."
            : "Consulta las actividades de tus profesores."
        }
      />
      <Button
        title={professor ? "Crear una clase" : "Unirme con un código"}
        icon={professor ? "add-circle-outline" : "enter-outline"}
        onPress={() => router.push(professor ? "/course/new" : "/course/join")}
      />
      <Text style={s.section}>
        {professor ? "Clases creadas" : "Cursos inscritos"}
      </Text>
      {courses.length ? (
        courses.map((c) => (
          <Pressable
            key={c.id}
            onPress={() => router.push(`/course/${c.id}`)}
            style={({ pressed }) => [s.course, pressed && { opacity: 0.7 }]}
          >
            <View style={s.icon}>
              <Ionicons name="school" size={27} color="white" />
            </View>
            <View style={{ flex: 1, gap: 5 }}>
              <Text style={s.name} numberOfLines={1}>
                {c.name}
              </Text>
              <Text style={s.description} numberOfLines={2}>
                {c.description || "Curso académico"}
              </Text>
              <View style={s.meta}>
                <Ionicons
                  name="people-outline"
                  size={15}
                  color={colors.muted}
                />
                <Text style={s.metaText}>{c.memberCount} miembros</Text>
                <View style={s.code}>
                  <Text style={s.codeText}>{c.joinCode}</Text>
                </View>
              </View>
            </View>
            <Ionicons name="chevron-forward" size={21} color={colors.muted} />
          </Pressable>
        ))
      ) : (
        <Empty
          icon="school-outline"
          title={professor ? "Crea tu primera clase" : "Aún no tienes cursos"}
          detail={
            professor
              ? "Recibirás un código para compartir."
              : "Ingresa el código que te entregue tu profesor."
          }
        />
      )}
    </Screen>
  );
}
const s = StyleSheet.create({
  section: { fontSize: 19, fontWeight: "900", color: colors.navy },
  course: {
    backgroundColor: "white",
    borderRadius: 21,
    padding: 15,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    ...shadow,
  },
  icon: {
    width: 56,
    height: 56,
    borderRadius: 18,
    backgroundColor: colors.blue,
    alignItems: "center",
    justifyContent: "center",
  },
  name: { fontSize: 17, fontWeight: "900", color: colors.navy },
  description: { fontSize: 13, lineHeight: 18, color: colors.muted },
  meta: { flexDirection: "row", alignItems: "center", gap: 5 },
  metaText: { fontSize: 12, color: colors.muted },
  code: {
    marginLeft: 5,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    backgroundColor: colors.blueSoft,
  },
  codeText: {
    fontSize: 11,
    fontWeight: "900",
    letterSpacing: 0.7,
    color: colors.blue,
  },
});
