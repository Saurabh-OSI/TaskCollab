# TaskCollab - Real-Time Collaborative Task Management System

TaskCollab is a full-stack collaborative task management app with boards, lists, tasks, authentication, and live updates over WebSockets.

## Features

1. JWT-based signup and login
2. Create and manage boards
3. Invite and manage board members
4. Organize work into task lists
5. Create, move, assign, and delete tasks
6. Search tasks by title, list, and assignee
7. View real-time updates with STOMP over WebSockets
8. Track activity across boards

## Tech Stack

### Backend

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- PostgreSQL
- WebSocket + STOMP
- Maven

### Frontend

- React 19
- Vite
- Tailwind CSS
- Axios
- React Router
- @hello-pangea/dnd
- @stomp/stompjs

## Project Structure

```text
taskcollab-backend/
  src/main/java/com/saurabh/taskcollab/
  src/main/resources/
  Dockerfile
  pom.xml

taskcollab-frontend/
  src/
  vercel.json
  package.json

.github/workflows/
render.yaml
DEPLOYMENT_GUIDE.md
```

## Local Development

### Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 18+
- PostgreSQL 12+

### Database

Create a local database:

```sql
CREATE DATABASE taskcollab;
```

### Backend

From `taskcollab-backend`, configure a local `.env` file from `.env.example`, then run:

```bash
mvn clean install
mvn spring-boot:run
```

The backend runs on `http://localhost:8080`.

### Frontend

From `taskcollab-frontend`, configure `.env` from `.env.example`, then run:

```bash
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`.

## Environment Variables

### Backend

- `DB_URL`: Full JDBC PostgreSQL URL. Example: `jdbc:postgresql://localhost:5432/taskcollab`
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `DB_URL_PARAMS`: Optional JDBC suffix such as `?sslmode=require`
- `JWT_SECRET`: JWT signing secret
- `FRONTEND_URL`: Allowed frontend origin for CORS and WebSocket handshakes

The backend also supports `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` for provider-based setups.

### Frontend

- `VITE_API_URL`: Backend base URL
- `VITE_WS_URL`: Optional explicit WebSocket URL. If omitted, the app derives it from `VITE_API_URL`

## Deployment

TaskCollab is now set up for:

- Render for the backend
- Neon for PostgreSQL
- Vercel for the frontend

The repository includes:

- `taskcollab-backend/Dockerfile` for the backend image
- `render.yaml` for Render Blueprint setup
- `taskcollab-frontend/vercel.json` for the frontend

See `DEPLOYMENT_GUIDE.md` for the full deployment walkthrough.

## Quick Checks

Local backend health endpoint:

```bash
curl http://localhost:8080/test
```

Expected response:

```text
Protected API Working
```
