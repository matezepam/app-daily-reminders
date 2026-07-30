import { RoleBadge } from '@/src/components/RoleBadge';
import { useAuth } from '@/src/context/AuthContext';
import { colors, shadow } from '@/src/theme';
import { Ionicons } from '@expo/vector-icons';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function ProfileScreen() {
  const { busy, session, signOut } = useAuth();

  return (
    <SafeAreaView style={styles.safeArea}>
      <View style={styles.page}>
        <Text style={styles.title}>Mi perfil</Text>
        <View style={styles.card}>
          <View style={styles.avatar}>
            <Ionicons color={colors.blue} name="person-outline" size={38} />
          </View>
          <Text style={styles.email}>{session?.email}</Text>
          <RoleBadge roles={session?.roles} />
          <Pressable
            disabled={busy}
            onPress={() => void signOut()}
            style={({ pressed }) => [styles.logout, pressed && { opacity: 0.76 }, busy && { opacity: 0.55 }]}
          >
            {busy ? <ActivityIndicator color={colors.danger} /> : <Ionicons color={colors.danger} name="log-out-outline" size={21} />}
            <Text style={styles.logoutText}>Cerrar sesión</Text>
          </Pressable>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: { backgroundColor: colors.background, flex: 1 },
  page: { alignSelf: 'center', gap: 22, maxWidth: 620, padding: 22, width: '100%' },
  title: { color: colors.navy, fontSize: 28, fontWeight: '800' },
  card: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: colors.lineSoft,
    borderRadius: 24,
    borderWidth: 1,
    gap: 16,
    padding: 24,
    ...shadow,
  },
  avatar: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderRadius: 34,
    height: 68,
    justifyContent: 'center',
    width: 68,
  },
  email: { color: colors.navy, fontSize: 17, fontWeight: '800' },
  logout: {
    alignItems: 'center',
    borderColor: '#F7B7B0',
    borderRadius: 14,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 9,
    justifyContent: 'center',
    marginTop: 8,
    minHeight: 52,
    width: '100%',
  },
  logoutText: { color: colors.danger, fontSize: 15, fontWeight: '800' },
});
