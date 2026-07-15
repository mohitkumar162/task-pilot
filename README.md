# TaskPilot

A full-stack, role-based task management platform built with Spring Boot, PostgreSQL, and JWT authentication — containerized with Docker and deployed through a GitHub Actions CI/CD pipeline.

Managers can create projects and assign tasks; developers can update task status. Every API request is authenticated and authorized based on role.

---

## Features

- **JWT authentication** — stateless, secure login/register flow
- **Role-based access control** — `MANAGER` and `DEVELOPER` roles with different permissions
  - Managers: create/update/delete projects, create tasks, assign tasks, set priority & deadlines
  - Developers: view assigned tasks, update task status
- **Project & task management** — create projects, break them into tasks, track progress
- **Automated testing** — Mockito unit tests covering the service layer
- **Dockerized** — multi-stage build, runs as a non-root user
- **CI/CD pipeline** — GitHub Actions runs tests on every push/PR, then builds and pushes a Docker image to GitHub Container Registry on merge to `main`
- **Cloud-deployed** — running on AWS EC2

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot, Spring Security, JPA/Hibernate |
| Database | PostgreSQL |
| Auth | JWT (JSON Web Tokens), BCrypt password hashing |
| Frontend | HTML, CSS, JavaScript (vanilla) |
| Testing | JUnit 5, Mockito |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions → GitHub Container Registry (GHCR) |
| Hosting | AWS EC2 |

---

## Architecture

```
┌─────────────┐      HTTP       ┌──────────────────┐      JDBC       ┌─────────────┐
│  Frontend    │ ───────────────▶│  Spring Boot API │────────────────▶│  PostgreSQL │
│ (HTML/JS/CSS)│◀─────────────── │  (JWT-secured)   │◀────────────────│             │
└─────────────┘   JSON + JWT     └──────────────────┘                 └─────────────┘
```

- `SecurityConfig` enforces stateless JWT auth on all `/api/**` routes; static frontend assets are served openly.
- `JwtAuthFilter` validates the token on every request before it reaches a controller.
- Role checks (`hasRole("MANAGER")`) gate write operations on projects and tasks.

---

## Running Locally

**Prerequisites:** Docker Desktop installed and running.

1. Clone the repo:
   ```bash
   git clone https://github.com/<your-username>/taskpilot.git
   cd taskpilot
   ```

2. Create your `.env` file from the template:
   ```bash
   cp .env.example .env
   ```
   Then edit `.env` and set real values:
   ```
   DB_PASSWORD=your_strong_password
   JWT_SECRET=your_32+_char_random_secret
   ```

3. Build and start:
   ```bash
   docker compose up -d --build
   ```

4. Open [http://localhost:8080](http://localhost:8080)

5. Register an account, log in, and start creating projects and tasks.

To stop:
```bash
docker compose down
```

---

## Running Tests

```bash
cd taskmanagerbackend
mvn clean verify
```

Test results are also generated automatically on every push via the CI pipeline (see below).

---

## CI/CD Pipeline

Defined in [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

1. **`build-and-test`** — runs on every push and pull request
   - Checks out code, sets up Java 17
   - Runs `mvn clean verify` (unit tests act as a gate — a failing test blocks the pipeline)
   - Uploads test results as a downloadable artifact

2. **`build-and-push-image`** — runs only after tests pass, only on push to `main`
   - Builds the Docker image from `taskmanagerbackend/Dockerfile`
   - Pushes to GitHub Container Registry tagged both `:latest` and `:<commit-sha>`

---

## Project Structure

```
taskpilot/
├── .github/workflows/ci.yml     # CI/CD pipeline
├── frontend/                    # Static HTML/CSS/JS client
├── taskmanagerbackend/
│   ├── src/main/java/com/taskmanager/
│   │   ├── config/               # Security & CORS config
│   │   ├── controller/           # REST controllers
│   │   ├── model/                # JPA entities
│   │   ├── repository/           # Spring Data repositories
│   │   ├── security/              # JWT filter & utils
│   │   └── service/               # Business logic
│   ├── src/test/java/            # Unit tests (Mockito)
│   └── Dockerfile
├── docker-compose.yml
├── docs/screenshots/             # Proof-of-work screenshots
└── .env.example                  # Environment variable template
```

---

## Security Notes

- Passwords are hashed with BCrypt — never stored in plain text.
- JWTs are stored in `sessionStorage` on the frontend (cleared when the tab closes) rather than `localStorage`, reducing exposure to XSS-based token theft.
- All secrets (`JWT_SECRET`, `DB_PASSWORD`) are supplied via environment variables / `.env`, never hardcoded in tracked files.

---

## License

This project is available under the MIT License.
