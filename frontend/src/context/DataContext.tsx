import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { apiRequest } from "@/src/api/client";
import { useAuth } from "@/src/context/AuthContext";
import type {
  Activity,
  ActivityInput,
  Attendance,
  AttendanceStatus,
  Course,
  Dashboard,
  PriorityCategory,
  Reminder,
  ReminderInput,
} from "@/src/types/domain";

const emptyDashboard: Dashboard = {
  pendingToday: 0,
  upcoming: 0,
  completedThisMonth: 0,
  reminders: [],
};
type DataValue = {
  courses: Course[];
  categories: PriorityCategory[];
  dashboard: Dashboard;
  loading: boolean;
  error: string;
  refresh: () => Promise<void>;
  createCourse: (n: string, d?: string) => Promise<Course>;
  joinCourse: (c: string) => Promise<Course>;
  deleteCourse: (id: number) => Promise<void>;
  getActivities: (id: number) => Promise<Activity[]>;
  saveActivity: (
    courseId: number,
    input: ActivityInput,
    id?: number,
  ) => Promise<Activity>;
  completeActivity: (id: number) => Promise<Activity>;
  deleteActivity: (id: number) => Promise<void>;
  saveReminder: (
    input: ReminderInput,
    courseId?: number,
    id?: number,
  ) => Promise<Reminder>;
  completeReminder: (id: number) => Promise<Reminder>;
  deleteReminder: (id: number) => Promise<void>;
  createCategory: (name: string, color: string) => Promise<PriorityCategory>;
  deleteCategory: (id: number) => Promise<void>;
  getAttendance: (courseId: number) => Promise<Attendance[]>;
  saveAttendance: (
    courseId: number,
    studentUserId: string,
    attendanceDate: string,
    status: AttendanceStatus,
  ) => Promise<Attendance>;
};
const Context = createContext<DataValue | null>(null);

export function DataProvider({ children }: PropsWithChildren) {
  const { session, getSession } = useAuth();
  const [courses, setCourses] = useState<Course[]>([]);
  const [categories, setCategories] = useState<PriorityCategory[]>([]);
  const [dashboard, setDashboard] = useState(emptyDashboard);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const call = useCallback(
    async <T,>(path: string, options?: { method?: string; body?: unknown }) =>
      apiRequest<T>(await getSession(), path, options),
    [getSession],
  );
  const refresh = useCallback(async () => {
    if (!session) return;
    setLoading(true);
    try {
      const [d, c, p] = await Promise.all([
        call<Dashboard>("/academic-reminder/dashboard"),
        call<Course[]>("/academic-reminder/courses/me"),
        session.profile.role === "USER"
          ? call<PriorityCategory[]>("/academic-reminder/priority-categories")
          : Promise.resolve([]),
      ]);
      setDashboard(d);
      setCourses(c);
      setCategories(p);
      setError("");
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "No se pudieron cargar los datos.",
      );
    } finally {
      setLoading(false);
    }
  }, [call, session]);
  useEffect(() => {
    void refresh();
  }, [refresh]);
  const mutate = useCallback(
    async <T,>(path: string, method: string, body?: unknown) => {
      const result = await call<T>(path, { method, body });
      await refresh();
      return result;
    },
    [call, refresh],
  );
  const value = useMemo<DataValue>(
    () => ({
      courses,
      categories,
      dashboard,
      loading,
      error,
      refresh,
      createCourse: (name, description) =>
        mutate("/academic-reminder/courses", "POST", { name, description }),
      joinCourse: (code) =>
        mutate("/academic-reminder/courses/join", "POST", {
          code: code.trim().toUpperCase(),
        }),
      deleteCourse: (id) =>
        mutate(`/academic-reminder/courses/${id}`, "DELETE"),
      getActivities: (id) =>
        call(`/academic-reminder/courses/${id}/activities`),
      saveActivity: (courseId, input, id) =>
        mutate(
          id
            ? `/academic-reminder/activities/${id}`
            : `/academic-reminder/courses/${courseId}/activities`,
          id ? "PUT" : "POST",
          input,
        ),
      completeActivity: (id) =>
        mutate(`/academic-reminder/activities/${id}/complete`, "PATCH"),
      deleteActivity: (id) =>
        mutate(`/academic-reminder/activities/${id}`, "DELETE"),
      saveReminder: (input, courseId, id) =>
        mutate(
          id
            ? `/academic-reminder/reminders/${id}`
            : courseId
              ? `/academic-reminder/courses/${courseId}/reminders`
              : "/academic-reminder/reminders",
          id ? "PUT" : "POST",
          input,
        ),
      completeReminder: (id) =>
        mutate(`/academic-reminder/reminders/${id}/complete`, "PATCH"),
      deleteReminder: (id) =>
        mutate(`/academic-reminder/reminders/${id}`, "DELETE"),
      createCategory: (name, color) =>
        mutate("/academic-reminder/priority-categories", "POST", {
          name,
          color,
          sortOrder: categories.length,
        }),
      deleteCategory: (id) =>
        mutate(`/academic-reminder/priority-categories/${id}`, "DELETE"),
      getAttendance: (id) =>
        call(`/academic-reminder/courses/${id}/attendance`),
      saveAttendance: (id, studentUserId, attendanceDate, status) =>
        mutate(`/academic-reminder/courses/${id}/attendance`, "PUT", {
          studentUserId,
          attendanceDate,
          status,
        }),
    }),
    [courses, categories, dashboard, loading, error, refresh, mutate, call],
  );
  return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function useData() {
  const value = useContext(Context);
  if (!value) throw new Error("useData must be used inside DataProvider");
  return value;
}
