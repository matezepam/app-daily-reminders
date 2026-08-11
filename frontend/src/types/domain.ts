export type ReminderType =
  | "TASK"
  | "EXAM"
  | "PROJECT"
  | "PRESENTATION"
  | "OTHER";
export type ReminderPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type ReminderStatus = "PENDING" | "COMPLETED" | "EXPIRED";
export type AttendanceStatus = "PRESENT" | "ABSENT" | "LATE" | "EXCUSED";

export type Course = {
  id: number;
  name: string;
  description?: string | null;
  joinCode: string;
  professorUserId: string;
  memberCount: number;
  createdAt: string;
  ownedByMe: boolean;
};
export type PriorityCategory = {
  id: number;
  name: string;
  color: string;
  sortOrder: number;
};
export type ReminderNotification = {
  id: number;
  reminderId: number;
  notifyAt: string;
  sent: boolean;
  cancelled: boolean;
};
export type Reminder = {
  id: number;
  courseId?: number | null;
  courseName?: string | null;
  personal: boolean;
  title: string;
  description?: string | null;
  type: ReminderType;
  dueAt: string;
  priority: ReminderPriority;
  customPriority?: PriorityCategory | null;
  status: ReminderStatus;
  editable: boolean;
  createdAt: string;
  notifications: ReminderNotification[];
};
export type ReminderInput = {
  title: string;
  description?: string;
  type: ReminderType;
  dueAt: string;
  priority: ReminderPriority;
  priorityCategoryId?: number | null;
  notificationOffsetsMinutes: number[];
};
export type Dashboard = {
  pendingToday: number;
  upcoming: number;
  completedThisMonth: number;
  reminders: Reminder[];
};
export type Activity = {
  id: number;
  activityNumber: number;
  courseId: number;
  title: string;
  description?: string | null;
  dueAt: string;
  createdByUserId: string;
  completed: boolean;
  completedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};
export type ActivityInput = {
  title: string;
  description?: string;
  dueAt: string;
};
export type Attendance = {
  id: number;
  courseId: number;
  studentUserId: string;
  attendanceDate: string;
  status: AttendanceStatus;
  recordedByUserId: string;
  createdAt: string;
  updatedAt: string;
};
