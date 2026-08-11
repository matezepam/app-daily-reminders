import { Ionicons } from "@expo/vector-icons";
import type { CSSProperties } from "react";
import { StyleSheet, Text, View } from "react-native";

import { colors, radius } from "@/src/theme";

function localDate(value: Date) {
  return [
    value.getFullYear(),
    String(value.getMonth() + 1).padStart(2, "0"),
    String(value.getDate()).padStart(2, "0"),
  ].join("-");
}

function localTime(value: Date) {
  return `${String(value.getHours()).padStart(2, "0")}:${String(value.getMinutes()).padStart(2, "0")}`;
}

export function DateTimeSelector({
  value,
  onChange,
  minimumDate = new Date(),
  maximumDate,
  dateOnly = false,
  label = "Fecha y hora límite",
}: {
  value: Date;
  onChange: (value: Date) => void;
  minimumDate?: Date;
  maximumDate?: Date;
  dateOnly?: boolean;
  label?: string;
}) {
  function changeDate(raw: string) {
    const [year, month, day] = raw.split("-").map(Number);
    if (!year || !month || !day) return;
    const next = new Date(value);
    next.setFullYear(year, month - 1, day);
    onChange(next);
  }

  function changeTime(raw: string) {
    const [hour, minute] = raw.split(":").map(Number);
    if (!Number.isFinite(hour) || !Number.isFinite(minute)) return;
    const next = new Date(value);
    next.setHours(hour, minute, 0, 0);
    onChange(next);
  }

  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.row}>
        <View style={styles.field}>
          <Ionicons name="calendar-outline" color={colors.blue} size={21} />
          <input
            aria-label="Seleccionar fecha"
            type="date"
            min={localDate(minimumDate)}
            max={maximumDate ? localDate(maximumDate) : undefined}
            value={localDate(value)}
            onChange={(event) => changeDate(event.currentTarget.value)}
            style={webInputStyle}
          />
        </View>
        {!dateOnly ? (
          <View style={styles.field}>
            <Ionicons name="time-outline" color={colors.blue} size={21} />
            <input
              aria-label="Seleccionar hora"
              type="time"
              value={localTime(value)}
              onChange={(event) => changeTime(event.currentTarget.value)}
              style={webInputStyle}
            />
          </View>
        ) : null}
      </View>
      <Text style={styles.help}>Haz clic para abrir el calendario o el reloj.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: 8 },
  label: { color: colors.text, fontSize: 14, fontWeight: "800" },
  row: { flexDirection: "row", gap: 10 },
  field: {
    minHeight: 58,
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: "#C9D7E8",
    borderRadius: radius.sm,
    backgroundColor: "#FBFDFF",
  },
  help: { color: colors.muted, fontSize: 12 },
});

const webInputStyle: CSSProperties = {
  flex: 1,
  minWidth: 0,
  border: 0,
  outline: "none",
  background: "transparent",
  color: colors.text,
  cursor: "pointer",
  fontFamily: "inherit",
  fontSize: 15,
  fontWeight: 700,
};
