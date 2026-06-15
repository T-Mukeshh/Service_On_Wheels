# 🚗 Service on Wheels

A full-stack roadside assistance platform built using **Angular, Spring Boot, MongoDB, and JWT Authentication**.

Service on Wheels enables users to request roadside assistance, share their location, manage service requests, and track the progress of their requests through a secure and modern web application.

---

## 📌 Overview

Service on Wheels is designed to help users quickly request roadside assistance during vehicle breakdowns or emergencies.

The platform provides:

* Secure user authentication
* Roadside assistance request management
* Interactive map-based location selection
* Request tracking and status monitoring
* User profile management
* Password recovery functionality

The application follows a modern full-stack architecture with Angular on the frontend, Spring Boot on the backend, and MongoDB as the database.

---

## ✨ Features

### Authentication & Security

* User Registration
* Secure Login
* JWT Authentication
* Route Protection
* BCrypt Password Encryption
* Forgot Password Workflow

### Dashboard

* Personalized User Dashboard
* Request Statistics
* Quick Access Navigation
* Account Overview

### Roadside Assistance Requests

Users can:

* Select vehicle type
* Enter vehicle details
* Choose roadside issues
* Add additional notes
* Share GPS location
* Select location using an interactive map
* Submit service requests

### Location Services

* OpenStreetMap Integration
* Leaflet Maps
* GPS Location Detection
* Reverse Geocoding
* Coordinate-Based Request Submission

### Request Management

* View Service Request History
* Track Request Status
* Manage Active Requests
* Request Lifecycle Monitoring

### User Profile

* View Account Information
* Account Status Display
* Profile Dashboard

---

## Tech Stack

### Frontend

* Angular
* TypeScript
* HTML5
* CSS3
* Leaflet Maps

### Backend

* Java 21
* Spring Boot
* Spring Security
* JWT Authentication
* REST APIs
* Maven

### Database

* MongoDB

### Tools

* Git
* GitHub
* VS Code
* MongoDB Compass

---

## System Architecture

```text
Angular Frontend
        │
        ▼
REST APIs + JWT Authentication
        │
        ▼
Spring Boot Backend
        │
        ▼
MongoDB Database
```bash
git clone https://github.com/PhoenixX18/ServiceOnWheels1.git
```

---

### Backend Setup

```bash
cd backend/auth-service/auth-service
mvn spring-boot:run
```

Backend URL:

```text
http://localhost:8081
```

---

### Frontend Setup

```bash
cd customer-app
npm install
ng serve
```

Frontend URL:

```text
http://localhost:4200
```

---

## Configuration

Configure the following values before running:

```properties
MONGODB_URI=
JWT_SECRET=
FRONTEND_RESET_URL=
MAIL_USERNAME=
MAIL_PASSWORD=
```

`JWT_SECRET` must be a cryptographically random secret of at least 32 bytes. Use
`.env.example` as a reference, but inject real values through the deployment
platform or shell environment. Spring Boot does not automatically load `.env`.

Never commit:

* .env files
* Application secrets
* JWT secrets
* SMTP credentials

---

## 🚀 Roadmap

Planned Enhancements:

* Mechanic Tracking System
* Mechanic Portal
* Role-Based Access Control
* Real-Time Status Updates
* Refresh Token Authentication
* Unit & Integration Testing
* Docker Containerization
* CI/CD Pipeline
* Cloud Deployment

---

## 🎯 Learning Objectives

This project was developed to strengthen practical experience in:

* Full-Stack Development
* REST API Design
* Authentication & Authorization
* Angular Development
* Spring Boot Development
* MongoDB Integration
* Secure Application Design
* Location-Based Services

---


## Project by
**T Mukesh**

GitHub: https://github.com/PhoenixX18
