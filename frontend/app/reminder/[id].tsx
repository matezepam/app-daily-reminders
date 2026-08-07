import { Ionicons } from "@expo/vector-icons";
import { router, useLocalSearchParams } from "expo-router";
import { Alert, StyleSheet, Text, View } from "react-native";
import { Button, Card, Empty, Header, Screen } from "@/src/components/Ui";
import { useAuth } from "@/src/context/AuthContext";
import { useData } from "@/src/context/DataContext";
import { colors, priorityMeta, typeMeta } from "@/src/theme";
export default function ReminderDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { session } = useAuth();
  const { dashboard, completeReminder, deleteReminder } = useData();
  const r = dashboard.reminders.find((x) => x.id === Number(id));
  if (!r)
    return (
      <Screen>
        <Header title="Recordatorio" back={() => router.back()} />
        <Empty
          title="No encontrado"
          detail="Actualiza el inicio e inténtalo otra vez."
        />
      </Screen>
    );
  const priority = r.customPriority
    ? {
        label: r.customPriority.name,
        color: r.customPriority.color,
        background: `${r.customPriority.color}16`,
      }
    : priorityMeta[r.priority];
  const type = typeMeta[r.type];
  const remove = () =>
    Alert.alert("Eliminar recordatorio", "Esta acción no se puede deshacer.", [
      { text: "Cancelar", style: "cancel" },
      {
        text: "Eliminar",
        style: "destructive",
        onPress: () => void deleteReminder(r.id).then(() => router.back()),
      },
    ]);
  return (
    <Screen>
      <Header
        title="Detalle"
        subtitle={r.courseName || "Recordatorio personal"}
        back={() => router.back()}
      />
      <View style={s.hero}>
        <View style={s.icon}>
          <Ionicons name={type.icon} size={34} color="white" />
        </View>
        <Text style={s.title}>{r.title}</Text>
        <View style={[s.pill, { backgroundColor: priority.background }]}>
          <View style={[s.dot, { backgroundColor: priority.color }]} />
          <Text style={{ color: priority.color, fontWeight: "900" }}>
            {priority.label}
          </Text>
        </View>
      </View>
      <Card>
        <Info
          icon="calendar-outline"
          label="Fecha y hora"
          value={new Date(r.dueAt).toLocaleString("es-EC", {
            dateStyle: "long",
            timeStyle: "short",
          })}
        />
        <Info icon="library-outline" label="Tipo" value={type.label} />
        <Info
          icon="checkmark-circle-outline"
          label="Estado"
          value={
            r.status === "PENDING"
              ? "Pendiente"
              : r.status === "COMPLETED"
                ? "Completado"
                : "Expirado"
          }
        />
        {r.description && (
          <Info
            icon="document-text-outline"
            label="Descripción"
            value={r.description}
          />
        )}
      </Card>
      {r.status === "PENDING" && session?.profile.role === "USER" && (
        <Button
          title="Marcar como completado"
          icon="checkmark-circle-outline"
          onPress={() => void completeReminder(r.id).then(() => router.back())}
        />
      )}{" "}
      {r.editable && (
        <Button
          title="Eliminar recordatorio"
          variant="danger"
          icon="trash-outline"
          onPress={remove}
        />
      )}
    </Screen>
  );
}
function Info({
  icon,
  label,
  value,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  value: string;
}) {
  return (
    <View style={s.info}>
      <View style={s.infoIcon}>
        <Ionicons name={icon} size={21} color={colors.blue} />
      </View>
      <View style={{ flex: 1 }}>
        <Text style={s.label}>{label}</Text>
        <Text style={s.value}>{value}</Text>
      </View>
    </View>
  );
}
const s = StyleSheet.create({
  hero: { alignItems: "center", gap: 10, paddingVertical: 8 },
  icon: {
    width: 72,
    height: 72,
    borderRadius: 24,
    backgroundColor: colors.blue,
    alignItems: "center",
    justifyContent: "center",
  },
  title: {
    fontSize: 24,
    fontWeight: "900",
    color: colors.navy,
    textAlign: "center",
  },
  pill: {
    paddingHorizontal: 11,
    paddingVertical: 7,
    borderRadius: 99,
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  dot: { width: 7, height: 7, borderRadius: 4 },
  info: { flexDirection: "row", alignItems: "flex-start", gap: 11 },
  infoIcon: {
    width: 42,
    height: 42,
    borderRadius: 13,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  label: {
    fontSize: 11,
    fontWeight: "800",
    color: colors.muted,
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
  value: {
    fontSize: 14,
    lineHeight: 20,
    fontWeight: "700",
    color: colors.text,
    marginTop: 3,
  },
});
