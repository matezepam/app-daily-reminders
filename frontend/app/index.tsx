import { StyleSheet, Text, View } from 'react-native';

export default function HomeScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>Daily Reminder</Text>
      <Text style={styles.subtitle}>Proyecto inicial</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    alignItems: 'center',
    backgroundColor: '#F7FAFF',
    flex: 1,
    justifyContent: 'center',
    padding: 24,
  },
  title: {
    color: '#0B2F66',
    fontSize: 32,
    fontWeight: '700',
  },
  subtitle: {
    color: '#5D6F89',
    fontSize: 18,
    marginTop: 8,
  },
});
