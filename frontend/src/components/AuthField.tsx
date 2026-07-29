import { colors } from '@/src/theme';
import { Ionicons } from '@expo/vector-icons';
import type { ComponentProps } from 'react';
import {
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  View,
} from 'react-native';

type IconName = ComponentProps<typeof Ionicons>['name'];

type AuthFieldProps = TextInputProps & {
  error?: string;
  icon: IconName;
  label: string;
  onToggleSecure?: () => void;
};

export function AuthField({ error, icon, label, onToggleSecure, secureTextEntry, ...inputProps }: AuthFieldProps) {
  return (
    <View style={styles.group}>
      <Text style={styles.label}>{label}</Text>
      <View style={[styles.field, error ? styles.fieldError : undefined]}>
        <View style={styles.leadingIcon}>
          <Ionicons color={colors.blue} name={icon} size={21} />
        </View>
        <TextInput
          accessibilityLabel={label}
          placeholderTextColor={colors.placeholder}
          secureTextEntry={secureTextEntry}
          style={styles.input}
          {...inputProps}
        />
        {onToggleSecure ? (
          <Pressable
            accessibilityLabel={secureTextEntry ? 'Show password' : 'Hide password'}
            accessibilityRole="button"
            hitSlop={10}
            onPress={onToggleSecure}
            style={styles.trailingButton}
          >
            <Ionicons
              color={colors.muted}
              name={secureTextEntry ? 'eye-outline' : 'eye-off-outline'}
              size={22}
            />
          </Pressable>
        ) : null}
      </View>
      {error ? <Text style={styles.error}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  group: {
    gap: 8,
  },
  label: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '700',
  },
  field: {
    alignItems: 'center',
    backgroundColor: colors.white,
    borderColor: colors.line,
    borderRadius: 13,
    borderWidth: 1,
    flexDirection: 'row',
    minHeight: 56,
  },
  fieldError: {
    borderColor: colors.danger,
  },
  leadingIcon: {
    alignItems: 'center',
    backgroundColor: colors.bluePale,
    borderRadius: 9,
    height: 40,
    justifyContent: 'center',
    marginLeft: 8,
    width: 40,
  },
  input: {
    color: colors.text,
    flex: 1,
    fontSize: 16,
    minHeight: 54,
    paddingHorizontal: 14,
    paddingVertical: 0,
  },
  trailingButton: {
    alignItems: 'center',
    height: 48,
    justifyContent: 'center',
    marginRight: 4,
    width: 44,
  },
  error: {
    color: colors.danger,
    fontSize: 12,
    lineHeight: 17,
  },
});
