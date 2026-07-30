import { colors } from '@/src/theme';
import { Ionicons } from '@expo/vector-icons';
import { Tabs } from 'expo-router';

export default function TabsLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: colors.blue,
        tabBarInactiveTintColor: colors.muted,
        tabBarLabelStyle: { fontSize: 12, fontWeight: '700' },
        tabBarStyle: { borderTopColor: colors.lineSoft, height: 66, paddingBottom: 8, paddingTop: 7 },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          tabBarIcon: ({ color, size }) => <Ionicons color={color} name="home-outline" size={size} />,
          title: 'Inicio',
        }}
      />
      <Tabs.Screen
        name="courses"
        options={{
          tabBarIcon: ({ color, size }) => <Ionicons color={color} name="albums-outline" size={size} />,
          title: 'Cursos',
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          tabBarIcon: ({ color, size }) => <Ionicons color={color} name="person-outline" size={size} />,
          title: 'Perfil',
        }}
      />
    </Tabs>
  );
}
