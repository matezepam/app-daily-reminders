import { colors } from '@/src/theme';
import { Ionicons } from '@expo/vector-icons';
import { StyleSheet, Text, View } from 'react-native';

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <View accessibilityLabel="Daily Reminder" style={styles.container}>
      <View style={[styles.mark, compact && styles.compactMark]}>
        <Ionicons color={colors.white} name="school" size={compact ? 27 : 34} />
        <View style={styles.calendarBadge}>
          <Ionicons color={colors.blue} name="checkmark" size={17} />
        </View>
      </View>
      <View>
        <Text style={[styles.daily, compact && styles.compactTitle]}>Daily</Text>
        <Text style={[styles.reminder, compact && styles.compactTitle]}>Reminder</Text>
        {!compact && <Text style={styles.tagline}>Academic reminders</Text>}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 14,
  },
  mark: {
    alignItems: 'center',
    backgroundColor: colors.navy,
    borderRadius: 18,
    height: 68,
    justifyContent: 'center',
    width: 68,
  },
  compactMark: {
    borderRadius: 14,
    height: 52,
    width: 52,
  },
  calendarBadge: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: colors.bluePale,
    borderRadius: 10,
    borderWidth: 2,
    bottom: -5,
    height: 27,
    justifyContent: 'center',
    position: 'absolute',
    right: -5,
    width: 27,
  },
  daily: {
    color: colors.navy,
    fontSize: 28,
    fontWeight: '800',
    letterSpacing: -0.7,
    lineHeight: 29,
  },
  reminder: {
    color: colors.blue,
    fontSize: 28,
    fontWeight: '800',
    letterSpacing: -0.7,
    lineHeight: 29,
  },
  compactTitle: {
    fontSize: 21,
    lineHeight: 21,
  },
  tagline: {
    color: colors.muted,
    fontSize: 12,
    marginTop: 4,
  },
});
