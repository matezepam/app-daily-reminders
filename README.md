# Daily Reminder

Estructura inicial del proyecto Daily Reminder.

## Tecnologías

- React Native con Expo y TypeScript
- Kotlin con Spring Boot y Gradle
- PostgreSQL
- Docker Compose

## Estructura

```text
app-daily-reminders/
├── backend/
│   ├── gradle/
│   └── src/
├── frontend/
│   ├── app/
│   ├── assets/
│   └── src/
├── .env.example
├── .gitignore
└── compose.yml
```

## Ejecución

```powershell
docker compose up --build -d
```

Frontend: http://localhost:3000

Backend: http://localhost:8080

Estado del backend: http://localhost:8080/actuator/health

## Desarrollo

```powershell
cd frontend
npm install
npm run web
```

```powershell
cd backend
.\gradlew.bat bootRun
```
