# Real-Time Chat App - Backend

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)

A robust backend service for a secure, real-time chat platform. This project provides seamless bidirectional communication, user management, and strict security standards using modern backend technologies.

## 🚀 Key Features

* **Real-Time Communication:** Low-latency, bidirectional messaging powered by **WebSockets**.
* **Secure Authentication:** Integration with **OAuth 2.0** for safe and seamless user login (e.g., Google/GitHub).
* **Email Verification:** Automated email verification system during user registration to prevent spam and fake accounts.
* **Push Notifications:** Reliable notification system alerting users of new messages and events.
* **RESTful API:** Clean and well-documented API endpoints for user management and chat history retrieval.

## 🛠️ Tech Stack

* **Framework:** Spring Boot (Java)
* **Real-Time:** WebSockets (STOMP protocol)
* **Security:** Spring Security, OAuth 2.0, JWT (JSON Web Tokens)
* **Database:** MySQL / Spring Data JPA (Hibernate)
* **Mailing:** Spring Boot Starter Mail

## ⚙️ Prerequisites

Before you begin, ensure you have met the following requirements:
* **Java 17** (or higher) installed.
* **Maven** installed for dependency management.
* **MySQL** server running locally or via Docker.
* SMTP server credentials (for email verification).
* OAuth 2.0 credentials (Client ID & Client Secret from Google/GitHub).

## 🚀 Getting Started

### 1. Clone the repository
\`\`\`bash
git clone https://github.com/moraveco/ChatBackend.git
cd ChatBackend
\`\`\`

### 2. Configure the application
Navigate to `src/main/resources/application.properties` (or `application.yml`) and update the following environment variables:

\`\`\`properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/chat_db
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD

# Mail Configuration (SMTP)
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_EMAIL_PASSWORD

# OAuth2 Configuration
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
\`\`\`

### 3. Build and Run
Build the project using Maven:
\`\`\`bash
mvn clean install
\`\`\`

Run the application:
\`\`\`bash
mvn spring-boot:run
\`\`\`
The application will start on `http://localhost:8080`.

## 👨‍💻 Author

**Ondřej Moravec** * GitHub: [@moraveco](https://github.com/moraveco)
* LinkedIn: [Ondřej Moravec] *(Add your LinkedIn link here if you have one)*
