import { Ionicons } from "@expo/vector-icons";
import { useFocusEffect } from "@react-navigation/native";
import { router } from "expo-router";
import { useCallback, useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { Card, Empty, Header, Message, Screen } from "@/src/components/Ui";
import { useData } from "@/src/context/DataContext";
import { colors } from "@/src/theme";
import type { ReminderNotification } from "@/src/types/domain";

export default function NotificationsPage() {
  const { dashboard, getUpcomingNotifications } = useData();
  const [items, setItems] = useState<ReminderNotification[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await getUpcomingNotifications());
      setError("");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudieron cargar los avisos.");
    } finally {
      setLoading(false);
    }
  }, [getUpcomingNotifications]);

  useFocusEffect(
    useCallback(() => {
      void load();
    }, [load]),
  );

  return (
    <Screen refreshing={loading} onRefresh={load}>
      <Header
        title="Centro de avisos"
        subtitle="Fechas programadas en tus recordatorios"
        back={() => router.back()}
      />
      <Message type="info">
        Aquí puedes consultar tus próximos avisos. Esta pantalla no activa alarmas ni permisos del teléfono.
      </Message>
      {error ? <Message>{error}</Message> : null}
      {items.length ? (
        items.map((notification) => {
          const reminder = dashboard.reminders.find(
            (candidate) => candidate.id === notification.reminderId,
          );
          return (
            <Pressable
              key={notification.id}
              disabled={!reminder}
              onPress={() => reminder && router.push(`/reminder/${reminder.id}`)}
            >
              <Card style={styles.card}>
                <View style={styles.icon}>
                  <Ionicons name="notifications" size={24} color={colors.blue} />
                </View>
                <View style={styles.copy}>
                  <Text style={styles.title}>{reminder?.title ?? "Recordatorio"}</Text>
                  <Text style={styles.course}>{reminder?.courseName ?? "Personal"}</Text>
                  <Text style={styles.date}>
                    Aviso: {new Date(notification.notifyAt).toLocaleString("es-EC", {
                      dateStyle: "medium",
                      timeStyle: "short",
                    })}
                  </Text>
                </View>
                <Ionicons name="chevron-forward" size={19} color={colors.muted} />
              </Card>
            </Pressable>
          );
        })
      ) : (
        <Empty
          icon="notifications-off-outline"
          title="Sin avisos próximos"
          detail="Los avisos que configures al crear un recordatorio aparecerán aquí."
        />
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  card: { flexDirection: "row", alignItems: "center", gap: 12 },
  icon: {
    width: 50,
    height: 50,
    borderRadius: 16,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  copy: { flex: 1, gap: 3 },
  title: { color: colors.navy, fontSize: 15, fontWeight: "900" },
  course: { color: colors.blue, fontSize: 11, fontWeight: "800" },
  date: { color: colors.muted, fontSize: 12, lineHeight: 17 },
});
