import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import { Brand } from "@/src/components/Brand";
import { Card, Empty, Screen, SectionTitle } from "@/src/components/Ui";
import { useAuth } from "@/src/context/AuthContext";
import { useData } from "@/src/context/DataContext";
import { colors, priorityMeta, shadow, typeMeta } from "@/src/theme";
export default function Home() {
  const { session } = useAuth();
  const { dashboard, courses, loading, refresh } = useData();
  const professor = session?.profile.role !== "USER";
  const pending = dashboard.reminders
    .filter((x) => x.status === "PENDING")
    .slice(0, 6);
  return (
    <Screen refreshing={loading} onRefresh={refresh}>
      <View style={s.top}>
        <Brand compact />
        <Pressable style={s.bell}>
          <Ionicons
            name="notifications-outline"
            size={24}
            color={colors.navy}
          />
          {dashboard.pendingToday > 0 && <View style={s.dot} />}
        </Pressable>
      </View>
      <View>
        <Text style={s.greeting}>
          Hola, {session?.profile.fullName.split(" ")[0]} 👋
        </Text>
        <Text style={s.subtitle}>
          {professor
            ? "Gestiona tus cursos y fechas importantes."
            : "Tu día académico, siempre organizado."}
        </Text>
      </View>
      <Card>
        <Text style={s.summaryTitle}>Resumen</Text>
        <View style={s.metrics}>
          <Metric
            icon="today-outline"
            value={dashboard.pendingToday}
            label="Para hoy"
          />
          <Metric
            icon="time-outline"
            value={dashboard.upcoming}
            label="Próximos"
          />
          <Metric
            icon="checkmark-circle-outline"
            value={dashboard.completedThisMonth}
            label="Completados"
            color={colors.success}
          />
        </View>
      </Card>
      <View style={s.quickRow}>
        <Quick
          icon={professor ? "add-circle-outline" : "alarm-outline"}
          label={professor ? "Crear curso" : "Recordatorio"}
          onPress={() =>
            router.push(professor ? "/course/new" : "/reminder/new")
          }
        />
        <Quick
          icon={professor ? "megaphone-outline" : "enter-outline"}
          label={professor ? "Publicar" : "Unirme"}
          onPress={() =>
            router.push(professor ? "/course/new" : "/course/join")
          }
        />
      </View>
      {professor && (
        <>
          <SectionTitle>Mis cursos</SectionTitle>
          {courses.length ? (
            <ScrollView
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={{ gap: 12 }}
            >
              {courses.slice(0, 4).map((c) => (
                <Pressable
                  key={c.id}
                  onPress={() => router.push(`/course/${c.id}`)}
                  style={s.course}
                >
                  <View style={s.courseIcon}>
                    <Ionicons name="school" size={25} color={colors.blue} />
                  </View>
                  <Text style={s.courseName} numberOfLines={1}>
                    {c.name}
                  </Text>
                  <Text style={s.courseMeta}>
                    {c.memberCount} miembros · {c.joinCode}
                  </Text>
                </Pressable>
              ))}
            </ScrollView>
          ) : (
            <Empty
              icon="school-outline"
              title="Crea tu primer curso"
              detail="Comparte el código con tus estudiantes."
            />
          )}
        </>
      )}
      <SectionTitle>
        {professor ? "Próximas fechas" : "Tus próximos recordatorios"}
      </SectionTitle>
      {pending.length ? (
        pending.map((r) => (
          <ReminderRow
            key={r.id}
            reminder={r}
            onPress={() => router.push(`/reminder/${r.id}`)}
          />
        ))
      ) : (
        <Empty
          title="Todo al día"
          detail="No tienes entregas pendientes por ahora."
        />
      )}
      <Pressable
        style={s.fab}
        onPress={() => router.push(professor ? "/course/new" : "/reminder/new")}
      >
        <Ionicons name="add" size={34} color="white" />
      </Pressable>
    </Screen>
  );
}
function Metric({
  icon,
  value,
  label,
  color = colors.blue,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  value: number;
  label: string;
  color?: string;
}) {
  return (
    <View style={s.metric}>
      <Ionicons name={icon} size={24} color={color} />
      <Text style={s.metricValue}>{value}</Text>
      <Text style={s.metricLabel}>{label}</Text>
    </View>
  );
}
function Quick({
  icon,
  label,
  onPress,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [s.quick, pressed && { opacity: 0.7 }]}
    >
      <View style={s.quickIcon}>
        <Ionicons name={icon} size={24} color={colors.blue} />
      </View>
      <Text style={s.quickText}>{label}</Text>
      <Ionicons name="chevron-forward" size={18} color={colors.muted} />
    </Pressable>
  );
}
function ReminderRow({
  reminder: r,
  onPress,
}: {
  reminder: any;
  onPress: () => void;
}) {
  const p = priorityMeta[r.priority as keyof typeof priorityMeta],
    t = typeMeta[r.type as keyof typeof typeMeta];
  return (
    <Pressable onPress={onPress} style={s.reminder}>
      <View style={s.reminderIcon}>
        <Ionicons name={t.icon} size={24} color="white" />
      </View>
      <View style={{ flex: 1, gap: 4 }}>
        <Text style={s.reminderTitle} numberOfLines={1}>
          {r.title}
        </Text>
        <Text style={s.reminderMeta}>
          {r.courseName || "Personal"} ·{" "}
          {new Date(r.dueAt).toLocaleDateString("es-EC", {
            day: "numeric",
            month: "short",
          })}
        </Text>
      </View>
      <View style={[s.priority, { backgroundColor: p.background }]}>
        <View style={[s.priorityDot, { backgroundColor: p.color }]} />
        <Text style={{ color: p.color, fontSize: 11, fontWeight: "800" }}>
          {p.label}
        </Text>
      </View>
    </Pressable>
  );
}
const s = StyleSheet.create({
  top: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  bell: {
    width: 46,
    height: 46,
    borderRadius: 16,
    backgroundColor: "white",
    alignItems: "center",
    justifyContent: "center",
    ...shadow,
  },
  dot: {
    position: "absolute",
    right: 10,
    top: 9,
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: colors.danger,
  },
  greeting: {
    fontSize: 30,
    fontWeight: "900",
    letterSpacing: -0.7,
    color: colors.navy,
  },
  subtitle: { fontSize: 15, color: colors.muted, marginTop: 5 },
  summaryTitle: { fontSize: 16, fontWeight: "900", color: colors.navy },
  metrics: { flexDirection: "row" },
  metric: {
    flex: 1,
    alignItems: "center",
    gap: 3,
    borderRightWidth: 1,
    borderRightColor: colors.line,
  },
  metricValue: { fontSize: 25, fontWeight: "900", color: colors.navy },
  metricLabel: { fontSize: 11, color: colors.muted },
  quickRow: { flexDirection: "row", gap: 11 },
  quick: {
    flex: 1,
    minHeight: 76,
    backgroundColor: "white",
    borderRadius: 18,
    padding: 12,
    flexDirection: "row",
    alignItems: "center",
    gap: 9,
    ...shadow,
  },
  quickIcon: {
    width: 42,
    height: 42,
    borderRadius: 14,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  quickText: { flex: 1, color: colors.navy, fontWeight: "800", fontSize: 13 },
  course: {
    width: 210,
    backgroundColor: "white",
    borderRadius: 20,
    padding: 16,
    gap: 8,
    ...shadow,
  },
  courseIcon: {
    width: 48,
    height: 48,
    borderRadius: 15,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  courseName: { fontWeight: "900", fontSize: 16, color: colors.navy },
  courseMeta: { fontSize: 11, color: colors.muted },
  reminder: {
    minHeight: 82,
    backgroundColor: "white",
    borderRadius: 19,
    padding: 13,
    flexDirection: "row",
    alignItems: "center",
    gap: 11,
    ...shadow,
  },
  reminderIcon: {
    width: 52,
    height: 52,
    borderRadius: 16,
    backgroundColor: colors.blue,
    alignItems: "center",
    justifyContent: "center",
  },
  reminderTitle: { fontSize: 15, fontWeight: "900", color: colors.navy },
  reminderMeta: { fontSize: 11, color: colors.muted },
  priority: {
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: 10,
    flexDirection: "row",
    alignItems: "center",
    gap: 5,
  },
  priorityDot: { width: 6, height: 6, borderRadius: 3 },
  fab: {
    position: "absolute",
    right: 22,
    bottom: 26,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: colors.blue,
    alignItems: "center",
    justifyContent: "center",
    elevation: 8,
    shadowColor: colors.blue,
    shadowOpacity: 0.35,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 7 },
  },
});
