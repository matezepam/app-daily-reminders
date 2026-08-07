/* eslint-disable react-hooks/exhaustive-deps */
import { Ionicons } from "@expo/vector-icons";
import { router, useLocalSearchParams } from "expo-router";
import { useEffect, useState } from "react";
import { Alert, Pressable, Share, StyleSheet, Text, View } from "react-native";
import {
  Button,
  Card,
  Empty,
  Header,
  Screen,
  SectionTitle,
} from "@/src/components/Ui";
import { useData } from "@/src/context/DataContext";
import type { Activity } from "@/src/types/domain";
import { colors, shadow } from "@/src/theme";
export default function CourseDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const courseId = Number(id);
  const {
    courses,
    getActivities,
    completeActivity,
    deleteActivity,
    deleteCourse,
  } = useData();
  const [activities, setActivities] = useState<Activity[]>([]);
  const [error, setError] = useState("");
  const course = courses.find((c) => c.id === courseId);
  async function load() {
    try {
      setActivities(await getActivities(courseId));
      setError("");
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo cargar.");
    }
  }
  useEffect(() => {
    void load();
  }, [courseId]);
  if (!course)
    return (
      <Screen>
        <Header title="Curso" back={() => router.back()} />
        <Empty
          icon="alert-circle-outline"
          title="Curso no encontrado"
          detail="Actualiza la lista e inténtalo nuevamente."
        />
      </Screen>
    );
  const removeCourse = () =>
    Alert.alert(
      "Eliminar curso",
      "Se eliminarán sus actividades y recordatorios. Esta acción no se puede deshacer.",
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Eliminar",
          style: "destructive",
          onPress: () =>
            void deleteCourse(courseId).then(() =>
              router.replace("/(tabs)/courses"),
            ),
        },
      ],
    );
  return (
    <Screen>
      <Header
        title={course.name}
        subtitle={course.description || "Curso académico"}
        back={() => router.back()}
      />
      <Card style={s.codeCard}>
        <View style={{ flex: 1 }}>
          <Text style={s.codeLabel}>CÓDIGO DE LA CLASE</Text>
          <Text selectable style={s.code}>
            {course.joinCode}
          </Text>
          <Text style={s.members}>
            {course.memberCount} miembro{course.memberCount === 1 ? "" : "s"}
          </Text>
        </View>
        <Pressable
          accessibilityLabel="Compartir código"
          style={s.share}
          onPress={() =>
            void Share.share({
              message: `Únete a ${course.name} con el código ${course.joinCode}`,
            })
          }
        >
          <Ionicons name="share-social-outline" size={24} color={colors.blue} />
        </Pressable>
      </Card>
      {course.ownedByMe && (
        <View style={s.actions}>
          <Button
            title="Nueva actividad"
            icon="add-circle-outline"
            onPress={() =>
              router.push({ pathname: "/activity/new", params: { courseId } })
            }
          />
          <View style={s.two}>
            <View style={{ flex: 1 }}>
              <Button
                compact
                title="Recordatorio"
                variant="soft"
                icon="alarm-outline"
                onPress={() =>
                  router.push({
                    pathname: "/reminder/new",
                    params: { courseId },
                  })
                }
              />
            </View>
            <View style={{ flex: 1 }}>
              <Button
                compact
                title="Asistencia"
                variant="soft"
                icon="people-outline"
                onPress={() => router.push(`/attendance/${courseId}`)}
              />
            </View>
          </View>
        </View>
      )}
      <SectionTitle>Actividades</SectionTitle>
      {error && <Text style={s.error}>{error}</Text>}
      {activities.length ? (
        activities.map((a) => (
          <View key={a.id} style={s.activity}>
            <View
              style={[
                s.activityIcon,
                a.completed && { backgroundColor: colors.successPale },
              ]}
            >
              <Ionicons
                name={
                  a.completed ? "checkmark-circle" : "document-text-outline"
                }
                size={24}
                color={a.completed ? colors.success : colors.blue}
              />
            </View>
            <View style={{ flex: 1, gap: 4 }}>
              <Text style={s.activityTitle}>{a.title}</Text>
              <Text style={s.activityMeta}>
                {new Date(a.dueAt).toLocaleString("es-EC")}
              </Text>
              <Text
                style={[
                  s.status,
                  { color: a.completed ? colors.success : colors.warning },
                ]}
              >
                {a.completed ? "Completada" : "Pendiente"}
              </Text>
            </View>
            {!a.completed && !course.ownedByMe && (
              <Pressable
                onPress={() => void completeActivity(a.id).then(load)}
                style={s.check}
              >
                <Ionicons name="checkmark" size={22} color="white" />
              </Pressable>
            )}
            {course.ownedByMe && (
              <Pressable onPress={() => void deleteActivity(a.id).then(load)}>
                <Ionicons
                  name="trash-outline"
                  size={21}
                  color={colors.danger}
                />
              </Pressable>
            )}
          </View>
        ))
      ) : (
        <Empty
          title="Sin actividades"
          detail={
            course.ownedByMe
              ? "Publica la primera actividad del curso."
              : "Tu profesor aún no ha publicado actividades."
          }
        />
      )}{" "}
      {course.ownedByMe && (
        <Button
          title="Eliminar curso"
          variant="danger"
          icon="trash-outline"
          onPress={removeCourse}
        />
      )}
    </Screen>
  );
}
const s = StyleSheet.create({
  codeCard: {
    backgroundColor: colors.navy,
    flexDirection: "row",
    alignItems: "center",
    borderWidth: 0,
  },
  codeLabel: {
    fontSize: 10,
    fontWeight: "900",
    letterSpacing: 1,
    color: "#BFD6F8",
  },
  code: {
    fontSize: 30,
    fontWeight: "900",
    letterSpacing: 2.5,
    color: "white",
    marginVertical: 5,
  },
  members: { fontSize: 12, color: "#D6E5FA" },
  share: {
    width: 50,
    height: 50,
    borderRadius: 16,
    backgroundColor: "white",
    alignItems: "center",
    justifyContent: "center",
  },
  actions: { gap: 10 },
  two: { flexDirection: "row", gap: 9 },
  error: { color: colors.danger },
  activity: {
    minHeight: 88,
    backgroundColor: "white",
    borderRadius: 19,
    padding: 14,
    flexDirection: "row",
    alignItems: "center",
    gap: 11,
    ...shadow,
  },
  activityIcon: {
    width: 50,
    height: 50,
    borderRadius: 16,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  activityTitle: { fontSize: 15, fontWeight: "900", color: colors.navy },
  activityMeta: { fontSize: 11, color: colors.muted },
  status: { fontSize: 11, fontWeight: "800" },
  check: {
    width: 38,
    height: 38,
    borderRadius: 13,
    backgroundColor: colors.success,
    alignItems: "center",
    justifyContent: "center",
  },
});
