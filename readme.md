# TaskCollab – Real-Time Collaborative Task Management System

TaskCollab is a **full-stack real-time task management (Kanban-style) web application**.
It allows users to create boards, manage tasks in lists, collaborate with team members, and receive **live updates using WebSockets**.

## 🎯 Features

1. User Authentication (JWT-based Login & Signup)
2. Create & Manage Boards
3. Add/Remove Board Members
4. Create Lists (To Do, In Progress, Done)
5. Create, Move, Delete Tasks
6. Drag & Drop Support
7. Assign/Unassign Users to Tasks
8. Search Tasks (by title, list, assignee)
9. Real-Time Updates (WebSocket + STOMP)
10. Activity Logs (who did what)


## 🛠️ Tech Stack

### Backend

* Java 17
* Spring Boot 3.5.10
* Spring Security
* JWT Authentication
* Spring Data JPA (Hibernate)
* PostgreSQL
* WebSocket (STOMP Protocol)
* Lombok
* Maven

### Frontend

* React 19
* Vite
* Tailwind CSS
* Axios
* React Router
* @hello-pangea/dnd (Drag & Drop)
* @stomp/stompjs (WebSocket)

## 🚀 Deployment

TaskCollab is configured for easy deployment on **Railway** (Backend) and **Vercel** (Frontend).

### Quick Start

1. **[Read the Complete Deployment Guide](./DEPLOYMENT_GUIDE.md)** - Step-by-step instructions for both platforms
2. **Backend**: Push to Railway with PostgreSQL integration
3. **Frontend**: Deploy React app to Vercel with one click

### What's Included

- ✅ `Dockerfile` - Multi-stage Docker build for optimized backend
- ✅ `railway.json` - Railway deployment configuration  
- ✅ `vercel.json` - Vercel frontend configuration
- ✅ `.env.example` - Environment variable templates
- ✅ GitHub Actions workflow - Automated CI/CD pipeline

### Environment Variables

**Backend:**
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - PostgreSQL credentials
- `JWT_SECRET` - JWT signing secret
- `FRONTEND_URL` - Frontend URL for CORS

**Frontend:**
- `VITE_API_URL` - Backend API URL

See [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) for detailed setup instructions.



## 📋 Prerequisites

Make sure you have installed:

* Java 17+
* Maven 3.9+
* Node.js 18+ 
* PostgreSQL 12+
* Git


## 🗄️ Database Setup

1. Open PostgreSQL
2. Create database:

```sql
CREATE DATABASE taskcollab;
```

3. Update credentials in `taskcollab-backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskcollab
spring.datasource.username=your_username
spring.datasource.password=your_password
```



## 🚀 Backend Setup (Spring Boot)

```bash
cd taskcollab-backend
mvn clean install
mvn spring-boot:run
```

Backend will run on: `http://localhost:8080`


## ⚛️ Frontend Setup (React + Vite)

```bash
cd taskcollab-frontend
npm install
npm run dev
```

Frontend will run on: `http://localhost:5173`

## 🔐 Environment Variables

### Frontend

Copy `.env.example` to `.env` in `taskcollab-frontend/`:

```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```

### Backend

Copy `.env.example` to `.env` in `taskcollab-backend/` (optional for local dev)


## 🔑 Authentication Flow

1. User registers with email and password
2. Logs in → receives JWT token
3. Token stored in `localStorage`
4. Token automatically attached in API requests
5. Unauthorized → auto logout


## 📡 WebSocket Flow

Connects to `/ws` endpoint and subscribes to:

* `/topic/boards/{user}` - User's boards
* `/topic/board/{id}` - Specific board updates
* `/topic/list/{id}` - List updates  
* `/topic/task/{id}` - Task updates

Enables real-time sync of:

* Boards
* Lists
* Tasks
* Activity Logs


## ✅ Testing the Backend

Test endpoint:

```bash
curl http://localhost:8080/test
```

Expected response:

```
Protected API Working
```


## 📁 Project Structure

### Backend

```
taskcollab-backend/
├── src/main/java/com/saurabh/taskcollab/
│   ├── config/              # Security, WebSocket, JWT config
│   ├── controller/          # REST API endpoints
│   ├── dto/                 # Data Transfer Objects
│   ├── entity/              # JPA entities
│   ├── exception/           # Custom exceptions
│   ├── repository/          # Data access layer
│   ├── service/             # Business logic
│   └── TaskcollabApplication.java
├── pom.xml
├── Dockerfile              # Docker build configuration
├── railway.json            # Railway deployment config
└── .env.example
```

### Frontend

```
taskcollab-frontend/
├── src/
│   ├── components/         # React components
│   ├── services/           # API & WebSocket services
│   ├── App.jsx
│   └── main.jsx
├── package.json
├── vite.config.js
├── vercel.json             # Vercel deployment config
└── .env.example
```

---

## 🚀 Deployment

TaskCollab is configured for easy deployment on **Railway** (Backend) and **Vercel** (Frontend).

### Quick Start

1. **[Read the Complete Deployment Guide](./DEPLOYMENT_GUIDE.md)** - Step-by-step instructions for both platforms
2. **Backend**: Push to Railway with PostgreSQL integration
3. **Frontend**: Deploy React app to Vercel with one click

### What's Included

- ✅ `Dockerfile` - Multi-stage Docker build for optimized backend
- ✅ `railway.json` - Railway deployment configuration  
- ✅ `vercel.json` - Vercel frontend configuration
- ✅ `.env.example` - Environment variable templates
- ✅ GitHub Actions workflow - Automated CI/CD pipeline

### Environment Variables

**Backend:**
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` - PostgreSQL credentials
- `JWT_SECRET` - JWT signing secret
- `FRONTEND_URL` - Frontend URL for CORS

**Frontend:**
- `VITE_API_URL` - Backend API URL

See [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) for detailed setup instructions.

---

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 👤 Author

**Saurabh**
- GitHub: [@Saurabh-OSI](https://github.com/Saurabh-OSI/TaskCollab)

## 📧 Support

For issues, questions, or suggestions, please open an issue on GitHub or contact the maintainer.
exception/
repository/
service/
```

## Frontend

```
components/
services/
```

---

## How to Use

1. Register a new user
2. Login
3. Create a board
4. Add members
5. Create lists
6. Add tasks
7. Drag & drop tasks
8. Assign users
9. See real-time updates


## Common Issues

#  Backend not starting

* Check DB credentials
* Check port 8080 availability

# WebSocket not connecting

* Ensure backend is running
* Check `.env` WS URL

# Token issues

* Clear localStorage and login again


#  Contribution

Feel free to fork and improve the project!


## Author ##

**Saurabh Kumar Singh**



