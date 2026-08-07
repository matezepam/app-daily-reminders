export const colors = {
  navy: "#082B63",
  blue: "#1769E0",
  blueDark: "#074CB6",
  blueSoft: "#EAF2FF",
  sky: "#D9E9FF",
  text: "#17365F",
  muted: "#66809F",
  placeholder: "#91A2B8",
  line: "#DCE6F3",
  lineSoft: "#EAF0F8",
  background: "#F4F8FD",
  surface: "#FFFFFF",
  white: "#FFFFFF",
  success: "#149653",
  successPale: "#E9F8F0",
  warning: "#E58A00",
  warningPale: "#FFF5DD",
  danger: "#D83A31",
  dangerPale: "#FFF0EF",
  purple: "#7857E5",
  bluePale: "#EAF2FF",
  blueSoftLegacy: "#BFD8FF",
  waveLight: "#C2DAFF",
};
export const radius = { xs: 10, sm: 14, md: 19, lg: 26, pill: 999 };
export const shadow = {
  elevation: 3,
  shadowColor: "#153B6F",
  shadowOffset: { width: 0, height: 8 },
  shadowOpacity: 0.08,
  shadowRadius: 18,
};
export const priorityMeta = {
  LOW: { label: "Baja", color: colors.success, background: colors.successPale },
  MEDIUM: {
    label: "Media",
    color: colors.warning,
    background: colors.warningPale,
  },
  HIGH: { label: "Alta", color: "#E35B19", background: "#FFF0E8" },
  URGENT: {
    label: "Urgente",
    color: colors.danger,
    background: colors.dangerPale,
  },
} as const;
export const typeMeta = {
  TASK: { label: "Tarea", icon: "book-outline" },
  EXAM: { label: "Examen", icon: "document-text-outline" },
  PROJECT: { label: "Proyecto", icon: "folder-open-outline" },
  PRESENTATION: { label: "Exposición", icon: "easel-outline" },
  OTHER: { label: "Otro", icon: "ellipsis-horizontal-circle-outline" },
} as const;
