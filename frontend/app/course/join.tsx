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
import { useData } from "@/src/context/DataContext";
import { colors } from "@/src/theme";
export default function Join() {
  const { joinCourse } = useData();
  const [code, setCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit() {
    if (!code.trim()) {
      setError("Ingresa el código de la clase.");
      return;
    }
    setBusy(true);
    try {
      await joinCourse(code);
      router.back();
    } catch (e) {
      setError(e instanceof Error ? e.message : "No se pudo unir.");
    } finally {
      setBusy(false);
    }
  }
  return (
    <Screen>
      <Header
        title="Unirme a una clase"
        subtitle="Solicita el código único a tu profesor"
        back={() => router.back()}
      />
      <View style={s.illustration}>
        <View style={s.circle}>
          <Ionicons name="people" size={44} color={colors.blue} />
        </View>
        <Text style={s.help}>
          Al unirte recibirás las actividades y recordatorios publicados por el
          profesor.
        </Text>
      </View>
      <Card>
        <Field
          label="Código de la clase"
          icon="key-outline"
          placeholder="Ej. ABC12345"
          value={code}
          autoCapitalize="characters"
          onChangeText={(v) => setCode(v.toUpperCase())}
          maxLength={20}
        />
        {error && <Message>{error}</Message>}
        <Button
          title="Unirme a la clase"
          icon="enter-outline"
          loading={busy}
          onPress={() => void submit()}
        />
      </Card>
    </Screen>
  );
}
const s = StyleSheet.create({
  illustration: { alignItems: "center", gap: 12, paddingVertical: 8 },
  circle: {
    width: 88,
    height: 88,
    borderRadius: 30,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  help: {
    maxWidth: 330,
    textAlign: "center",
    fontSize: 14,
    lineHeight: 21,
    color: colors.muted,
  },
});
