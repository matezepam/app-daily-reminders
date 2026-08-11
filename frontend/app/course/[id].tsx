import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";
import { router, useLocalSearchParams } from "expo-router";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Pressable, Share, StyleSheet, Text, View } from "react-native";

import { Button, Card, Empty, Header, Screen, SectionTitle } from "@/src/components/Ui";
import { useData } from "@/src/context/DataContext";
import { colors, shadow } from "@/src/theme";
import type { Activity } from "@/src/types/domain";
import { confirmDestructive } from "@/src/utils/confirm";

const URGENT_WINDOW_MS = 24 * 60 * 60 * 1000;

type ActivityState = "completed" | "overdue" | "urgent" | "pending";

function stateFor(activity: Activity, now: number): ActivityState {
  if (activity.completed) return "completed";
  const remaining = new Date(activity.dueAt).getTime() - now;
  if (remaining <= 0) return "overdue";
  if (remaining <= URGENT_WINDOW_MS) return "urgent";
  return "pending";
}

function countdown(dueAt: string, now: number) {
  const remaining = Math.max(0, new Date(dueAt).getTime() - now);
  const totalMinutes = Math.max(1, Math.ceil(remaining / 60_000));
  if (totalMinutes < 60) return `${totalMinutes} min`;
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return minutes ? `${hours} h ${minutes} min` : `${hours} h`;
}

