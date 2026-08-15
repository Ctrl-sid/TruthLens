# TruthLens - AI News Genuineness Verification Platform

TruthLens is a full-stack platform designed to measure the genuineness of news content submitted via **Text prompts**, **URLs**, or **Image uploads**. TruthLens performs NLP-driven entity extraction, sentiment & bias classification, sensationalism/clickbait detection, and cross-references reliable wire repositories (Reuters, AP News, Snopes, PolitiFact, BBC) to produce an aggregated genuineness rating percentage (0-100%) and transparent rationale.

---

## 🛠️ Technology Stack

- **Front-End**: React.js (Vite), HTML5, CSS3 (Glassmorphic Cyber Dark Theme), Bootstrap 5.
- **Back-End**: Spring Boot 3.x (Java 17), RESTful APIs, Spring Security with JWT Authentication.
- **Database**: PostgreSQL (with JSONB support for NLP metrics & evidence trees) / H2 in-memory fallback.
- **NLP Engine**: Integrated Java NLP Pipeline (Named Entity Recognition, Sentiment & Subjectivity Analyzer, TF-IDF + Cosine Similarity, Clickbait Classifier).

---

## 🚀 Directory Structure

```
truthlens/
├── backend/                  # Spring Boot (Java) REST API
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/truthlens/api/
│       │   ├── config/       # SecurityConfig, JWT Filters, CORS
│       │   ├── controller/   # Auth, FactCheck, Sources, History, NLP REST endpoints
│       │   ├── dto/          # Data Transfer Objects
│       │   ├── model/        # JPA Entities (User, VerifiedSource, FactCheckHistory)
│       │   ├── nlp/          # NER, Sentiment, TF-IDF, Clickbait NLP module
│       │   └── service/      # Fact-Check & OCR services
│       └── resources/        # application.yml & db/schema.sql
│
└── frontend/                 # React.js (Vite + Bootstrap 5) Application
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── components/       # Input workspace, Gauges, NLP breakdown, History, Auth
        ├── context/          # AuthContext for JWT management
        └── services/         # Axios API client & Verification services
```

---

## 💻 Running the Application

### 1. Back-End Setup (Spring Boot)

From `truthlens/backend`:
```bash
# Build the Spring Boot application
mvn clean package

# Run the API server (Runs on http://localhost:8080)
mvn spring-boot:run
```

#### PostgreSQL Connection (Optional Configuration):
Set environment variables in `application.yml` or your shell:
- `SPRING_DATASOURCE_URL`: `jdbc:postgresql://localhost:5432/truthlensdb`
- `SPRING_DATASOURCE_USERNAME`: `postgres`
- `SPRING_DATASOURCE_PASSWORD`: `yourpassword`

*(If PostgreSQL is not running, Spring Boot automatically falls back to an embedded H2 database with H2 Console at `http://localhost:8080/h2-console`)*

---

### 2. Front-End Setup (React.js)

From `truthlens/frontend`:
```bash
# Install dependencies
npm install

# Start development server (Runs on http://localhost:3000)
npm run dev
```

---

## 🔑 REST API Endpoints Summary

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register new user account | No |
| `POST` | `/api/auth/login` | Authenticate user & receive JWT | No |
| `POST` | `/api/verify/claim` | Verify claim via Text, URL, or Image | No |
| `GET` | `/api/sources` | Get list of accredited wire sources | No |
| `POST` | `/api/nlp/analyze` | Standalone NLP diagnostics endpoint | No |
| `GET` | `/api/history` | Retrieve user verification history | JWT Bearer Token |

---

## 🛡️ License & Copyright
Developed for **TruthLens Platform** — All rights reserved.
