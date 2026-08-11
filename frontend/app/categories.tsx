import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, View } from "react-native";

import {
  Button,
  Card,
  Field,
  Header,
  Message,
  Screen,
} from "@/src/components/Ui";
import { FullColorPicker } from "@/src/components/FullColorPicker";
import { useData } from "@/src/context/DataContext";
import { colors } from "@/src/theme";

const palette = [
  "#1769E0",
  "#0284C7",
  "#0891B2",
  "#0D9488",
  "#059669",
  "#16A34A",
  "#65A30D",
  "#CA8A04",
  "#E58A00",
  "#EA580C",
  "#DC2626",
  "#E11D48",
  "#DB2777",
  "#C026D3",
  "#9333EA",
  "#7C3AED",
  "#4F46E5",
  "#334155",
  "#60A5FA",
  "#22D3EE",
  "#2DD4BF",
  "#4ADE80",
  "#A3E635",
  "#FACC15",
  "#FB923C",
  "#FB7185",
  "#F472B6",
  "#C084FC",
  "#818CF8",
  "#64748B",
];

function readableText(background: string) {
  const red = Number.parseInt(background.slice(1, 3), 16);
  const green = Number.parseInt(background.slice(3, 5), 16);
  const blue = Number.parseInt(background.slice(5, 7), 16);
  return red * 0.299 + green * 0.587 + blue * 0.114 > 165
    ? colors.navy
    : colors.white;
}

export default function Categories() {
  const { categories, createCategory, deleteCategory } = useData();
  const [name, setName] = useState("");
  const [color, setColor] = useState(palette[0]);
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  async function add() {
    if (!name.trim()) {
      setMessage("Escribe un nombre para tu nueva prioridad.");
      return;
    }
    setBusy(true);
    try {
      await createCategory(name.trim(), color);
      setName("");
      setMessage("");
    } catch (cause) {
      setMessage(cause instanceof Error ? cause.message : "No se pudo crear.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen>
      <Header
        title="Mis prioridades"
        subtitle="Crea etiquetas visuales para organizarte mejor"
        back={() => router.back()}
      />

      <Card>
        <Text style={styles.heading}>Nueva prioridad</Text>
        <Field
          label="Nombre"
          icon="pricetag-outline"
          value={name}
          onChangeText={(value) => {
            setName(value);
            setMessage("");
          }}
          placeholder="Ej. Muy importante"
          maxLength={40}
        />

        <View style={styles.paletteHeader}>
          <View>
            <Text style={styles.label}>Elige un color</Text>
            <Text style={styles.help}>Toca el tono que más te guste.</Text>
          </View>
          <View style={[styles.selectedColor, { backgroundColor: color }]} />
        </View>

        <View style={styles.palette}>
          {palette.map((item) => {
            const selected = color === item;
            return (
              <Pressable
                accessibilityLabel={`Seleccionar color ${item}`}
                accessibilityRole="button"
                key={item}
                onPress={() => setColor(item)}
                style={({ pressed }) => [
                  styles.swatch,
                  { backgroundColor: item },
                  selected && styles.swatchSelected,
                  pressed && styles.swatchPressed,
                ]}
              >
                <Ionicons
                  name="checkmark"
                  size={20}
                  color={selected ? readableText(item) : "transparent"}
                />
              </Pressable>
            );
          })}
        </View>

        <FullColorPicker value={color} onChange={setColor} />

        <View style={[styles.preview, { backgroundColor: color }]}>
          <Ionicons name="flag" size={18} color={readableText(color)} />
          <Text style={[styles.previewText, { color: readableText(color) }]}>
            {name.trim() || "Así se verá tu prioridad"}
          </Text>
        </View>

        {message ? <Message>{message}</Message> : null}
        <Button
          title="Crear prioridad"
          icon="add-circle-outline"
          loading={busy}
          onPress={() => void add()}
        />
      </Card>

      {categories.length ? (
        <View style={styles.list}>
          <Text style={styles.listTitle}>Prioridades creadas</Text>
          {categories.map((category) => (
            <Card key={category.id} style={styles.row}>
              <View style={[styles.color, { backgroundColor: category.color }]}>
                <Ionicons
                  name="flag"
                  size={19}
                  color={readableText(category.color)}
                />
              </View>
              <Text style={styles.name}>{category.name}</Text>
              <Pressable
                accessibilityLabel={`Eliminar ${category.name}`}
                accessibilityRole="button"
                onPress={() => void deleteCategory(category.id)}
                style={({ pressed }) => [
                  styles.delete,
                  pressed && { opacity: 0.6 },
                ]}
              >
                <Ionicons
                  name="trash-outline"
                  size={20}
                  color={colors.danger}
                />
              </Pressable>
            </Card>
          ))}
        </View>
      ) : (
        <View style={styles.empty}>
          <View style={styles.emptyIcon}>
            <Ionicons
              name="color-palette-outline"
              size={31}
              color={colors.blue}
            />
          </View>
          <Text style={styles.emptyTitle}>
            Aún no tienes prioridades propias
          </Text>
          <Text style={styles.emptyText}>
            Elige un nombre y un color para crear la primera.
          </Text>
        </View>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  heading: { fontSize: 19, fontWeight: "900", color: colors.navy },
  label: { color: colors.text, fontSize: 14, fontWeight: "800" },
  help: { color: colors.muted, fontSize: 12, marginTop: 3 },
  paletteHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  selectedColor: {
    width: 34,
    height: 34,
    borderRadius: 11,
    borderWidth: 3,
    borderColor: colors.white,
    shadowColor: colors.navy,
    shadowOpacity: 0.18,
    shadowRadius: 5,
    elevation: 3,
  },
  palette: { flexDirection: "row", flexWrap: "wrap", gap: 9 },
  swatch: {
    width: 42,
    height: 42,
    borderRadius: 13,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 2,
    borderColor: "transparent",
  },
  swatchSelected: {
    borderColor: colors.navy,
    transform: [{ scale: 1.08 }],
  },
  swatchPressed: { opacity: 0.68, transform: [{ scale: 0.94 }] },
  preview: {
    minHeight: 52,
    borderRadius: 15,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 8,
    paddingHorizontal: 14,
  },
  previewText: { fontWeight: "900", fontSize: 15 },
  list: { gap: 12 },
  listTitle: { fontSize: 18, fontWeight: "900", color: colors.navy },
  row: { flexDirection: "row", alignItems: "center", padding: 13 },
  color: {
    width: 45,
    height: 45,
    borderRadius: 14,
    alignItems: "center",
    justifyContent: "center",
  },
  name: { flex: 1, fontSize: 15, fontWeight: "900", color: colors.navy },
  delete: {
    width: 42,
    height: 42,
    borderRadius: 13,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.dangerPale,
  },
  empty: { alignItems: "center", gap: 7, padding: 25 },
  emptyIcon: {
    width: 62,
    height: 62,
    borderRadius: 21,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 4,
  },
  emptyTitle: { color: colors.navy, fontWeight: "900", fontSize: 16 },
  emptyText: { color: colors.muted, textAlign: "center", lineHeight: 19 },
});
