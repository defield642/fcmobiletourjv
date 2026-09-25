# FC Mobile Tournament

Spring Boot + PostgreSQL backend and responsive single-page frontend for registrations, generated fixtures, AI squad analysis, and screenshot-based match results.

## Local run

```bash
mvn package -DskipTests
java -jar target/fcmobtourn-1.0.0.jar
```

Open `FC Mobile Tournament.html` through a static server. The API runs on port 8080.

## Render

Use the included `render.yaml` Blueprint, or create a Java Web Service with build command `mvn package -DskipTests` and start command `java -jar target/fcmobtourn-1.0.0.jar`. Add `OPENROUTER_API_KEY` in the Render Environment settings. `DATABASE_URL` is intentionally used as the application database variable for compatibility with this project, but it must contain the Render PostgreSQL connection string.


After the backend deploys, copy its `https://...onrender.com` URL and set `window.__FCMOBILE_API__` near the top of `FC Mobile Tournament.html` to `https://YOUR-BACKEND.onrender.com/api` before deploying the static site. For local development, the page automatically uses `http://127.0.0.1:8080/api`.
