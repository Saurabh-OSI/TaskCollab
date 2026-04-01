#  TaskCollab – Real-Time Collaborative Task Management System


TaskCollab is a **full-stack real-time task management (Kanban-style) web application**.
It allows users to create boards, manage tasks in lists, collaborate with team members, and receive **live updates using WebSockets**.

# Features

1.User Authentication (JWT-based Login & Signup)
2.Create & Manage Boards
3.Add/Remove Board Members
4.Create Lists (To Do, In Progress, Done)
5.Create, Move, Delete Tasks
6.Drag & Drop Support
7.Assign/Unassign Users to Tasks
8.Search Tasks (by title, list, assignee)
9.Real-Time Updates (WebSocket + STOMP)
10.Activity Logs (who did what)



##  Tech Stack

# Backend

* Java
* Spring Boot
* Spring Security
* JWT Authentication
* Spring Data JPA (Hibernate)
* PostgreSQL

# Frontend

* React (Vite)
* Tailwind CSS
* Axios

# Realtime

* WebSocket (STOMP Protocol)

# Tools

* Maven
* Lombok
* Git & GitHub
* Postman (API Testing)



# Prerequisites

Make sure you have installed:

* Java 17+
* Maven
* Node.js (v18+ recommended)
* PostgreSQL


# Database Setup

1. Open PostgreSQL
2. Create database:

```sql
CREATE DATABASE taskcollab;
```

3. Update credentials in:

```
backend/src/main/resources/application.properties
```

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskcollab
spring.datasource.username=your_username
spring.datasource.password=your_password
```



# Backend Setup (Spring Boot)

```bash
cd taskcollab
mvn clean install
mvn spring-boot:run
```

Backend will run on:

```
http://localhost:8080
```


# Frontend Setup (React)

```bash
cd taskcollab-frontend
npm install
npm run dev
```

Frontend will run on:

```
http://localhost:5173
```

#  Environment Variables (Frontend)

Create `.env` file:

```env
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
```


#  Authentication Flow

1. User registers
2. Logs in → receives JWT token
3. Token stored in `localStorage`
4. Token automatically attached in API requests
5. Unauthorized → auto logout


#  WebSocket Flow

* Connects to `/ws`
* Subscribes to:

  * `/topic/boards/{user}`
  * `/topic/board/{id}`
  * `/topic/list/{id}`
* Enables real-time sync of:

  * Boards
  * Lists
  * Tasks
  * Activity


## Test API

```
GET /test
```

Response:

```
Protected API Working
```


## Project Structure

# Backend

```
config/
controller/
dto/
entity/
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



