# FC Mobile Tournament on Replit

- The project uses Java/Spring Boot with Maven; the original frontend files are in the project root.
- Use the **Start application** workflow to build and start the server. The Replit preview opens the registration page at `/`; the API is under `/api`.
- The app binds to `0.0.0.0:5000` in this workflow. Without `DATABASE_URL`, it uses an in-memory H2 database, so registrations and results are lost when the server restarts. With a provisioned PostgreSQL `DATABASE_URL`, it persists data.
- Screenshot-based squad and match analysis requires an `OPENROUTER_API_KEY` secret. Without it, the existing code uses fallback behavior rather than AI analysis. `ADMIN_ACCESS_KEY` is also required to use protected admin API endpoints.
- To build manually, run `mvn package -DskipTests`; to run locally outside the workflow, run `java -jar target/fcmobtourn-1.0.0.jar`.