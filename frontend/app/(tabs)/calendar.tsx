import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { Empty, Header, Screen } from "@/src/components/Ui";
import { useData } from "@/src/context/DataContext";
import { colors, shadow, typeMeta } from "@/src/theme";
export default function Calendar() {
  const { dashboard, loading, refresh } = useData();
  const items = [...dashboard.reminders]
    .filter((x) => x.status === "PENDING")
    .sort((a, b) => a.dueAt.localeCompare(b.dueAt));
  const groups = items.reduce<Record<string, typeof items>>((a, r) => {
    const k = new Date(r.dueAt).toLocaleDateString("es-EC", {
      weekday: "long",
      day: "numeric",
      month: "long",
    });
    (a[k] ??= []).push(r);
    return a;
  }, {});
  return (
    <Screen refreshing={loading} onRefresh={refresh}>
      <Header title="Agenda" subtitle="Tus fechas académicas en orden" />
      <View style={s.month}>
        <View>
          <Text style={s.monthName}>
            {new Date().toLocaleDateString("es-EC", {
              month: "long",
              year: "numeric",
            })}
          </Text>
          <Text style={s.monthText}>Organiza tus entregas y evaluaciones</Text>
        </View>
        <View style={s.count}>
          <Text style={s.countNumber}>{items.length}</Text>
          <Text style={s.countLabel}>pendientes</Text>
        </View>
      </View>
      {Object.keys(groups).length ? (
        Object.entries(groups).map(([date, list]) => (
          <View key={date} style={{ gap: 10 }}>
            <Text style={s.date}>{date}</Text>
            {list.map((r) => {
              const meta = typeMeta[r.type];
              return (
                <Pressable
                  key={r.id}
                  onPress={() => router.push(`/reminder/${r.id}`)}
                  style={s.item}
                >
                  <View style={s.time}>
                    <Text style={s.timeText}>
                      {new Date(r.dueAt).toLocaleTimeString("es-EC", {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </Text>
                  </View>
                  <View style={s.icon}>
                    <Ionicons name={meta.icon} size={22} color={colors.blue} />
                  </View>
                  <View style={{ flex: 1 }}>
                    <Text style={s.title}>{r.title}</Text>
                    <Text style={s.meta}>
                      {r.courseName || "Personal"} · {meta.label}
                    </Text>
                  </View>
                  <Ionicons
                    name="chevron-forward"
                    color={colors.muted}
                    size={18}
                  />
                </Pressable>
              );
            })}
          </View>
        ))
      ) : (
        <Empty
          title="Tu agenda está libre"
          detail="Los recordatorios y actividades aparecerán aquí."
        />
      )}
    </Screen>
  );
}
const s = StyleSheet.create({
  month: {
    borderRadius: 24,
    padding: 19,
    backgroundColor: colors.navy,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  monthName: {
    color: "white",
    fontSize: 21,
    fontWeight: "900",
    textTransform: "capitalize",
  },
  monthText: { color: "#C9DCF8", fontSize: 12, marginTop: 4 },
  count: {
    alignItems: "center",
    backgroundColor: "#FFFFFF18",
    borderRadius: 15,
    paddingHorizontal: 13,
    paddingVertical: 8,
  },
  countNumber: { color: "white", fontSize: 21, fontWeight: "900" },
  countLabel: { color: "#DCEAFF", fontSize: 10 },
  date: {
    textTransform: "capitalize",
    fontSize: 15,
    fontWeight: "900",
    color: colors.navy,
    marginTop: 4,
  },
  item: {
    minHeight: 76,
    backgroundColor: "white",
    borderRadius: 18,
    padding: 12,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    ...shadow,
  },
  time: { width: 50, alignItems: "center" },
  timeText: { fontSize: 11, fontWeight: "800", color: colors.muted },
  icon: {
    width: 43,
    height: 43,
    borderRadius: 14,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  title: { fontSize: 15, fontWeight: "900", color: colors.navy },
  meta: { fontSize: 11, color: colors.muted, marginTop: 3 },
});
