# Tempo - Time Management and Goal Setting Tool

Java Servlets + JSP + JDBC web application (Maven WAR, Tomcat 10.1, Java 25 LTS+).
Database: embedded **H2** by default (zero setup) or **MySQL 8** (one line to switch).

**Team:** [Team name] - [Member 1 (leader)], [Member 2], [Member 3], [Member 4]
**Course:** Java Programming, Galgotias University x GUVI-HCL, B.Tech CSE (2029 batch) - Topic 66

## What it does
- **User:** register / sign in, set goals (name, type, target hours, deadline), log time (start, end, goal), see progress bars and a summary, get deadline reminders. A goal completes automatically when logged time reaches its target.
- **Admin:** configure goal types / tracking metric / target cap, add - deactivate - delete accounts, view usage totals and charts.

Default admin (created on first start): `admin@tempo.app` / `Admin@123`
(set the environment variable `ADMIN_PASSWORD` before the first start to use a different password).

## Run it locally

Needs: JDK 25+, Maven, Tomcat 10.1 (Tomcat 9 or older will not work - the code uses `jakarta.*`).

```
mvn clean package            # runs the unit tests and builds target/tempo.war
```
Copy `target/tempo.war` into Tomcat's `webapps/` folder, start Tomcat, open http://localhost:8080/tempo

The database needs no setup: on first start the app creates its tables (`src/main/resources/schema.sql`),
the starter goal types and the admin account. The H2 data file lives in `~/tempo-data/`.

### Using MySQL instead
1. Create an empty database once: `CREATE DATABASE goal_tracker;`
2. In `src/main/resources/db.properties` comment out the three H2 lines and un-comment the MySQL block (set your password).
3. Rebuild and redeploy. The app creates the tables itself.

Never commit a real password. For hosting use the environment variables `DB_URL`, `DB_USER`, `DB_PASSWORD`, which override the file.

## Host it online (Render, Docker)
The included `Dockerfile` builds the WAR and runs it on Tomcat, so nothing needs installing on your computer.

1. Push this repo to GitHub.
2. On render.com: **New -> Blueprint** (or **New -> Web Service**), pick the repo, runtime **Docker**, plan **Free**.
   Set environment variables `PORT=8080` and `ADMIN_PASSWORD=<your own password>`.
3. Wait for the build (about 5-10 minutes). Render gives you a public `https://....onrender.com` link.

Free-plan notes (check Render's current terms): the service sleeps when idle and takes up to a minute to wake;
with the built-in H2 database the data **resets whenever the service restarts or redeploys** - fine for a demo.
To keep data permanently, create a free hosted MySQL (for example Aiven or TiDB Cloud) and add the env vars
`DB_URL` (format `jdbc:mysql://HOST:PORT/DBNAME?sslMode=REQUIRED`), `DB_USER`, `DB_PASSWORD`.

## Tests
`mvn test` runs JUnit 5 tests for `Validator`, `PasswordUtil`, `JsonUtil`, `Goal` and `ProgressService`.

## Rubric B mapping
| Rubric item | Where in the code |
|---|---|
| Problem understanding and solution design | Topic 66 requirements; layered packages (model / dao / service / servlet); `schema.sql` |
| Core Java: OOP | `BaseEntity` (abstract) -> `User`, `Goal`, `GoalType`, `TimeLog`; `CrudDao<T>` interface; polymorphic `describe()` |
| Core Java: Collections | `ProgressService` (streams, `groupingBy` into `TreeMap`), `LinkedHashMap` chart data, `ConcurrentHashMap` reminders |
| Core Java: Exceptions | `AppException` -> `ValidationException`; `Validator` |
| Core Java: Threads | `AppInitListener` - `ScheduledExecutorService` deadline-reminder thread |
| JDBC | `dao/*` - `PreparedStatement` CRUD; transactions in `UserDAO.delete`, `TimeLogDAO.create/delete` |
| Servlets and web integration | `AuthServlet`, `UserServlet`, `AdminServlet`, `AuthFilter` (role check), `HttpSession`, JSP + JSTL |
| Code quality and testing | layered code, Javadoc per class, `src/test` |

## Known limits
Salted SHA-256 instead of BCrypt; no CSRF tokens; no password reset or change-password page.
