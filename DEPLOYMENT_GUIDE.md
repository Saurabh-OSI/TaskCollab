# Deployment Guide - TaskCollab

Complete guide for deploying TaskCollab on Railway (Backend) and Vercel (Frontend).

## Prerequisites

- [Railway Account](https://railway.app) (free tier available)
- [Vercel Account](https://vercel.com) (free tier available)
- GitHub account with your repository pushed
- PostgreSQL database (Railway provides this)

---

## Part 1: Railway Backend Deployment

### Step 1: Create PostgreSQL Database on Railway

1. Go to [railway.app](https://railway.app)
2. Click **"New Project"** → **"Provision PostgreSQL"**
3. Wait for the database to be created
4. Click on the PostgreSQL service to view credentials
5. Note down these environment variables:
   - `DATABASE_URL` (or construct from: host, port, username, password, database name)

### Step 2: Deploy Backend Service

1. In Railway dashboard, click **"New"** → **"GitHub Repo"**
2. Select your TaskCollab repository
3. Select the `taskcollab-backend` directory as the root
4. Railway will auto-detect it's a Java project

### Step 3: Configure Environment Variables for Backend

After service creation, go to **Variables** and add:

```
DB_URL=postgresql://<username>:<password>@<host>:<port>/<database_name>
DB_USERNAME=<postgres_username>
DB_PASSWORD=<postgres_password>
JWT_SECRET=YourV3ryStr0ngJWTSecretKey123!@#$%^&*()_+
FRONTEND_URL=https://yourfrontend.vercel.app
```

**Important:** 
- Replace values with actual database credentials from PostgreSQL service
- Generate a strong JWT secret (use a random 32+ character string)
- Update `FRONTEND_URL` after deploying frontend

### Step 4: Update Spring Boot Configuration

The application.properties already supports environment variables. Railway will automatically set port, but ensure the file reads from env:

```properties
server.port=${PORT:8080}
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/taskcollab}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:password}
app.jwt.secret=${JWT_SECRET:DefaultSecretForLocal}
app.frontend.url=${FRONTEND_URL:http://localhost:5173}
```

Your `application.properties` is already configured correctly! ✅

### Step 5: Deploy Backend

1. The deployment should start automatically
2. Watch the **Deploy** tab for build logs
3. Once successful, you'll get a URL like: `https://taskcollab-backend-prod.up.railway.app`
4. Note this URL - you'll need it for frontend configuration

---

## Part 2: Vercel Frontend Deployment

### Step 1: Connect Repository to Vercel

1. Go to [vercel.com](https://vercel.com)
2. Click **"Add New..."** → **"Project"**
3. Import your GitHub repository
4. Select `taskcollab-frontend` directory as root

### Step 2: Configure Build Settings

Vercel should auto-detect:
- **Framework**: Vite
- **Build Command**: `npm run build`
- **Output Directory**: `dist`
- **Install Command**: `npm install`

### Step 3: Set Environment Variables

In Vercel project settings, go to **Environment Variables** and add:

```
VITE_API_URL=https://your-backend-railway-url.railway.app
```

Use the Railway backend URL from Step 1, Part 1.

### Step 4: Automatic Deployment (via GitHub Actions)

The GitHub Actions workflow will automatically deploy your frontend to Vercel when you push to `main`. 

To enable this, add Vercel secrets to GitHub:
- Go to your GitHub repo → **Settings** → **Secrets and variables** → **Actions**
- Add these secrets:
  - `VERCEL_TOKEN` - [Get from Vercel Account Settings](https://vercel.com/account/tokens)
  - `VERCEL_ORG_ID` - [Get from Vercel Team Settings](https://vercel.com/teams)
  - `VERCEL_PROJECT_ID` - [Get from Vercel Project Settings](https://vercel.com/dashboard)

### Step 5: Deploy

**Automatic (Recommended):**
1. Just commit and push to `main`
2. GitHub Actions will automatically deploy to Vercel
3. Watch the deployment in **GitHub Actions** tab

**Manual:**
1. Click **Deploy** in Vercel dashboard
2. Wait for build to complete
3. You'll get a URL like: `https://taskcollab-frontend.vercel.app`

### Step 6: Update Backend FRONTEND_URL

1. Go back to Railway backend project
2. Update the `FRONTEND_URL` variable to: `https://your-frontend-vercel-url.vercel.app`

---

## Part 3: Setting Up GitHub Secrets for Automated CI/CD

This is required for automatic deployment via GitHub Actions.

### Step 1: Get Railway Token

1. Go to [railway.app](https://railway.app)
2. Click your **Account** (top-left)
3. Go to **Tokens**
4. Click **"New"** and copy the token

### Step 2: Get Vercel Credentials

1. Go to [vercel.com](https://vercel.com)
2. **Vercel Token**: Account Settings → Tokens → Create
3. **Vercel Org ID**: Team Settings → General → Copy the ID
4. **Vercel Project ID**: Project Settings → General → Copy the ID

### Step 3: Add Secrets to GitHub

1. Go to your GitHub repo → **Settings** → **Secrets and variables** → **Actions**
2. Click **"New repository secret"** and add:

| Secret Name | Value |
|------------|-------|
| `RAILWAY_TOKEN` | Your Railway token |
| `VERCEL_TOKEN` | Your Vercel token |
| `VERCEL_ORG_ID` | Your Vercel Org ID |
| `VERCEL_PROJECT_ID` | Your Vercel Project ID |

### Step 4: Enable Automatic Deployment

Now every time you push to `main`:
- ✅ Backend automatically deploys to Railway
- ✅ Frontend automatically deploys to Vercel
- ✅ Watch progress in **GitHub Actions** tab

---

## Part 4: Connecting Frontend to Backend

Your frontend already uses the `VITE_API_URL` environment variable in `src/services/api.js`:

```javascript
const API = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080",
});
```

When deployed:
- **Vercel**: Frontend will use `VITE_API_URL` pointing to Railway backend
- **Railway**: Backend will use `FRONTEND_URL` for CORS configuration

---

## Environment Variables Checklist

### Railway Backend Variables
- [ ] `DB_URL` - PostgreSQL connection string
- [ ] `DB_USERNAME` - Postgres username
- [ ] `DB_PASSWORD` - Postgres password
- [ ] `JWT_SECRET` - Strong random secret (min 32 chars)
- [ ] `FRONTEND_URL` - Vercel frontend URL

### Vercel Frontend Variables
- [ ] `VITE_API_URL` - Railway backend URL

### GitHub Secrets (for CI/CD)
- [ ] `RAILWAY_TOKEN` - Railway authentication token
- [ ] `VERCEL_TOKEN` - Vercel authentication token
- [ ] `VERCEL_ORG_ID` - Vercel organization ID
- [ ] `VERCEL_PROJECT_ID` - Vercel project ID

---

## Troubleshooting

### Backend Issues

**Build fails on Railway:**
- Check Java version (requires Java 17)
- Verify pom.xml is valid
- Check if Dockerfile is in the correct directory

**Connection to database fails:**
- Verify `DB_URL` is correct format
- Ensure PostgreSQL service is running on Railway
- Check username and password in environment variables

**CORS errors:**
- Verify `FRONTEND_URL` is set in Railway backend
- Update Spring Security/CORS configuration if needed

### Frontend Issues

**API calls fail:**
- Verify `VITE_API_URL` is set in Vercel
- Check network tab in browser DevTools
- Ensure backend is running and accessible

**Blank page or routing issues:**
- Vercel configuration already includes rewrite rule for SPA routing
- Check browser console for errors
- Verify JavaScript bundle loaded successfully

---

## Post-Deployment Testing

### Test Backend

```bash
curl -X GET https://your-backend-railway-url/api/health
```

### Test Frontend

1. Open your Vercel frontend URL
2. Try to register/login
3. Test creating a board and task
4. Check WebSocket connection in browser DevTools

---

## Rolling Back

**Railway**: Click the previous deployment in the "Deploy" tab
**Vercel**: Click "Deployments" tab and select previous version

---

## Next Steps

- ✅ Set up GitHub Actions CI/CD (automatic deployment on every push)
- [ ] Configure custom domain for both services
- [ ] Set up monitoring and logging
- [ ] Implement automated backups for PostgreSQL
- [ ] Set up staging environment for testing

---

## 🎉 Summary: Automated Deployment Workflow

After completing all steps, your workflow becomes:

```
You make changes locally
         ↓
    git push origin main
         ↓
GitHub Actions triggers automatically
         ↓
    Railway Backend Deploys  +  Vercel Frontend Deploys
         ↓
    Your app is live!
```

**No manual deployments needed!** Just push your code and everything deploys automatically. ✨
