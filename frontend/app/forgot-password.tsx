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
  const [busy, setBusy] = useState(false);
  async function request() {
    const normalized = email.trim().toLowerCase();
    if (!/^\S+@\S+\.\S+$/.test(normalized)) {
      setMsg("Escribe un correo electrónico válido.");
      return;
    }
    setBusy(true);
    try {
      await forgotPassword(normalized);
      setSent(true);
      setMsg("Código enviado a tu correo.");
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "No se pudo enviar.");
    } finally {
      setBusy(false);
    }
  }
  async function confirm() {
    if (!/^\d{6}$/.test(code.trim())) {
      setMsg("Ingresa el código de 6 dígitos recibido por correo.");
      return;
    }
    if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(password)) {
      setMsg("Usa 8 caracteres o más, con mayúscula, minúscula, número y símbolo.");
      return;
    }
    setBusy(true);
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
    } finally {
      setBusy(false);
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
          loading={busy}
          onPress={() => void (sent ? confirm() : request())}
        />
      </Card>
    </Screen>
  );
}
