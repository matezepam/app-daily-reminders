import { useState } from "react";
import { router } from "expo-router";
import { Text } from "react-native";
import { signUp } from "@/src/api/client";
import { Button, Card, Field, Header, Screen, ui } from "@/src/components/Ui";
export default function Register() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit() {
    const normalized = email.trim().toLowerCase();
    if (
      !name.trim() ||
      !/^\S+@\S+\.\S+$/.test(normalized) ||
      password.length < 8 ||
      password !== confirm
    ) {
      setError(
        "Completa los datos. La contraseña debe tener al menos 8 caracteres y coincidir.",
      );
      return;
    }
    setBusy(true);
    try {
      const result = await signUp(normalized, password, name.trim());
      if (result.UserConfirmed) {
        router.replace("/");
      } else {
        router.replace({ pathname: "/confirm", params: { email: normalized } });
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo crear la cuenta.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen>
      <Header
        title="Crear cuenta"
        subtitle="Las cuentas nuevas se registran como estudiantes"
        back={() => router.back()}
      />
      <Card>
        <Field label="Nombre completo" value={name} onChangeText={setName} />
        <Field
          label="Correo electrónico"
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
          keyboardType="email-address"
        />
        <Field
          label="Contraseña"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
        />
        <Field
          label="Repite la contraseña"
          value={confirm}
          onChangeText={setConfirm}
          secureTextEntry
        />
        {error && <Text style={ui.subtitle}>{error}</Text>}
        <Button
          title="Crear cuenta"
          icon="person-add-outline"
          loading={busy}
          onPress={() => void submit()}
        />
      </Card>
    </Screen>
  );
}
