/* eslint-disable react-hooks/exhaustive-deps */
import { useEffect, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { Text, View } from "react-native";
import { Button, Card, Field, Header, Screen, ui } from "@/src/components/Ui";
import { useData } from "@/src/context/DataContext";
import type { Attendance, AttendanceStatus } from "@/src/types/domain";
const statuses: AttendanceStatus[] = ["PRESENT", "ABSENT", "LATE", "EXCUSED"];
export default function AttendancePage() {
  const { courseId } = useLocalSearchParams<{ courseId: string }>();
  const id = Number(courseId);
  const { getAttendance, saveAttendance } = useData();
  const [items, setItems] = useState<Attendance[]>([]);
  const [student, setStudent] = useState("");
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [status, setStatus] = useState<AttendanceStatus>("PRESENT");
  const [msg, setMsg] = useState("");
  async function load() {
    try {
      setItems(await getAttendance(id));
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "Error");
    }
  }
  useEffect(() => {
    void load();
  }, [id]);
  async function save() {
    if (!student.trim()) {
      setMsg("Ingresa el cognitoSub del estudiante.");
      return;
    }
    try {
      await saveAttendance(id, student.trim(), date, status);
      setStudent("");
      await load();
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "No se pudo guardar.");
    }
  }
  return (
    <Screen>
      <Header
        title="Asistencia"
        subtitle="Registro por estudiante y fecha"
        back={() => router.back()}
      />
      <Card>
        <Field
          label="Cognito sub del estudiante"
          value={student}
          onChangeText={setStudent}
        />
        <Field label="Fecha (AAAA-MM-DD)" value={date} onChangeText={setDate} />
        <Text style={ui.label}>Estado</Text>
        <View style={{ gap: 7 }}>
          {statuses.map((x) => (
            <Button
              key={x}
              title={x}
              icon={x === status ? "checkmark" : "ellipse-outline"}
              onPress={() => setStatus(x)}
            />
          ))}
        </View>
        <Button
          title="Guardar asistencia"
          icon="save"
          onPress={() => void save()}
        />
        {msg && <Text style={ui.subtitle}>{msg}</Text>}
      </Card>
      {items.map((a) => (
        <Card key={a.id}>
          <Text style={ui.label}>{a.studentUserId}</Text>
          <Text style={ui.subtitle}>
            {a.attendanceDate} · {a.status}
          </Text>
        </Card>
      ))}
    </Screen>
  );
}
