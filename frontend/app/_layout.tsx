import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import {
  ActivityIndicator,
  Platform,
  StyleSheet,
  Text,
  useWindowDimensions,
  View,
} from "react-native";
import { SafeAreaProvider } from "react-native-safe-area-context";

import { AuthProvider, useAuth } from "@/src/context/AuthContext";
import { DataProvider } from "@/src/context/DataContext";
import { colors } from "@/src/theme";

function RootNavigator() {
  const { initializing, session } = useAuth();

  if (initializing) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator color={colors.blue} size="large" />
        <Text style={styles.loadingText}>Restaurando tu sesión...</Text>
      </View>
    );
  }

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Protected guard={!session}>
        <Stack.Screen name="index" />
        <Stack.Screen name="register" />
        <Stack.Screen name="confirm" />
        <Stack.Screen name="forgot-password" />
      </Stack.Protected>
      <Stack.Protected guard={Boolean(session)}>
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="course/join" />
        <Stack.Screen name="course/new" />
        <Stack.Screen name="course/[id]" />
        <Stack.Screen name="reminder/new" />
        <Stack.Screen name="reminder/[id]" />
        <Stack.Screen name="activity/new" />
        <Stack.Screen name="categories" />
        <Stack.Screen name="attendance/[courseId]" />
        <Stack.Screen name="notifications" />
      </Stack.Protected>
    </Stack>
  );
}

export default function RootLayout() {
  const { width, height } = useWindowDimensions();
  const showPhoneFrame = Platform.OS === "web" && width >= 700;

  return (
    <SafeAreaProvider>
      <View style={styles.stage}>
        <View
          style={[
            styles.app,
            showPhoneFrame && styles.phone,
            showPhoneFrame && { height: Math.min(874, height - 24) },
          ]}
        >
          {showPhoneFrame && (
            <View pointerEvents="none" style={styles.island} />
          )}
          <View style={[styles.content, showPhoneFrame && styles.phoneContent]}>
            <AuthProvider>
              <DataProvider>
                <StatusBar style="dark" />
                <RootNavigator />
              </DataProvider>
            </AuthProvider>
          </View>
          {showPhoneFrame && (
            <View pointerEvents="none" style={styles.homeIndicator} />
          )}
        </View>
      </View>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  stage: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#DCE6F2",
  },
  app: {
    width: "100%",
    height: "100%",
    backgroundColor: colors.background,
  },
  phone: {
    width: 420,
    borderRadius: 46,
    overflow: "hidden",
    borderWidth: 8,
    borderColor: "#111827",
    shadowColor: "#081426",
    shadowOpacity: 0.28,
    shadowRadius: 28,
    shadowOffset: { width: 0, height: 14 },
    elevation: 18,
  },
  content: { flex: 1 },
  phoneContent: { paddingTop: 24, paddingBottom: 8 },
  island: {
    position: "absolute",
    zIndex: 20,
    top: 9,
    alignSelf: "center",
    width: 112,
    height: 27,
    borderRadius: 18,
    backgroundColor: "#05070B",
  },
  homeIndicator: {
    position: "absolute",
    zIndex: 20,
    bottom: 5,
    alignSelf: "center",
    width: 126,
    height: 5,
    borderRadius: 999,
    backgroundColor: "#111827",
    opacity: 0.82,
  },
  loading: {
    alignItems: "center",
    backgroundColor: colors.background,
    flex: 1,
    gap: 12,
    justifyContent: "center",
  },
  loadingText: { color: colors.muted, fontSize: 14 },
});
