import { createElement } from "react";
import { Platform, StyleSheet, Text, View } from "react-native";

import { colors } from "@/src/theme";

export function FullColorPicker({
  value,
  onChange,
}: {
  value: string;
  onChange: (color: string) => void;
}) {
  if (Platform.OS !== "web") return null;

  return (
    <View style={styles.container}>
      <View style={styles.copy}>
        <Text style={styles.title}>¿Quieres otro tono?</Text>
        <Text style={styles.detail}>Abre el selector completo de colores.</Text>
      </View>
      {createElement("input", {
        "aria-label": "Seleccionar cualquier color",
        type: "color",
        value,
        onChange: (event: { currentTarget: { value: string } }) =>
          onChange(event.currentTarget.value.toUpperCase()),
        style: {
          width: 54,
          height: 44,
          padding: 0,
          border: 0,
          borderRadius: 12,
          background: "transparent",
          cursor: "pointer",
        },
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    minHeight: 58,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 14,
    paddingHorizontal: 13,
    paddingVertical: 8,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    backgroundColor: colors.blueSoft,
  },
  copy: { flex: 1 },
  title: { color: colors.navy, fontWeight: "800", fontSize: 13 },
  detail: { color: colors.muted, fontSize: 11, marginTop: 2 },
});
