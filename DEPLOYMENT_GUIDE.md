# Deployment Guide - TaskCollab

This guide deploys:

- Backend on Render
- Database on Neon
- Frontend on Vercel

## Prerequisites

- GitHub repository pushed and accessible from Render and Vercel
- Render account
- Neon account
- Vercel account

## Part 1: Create the Neon Database

1. Sign in to [Neon](https://neon.com/).
2. Create a new project.
3. Open the project's **Connect** dialog.
4. Copy these values:
   - Host
   - Database name
   - Username
   - Password
5. Build the JDBC URL for Spring Boot:

```text
jdbc:postgresql://<host>:5432/<database>?sslmode=require
```

If Neon shows a pooled host ending with `-pooler`, you can use that host in the same JDBC format.

## Part 2: Deploy the Backend on Render

### Option A: Use the included `render.yaml`

1. In Render, click **New** -> **Blueprint**.
2. Connect the `TaskCollab` repository.
3. Render will detect the root-level `render.yaml`.
4. When prompted, provide values for:
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `FRONTEND_URL`
5. Render will generate `JWT_SECRET` automatically from the Blueprint.

### Option B: Create the Web Service manually

1. In Render, click **New** -> **Web Service**.
2. Connect the `TaskCollab` repository.
3. Use these settings:
   - Name: `taskcollab-backend`
   - Runtime: `Docker`
   - Root Directory: `taskcollab-backend`
   - Dockerfile Path: `taskcollab-backend/Dockerfile`
   - Docker Context: `taskcollab-backend`
   - Instance Type: `Free`
   - Auto-Deploy: `Yes`
   - Health Check Path: `/test`

### Render environment variables

Set these variables on the backend service:

```text
DB_URL=jdbc:postgresql://<host>:5432/<database>?sslmode=require
DB_USERNAME=<neon-username>
DB_PASSWORD=<neon-password>
JWT_SECRET=<strong-random-secret>
FRONTEND_URL=https://<your-frontend>.vercel.app
```

### Notes

- Render web services must listen on `PORT`; the backend already does this.
- The backend can also read `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD`, but `DB_URL` is the simplest setup for Neon.
- The health endpoint is `GET /test`.

## Part 3: Deploy the Frontend on Vercel

1. In Vercel, click **Add New** -> **Project**.
2. Import the same GitHub repository.
3. Set the root directory to `taskcollab-frontend`.
4. Confirm these settings:
   - Framework: `Vite`
   - Build Command: `npm run build`
   - Output Directory: `dist`
5. Add this environment variable:

```text
VITE_API_URL=https://<your-render-service>.onrender.com
```

You usually do not need `VITE_WS_URL`. The frontend derives the WebSocket URL from `VITE_API_URL`.

## Part 4: Finish the Backend CORS Configuration

After the frontend deploy finishes:

1. Copy the Vercel production URL.
2. Go back to Render.
3. Update `FRONTEND_URL` on the backend service to match the Vercel URL exactly.
4. Redeploy the backend if Render does not redeploy automatically after the variable change.

## Part 5: GitHub Integration

This repository does not require GitHub Actions for deployment.

- Backend deploys are handled by Render's Git integration
- Frontend deploys are handled by Vercel's Git integration

Once Render and Vercel are connected to this repository, new pushes to `main` can deploy automatically from those platforms.

## Environment Variable Checklist

### Render backend

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `FRONTEND_URL`

### Vercel frontend

- `VITE_API_URL`

## Post-Deployment Testing

### Backend

Open:

```text
https://<your-render-service>.onrender.com/test
```

Expected response:

```text
Protected API Working
```

### Frontend

1. Open the Vercel URL.
2. Register or log in.
3. Create a board.
4. Create a task.
5. Confirm live updates still work between tabs or sessions.

## Troubleshooting

### Backend fails to start on Render

- Check that the root directory is `taskcollab-backend`
- Check that `DB_URL` uses the `jdbc:postgresql://...` format
- Check that the Neon password is correct
- Confirm `FRONTEND_URL` is a full `https://` URL

### Database connection fails

- Make sure the JDBC URL ends with `?sslmode=require`
- Recopy the Neon credentials from the Neon dashboard
- If using a pooled Neon host, keep the same JDBC format and use the pooled hostname

### Frontend cannot call the backend

- Verify `VITE_API_URL` points to the Render backend URL
- Verify `FRONTEND_URL` on Render exactly matches the Vercel production domain

### WebSocket connection fails

- Confirm the backend is live and reachable on Render
- Confirm the frontend is using the same Render base URL for API and WebSocket traffic
- Check browser developer tools for CORS or connection errors
