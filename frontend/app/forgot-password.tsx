import { useState } from "react";
import { router } from "expo-router";
import { Text } from "react-native";
import { confirmForgotPassword, forgotPassword } from "@/src/api/client";
import { Button, Card, Field, Header, Screen, ui } from "@/src/components/Ui";
export default function Forgot() {
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [sent, setSent] = useState(false);
  const [msg, setMsg] = useState("");
  async function request() {
    try {
      await forgotPassword(email.trim().toLowerCase());
      setSent(true);
      setMsg("Código enviado a tu correo.");
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "No se pudo enviar.");
    }
  }
  async function confirm() {
    if (password.length < 8) {
      setMsg("La contraseña debe tener al menos 8 caracteres.");
      return;
    }
    try {
      await confirmForgotPassword(
        email.trim().toLowerCase(),
        code.trim(),
        password,
      );
      setMsg("Contraseña actualizada.");
      setTimeout(() => router.replace("/"), 900);
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "No se pudo actualizar.");
    }
  }
  return (
    <Screen>
      <Header title="Recuperar contraseña" back={() => router.back()} />
      <Card>
        <Field
          label="Correo electrónico"
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
        />
        {sent && (
          <>
            <Field
              label="Código recibido"
              value={code}
              onChangeText={setCode}
              keyboardType="number-pad"
            />
            <Field
              label="Nueva contraseña"
              value={password}
              onChangeText={setPassword}
              secureTextEntry
            />
          </>
        )}
        {msg && <Text style={ui.subtitle}>{msg}</Text>}
        <Button
          title={sent ? "Cambiar contraseña" : "Enviar código"}
          icon="key-outline"
          onPress={() => void (sent ? confirm() : request())}
        />
      </Card>
    </Screen>
  );
}
