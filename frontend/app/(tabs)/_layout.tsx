import { Ionicons } from "@expo/vector-icons";
import { Tabs } from "expo-router";
import { Platform } from "react-native";
import { colors, shadow } from "@/src/theme";

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.blue,
        tabBarInactiveTintColor: "#8295AD",
        tabBarHideOnKeyboard: true,
        tabBarLabelStyle: { fontSize: 11, fontWeight: "800", marginTop: 2 },
        tabBarItemStyle: { paddingTop: 6 },
        tabBarStyle: {
          position: "absolute",
          height: Platform.OS === "ios" ? 86 : 72,
          paddingBottom: Platform.OS === "ios" ? 22 : 9,
          paddingTop: 5,
          marginHorizontal: 12,
          marginBottom: 10,
          borderRadius: 22,
          borderTopWidth: 0,
          backgroundColor: "white",
          ...shadow,
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "Inicio",
          tabBarIcon: ({ color, focused }) => (
            <Ionicons
              color={color}
              name={focused ? "home" : "home-outline"}
              size={24}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="calendar"
        options={{
          title: "Agenda",
          tabBarIcon: ({ color, focused }) => (
            <Ionicons
              color={color}
              name={focused ? "calendar" : "calendar-outline"}
              size={24}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="courses"
        options={{
          title: "Cursos",
          tabBarIcon: ({ color, focused }) => (
            <Ionicons
              color={color}
              name={focused ? "school" : "school-outline"}
              size={24}
            />
          ),
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: "Perfil",
          tabBarIcon: ({ color, focused }) => (
            <Ionicons
              color={color}
              name={focused ? "person" : "person-outline"}
              size={24}
            />
          ),
        }}
      />
    </Tabs>
  );
}
