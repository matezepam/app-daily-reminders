import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import {
  Button,
  Card,
  Field,
  Header,
  Message,
  Screen,
} from "@/src/components/Ui";
import { useAuth } from "@/src/context/AuthContext";
import { colors } from "@/src/theme";
export default function Profile() {
  const { session, updateProfile, signOut } = useAuth();
  const [name, setName] = useState(session?.profile.fullName ?? "");
  const [msg, setMsg] = useState("");
  const [success, setSuccess] = useState(false);
  async function save() {
    try {
      const updated = await updateProfile(name);
      setName(updated.fullName);
      setSuccess(true);
      setMsg("Nombre actualizado correctamente.");
    } catch (e) {
      setSuccess(false);
      setMsg(e instanceof Error ? e.message : "No se pudo actualizar.");
    }
  }
  const initials = (session?.profile.fullName ?? "U")
    .split(" ")
    .slice(0, 2)
    .map((x) => x[0])
    .join("")
    .toUpperCase();
  return (
    <Screen>
      <Header title="Mi perfil" subtitle="Cuenta, identidad y preferencias" />
      <View style={s.hero}>
        <View style={s.avatar}>
          <Text style={s.initials}>{initials}</Text>
        </View>
        <Text style={s.name}>{session?.profile.fullName}</Text>
        <Text style={s.email}>{session?.profile.email}</Text>
        <View style={s.role}>
          <Ionicons
            name={
              session?.profile.role === "STUDENT"
                ? "school-outline"
                : "briefcase-outline"
            }
            size={16}
            color={colors.blue}
          />
          <Text style={s.roleText}>
            {session?.profile.role === "STUDENT"
              ? "Estudiante"
              : "Profesor / Administrador"}
          </Text>
        </View>
      </View>
      <Card>
        <Text style={s.cardTitle}>Información personal</Text>
        <Field
          label="Nombre completo"
          icon="person-outline"
          value={name}
          onChangeText={setName}
        />
        {msg && <Message type={success ? "success" : "error"}>{msg}</Message>}
        <Button
          title="Guardar cambios"
          icon="save-outline"
          onPress={() => void save()}
        />
      </Card>
      <Card>
        <View style={s.setting}>
          <View style={s.settingIcon}>
            <Ionicons
              name="color-palette-outline"
              size={23}
              color={colors.blue}
            />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={s.settingTitle}>Prioridades personalizadas</Text>
            <Text style={s.settingText}>
              Crea etiquetas y colores para organizar tus recordatorios.
            </Text>
          </View>
        </View>
        <Button
          title="Administrar prioridades"
          variant="soft"
          icon="arrow-forward"
          onPress={() => router.push("/categories")}
        />
      </Card>
      <Button
        title="Cerrar sesión"
        variant="danger"
        icon="log-out-outline"
        onPress={() => void signOut()}
      />
    </Screen>
  );
}
const s = StyleSheet.create({
  hero: { alignItems: "center", gap: 7, paddingVertical: 8 },
  avatar: {
    width: 86,
    height: 86,
    borderRadius: 30,
    backgroundColor: colors.blue,
    alignItems: "center",
    justifyContent: "center",
  },
  initials: { fontSize: 29, fontWeight: "900", color: "white" },
  name: { fontSize: 21, fontWeight: "900", color: colors.navy, marginTop: 4 },
  email: { fontSize: 13, color: colors.muted },
  role: {
    marginTop: 4,
    paddingHorizontal: 11,
    paddingVertical: 6,
    borderRadius: 99,
    backgroundColor: colors.blueSoft,
    flexDirection: "row",
    gap: 6,
    alignItems: "center",
  },
  roleText: { fontSize: 12, fontWeight: "800", color: colors.blue },
  cardTitle: { fontSize: 17, fontWeight: "900", color: colors.navy },
  setting: { flexDirection: "row", alignItems: "center", gap: 12 },
  settingIcon: {
    width: 48,
    height: 48,
    borderRadius: 15,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  settingTitle: { fontSize: 15, fontWeight: "900", color: colors.navy },
  settingText: {
    fontSize: 12,
    lineHeight: 17,
    color: colors.muted,
    marginTop: 3,
  },
});
