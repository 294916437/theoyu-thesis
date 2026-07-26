import React from "react";
import {SafeAreaView, StatusBar, StyleSheet, Text, View} from "react-native";

function App(): React.JSX.Element {
  return (
    <SafeAreaView style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor="#111827" />
      <View style={styles.content}>
        <Text style={styles.title}>BlueSky Hybrid</Text>
        <Text style={styles.subtitle}>React Native runtime is ready.</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#111827",
  },
  content: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 24,
  },
  title: {
    color: "white",
    fontSize: 24,
    fontWeight: "800",
  },
  subtitle: {
    color: "#d1d5db",
    marginTop: 8,
    fontSize: 14,
  },
});

export default App;
