import { router } from "expo-router";
import { useState } from "react";
import {
  Button,
  Card,
  Empty,
  Field,
  Header,
  Message,
  Screen,
} from "@/src/components/Ui";
import { useAuth } from "@/src/context/AuthContext";
import { useData } from "@/src/context/DataContext";

const DUPLICATE_MESSAGE =
  "Ya existe una clase con el mismo nombre y descripción. Cambia al menos uno para continuar.";

function normalize(value?: string | null) {
  return (value ?? "").trim().toLowerCase();
}

export default function NewCourse() {
  const { session } = useAuth();
  const { courses, createCourse } = useData();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  if (session?.profile.role !== "ADMIN") {
    return (
      <Screen>
        <Header title="Crear clase" back={() => router.back()} />
        <Empty
          icon="lock-closed-outline"
          title="Disponible para profesores"
          detail="Los estudiantes pueden unirse a una clase con el código de su profesor."
        />
      </Screen>
    );
  }
  const duplicate =
    Boolean(name.trim()) &&
    courses.some(
      (course) =>
        course.ownedByMe &&
        normalize(course.name) === normalize(name) &&
        normalize(course.description) === normalize(description),
    );
  async function submit() {
    if (!name.trim()) {
      setError("El nombre es obligatorio.");
      return;
    }
    if (duplicate) {
      setError(DUPLICATE_MESSAGE);
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
          onChangeText={(value) => {
            setName(value);
            setError("");
          }}
          maxLength={100}
        />
        <Field
          label="Descripción"
          icon="document-text-outline"
          placeholder="Periodo, horario o información importante"
          value={description}
          onChangeText={(value) => {
            setDescription(value);
            setError("");
          }}
          multiline
          maxLength={255}
        />
      </Card>
      {(duplicate || error) && <Message>{duplicate ? DUPLICATE_MESSAGE : error}</Message>}
      <Button
        title="Crear clase"
        icon="add-circle-outline"
        loading={busy}
        disabled={duplicate}
        onPress={() => void submit()}
      />
    </Screen>
  );
}
