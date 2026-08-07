import { Ionicons } from "@expo/vector-icons";
import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";

import {
  Button,
  Card,
  Chip,
  Field,
  Header,
  Message,
  Screen,
} from "@/src/components/Ui";
import { useData } from "@/src/context/DataContext";
import { colors, priorityMeta, typeMeta } from "@/src/theme";
import type { ReminderPriority, ReminderType } from "@/src/types/domain";

const reminderTypes = Object.keys(typeMeta) as ReminderType[];
const priorities = Object.keys(priorityMeta) as ReminderPriority[];
const quickOffsets = [5, 10, 20, 40];

function tomorrowDate() {
  const date = new Date(Date.now() + 86_400_000);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function offsetLabel(minutes: number) {
  if (minutes < 60) return `${minutes} min`;
  if (minutes % 1440 === 0)
    return `${minutes / 1440} día${minutes === 1440 ? "" : "s"}`;
  if (minutes % 60 === 0)
    return `${minutes / 60} hora${minutes === 60 ? "" : "s"}`;
  return `${minutes} min`;
}

export default function NewReminder() {
  const { courseId } = useLocalSearchParams<{ courseId?: string }>();
  const { categories, saveReminder } = useData();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [date, setDate] = useState(tomorrowDate());
  const [time, setTime] = useState("09:00");
  const [type, setType] = useState<ReminderType>("TASK");
  const [priority, setPriority] = useState<ReminderPriority>("MEDIUM");
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [offsets, setOffsets] = useState<number[]>([10]);
  const [customOffset, setCustomOffset] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function toggleOffset(minutes: number) {
    setError("");
    setOffsets((current) =>
      current.includes(minutes)
        ? current.filter((item) => item !== minutes)
        : [...current, minutes].sort((a, b) => a - b),
    );
  }

  function addCustomOffset() {
    const minutes = Number(customOffset);
    if (!Number.isInteger(minutes) || minutes < 1 || minutes > 10_080) {
      setError("El aviso personalizado debe estar entre 1 minuto y 7 días.");
      return;
    }
    if (!offsets.includes(minutes) && offsets.length >= 10) {
      setError("Puedes configurar como máximo 10 avisos por recordatorio.");
      return;
    }
    setOffsets((current) =>
      [...new Set([...current, minutes])].sort((a, b) => a - b),
    );
    setCustomOffset("");
    setError("");
  }

  async function submit() {
    const dueAt = new Date(`${date}T${time}:00`);
    if (!title.trim()) {
      setError("Escribe un título para identificar el recordatorio.");
      return;
    }
    if (Number.isNaN(dueAt.getTime()) || dueAt <= new Date()) {
      setError("Selecciona una fecha y hora futuras válidas.");
      return;
    }
    if (!offsets.length) {
      setError("Selecciona al menos un momento para recibir el aviso.");
      return;
    }
    if (
      offsets.some(
        (minutes) => dueAt.getTime() - minutes * 60_000 <= Date.now(),
      )
    ) {
      setError(
        "Uno de los avisos ocurriría antes de este momento. Elige menos minutos o una fecha más lejana.",
      );
      return;
    }

    setBusy(true);
    setError("");
    try {
      const saved = await saveReminder(
        {
          title: title.trim(),
          description: description.trim() || undefined,
          type,
          dueAt: dueAt.toISOString(),
          priority,
          priorityCategoryId: categoryId,
          notificationOffsetsMinutes: [...offsets].sort((a, b) => a - b),
        },
        courseId ? Number(courseId) : undefined,
      );
      router.replace(`/reminder/${saved.id}`);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "No se pudo guardar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Screen>
      <Header
        title="Nuevo recordatorio"
        subtitle={
          courseId
            ? "Se publicará para todo el curso"
            : "Solo será visible para ti"
        }
        back={() => router.back()}
      />

      <Card>
        <Field
          label="Título"
          icon="create-outline"
          placeholder="Ej. Estudiar para el examen"
          value={title}
          onChangeText={(value) => {
            setTitle(value);
            setError("");
          }}
          maxLength={100}
        />
        <Field
          label="Descripción opcional"
          icon="document-text-outline"
          placeholder="Agrega detalles que te ayuden a recordar"
          value={description}
          onChangeText={setDescription}
          multiline
          maxLength={500}
        />
        <View style={styles.dateRow}>
          <View style={styles.dateField}>
            <Field
              label="Fecha"
              icon="calendar-outline"
              placeholder="AAAA-MM-DD"
              value={date}
              onChangeText={setDate}
              keyboardType="numbers-and-punctuation"
            />
          </View>
          <View style={styles.timeField}>
            <Field
              label="Hora"
              icon="time-outline"
              placeholder="HH:mm"
              value={time}
              onChangeText={setTime}
              keyboardType="numbers-and-punctuation"
            />
          </View>
        </View>
      </Card>

      <Card>
        <Text style={styles.sectionTitle}>¿Qué tipo de actividad es?</Text>
        <View style={styles.wrap}>
          {reminderTypes.map((item) => (
            <Chip
              key={item}
              label={typeMeta[item].label}
              icon={typeMeta[item].icon}
              selected={item === type}
              onPress={() => setType(item)}
            />
          ))}
        </View>

        <Text style={styles.sectionTitle}>Prioridad</Text>
        <Text style={styles.help}>
          El color te permite reconocerla rápidamente.
        </Text>
        <View style={styles.wrap}>
          {priorities.map((item) => (
            <Chip
              key={item}
              label={priorityMeta[item].label}
              color={priorityMeta[item].color}
              selected={item === priority && categoryId === null}
              onPress={() => {
                setPriority(item);
                setCategoryId(null);
              }}
            />
          ))}
        </View>

        {categories.length > 0 && (
          <>
            <Text style={styles.sectionTitle}>Mi prioridad personalizada</Text>
            <View style={styles.wrap}>
              {categories.map((category) => (
                <Chip
                  key={category.id}
                  label={category.name}
                  color={category.color}
                  selected={categoryId === category.id}
                  onPress={() =>
                    setCategoryId(
                      categoryId === category.id ? null : category.id,
                    )
                  }
                />
              ))}
            </View>
          </>
        )}
      </Card>

      <Card>
        <View style={styles.notificationTitle}>
          <View style={styles.notificationIcon}>
            <Ionicons
              name="notifications-outline"
              size={24}
              color={colors.blue}
            />
          </View>
          <View style={styles.notificationCopy}>
            <Text style={styles.notificationHeading}>Avisarme antes</Text>
            <Text style={styles.help}>Puedes elegir una o varias alertas.</Text>
          </View>
        </View>

        <View style={styles.wrap}>
          {quickOffsets.map((minutes) => (
            <Chip
              key={minutes}
              label={`${minutes} minutos`}
              icon={
                offsets.includes(minutes) ? "checkmark-circle" : "time-outline"
              }
              selected={offsets.includes(minutes)}
              onPress={() => toggleOffset(minutes)}
            />
          ))}
        </View>

        {offsets.some((item) => !quickOffsets.includes(item)) && (
          <View style={styles.customSelections}>
            {offsets
              .filter((item) => !quickOffsets.includes(item))
              .map((minutes) => (
                <Chip
                  key={minutes}
                  label={`${offsetLabel(minutes)} antes ×`}
                  selected
                  onPress={() => toggleOffset(minutes)}
                />
              ))}
          </View>
        )}

        <View style={styles.customRow}>
          <View style={styles.customField}>
            <Field
              label="Otro intervalo"
              icon="options-outline"
              placeholder="Ej. 60"
              keyboardType="number-pad"
              value={customOffset}
              onChangeText={(value) =>
                setCustomOffset(value.replace(/\D/g, ""))
              }
              onSubmitEditing={addCustomOffset}
            />
          </View>
          <View style={styles.customAction}>
            <Button
              compact
              title="Añadir"
              variant="soft"
              icon="add-circle-outline"
              disabled={!customOffset}
              onPress={addCustomOffset}
            />
          </View>
        </View>
        <Text style={styles.hint}>
          El valor personalizado se escribe en minutos.
        </Text>
      </Card>

      {error ? <Message>{error}</Message> : null}
      <Button
        title="Guardar recordatorio"
        icon="save-outline"
        loading={busy}
        onPress={() => void submit()}
      />
    </Screen>
  );
}

const styles = StyleSheet.create({
  dateRow: { flexDirection: "row", gap: 10 },
  dateField: { flex: 1.25 },
  timeField: { flex: 0.85 },
  sectionTitle: {
    color: colors.navy,
    fontSize: 16,
    fontWeight: "900",
    marginTop: 2,
  },
  help: { color: colors.muted, fontSize: 13, lineHeight: 18 },
  hint: { color: colors.muted, fontSize: 12, marginTop: -5 },
  wrap: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  customSelections: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  notificationTitle: { flexDirection: "row", alignItems: "center", gap: 11 },
  notificationIcon: {
    width: 46,
    height: 46,
    borderRadius: 15,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.blueSoft,
  },
  notificationCopy: { flex: 1 },
  notificationHeading: {
    color: colors.navy,
    fontSize: 18,
    fontWeight: "900",
  },
  customRow: { flexDirection: "row", alignItems: "stretch", gap: 10 },
  customField: { flex: 1 },
  customAction: { justifyContent: "flex-end", paddingBottom: 1 },
});