export default function CourseDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const courseId = Number(id);
  const { courses, getActivities, completeActivity, deleteActivity, deleteCourse } = useData();
  const [activities, setActivities] = useState<Activity[]>([]);
  const [error, setError] = useState("");
  const [now, setNow] = useState(Date.now());
  const course = courses.find((candidate) => candidate.id === courseId);

  const load = useCallback(async () => {
    try {
      setActivities(await getActivities(courseId));
      setError("");
      setNow(Date.now());
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo cargar.");
    }
  }, [courseId, getActivities]);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 60_000);
    return () => clearInterval(timer);
  }, []);

  const { current, history } = useMemo(() => {
    const active: Activity[] = [];
    const past: Activity[] = [];
    activities.forEach((activity) => {
      const state = stateFor(activity, now);
      (state === "completed" || state === "overdue" ? past : active).push(activity);
    });
    active.sort((left, right) => left.dueAt.localeCompare(right.dueAt));
    past.sort((left, right) => {
      const leftDate = left.completedAt ?? left.dueAt;
      const rightDate = right.completedAt ?? right.dueAt;
      return rightDate.localeCompare(leftDate);
    });
    return { current: active, history: past };
  }, [activities, now]);

  if (!course) {
    return (
      <Screen>
        <Header title="Curso" back={() => router.back()} />
        <Empty icon="alert-circle-outline" title="Curso no encontrado" detail="Actualiza la lista e inténtalo nuevamente." />
      </Screen>
    );
  }

  const removeCourse = () =>
    confirmDestructive({
      title: "Eliminar curso",
      message: "Se eliminarán sus actividades y recordatorios. Esta acción no se puede deshacer.",
      onConfirm: () => void deleteCourse(courseId).then(() => router.replace("/(tabs)/courses")),
    });

  const removeActivity = (activity: Activity) =>
    confirmDestructive({
      title: "Eliminar actividad",
      message: `Se eliminará “${activity.title}” y su historial de entregas.`,
      onConfirm: () => void deleteActivity(activity.id).then(load),
    });

  const editActivity = (activity: Activity) =>
    router.push({
      pathname: "/activity/new",
      params: {
        courseId: String(courseId),
        id: String(activity.id),
        title: activity.title,
        description: activity.description ?? "",
        dueAt: activity.dueAt,
      },
    });

  return (
    <Screen>
      <Header title={course.name} subtitle={course.description || "Curso académico"} back={() => router.back()} />
      <Card style={s.codeCard}>
        <View style={{ flex: 1 }}>
          <Text style={s.codeLabel}>CÓDIGO DE LA CLASE</Text>
          <Text selectable style={s.code}>{course.joinCode}</Text>
          <Text style={s.members}>{course.memberCount} miembro{course.memberCount === 1 ? "" : "s"}</Text>
        </View>
        <Pressable
          accessibilityLabel="Compartir código"
          style={s.share}
          onPress={() => void Share.share({ message: `Únete a ${course.name} con el código ${course.joinCode}` })}
        >
          <Ionicons name="share-social-outline" size={24} color={colors.blue} />
        </Pressable>
      </Card>

      {course.ownedByMe && (
        <View style={s.actions}>
          <Button
            title="Nueva actividad"
            icon="add-circle-outline"
            onPress={() => router.push({ pathname: "/activity/new", params: { courseId } })}
          />
          <View style={s.two}>
            <View style={{ flex: 1 }}>
              <Button
                compact
                title="Aviso a la clase"
                variant="soft"
                icon="alarm-outline"
                onPress={() => router.push({ pathname: "/reminder/new", params: { courseId } })}
              />
            </View>
            <View style={{ flex: 1 }}>
              <Button compact title="Asistencia" variant="soft" icon="people-outline" onPress={() => router.push(`/attendance/${courseId}`)} />
            </View>
          </View>
        </View>
      )}

      <SectionTitle>Actividades pendientes</SectionTitle>
      {error && <Text style={s.error}>{error}</Text>}
      {current.length ? (
        current.map((activity) => (
          <ActivityRow
            key={activity.id}
            activity={activity}
            now={now}
            ownedByMe={course.ownedByMe}
            onComplete={() => void completeActivity(activity.id).then(load)}
            onEdit={() => editActivity(activity)}
            onDelete={() => removeActivity(activity)}
          />
        ))
      ) : (
        <Empty
          icon="checkmark-done-circle-outline"
          title="Sin actividades pendientes"
          detail={course.ownedByMe ? "Publica una nueva actividad cuando la necesites." : "Estás al día con este curso."}
        />
      )}

      <SectionTitle>Historial</SectionTitle>
      {history.length ? (
        history.map((activity) => (
          <ActivityRow
            key={activity.id}
            activity={activity}
            now={now}
            ownedByMe={course.ownedByMe}
            onComplete={() => undefined}
            onEdit={() => undefined}
            onDelete={() => removeActivity(activity)}
          />
        ))
      ) : (
        <Empty title="Aún no hay historial" detail="Aquí aparecerán las actividades completadas o vencidas." />
      )}

      {course.ownedByMe && <Button title="Eliminar curso" variant="danger" icon="trash-outline" onPress={removeCourse} />}
    </Screen>
  );
}

