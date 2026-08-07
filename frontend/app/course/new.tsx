import { router } from "expo-router";
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
export default function NewCourse() {
  const { createCourse } = useData();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit() {
    if (!name.trim()) {
      setError("El nombre es obligatorio.");
      return;
    }
    setBusy(true);
    try {
      await createCourse(name.trim(), description.trim() || undefined);
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo crear.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen>
      <Header
        title="Crear clase"
        subtitle="Generaremos un código único para compartir"
        back={() => router.back()}
      />
      <Card>
        <Field
          label="Nombre de la clase"
          icon="school-outline"
          placeholder="Ej. Arquitectura Empresarial"
          value={name}
          onChangeText={setName}
          maxLength={100}
        />
        <Field
          label="Descripción"
          icon="document-text-outline"
          placeholder="Periodo, horario o información importante"
          value={description}
          onChangeText={setDescription}
          multiline
          maxLength={255}
        />
      </Card>
      {error && <Message>{error}</Message>}
      <Button
        title="Crear clase"
        icon="add-circle-outline"
        loading={busy}
        onPress={() => void submit()}
      />
    </Screen>
  );
}
