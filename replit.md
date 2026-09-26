# FC Mobile Tournament on Replit

- The project uses Java/Spring Boot with Maven; the original frontend files are in the project root.
- Use the **Start application** workflow to build and start the server. The Replit preview opens the registration page at `/`; the API is under `/api`.
- The app binds to `0.0.0.0:5000` in this workflow. With the provisioned PostgreSQL `DATABASE_URL`, registrations, fixtures, scores, standings, and knockout rounds persist across restarts. Without it, the existing H2 fallback is temporary.
- This version intentionally has no screenshot uploads or AI integration. The administrator signs in as `Timmy45G` with the four-character `ADMIN_ACCESS_PIN` and enters scores manually.
- The tournament accepts exactly eight active players, assigns two pots of four in registration order, creates seven round-robin matchdays with four matches each, and creates two semifinals plus a final after the league table is complete.
- To build manually, run `mvn package -DskipTests`; to run locally outside the workflow, run `java -jar target/fcmobtourn-1.0.0.jar`.