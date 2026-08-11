import { useState } from "react";
import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";
import { signUp } from "@/src/api/client";
import { Button, Card, Field, Header, Screen, ui } from "@/src/components/Ui";
import { colors } from "@/src/theme";
import type { RegistrationRole } from "@/src/types/auth";
export default function Register() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [role, setRole] = useState<RegistrationRole>("STUDENT");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit() {
    const normalized = email.trim().toLowerCase();
    if (
      !name.trim() ||
      !/^\S+@\S+\.\S+$/.test(normalized) ||
      !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(password) ||
      password !== confirm
    ) {
      setError(
        "Completa los datos. Usa 8+ caracteres con mayúscula, minúscula, número y símbolo; ambas contraseñas deben coincidir.",
      );
      return;
    }
    setBusy(true);
    try {
      const result = await signUp(normalized, password, name.trim(), role);
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
        subtitle="Elige si usarás la aplicación como estudiante o profesor"
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
        <Text style={styles.label}>Tipo de cuenta</Text>
        <View style={styles.roles}>
          {(
            [
              ["STUDENT", "Estudiante", "Se une a clases"],
              ["ADMIN", "Profesor", "Crea y administra clases"],
            ] as const
          ).map(([value, title, subtitle]) => (
            <Pressable
              key={value}
              accessibilityRole="radio"
              accessibilityState={{ checked: role === value }}
              onPress={() => setRole(value)}
              style={[styles.role, role === value && styles.roleActive]}
            >
              <Text
                style={[
                  styles.roleTitle,
                  role === value && styles.roleTitleActive,
                ]}
              >
                {title}
              </Text>
              <Text style={styles.roleText}>{subtitle}</Text>
            </Pressable>
          ))}
        </View>
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

const styles = StyleSheet.create({
  label: { color: colors.text, fontSize: 14, fontWeight: "800" },
  roles: { flexDirection: "row", gap: 10 },
  role: {
    flex: 1,
    minHeight: 66,
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 8,
  },
  roleActive: { borderColor: colors.blue, backgroundColor: colors.blueSoft },
  roleTitle: { color: colors.text, fontWeight: "900" },
  roleTitleActive: { color: colors.blue },
  roleText: { color: colors.muted, fontSize: 10, textAlign: "center" },
});
