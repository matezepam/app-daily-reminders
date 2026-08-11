import { useCallback, useEffect, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { StyleSheet, Text, View } from "react-native";
import { Button, Card, Chip, Header, Message, Screen, ui } from "@/src/components/Ui";
import { DateTimeSelector } from "@/src/components/DateTimeSelector";
import { useData } from "@/src/context/DataContext";
import { colors } from "@/src/theme";
import type { Attendance, AttendanceStatus } from "@/src/types/domain";
import type { UserProfile } from "@/src/types/auth";
const statuses: AttendanceStatus[] = ["PRESENT", "ABSENT", "LATE", "EXCUSED"];
const statusLabels: Record<AttendanceStatus, string> = {
  PRESENT: "Presente",
  ABSENT: "Ausente",
  LATE: "Atraso",
  EXCUSED: "Justificado",
};
const statusColors: Record<AttendanceStatus, string> = {
  PRESENT: colors.success,
  ABSENT: colors.danger,
  LATE: colors.warning,
  EXCUSED: colors.blue,
};
function localDate(value = new Date()) {
  const offset = value.getTimezoneOffset() * 60_000;
  return new Date(value.getTime() - offset).toISOString().slice(0, 10);
}
export default function AttendancePage() {
  const { courseId } = useLocalSearchParams<{ courseId: string }>();
  const id = Number(courseId);
  const { getAttendance, getCourseStudents, saveAttendance } = useData();
  const [items, setItems] = useState<Attendance[]>([]);
  const [students, setStudents] = useState<UserProfile[]>([]);
  const [student, setStudent] = useState("");
  const [date, setDate] = useState(new Date());
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
    if (isNaN(date.getTime()) || localDate(date) > localDate()) {
      setMsg("Selecciona una fecha válida que no sea futura.");
      return;
    }
    try {
      await saveAttendance(id, student.trim(), localDate(date), status);
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
        <DateTimeSelector
          value={date}
          onChange={setDate}
          maximumDate={new Date()}
          minimumDate={new Date(2020, 0, 1)}
          dateOnly
          label="Fecha de asistencia"
        />
        <Text style={ui.label}>Estado</Text>
        <View style={styles.statuses}>
          {statuses.map((x) => (
            <Chip
              key={x}
              label={statusLabels[x]}
              icon={x === status ? "checkmark-circle" : "ellipse-outline"}
              color={statusColors[x]}
              selected={x === status}
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
      {items.length ? (
        <View style={styles.summary}>
          <Text style={styles.summaryTitle}>Registros guardados</Text>
          <Text style={styles.summaryCount}>{items.length}</Text>
        </View>
      ) : null}
      {[...items].sort((left, right) => right.attendanceDate.localeCompare(left.attendanceDate)).map((a) => {
        const fullName = students.find((item) => item.cognitoSub === a.studentUserId)?.fullName ?? "Estudiante";
        const initials = fullName.split(" ").slice(0, 2).map((part) => part[0]).join("").toUpperCase();
        return (
        <Card key={a.id} style={styles.record}>
          <View style={[styles.avatar, { backgroundColor: `${statusColors[a.status]}18` }]}>
            <Text style={[styles.initials, { color: statusColors[a.status] }]}>{initials}</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.student}>{fullName}</Text>
            <Text style={styles.recordDate}>{new Date(`${a.attendanceDate}T12:00:00`).toLocaleDateString("es-EC", { dateStyle: "long" })}</Text>
          </View>
          <View style={[styles.badge, { backgroundColor: `${statusColors[a.status]}15` }]}>
            <Text style={[styles.badgeText, { color: statusColors[a.status] }]}>{statusLabels[a.status]}</Text>
          </View>
        </Card>
      );})}
    </Screen>
  );
}

const styles = StyleSheet.create({
  statuses: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  summary: { flexDirection: "row", alignItems: "center", justifyContent: "space-between" },
  summaryTitle: { color: colors.navy, fontSize: 18, fontWeight: "900" },
  summaryCount: { color: colors.blue, fontSize: 20, fontWeight: "900" },
  record: { flexDirection: "row", alignItems: "center", gap: 11 },
  avatar: { width: 48, height: 48, borderRadius: 16, alignItems: "center", justifyContent: "center" },
  initials: { fontSize: 15, fontWeight: "900" },
  student: { color: colors.navy, fontSize: 15, fontWeight: "900" },
  recordDate: { color: colors.muted, fontSize: 11, marginTop: 3 },
  badge: { borderRadius: 99, paddingHorizontal: 10, paddingVertical: 7 },
  badgeText: { fontSize: 11, fontWeight: "900" },
});
