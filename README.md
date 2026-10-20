# ShareMats — College Resource Sharing System

ShareMats is a semester integrated project built with Java, Spring Boot, Spring MVC, Thymeleaf, MySQL, and optional R analysis. It lets students browse and contribute academic resources, with an admin review step before a student upload becomes public.

The programme and subject names are illustrative demo categories. They are not verified official curricula, and fictional papers are not official exams or faculty-verified documents.

## What is included

- Database-backed programmes, semesters, and subjects. The MySQL migrations seed five illustrative programmes.
- The local H2 profile adds 12 more illustrative programmes and 32 sample resources. The local samples are pre-approved only to demonstrate browsing and downloading; each uses a generic placeholder PDF, not real course materials or an official paper.
- Search, course/semester/subject/type filters, exam-paper filters, sorting, and pagination.
- Student registration/login, saved course preferences, resource upload, and “My uploads”.
- Admin approval or rejection before student uploads are publicly visible.
- Uploaded files are stored separately under `uploads/`; the active database stores resource details and event history.
- The default MySQL profile does not preload browseable resource files. Local sample resources are separate from student uploads; student uploads remain pending until an admin reviews and approves them.
- Admin-only CSV export of non-identifying upload/download usage fields for R analysis.

## Requirements

- Java 17 or newer. The current project has been built with Java 23.
- Internet access the first time Maven downloads libraries.
- Optional: MySQL 8 or newer for the normal profile.
- Optional: R for running scripts in `analysis/`.

Maven is included as a wrapper, so installing Maven separately is not required. On Windows use `mvnw.cmd`.

## Local H2 development setup on Windows (no MySQL setup)

The `local` profile uses a persistent H2 file database under `data/`, so it does not need MySQL and keeps accounts, resources, and moderation decisions between restarts. Flyway seeds the five-programme catalogue, and the local-only initializer adds 12 illustrative programmes and 32 sample resources. Those sample resources are approved for browsing and downloading so you can demonstrate the interface. They all use the same generic placeholder PDF and are not real course materials or official papers; they are not student uploads. Student uploads still enter the normal Pending review flow and appear publicly only after an admin approves them. The default profile continues to use the MySQL settings in `application.properties`.

1. Open this folder in VS Code.
2. Choose **Terminal → New Terminal**.
3. Set the administrator email and password in that same PowerShell terminal. Replace the examples with values you choose, use a password of at least 12 characters, and do not commit it:

   ```powershell
   $env:COLLEGEHUB_ADMIN_EMAIL = 'your-admin@example.test'
   $env:COLLEGEHUB_ADMIN_PASSWORD = 'your-private-password-at-least-12-chars'
   ```

   The variable names and placeholder values are listed in `.env.example`. Spring reads environment variables; it does not load that file automatically.
4. Start the local server:

   ```powershell
   .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
   ```
5. Wait for `Started CollegeResourceSharingApplication`, then open <http://localhost:8080>.
6. Open <http://localhost:8080/login> and sign in with the email and password you set above.
7. Open <http://localhost:8080/admin/uploads>, review a pending item, then choose **Approve** or enter a reason and choose **Reject**.
8. Stop the server from the VS Code terminal with **Ctrl+C**. Use the same profile command to start it again.

Local database data is stored in `data/`; uploaded files are stored separately in `uploads/`. Both directories are ignored by Git. Keep them in place if you want to retain local data.

### Admin account setup and safety

The app creates the configured admin only when that email is not already in the local database. It hashes the password with the same Spring Security password encoder used by sign-in. On later starts it leaves an existing ADMIN account and its password alone; changing the environment password does not reset it. If only one variable is set, or the password is shorter than 12 characters, startup stops with an explanation.

If the configured email already belongs to a student, startup stops instead of upgrading that account. Set a different email for the administrator and restart. The normal registration form creates student accounts only.

For a workflow demonstration, register a fictional student, sign in, upload a fictional PDF with the permission checkbox selected, and open **My uploads**. It will show **Pending** and stay hidden from public browsing. Sign in as the administrator in another browser profile (or sign out first), open **Admin review** or `/admin/uploads`, and download the file to inspect it. Approve it to make it public and downloadable; reject it with a reason to show that note in the student's **My uploads** page.

