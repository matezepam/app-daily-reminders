import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";
import { router } from "expo-router";
import { useCallback, useMemo, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { Button, Card, Empty, Header, Message, Screen, SectionTitle } from "@/src/components/Ui";
import { useAuth } from "@/src/context/AuthContext";
import { useData } from "@/src/context/DataContext";
import { colors } from "@/src/theme";
import type { Activity, Course } from "@/src/types/domain";

type CourseActivity = { activity: Activity; course: Course };

export default function HistoryPage() {
  const { session } = useAuth();
  const {
    courses,
    dashboard,
    getActivities,
    uncompleteActivity,
    uncompleteReminder,
  } = useData();
  const [activities, setActivities] = useState<CourseActivity[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [pendingUndo, setPendingUndo] = useState<string | null>(null);
  const professor = session?.profile.role === "ADMIN";

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const results = await Promise.all(
        courses.map(async (course) => ({
          course,
          activities: await getActivities(course.id),
        })),
      );
      setActivities(
        results.flatMap(({ course, activities: items }) =>
          items.map((activity) => ({ course, activity })),
        ),
      );
      setError("");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo cargar el historial.");
    } finally {
      setLoading(false);
    }
  }, [courses, getActivities]);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  const pastReminders = useMemo(
    () =>
      dashboard.reminders
        .filter((reminder) => reminder.status !== "PENDING")
        .sort((left, right) => right.dueAt.localeCompare(left.dueAt)),
    [dashboard.reminders],
  );
  const pastActivities = useMemo(
    () =>
      activities
        .filter(({ activity }) =>
          professor
            ? activity.completionCount > 0 || new Date(activity.dueAt).getTime() <= Date.now()
            : activity.completed || new Date(activity.dueAt).getTime() <= Date.now(),
        )
        .sort((left, right) => {
          const leftDate = left.activity.completedAt ?? left.activity.completions?.[0]?.completedAt ?? left.activity.dueAt;
          const rightDate = right.activity.completedAt ?? right.activity.completions?.[0]?.completedAt ?? right.activity.dueAt;
          return rightDate.localeCompare(leftDate);
        }),
    [activities, professor],
  );

  async function undo(activityId: number) {
    const key = `activity-${activityId}`;
    if (pendingUndo) return;
    setPendingUndo(key);
    setError("");
    try {
      await uncompleteActivity(activityId);
      await load();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo deshacer la finalización.");
    } finally {
      setPendingUndo(null);
    }
  }

  async function undoReminder(reminderId: number) {
    const key = `reminder-${reminderId}`;
    if (pendingUndo) return;
    setPendingUndo(key);
    setError("");
    try {
      await uncompleteReminder(reminderId);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo deshacer la finalización.");
    } finally {
      setPendingUndo(null);
    }
  }

  return (
    <Screen refreshing={loading} onRefresh={load}>
      <Header
        title="Historial"
        subtitle={professor ? "Entregas y fechas de tus cursos" : "Tu trabajo completado y vencido"}
      />
      {error ? <Message>{error}</Message> : null}

      <SectionTitle>Actividades</SectionTitle>
      {pastActivities.length ? (
        pastActivities.map(({ activity, course }) => (
          <Card key={`${course.id}-${activity.id}`}>
            <Pressable
              accessibilityRole="button"
              onPress={() => router.push(`/course/${course.id}`)}
              style={styles.row}
            >
              <View style={[styles.icon, { backgroundColor: activity.completed ? colors.successPale : colors.warningPale }]}>
                <Ionicons
                  name={activity.completed ? "checkmark-done" : professor && activity.completionCount > 0 ? "people" : "time"}
                  size={23}
                  color={activity.completed ? colors.success : professor && activity.completionCount > 0 ? colors.blue : colors.warning}
                />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.title}>{activity.title}</Text>
                <Text style={styles.meta}>{course.name} · Actividad #{activity.activityNumber}</Text>
                <Text style={styles.date}>
                  {professor && activity.completionCount > 0
                    ? `${activity.completionCount} estudiante${activity.completionCount === 1 ? "" : "s"} completó`
                    : activity.completedAt
                      ? `Completada ${new Date(activity.completedAt).toLocaleString("es-EC")}`
                      : `Venció ${new Date(activity.dueAt).toLocaleString("es-EC")}`}
                </Text>
              </View>
              <Ionicons name="chevron-forward" color={colors.muted} size={18} />
            </Pressable>
            {!professor && activity.completed ? (
              <Button
                compact
                variant="ghost"
                icon="arrow-undo"
                title="Deshacer finalización"
                loading={pendingUndo === `activity-${activity.id}`}
                disabled={pendingUndo !== null}
                onPress={() => void undo(activity.id)}
              />
            ) : null}
          </Card>
        ))
      ) : (
        <Empty title="Sin actividades en el historial" detail="Las completadas y vencidas aparecerán aquí." />
      )}

      <SectionTitle>Recordatorios</SectionTitle>
      {pastReminders.length ? (
        pastReminders.map((reminder) => (
          <Card key={reminder.id}>
            <Pressable onPress={() => router.push(`/reminder/${reminder.id}`)} style={styles.row}>
                <View style={[styles.icon, { backgroundColor: reminder.status === "COMPLETED" ? colors.successPale : colors.dangerPale }]}>
                  <Ionicons
                    name={reminder.status === "COMPLETED" ? "checkmark-circle" : "alert-circle"}
                    size={23}
                    color={reminder.status === "COMPLETED" ? colors.success : colors.danger}
                  />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.title}>{reminder.title}</Text>
                  <Text style={styles.meta}>{reminder.courseName ?? "Personal"}</Text>
                  <Text style={styles.date}>
                    {reminder.status === "COMPLETED" ? "Completado" : "Expirado"} · {new Date(reminder.completedAt ?? reminder.dueAt).toLocaleString("es-EC")}
                  </Text>
                </View>
                <Ionicons name="chevron-forward" color={colors.muted} size={18} />
            </Pressable>
            {reminder.status === "COMPLETED" && (!professor || reminder.personal) ? (
              <Button
                compact
                variant="ghost"
                icon="arrow-undo"
                title="Deshacer finalización"
                loading={pendingUndo === `reminder-${reminder.id}`}
                disabled={pendingUndo !== null}
                onPress={() => void undoReminder(reminder.id)}
              />
            ) : null}
          </Card>
        ))
      ) : (
        <Empty title="Sin recordatorios en el historial" detail="Los completados y expirados aparecerán aquí." />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: "row", alignItems: "center", gap: 11 },
  icon: {
    width: 48,
    height: 48,
    borderRadius: 15,
    alignItems: "center",
    justifyContent: "center",
  },
  title: { color: colors.navy, fontSize: 15, fontWeight: "900" },
  meta: { color: colors.blue, fontSize: 11, fontWeight: "800", marginTop: 3 },
  date: { color: colors.muted, fontSize: 11, lineHeight: 16, marginTop: 3 },
});
