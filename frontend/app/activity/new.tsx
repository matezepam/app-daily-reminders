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
import { DateTimeSelector } from "@/src/components/DateTimeSelector";
import { useData } from "@/src/context/DataContext";

function defaultDueAt() {
  const date = new Date(Date.now() + 86_400_000);
  date.setHours(9, 0, 0, 0);
  return date;
}
export default function NewActivity() {
  const { courseId, id, title: initialTitle, description: initialDescription, dueAt: initialDueAt } =
    useLocalSearchParams<{
      courseId: string;
      id?: string;
      title?: string;
      description?: string;
      dueAt?: string;
    }>();
  const { saveActivity } = useData();
  const parsedDueAt = initialDueAt ? new Date(initialDueAt) : null;
  const editingId = id ? Number(id) : undefined;
  const editing = Number.isFinite(editingId);
  const [title, setTitle] = useState(initialTitle ?? "");
  const [description, setDescription] = useState(initialDescription ?? "");
  const [dueAt, setDueAt] = useState(
    parsedDueAt && !isNaN(parsedDueAt.getTime()) ? parsedDueAt : defaultDueAt(),
  );
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit() {
    if (!title.trim() || isNaN(dueAt.getTime()) || dueAt <= new Date()) {
      setError(
        "Completa el título y selecciona una fecha y hora futuras válidas.",
      );
      return;
    }
    setBusy(true);
    try {
      await saveActivity(
        Number(courseId),
        {
          title: title.trim(),
          description: description.trim() || undefined,
          dueAt: dueAt.toISOString(),
        },
        editing ? editingId : undefined,
      );
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : editing ? "No se pudo guardar." : "No se pudo publicar.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen>
      <Header
        title={editing ? "Editar actividad" : "Nueva actividad"}
        subtitle={editing ? "Actualiza el contenido y la fecha límite" : "Los estudiantes la verán en este curso"}
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
        <DateTimeSelector value={dueAt} onChange={setDueAt} />
      </Card>
      {error && <Message>{error}</Message>}
      <Button
        title={editing ? "Guardar cambios" : "Publicar actividad"}
        icon={editing ? "save-outline" : "send-outline"}
        loading={busy}
        onPress={() => void submit()}
      />
    </Screen>
  );
}
