import { spawn } from "node:child_process";

const command = process.platform === "win32" ? "expo.cmd" : "expo";
const child = spawn(command, ["start", "--clear"], {
  stdio: "inherit",
  env: {
    ...process.env,
    EXPO_PUBLIC_API_URL:
      "https://5d1dvnzh90.execute-api.us-east-1.amazonaws.com",
    EXPO_PUBLIC_COGNITO_REGION: "us-east-1",
    EXPO_PUBLIC_COGNITO_CLIENT_ID: "21jjggihppq5ba7dsk1532eada",
  },
});

child.on("error", (error) => {
  console.error(`No se pudo iniciar Expo: ${error.message}`);
  process.exitCode = 1;
});

child.on("exit", (code, signal) => {
  if (signal) {
    console.error(`Expo terminó por la señal ${signal}.`);
    process.exitCode = 1;
    return;
  }
  process.exitCode = code ?? 1;
});
