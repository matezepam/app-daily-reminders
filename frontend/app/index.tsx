import { router } from "expo-router";
import { useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from "react-native";
import { confirmSignUp, resendConfirmation, signUp } from "@/src/api/client";
import { Brand } from "@/src/components/Brand";
import { Button, Card, Field, Screen } from "@/src/components/Ui";
import { useAuth } from "@/src/context/AuthContext";
import { colors } from "@/src/theme";
type Mode = "login" | "register" | "confirm";
export default function Access() {
  const { session, initializing, signIn, error, clearError } = useAuth();
  const [mode, setMode] = useState<Mode>("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [code, setCode] = useState("");
  const [notice, setNotice] = useState("");
  const [validation, setValidation] = useState("");
  const [busy, setBusy] = useState(false);
  if (initializing)
    return (
      <View style={s.loading}>
        <ActivityIndicator size="large" color={colors.blue} />
      </View>
    );
  if (session) return null;
  function change(next: Mode) {
    setMode(next);
    setValidation("");
    setNotice("");
    clearError();
  }
  async function submit() {
    const normalized = email.trim().toLowerCase();
    if (mode === "login" && (!normalized || !password)) {
      setValidation("Escribe tu correo y contraseña.");
      return;
    }
    if (
      mode === "register" &&
      (!name.trim() || !/^\S+@\S+\.\S+$/.test(normalized) || !password)
    ) {
      setValidation("Completa un nombre, correo válido y contraseña.");
      return;
    }
    if (
      mode === "register" &&
      !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(password)
    ) {
      setValidation(
        "Usa 8 caracteres o más, con mayúscula, minúscula, número y símbolo.",
      );
      return;
    }
    if (mode === "confirm" && !/^\d{6}$/.test(code.trim())) {
      setValidation("Ingresa el código de 6 dígitos enviado a tu correo.");
      return;
    }
    setBusy(true);
    setValidation("");
    setNotice("");
    clearError();
    try {
      if (mode === "login") await signIn(normalized, password);
      else if (mode === "register") {
        await signUp(normalized, password, name.trim());
        setMode("confirm");
        setNotice(`Enviamos un código a ${normalized}.`);
      } else {
        await confirmSignUp(normalized, code.trim());
        setNotice("Cuenta confirmada. Iniciando sesión…");
        await signIn(normalized, password);
      }
    } catch (e) {
      setValidation(
        e instanceof Error ? e.message : "No se pudo completar la solicitud.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function resend() {
    try {
      await resendConfirmation(email.trim().toLowerCase());
      setNotice("Código reenviado. Revisa también la carpeta de spam.");
    } catch (e) {
      setValidation(e instanceof Error ? e.message : "No se pudo reenviar.");
    }
  }
  const title =
    mode === "login"
      ? "Bienvenido"
      : mode === "register"
        ? "Crea tu cuenta"
        : "Confirma tu correo";
  const subtitle =
    mode === "login"
      ? "Ingresa para organizar tu vida académica"
      : mode === "register"
        ? "Tu cuenta estará protegida por Amazon Cognito"
        : `Escribe el código enviado a ${email}`;
  return (
    <Screen contentStyle={s.screen}>
      <View style={s.hero}>
        <Brand />
        <Text style={s.welcome}>{title}</Text>
        <Text style={s.subtitle}>{subtitle}</Text>
      </View>
      <Card style={s.card}>
        {mode !== "confirm" && (
          <View style={s.tabs}>
            <Pressable
              onPress={() => change("login")}
              style={[s.tab, mode === "login" && s.tabActive]}
            >
              <Text style={[s.tabText, mode === "login" && s.tabTextActive]}>
                Iniciar sesión
              </Text>
            </Pressable>
            <Pressable
              onPress={() => change("register")}
              style={[s.tab, mode === "register" && s.tabActive]}
            >
              <Text style={[s.tabText, mode === "register" && s.tabTextActive]}>
                Crear cuenta
              </Text>
            </Pressable>
          </View>
        )}
        {mode === "login" ? (
          <>
            <Field
              label="Correo electrónico"
              icon="mail-outline"
              placeholder="nombre@correo.com"
              keyboardType="email-address"
              autoCapitalize="none"
              value={email}
              onChangeText={(v) => {
                setEmail(v);
                setValidation("");
              }}
            />
            <Field
              label="Contraseña"
              icon="lock-closed-outline"
              placeholder="Ingresa tu contraseña"
              secureTextEntry
              value={password}
              onChangeText={(v) => {
                setPassword(v);
                setValidation("");
              }}
            />
            <Pressable onPress={() => router.push("/forgot-password")}>
              <Text style={s.link}>¿Olvidaste tu contraseña?</Text>
            </Pressable>
          </>
        ) : mode === "register" ? (
          <>
            <Field
              label="Nombre completo"
              icon="person-outline"
              placeholder="Ej. Ana Torres"
              value={name}
              onChangeText={setName}
            />
            <Field
              label="Correo electrónico"
              icon="mail-outline"
              placeholder="nombre@correo.com"
              keyboardType="email-address"
              autoCapitalize="none"
              value={email}
              onChangeText={setEmail}
            />
            <Field
              label="Contraseña segura"
              icon="lock-closed-outline"
              placeholder="8+ caracteres, Aa, 1 y símbolo"
              secureTextEntry
              value={password}
              onChangeText={setPassword}
            />
            <View>
              <Text style={s.label}>Tipo de cuenta</Text>
              <View style={s.studentRole}>
                <Text style={s.studentText}>Estudiante</Text>
              </View>
              <Text style={s.roleHelp}>
                Los perfiles de profesor se asignan administrativamente.
              </Text>
            </View>
          </>
        ) : (
          <>
            <View style={s.codeIcon}>
              <Text style={{ fontSize: 30 }}>✉️</Text>
            </View>
            <Field
              label="Código de confirmación"
              icon="keypad-outline"
              placeholder="000000"
              keyboardType="number-pad"
              maxLength={6}
              value={code}
              onChangeText={(v) => setCode(v.replace(/\D/g, ""))}
            />
            <Pressable disabled={busy} onPress={() => void resend()}>
              <Text style={s.link}>Reenviar código</Text>
            </Pressable>
            <Pressable onPress={() => change("register")}>
              <Text style={s.secondary}>Usar otro correo</Text>
            </Pressable>
          </>
        )}
        {(validation || error) && (
          <View style={s.error}>
            <Text style={s.errorText}>{validation || error}</Text>
          </View>
        )}
        {notice && (
          <View style={s.success}>
            <Text style={s.successText}>{notice}</Text>
          </View>
        )}
        <Button
          title={
            mode === "login"
              ? "Iniciar sesión"
              : mode === "register"
                ? "Crear mi cuenta"
                : "Confirmar y entrar"
          }
          icon={
            mode === "login"
              ? "log-in-outline"
              : mode === "register"
                ? "person-add-outline"
                : "checkmark-circle-outline"
          }
          loading={busy}
          onPress={() => void submit()}
        />
        <View style={s.secure}>
          <View style={s.shield}>
            <Text>🛡️</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={s.secureTitle}>Protegido por Amazon Cognito</Text>
            <Text style={s.secureText}>
              Contraseñas y sesiones se administran de forma segura en AWS
            </Text>
          </View>
        </View>
      </Card>
    </Screen>
  );
}
const s = StyleSheet.create({
  screen: { paddingBottom: 40 },
  loading: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.background,
  },
  hero: { alignItems: "center", gap: 7, paddingTop: 8 },
  welcome: {
    fontSize: 28,
    fontWeight: "900",
    color: colors.navy,
    marginTop: 9,
  },
  subtitle: { fontSize: 14, color: colors.muted, textAlign: "center" },
  card: { maxWidth: 520, width: "100%", alignSelf: "center" },
  tabs: {
    flexDirection: "row",
    padding: 4,
    borderRadius: 14,
    backgroundColor: colors.blueSoft,
  },
  tab: {
    flex: 1,
    minHeight: 43,
    borderRadius: 11,
    alignItems: "center",
    justifyContent: "center",
  },
  tabActive: { backgroundColor: colors.blue },
  tabText: { fontWeight: "800", color: colors.muted },
  tabTextActive: { color: "white" },
  link: { color: colors.blue, fontWeight: "800", textAlign: "center" },
  secondary: { color: colors.muted, fontWeight: "700", textAlign: "center" },
  label: {
    fontSize: 14,
    fontWeight: "800",
    color: colors.text,
    marginBottom: 7,
  },
  studentRole: {
    height: 44,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.blue,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  studentText: { color: colors.blue, fontWeight: "900" },
  roleHelp: {
    fontSize: 11,
    color: colors.muted,
    textAlign: "center",
    marginTop: 6,
  },
  codeIcon: {
    width: 70,
    height: 70,
    borderRadius: 24,
    alignSelf: "center",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: colors.blueSoft,
  },
  error: { padding: 12, backgroundColor: colors.dangerPale, borderRadius: 12 },
  errorText: { color: colors.danger, lineHeight: 18 },
  success: {
    padding: 12,
    backgroundColor: colors.successPale,
    borderRadius: 12,
  },
  successText: { color: colors.success, lineHeight: 18 },
  secure: { flexDirection: "row", alignItems: "center", gap: 10 },
  shield: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  secureTitle: { color: colors.text, fontWeight: "800" },
  secureText: { color: colors.muted, fontSize: 11, marginTop: 2 },
});
