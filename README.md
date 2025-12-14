# NL2SQL Web Application — Spring Boot Java Backend

[![Java](https://img.shields.io/badge/java-17%2B-blue.svg)](https://adoptopenjdk.net/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.2.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

---

## Table of Contents
- [About](#about)
- [Architecture Overview](#architecture-overview)
- [Project Structure](#project-structure)
- [Logic & System Design](#logic--system-design)
- [Tech Stack](#tech-stack)
- [Setup and Installation](#setup-and-installation)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Usage Flow](#usage-flow)
- [Frontend Integration](#frontend-integration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

---

## About

**NL2SQL Web Application - Spring Boot Backend** is a robust, production-grade Java backend for translating natural language queries into SQL via a high-performance pre-existing C++ engine (`nl2sql.exe`). This application provides a secure, API-driven bridge between your NLP/AI-powered SQL compiler and any frontend interface.

This backend replaces the former Python+FastAPI backend and upholds strict software engineering standards for enterprise portability, maintainability, and extensibility.

---

## Architecture Overview

```
+-------------+    HTTP/API    +---------------------+     stdin/stdout     +--------------+
|  Web Frontend|  <--------->  | Spring Boot Backend  |  <--------------->  | C++ nl2sql   |
|(JS, React, etc.)             |  (this project)      |     (nl2sql.exe)    |   Engine     |
+-------------+                +---------------------+                      +--------------+
```

Key architectural features:
- **Stateless REST APIs**: Well-documented JSON endpoints
- **Process Bridge**: Secure integration with C++ executable
- **Pluggable**: Ready for enterprise extension (DB, security, Docker, more)
- **Error Handling**: API-level and process-level fault tolerance
- **Portable/Isolated**: All binaries and code are self-contained

---

## Project Structure

```
springboot-backend/
├── pom.xml                          # Maven build configuration (dependencies, build, plugins)
├── README.md                        # (This file)
├── src/
│   └── main/
│       ├── java/com/nl2sqlwebapp/   # All Java code
│       │   ├── controller/          # REST endpoints
│       │   ├── model/               # API models (DTOs)
│       │   └── service/             # External process invocation, business logic
│       └── resources/
│           └── application.properties # Config (executable path, port, etc.)
└── nl2sql-exe/                      # C++ executable and dependencies
    ├── nl2sql.exe
    ├── libcrypto-1_1-x64.dll
    ├── libssl-1_1-x64.dll
    ├── mysqlcppconn-9-vs14.dll
    └── mysqlcppconn8-2-vs14.dll
```

---

## Logic & System Design

**1. API Layer (Controller):**
- Offers `/connect`, `/query`, and `/status` endpoints using Spring Boot's REST.
- Handles HTTP requests from the UI or automation clients.
- Returns structured, well-typed JSON objects for ease of use and error handling.

**2. Service Layer (Nl2SqlService):**
- Bridges Java and the C++ NLP executable using `ProcessBuilder`.
- **/connect**: Invokes the executable with DB parameters, writes to its stdin to perform a handshake, and parses stdout for response/status.
- **/query**: Submits a natural language query to the executable, collects multi-line stdout, and parses it using regular expressions and line section logic to fill a type-safe POJO (`QueryResponse`).
- Automatically distinguishes between successful runs, failure codes, and timeouts, ensuring the frontend always receives a meaningful response.

**3. Parsing & Data Structuring:**
- Output from the C++ process (which includes SQL analyses, result tables, and other metadata) is split by line.
- Regular expressions and field/section markers extract analysis, detected intent, base table, SQL, tabular results, row counts, and execution time.
- Java collections are used for stable and safe representation of rows and metadata, even when the result schema changes query-to-query.

**4. Error Handling:**
- Every interaction with the underlying C++ process is wrapped in robust try/catch blocks and response wrappers with clear `success` status and meaningful error messages.
- API endpoints are CORS-enabled for web integration and can be hardened for enterprise security (future).

**5. Standalone Operation:**
- All required resources for operation (binaries, config, logic, build specs) are within the repository/project root. No code pointers to any outside directories or legacy projects.

---

## Tech Stack

- **Java 17+** — Core language for modern language/syntax support
- **Spring Boot 3.2.x** — Microservice framework (provides REST APIs, inversion of control, config, testing, etc.)
- **Maven** — Standardized dependency management and build tool (see `pom.xml`)
- **Lombok** — Boilerplate code reduction in models and services (e.g., `@Data`, `@AllArgsConstructor`)
- **ProcessBuilder API** — Under-the-hood Java library for robust process execution and I/O piping (communication with C++ engine)
- **JSON (Jackson)** — Serialization/de-serialization of API payloads and Java objects
- **C++ Executable (`nl2sql.exe`)** — Handles Natural Language Processing (NLP), Intent Detection, SQL Generation, and DB querying
- **Native DLLs** — MySQL and OpenSSL support for the C++ engine on Windows

**Why this stack?**
- Spring Boot is globally adopted for high-performance, scalable, enterprise-tier APIs.
- ProcessBuilder enables easy integration with native C++/Python/other ML/AI binaries.
- Lombok and Jackson ensure concise, readable Java for maintainable DTOs and services.
- The architecture allows you to future-proof for Docker/Kubernetes, cloud migration, and additional endpoints or engines.

---

## Setup and Installation

### Prerequisites
- **Java 17+** — [Download here](https://adoptium.net/)
- **Maven 3.8+** — [Install guide](https://maven.apache.org/install.html)
- **C++ Artifacts:** All DLLs and `nl2sql.exe` in `nl2sql-exe/`

### 1. Clone Repository
```sh
git clone <your-repo-url>
cd springboot-backend
```

### 2. Verify Executable and DLLs
Ensure `springboot-backend/nl2sql-exe/` contains:
- nl2sql.exe
- libcrypto-1_1-x64.dll
- libssl-1_1-x64.dll
- mysqlcppconn-9-vs14.dll
- mysqlcppconn8-2-vs14.dll

### 3. Build Project
```sh
mvn clean install
```

### 4. Run the Server
```sh
java -jar target/springboot-backend-1.0-SNAPSHOT.jar
```
Visit [http://localhost:8000/](http://localhost:8000/)

---

## Configuration

Edit `src/main/resources/application.properties`:
```properties
nl2sql.exe.path=nl2sql-exe/nl2sql.exe
server.port=8000
```
- Adjust `server.port` if port 8000 is unavailable.
- **Security:** For production, configure CORS, authentication, and secure network boundaries.

---

## API Reference

### POST `/connect`
Establish a test database connection using the C++ backend.
- **Request:**
```json
{
  "host": "localhost",
  "user": "root",
  "password": "example",
  "database": "dbname",
  "port": "3306"
}
```
- **Response:**
```json
{
  "success": true,
  "message": "Connected to dbname on localhost"
}
```

### POST `/query`
Submit a natural language query for interpretation and SQL execution.
- **Request:**
```json
{
  "query": "List all employees from Pune"
}
```
- **Response:** (fields detailed)
```json
{
  "success": true,
  "queryAnalysis": "...",
  "detectedIntent": "...",
  "baseTable": "...",
  "selectColumns": ["..."],
  "whereConditions": "...",
  "generatedSql": "...",
  "resultRows": [ {"col": "val"} ],
  "rowsReturned": 5,
  "executionTime": "...",
  "errorMessage": null
}
```

### GET `/status`
Retrieve the current backend and process status.
- **Response:**
```json
{
  "connected": true,
  "connectionInfo": {"host": "localhost", ...},
  "nl2sqlExeFound": true
}
```

---

## Usage Flow

1. **Start the Backend:**
   - Build if needed (`mvn clean install`)
   - Run (`java -jar ...`)
2. **Connect Database:**
   - Use `/connect` with your DB info.
3. **Query with Natural Language:**
   - Use `/query` to translate NLQ → SQL and get structured results.
4. **Monitor Status:**
   - Use `/status` to check connection and executable readiness.
5. **Integrate with Frontend:**
   - Point your frontend API calls to this backend (default `localhost:8000`)

---

## Frontend Integration

This backend is fully compatible with any web frontend (React, Vue, Angular, Vanilla JS, etc.) that can make HTTP requests:
- Point your AJAX/REST calls (e.g., Axios, fetch) to the corresponding `/connect`, `/query`, `/status` endpoints.
- Use HTTPS and proper CORS setup in production environments.

---

## Troubleshooting

| Problem                         | Solution                                                          |
|---------------------------------|-------------------------------------------------------------------|
| Executable not found            | Ensure accurate `nl2sql.exe.path`. Check file permissions.         |
| Port conflict                   | Modify `server.port` in `application.properties`.                  |
| Java/Maven missing              | [Install Java](https://adoptium.net/) / [Install Maven](https://maven.apache.org/) |
| Dependency errors (Lombok)      | Enable annotation processing in your IDE.                          |
| Database connection failure     | Verify DB info, network, and MySQL server status.                  |
| C++ process error               | Check that all required DLLs are present and compatible.           |

---

## Contributing

We welcome contributions from the community and teams using this backend!

**How to contribute:**
1. Fork this repository
2. Create a branch with a descriptive name for your change
3. Commit code and push changes to your fork
4. Open a Pull Request (PR) to the main repository. Describe your rationale and test cases clearly
5. All contributions must follow code review, style, and documentation guidelines

Issues and feature requests should be submitted via the [GitHub Issues page](../../issues).

---

## License

This project is [MIT licensed](LICENSE). See LICENSE for full details.

---

## Acknowledgments
- [Spring Boot](https://spring.io/projects/spring-boot) — Application Framework
- [Lombok](https://projectlombok.org/) — Boilerplate Reduction
- [Your C++ NL2SQL Team] — Query-to-SQL Engine
- [Open Source Contributors](https://github.com/)

---

**Contact:** For support, features, or commercial services, open an issue or email the maintainer.

---

**Enjoy using the next generation Java Spring Boot NL2SQL backend for your enterprise or academic projects!**