Admins can also remove a pending item directly from the review queue, or open an approved resource from **Browse resources** and use the admin removal panel on its detail page. Enter a reason and confirm. The file is deleted after the removal is committed, public search and downloads stop working, and the uploader sees the removal status and reason in **My uploads**. The resource row remains in the database with the admin, time, and reason recorded.

## MySQL setup (default profile)

The default profile connects to MySQL. Its migrations seed five illustrative programmes, semesters, and subjects, but do not add browseable resource files. The 12 additional local programmes and 32 preloaded sample resources are created only by the `local` H2 profile. Student uploads in MySQL also stay pending until an admin approves them. Create a database (or allow the configured MySQL account to create it) and provide credentials with environment variables so passwords stay out of source control:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/collegehub?createDatabaseIfNotExist=true&serverTimezone=UTC'
$env:DB_USERNAME = 'your-mysql-user'
$env:DB_PASSWORD = 'your-mysql-password'
$env:COLLEGEHUB_STORAGE_LOCATION = 'C:\ShareMatsFiles'
$env:COLLEGEHUB_ADMIN_EMAIL = 'admin@your-college.example'
$env:COLLEGEHUB_ADMIN_PASSWORD = 'use-a-long-private-password'
.\mvnw.cmd spring-boot:run
```

Flyway applies SQL files in `src/main/resources/db/migration/` when the application starts. The schema uses additive migrations and Hibernate validation; it does not use Hibernate to drop or recreate tables. Back up an existing database before applying any migration in a shared environment.

## Useful developer commands

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

## Beginner architecture map

- `home/`, `resources/`, `accounts/`, and `admin/` contain Spring MVC controllers and feature logic.
- `catalog/` contains the database-backed programme, semester, and subject model.
- `src/main/resources/templates/` contains Thymeleaf pages; `static/css/` and `static/js/` contain styling and browser behavior.
- `src/main/resources/db/migration/` contains versioned schema and demo catalogue SQL.
- `uploads/` (or `COLLEGEHUB_STORAGE_LOCATION`) contains generated-name files. The original display filename is kept as metadata.
- `usage/` exports aggregate-safe CSV rows with event date, programme, semester, subject, and resource type. It does not include names or email addresses.
- `analysis/` contains R examples for analyzing the CSV export.

### Reproduce the analysis with synthetic data

The file `analysis/sample-data/synthetic-usage.csv` contains 120 fictional upload and download events across five programmes and eleven subjects over 30 days. The values are deliberately uneven to make the examples useful for practice. Its summaries and charts are illustrative only: they are not actual student activity and are not evidence of improved learning. The file contains no student names, emails, or other personal information.

From the project root, run:

```powershell
Rscript analysis/analyze_usage.R analysis/sample-data/synthetic-usage.csv analysis/example-output
```

The script writes `usage_by_programme.csv`, `usage_by_subject.csv`, and `usage_by_day.csv`, plus `downloads_by_subject.png`, `daily_activity.png`, and `activity_by_programme.png` to `analysis/example-output/`. All outputs from this synthetic input are illustrative. The script uses base R only.

To analyze real activity, sign in as an admin, choose **Download usage CSV** on the review queue, then run:

```powershell
Rscript analysis/analyze_usage.R path\to\collegehub-usage.csv
```

The script writes summaries by programme, subject, and day beside the input CSV, and a subject download chart when download events exist. It uses base R only.

## Academic SQL examples

`sql/project_queries.sql` contains four read-only examples based on the project schema: joins that list resources with programme, semester, and subject names; filtering by a sample keyword; grouping and aggregation of approved resources by programme and type; and aggregation of recorded downloads by programme, subject, and date. Download totals count recorded events, not unique students. These examples are for learning and do not contain precomputed or claimed results.

The upload form checks file extensions and file contents, and the server checks programme/semester/subject relationships. Only approved resources can be read by public detail and download routes. CSRF protection and password hashing are enabled by Spring Security.

## Git collaboration

Use a branch for each feature, commit code and migrations together, and open a pull request for teammate review. Do not commit `.env` files, database credentials, `data/`, or `uploads/`. For GitHub Pages, the `docs/` folder is only a static preview; authentication, database, file uploads, moderation, and usage tracking run in Spring Boot and are not provided by Pages.

See [CONTRIBUTING.md](CONTRIBUTING.md) for the contributor workflow and submission checklist.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
