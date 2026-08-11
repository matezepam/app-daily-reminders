import { Ionicons } from "@expo/vector-icons";
import type { PropsWithChildren, ReactNode } from "react";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  View,
  type ViewStyle,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { colors, radius, shadow } from "@/src/theme";

export function Screen({
  children,
  refreshing,
  onRefresh,
  contentStyle,
}: PropsWithChildren<{
  refreshing?: boolean;
  onRefresh?: () => void;
  contentStyle?: ViewStyle;
}>) {
  return (
    <SafeAreaView style={s.safe} edges={["top"]}>
      <KeyboardAvoidingView
        style={{ flex: 1 }}
        behavior={Platform.OS === "ios" ? "padding" : undefined}
      >
        <ScrollView
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          refreshControl={
            onRefresh ? (
              <RefreshControl
                refreshing={!!refreshing}
                onRefresh={onRefresh}
                tintColor={colors.blue}
              />
            ) : undefined
          }
          contentContainerStyle={[s.page, contentStyle]}
        >
          {children}
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
export function Header({
  title,
  subtitle,
  back,
  right,
}: {
  title: string;
  subtitle?: string;
  back?: () => void;
  right?: ReactNode;
}) {
  return (
    <View style={s.header}>
      {back && (
        <Pressable
          accessibilityLabel="Volver"
          accessibilityRole="button"
          hitSlop={8}
          onPress={back}
          style={s.back}
        >
          <Ionicons name="chevron-back" size={23} color={colors.navy} />
        </Pressable>
      )}
      <View style={{ flex: 1 }}>
        <Text style={s.title}>{title}</Text>
        {subtitle && <Text style={s.subtitle}>{subtitle}</Text>}
      </View>
      {right}
    </View>
  );
}
export function Card({
  children,
  style,
}: PropsWithChildren<{ style?: ViewStyle }>) {
  return <View style={[s.card, style]}>{children}</View>;
}
export function Field({
  label,
  icon,
  error,
  ...props
}: TextInputProps & {
  label?: string;
  icon?: keyof typeof Ionicons.glyphMap;
  error?: string;
}) {
  return (
    <View style={s.fieldWrap}>
      {label && <Text style={s.label}>{label}</Text>}
      <View
        style={[s.field, error && s.fieldError, props.multiline && s.multiline]}
      >
        {icon && (
          <Ionicons
            name={icon}
            size={20}
            color={error ? colors.danger : colors.blue}
          />
        )}
        <TextInput
          placeholderTextColor={colors.placeholder}
          style={[s.input, props.multiline && s.inputMultiline]}
          {...props}
        />
      </View>
      {error && <Text style={s.error}>{error}</Text>}
    </View>
  );
}
export function Button({
  title,
  onPress,
  icon = "arrow-forward",
  variant = "primary",
  loading = false,
  disabled = false,
  compact = false,
}: {
  title: string;
  onPress: () => void;
  icon?: keyof typeof Ionicons.glyphMap;
  variant?: "primary" | "secondary" | "soft" | "danger" | "ghost";
  loading?: boolean;
  disabled?: boolean;
  compact?: boolean;
}) {
  const color = variant === "danger" ? colors.danger : colors.blue;
  const filled = variant === "primary" || variant === "danger";
  return (
    <Pressable
      accessibilityRole="button"
      disabled={loading || disabled}
      onPress={onPress}
      style={({ pressed }) => [
        s.button,
        compact && s.compact,
        filled
          ? { backgroundColor: color }
          : {
              backgroundColor:
                variant === "soft" ? colors.blueSoft : "transparent",
              borderWidth: variant === "ghost" ? 0 : 1,
              borderColor: color,
            },
        (pressed || disabled) && { opacity: 0.62 },
      ]}
    >
      {loading ? (
        <ActivityIndicator color={filled ? "white" : color} />
      ) : (
        <>
          <Ionicons
            name={icon}
            color={filled ? "white" : color}
            size={compact ? 18 : 20}
          />
          <Text
            style={[
              s.buttonText,
              !filled && { color },
              compact && { fontSize: 14 },
            ]}
          >
            {title}
          </Text>
        </>
      )}
    </Pressable>
  );
}
export function Chip({
  label,
  selected,
  onPress,
  color = colors.blue,
  icon,
}: {
  label: string;
  selected?: boolean;
  onPress: () => void;
  color?: string;
  icon?: keyof typeof Ionicons.glyphMap;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={[
        s.chip,
        selected && { borderColor: color, backgroundColor: `${color}14` },
      ]}
    >
      {icon && (
        <Ionicons
          name={icon}
          color={selected ? color : colors.muted}
          size={16}
        />
      )}
      <Text style={[s.chipText, selected && { color }]}>{label}</Text>
    </Pressable>
  );
}
export function Empty({
  title,
  detail,
  icon = "calendar-outline",
}: {
  title: string;
  detail: string;
  icon?: keyof typeof Ionicons.glyphMap;
}) {
  return (
    <Card style={s.empty}>
      <View style={s.emptyIcon}>
        <Ionicons name={icon} size={31} color={colors.blue} />
      </View>
      <Text style={s.emptyTitle}>{title}</Text>
      <Text style={[s.subtitle, { textAlign: "center" }]}>{detail}</Text>
    </Card>
  );
}
export function SectionTitle({
  children,
  action,
}: PropsWithChildren<{ action?: ReactNode }>) {
  return (
    <View style={s.sectionRow}>
      <Text style={s.section}>{children}</Text>
      {action}
    </View>
  );
}
export function Message({
  children,
  type = "error",
}: PropsWithChildren<{ type?: "error" | "success" | "info" }>) {
  const palette =
    type === "success"
      ? colors.success
      : type === "info"
        ? colors.blue
        : colors.danger;
  return (
    <View style={[s.message, { backgroundColor: `${palette}12` }]}>
      <Ionicons
        name={type === "success" ? "checkmark-circle" : "information-circle"}
        color={palette}
        size={20}
      />
      <Text style={[s.messageText, { color: palette }]}>{children}</Text>
    </View>
  );
}
const s = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  page: {
    width: "100%",
    maxWidth: 720,
    alignSelf: "center",
    paddingHorizontal: 20,
    paddingTop: 14,
    paddingBottom: 108,
    gap: 18,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    minHeight: 58,
  },
  back: {
    width: 44,
    height: 44,
    borderRadius: 15,
    backgroundColor: "white",
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: colors.lineSoft,
    ...shadow,
  },
  title: {
    fontSize: 28,
    fontWeight: "900",
    letterSpacing: -0.6,
    color: colors.navy,
  },
  subtitle: { fontSize: 14, lineHeight: 20, color: colors.muted, marginTop: 3 },
  card: {
    backgroundColor: colors.surface,
    borderRadius: radius.md,
    padding: 18,
    gap: 14,
    borderWidth: 1,
    borderColor: colors.lineSoft,
    ...shadow,
  },
  fieldWrap: { gap: 7 },
  label: { fontSize: 14, fontWeight: "800", color: colors.text },
  field: {
    minHeight: 54,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
    borderWidth: 1,
    borderColor: "#C9D7E8",
    borderRadius: radius.sm,
    paddingHorizontal: 14,
    backgroundColor: "#FBFDFF",
  },
  fieldError: {
    borderColor: colors.danger,
    backgroundColor: colors.dangerPale,
  },
  multiline: { alignItems: "flex-start", minHeight: 104, paddingTop: 14 },
  input: { flex: 1, color: colors.text, fontSize: 16, paddingVertical: 13 },
  inputMultiline: {
    minHeight: 76,
    textAlignVertical: "top",
    paddingVertical: 0,
  },
  error: { color: colors.danger, fontSize: 12 },
  button: {
    minHeight: 54,
    borderRadius: radius.sm,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 9,
    paddingHorizontal: 18,
  },
  compact: { minHeight: 42, paddingHorizontal: 13 },
  buttonText: { color: "white", fontWeight: "900", fontSize: 16 },
  chip: {
    minHeight: 42,
    paddingHorizontal: 13,
    borderRadius: radius.pill,
    borderWidth: 1,
    borderColor: colors.line,
    backgroundColor: "white",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
  },
  chipText: { color: colors.muted, fontWeight: "700", fontSize: 13 },
  empty: { alignItems: "center", paddingVertical: 28 },
  emptyIcon: {
    width: 64,
    height: 64,
    borderRadius: 22,
    backgroundColor: colors.blueSoft,
    alignItems: "center",
    justifyContent: "center",
  },
  emptyTitle: { fontSize: 18, fontWeight: "900", color: colors.navy },
  sectionRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  section: { fontSize: 19, fontWeight: "900", color: colors.navy },
  message: {
    borderRadius: 12,
    padding: 12,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  messageText: { flex: 1, fontSize: 13, lineHeight: 18, fontWeight: "600" },
});
export const ui = s;
