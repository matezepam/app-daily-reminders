import { useCallback, useEffect, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { Text, View } from "react-native";
import { Button, Card, Chip, Field, Header, Message, Screen, ui } from "@/src/components/Ui";
import { useData } from "@/src/context/DataContext";
import type { Attendance, AttendanceStatus } from "@/src/types/domain";
import type { UserProfile } from "@/src/types/auth";
const statuses: AttendanceStatus[] = ["PRESENT", "ABSENT", "LATE", "EXCUSED"];
const statusLabels: Record<AttendanceStatus, string> = {
  PRESENT: "Presente",
  ABSENT: "Ausente",
  LATE: "Atraso",
  EXCUSED: "Justificado",
};
function localDate() {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}
export default function AttendancePage() {
  const { courseId } = useLocalSearchParams<{ courseId: string }>();
  const id = Number(courseId);
  const { getAttendance, getCourseStudents, saveAttendance } = useData();
  const [items, setItems] = useState<Attendance[]>([]);
  const [students, setStudents] = useState<UserProfile[]>([]);
  const [student, setStudent] = useState("");
  const [date, setDate] = useState(localDate());
  const [status, setStatus] = useState<AttendanceStatus>("PRESENT");
  const [msg, setMsg] = useState("");
  const load = useCallback(async () => {
    try {
      const [attendance, enrolled] = await Promise.all([
        getAttendance(id),
        getCourseStudents(id),
      ]);
      setItems(attendance);
      setStudents(enrolled);
      setMsg("");
    } catch (e) {
      setMsg(e instanceof Error ? e.message : "Error");
    }
  }, [getAttendance, getCourseStudents, id]);
  useEffect(() => {
    void load();
  }, [load]);
  async function save() {
    if (!student.trim()) {
      setMsg("Selecciona un estudiante inscrito.");
      return;
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || date > localDate()) {
      setMsg("Escribe una fecha válida que no sea futura.");
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
        <Text style={ui.label}>Estudiante inscrito</Text>
        {students.length ? (
          <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 8 }}>
            {students.map((item) => (
              <Chip
                key={item.cognitoSub}
                label={item.fullName}
                selected={student === item.cognitoSub}
                onPress={() => setStudent(item.cognitoSub)}
              />
            ))}
          </View>
        ) : (
          <Message type="info">Este curso todavía no tiene estudiantes inscritos.</Message>
        )}
        <Field label="Fecha (AAAA-MM-DD)" value={date} onChangeText={setDate} />
        <Text style={ui.label}>Estado</Text>
        <View style={{ gap: 7 }}>
          {statuses.map((x) => (
            <Button
              key={x}
              title={statusLabels[x]}
              icon={x === status ? "checkmark" : "ellipse-outline"}
              onPress={() => setStatus(x)}
            />
          ))}
        </View>
        <Button
          title="Guardar asistencia"
          icon="save"
          disabled={!students.length}
          onPress={() => void save()}
        />
        {msg && <Text style={ui.subtitle}>{msg}</Text>}
      </Card>
      {items.map((a) => (
        <Card key={a.id}>
          <Text style={ui.label}>
            {students.find((item) => item.cognitoSub === a.studentUserId)?.fullName ?? "Estudiante"}
          </Text>
          <Text style={ui.subtitle}>
            {a.attendanceDate} · {statusLabels[a.status]}
          </Text>
        </Card>
      ))}
    </Screen>
  );
}
