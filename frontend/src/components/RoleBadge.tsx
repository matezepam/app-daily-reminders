import { Ionicons } from "@expo/vector-icons";
import { StyleSheet, Text, View } from "react-native";

import { colors } from "@/src/theme";
import type { SessionRole } from "@/src/types/auth";

const labels: Record<SessionRole, string> = {
  ADMIN: "Administrador",
  USER: "Estudiante",
  PROFESSOR: "Profesor",
};

export function RoleBadge({ roles = [] }: { roles?: SessionRole[] }) {
  const role = roles.includes("ADMIN")
    ? "ADMIN"
    : roles.includes("PROFESSOR")
      ? "PROFESSOR"
      : "USER";

  return (
    <View style={styles.badge}>
      <Ionicons
        color={colors.blue}
        name={role === "USER" ? "school-outline" : "briefcase-outline"}
        size={17}
      />
      <Text style={styles.label}>{labels[role]}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    alignItems: "center",
    alignSelf: "flex-start",
    backgroundColor: colors.bluePale,
    borderRadius: 999,
    flexDirection: "row",
    gap: 6,
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  label: {
    color: colors.blue,
    fontSize: 13,
    fontWeight: "800",
  },
});
