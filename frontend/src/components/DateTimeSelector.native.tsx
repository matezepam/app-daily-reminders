import DateTimePicker, {
  type DateTimePickerEvent,
} from "@react-native-community/datetimepicker";
import { Ionicons } from "@expo/vector-icons";
import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import { colors, radius } from "@/src/theme";

type PickerMode = "date" | "time";

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
  const [mode, setMode] = useState<PickerMode | null>(null);

  function select(event: DateTimePickerEvent, selected?: Date) {
    setMode(null);
    if (event.type === "dismissed" || !selected || !mode) return;
    const next = new Date(value);
    if (mode === "date") {
      next.setFullYear(
        selected.getFullYear(),
        selected.getMonth(),
        selected.getDate(),
      );
    } else {
      next.setHours(selected.getHours(), selected.getMinutes(), 0, 0);
    }
    onChange(next);
  }

  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.row}>
        <PickerButton
          icon="calendar-outline"
          label="Fecha"
          value={value.toLocaleDateString("es-EC", {
            day: "2-digit",
            month: "short",
            year: "numeric",
          })}
          onPress={() => setMode("date")}
        />
        {!dateOnly ? (
          <PickerButton
            icon="time-outline"
            label="Hora"
            value={value.toLocaleTimeString("es-EC", {
              hour: "2-digit",
              minute: "2-digit",
            })}
            onPress={() => setMode("time")}
          />
        ) : null}
      </View>
      <Text style={styles.help}>Toca cada campo para abrir el selector.</Text>
      {mode ? (
        <DateTimePicker
          value={value}
          mode={mode}
          display="default"
          minimumDate={mode === "date" ? minimumDate : undefined}
          maximumDate={mode === "date" ? maximumDate : undefined}
          is24Hour
          onChange={select}
        />
      ) : null}
    </View>
  );
}

function PickerButton({
  icon,
  label,
  value,
  onPress,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  value: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Seleccionar ${label.toLowerCase()}`}
      onPress={onPress}
      style={({ pressed }) => [styles.field, pressed && styles.pressed]}
    >
      <Ionicons name={icon} color={colors.blue} size={21} />
      <View style={{ flex: 1 }}>
        <Text style={styles.fieldLabel}>{label}</Text>
        <Text style={styles.value}>{value}</Text>
      </View>
      <Ionicons name="chevron-down" color={colors.muted} size={18} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  wrap: { gap: 8 },
  label: { color: colors.text, fontSize: 14, fontWeight: "800" },
  row: { flexDirection: "row", gap: 10 },
  field: {
    minHeight: 64,
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    gap: 9,
    paddingHorizontal: 12,
    borderWidth: 1,
    borderColor: "#C9D7E8",
    borderRadius: radius.sm,
    backgroundColor: "#FBFDFF",
  },
  pressed: { opacity: 0.68 },
  fieldLabel: { color: colors.muted, fontSize: 10, fontWeight: "800" },
  value: { color: colors.text, fontSize: 14, fontWeight: "800", marginTop: 2 },
  help: { color: colors.muted, fontSize: 12 },
});
