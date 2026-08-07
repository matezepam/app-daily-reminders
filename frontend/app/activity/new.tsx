import { router, useLocalSearchParams } from "expo-router";
import { useState } from "react";
import {
  Button,
  Card,
  Field,
  Header,
  Message,
  Screen,
} from "@/src/components/Ui";
import { useData } from "@/src/context/DataContext";
export default function NewActivity() {
  const { courseId } = useLocalSearchParams<{ courseId: string }>();
  const { saveActivity } = useData();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [due, setDue] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit() {
    const date = new Date(due);
    if (!title.trim() || isNaN(date.getTime()) || date <= new Date()) {
      setError(
        "Completa el título y una fecha futura válida. Ejemplo: 2026-08-20 14:30",
      );
      return;
    }
    setBusy(true);
    try {
      await saveActivity(Number(courseId), {
        title: title.trim(),
        description: description.trim() || undefined,
        dueAt: date.toISOString(),
      });
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo publicar.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen>
      <Header
        title="Nueva actividad"
        subtitle="Los estudiantes la verán en este curso"
        back={() => router.back()}
      />
      <Card>
        <Field
          label="Título"
          icon="create-outline"
          placeholder="Ej. Resolver ejercicios 1 al 10"
          value={title}
          onChangeText={setTitle}
          maxLength={120}
        />
        <Field
          label="Instrucciones"
          icon="document-text-outline"
          placeholder="Describe la actividad"
          value={description}
          onChangeText={setDescription}
          multiline
          maxLength={1000}
        />
        <Field
          label="Fecha límite"
          icon="calendar-outline"
          placeholder="2026-08-20 14:30"
          value={due}
          onChangeText={setDue}
        />
      </Card>
      {error && <Message>{error}</Message>}
      <Button
        title="Publicar actividad"
        icon="send-outline"
        loading={busy}
        onPress={() => void submit()}
      />
    </Screen>
  );
}
