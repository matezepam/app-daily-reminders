import { useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { Text } from "react-native";
import { confirmSignUp, resendConfirmation } from "@/src/api/client";
import { Button, Card, Field, Header, Screen, ui } from "@/src/components/Ui";
export default function Confirm() {
  const params = useLocalSearchParams<{ email?: string }>();
  const [email, setEmail] = useState(params.email ?? "");
  const [code, setCode] = useState("");
  const [msg, setMsg] = useState(
    "Revisa tu correo e ingresa el código enviado por Cognito.",
  );
  const [busy, setBusy] = useState(false);
  async function submit() {
    const normalized = email.trim().toLowerCase();
    if (!/^\S+@\S+\.\S+$/.test(normalized) || !/^\d{6}$/.test(code.trim())) {
      setMsg("Ingresa un correo válido y el código de 6 dígitos.");
      return;
    }
    setBusy(true);
    try {
      await confirmSignUp(normalized, code.trim());
      setMsg("Cuenta confirmada. Ya puedes iniciar sesión.");
      setTimeout(() => router.replace("/"), 900);
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "No se pudo confirmar.");
    } finally {
      setBusy(false);
    }
  }
  async function resend() {
    const normalized = email.trim().toLowerCase();
    if (!/^\S+@\S+\.\S+$/.test(normalized)) {
      setMsg("Ingresa un correo electrónico válido.");
      return;
    }
    try {
      await resendConfirmation(normalized);
      setMsg("Código reenviado. Revisa también spam.");
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "No se pudo reenviar.");
    }
  }
  return (
    <Screen>
      <Header title="Verificar correo" back={() => router.back()} />
      <Card>
        <Field
          label="Correo electrónico"
          value={email}
          onChangeText={setEmail}
          autoCapitalize="none"
        />
        <Field
          label="Código de verificación"
          value={code}
          onChangeText={setCode}
          keyboardType="number-pad"
        />
        <Text style={ui.subtitle}>{msg}</Text>
        <Button
          title="Confirmar cuenta"
          icon="checkmark-circle-outline"
          loading={busy}
          onPress={() => void submit()}
        />
        <Button
          title="Reenviar código"
          icon="mail-outline"
          onPress={() => void resend()}
        />
      </Card>
    </Screen>
  );
}