function ActivityRow({
  activity,
  now,
  ownedByMe,
  onComplete,
  onEdit,
  onDelete,
}: {
  activity: Activity;
  now: number;
  ownedByMe: boolean;
  onComplete: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const state = stateFor(activity, now);
  const meta = {
    completed: { label: "Completada", color: colors.success, background: colors.successPale, icon: "checkmark-circle" as const },
    overdue: { label: "Vencida", color: colors.danger, background: colors.dangerPale, icon: "alert-circle" as const },
    urgent: { label: `Por vencer · ${countdown(activity.dueAt, now)}`, color: colors.warning, background: colors.warningPale, icon: "time" as const },
    pending: { label: "Pendiente", color: colors.blue, background: colors.blueSoft, icon: "document-text-outline" as const },
  }[state];
  const canEdit = ownedByMe && (state === "pending" || state === "urgent");

  return (
    <View style={[s.activity, state === "urgent" && s.urgentActivity, state === "overdue" && s.overdueActivity]}>
      <View style={[s.activityIcon, { backgroundColor: meta.background }]}>
        <Ionicons name={meta.icon} size={24} color={meta.color} />
      </View>
      <View style={s.activityBody}>
        <Text style={s.activityTitle}>{activity.title}</Text>
        <Text style={s.activityNumber}>Actividad #{activity.activityNumber}</Text>
        {activity.description ? <Text style={s.description}>{activity.description}</Text> : null}
        <Text style={s.activityMeta}>{new Date(activity.dueAt).toLocaleString("es-EC")}</Text>
        {activity.completedAt ? (
          <Text style={s.completedAt}>Completada el {new Date(activity.completedAt).toLocaleString("es-EC")}</Text>
        ) : null}
        <View style={[s.statusPill, { backgroundColor: meta.background }]}>
          <Text style={[s.status, { color: meta.color }]}>{meta.label}</Text>
        </View>
      </View>
      {!ownedByMe && state !== "completed" && state !== "overdue" ? (
        <Pressable accessibilityLabel="Marcar actividad como completada" onPress={onComplete} style={s.check}>
          <Ionicons name="checkmark" size={22} color="white" />
        </Pressable>
      ) : null}
      {ownedByMe ? (
        <View style={s.rowActions}>
          {canEdit ? (
            <Pressable accessibilityLabel="Editar actividad" onPress={onEdit} style={s.iconButton}>
              <Ionicons name="create-outline" size={20} color={colors.blue} />
            </Pressable>
          ) : null}
          <Pressable accessibilityLabel="Eliminar actividad" onPress={onDelete} style={[s.iconButton, s.deleteButton]}>
            <Ionicons name="trash-outline" size={20} color={colors.danger} />
          </Pressable>
        </View>
      ) : null}
    </View>
  );
}

const s = StyleSheet.create({
  codeCard: { backgroundColor: colors.navy, flexDirection: "row", alignItems: "center", borderWidth: 0 },
  codeLabel: { fontSize: 10, fontWeight: "900", letterSpacing: 1, color: "#BFD6F8" },
  code: { fontSize: 30, fontWeight: "900", letterSpacing: 2.5, color: "white", marginVertical: 5 },
  members: { fontSize: 12, color: "#D6E5FA" },
  share: { width: 50, height: 50, borderRadius: 16, backgroundColor: "white", alignItems: "center", justifyContent: "center" },
  actions: { gap: 10 },
  two: { flexDirection: "row", gap: 9 },
  error: { color: colors.danger },
  activity: { minHeight: 96, backgroundColor: "white", borderRadius: 19, padding: 14, flexDirection: "row", alignItems: "flex-start", gap: 11, borderWidth: 1, borderColor: "transparent", ...shadow },
  urgentActivity: { borderColor: colors.warning, backgroundColor: "#FFFCF5" },
  overdueActivity: { borderColor: colors.line, opacity: 0.88 },
  activityIcon: { width: 50, height: 50, borderRadius: 16, alignItems: "center", justifyContent: "center" },
  activityBody: { flex: 1, gap: 4 },
  activityTitle: { fontSize: 15, fontWeight: "900", color: colors.navy },
  activityNumber: { fontSize: 11, fontWeight: "800", color: colors.blue },
  description: { fontSize: 12, lineHeight: 17, color: colors.text },
  activityMeta: { fontSize: 11, color: colors.muted },
  completedAt: { fontSize: 11, color: colors.success },
  statusPill: { alignSelf: "flex-start", borderRadius: 99, paddingHorizontal: 9, paddingVertical: 5, marginTop: 2 },
  status: { fontSize: 11, fontWeight: "900" },
  check: { width: 38, height: 38, borderRadius: 13, backgroundColor: colors.success, alignItems: "center", justifyContent: "center" },
  rowActions: { gap: 7 },
  iconButton: { width: 36, height: 36, borderRadius: 12, backgroundColor: colors.blueSoft, alignItems: "center", justifyContent: "center" },
  deleteButton: { backgroundColor: colors.dangerPale },
});
