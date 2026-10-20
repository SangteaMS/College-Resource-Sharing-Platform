# Make ShareMats available on the internet

This project needs a Java server, MySQL, and persistent file storage. GitHub Pages can host the static preview in `docs/`, but it cannot run Spring Boot, account login, moderation, or uploads.

## Suggested beginner deployment: Railway

Railway can build this repository with the included `Dockerfile`, provide a MySQL service, and issue a public URL. Railway currently requires a Railway account and a GitHub repository connection for the dashboard workflow. Check Railway's current plans before deploying; hosting may have a cost.

1. Create a GitHub repository for this project. Keep `data/`, `uploads/`, credentials, and `.env` files out of it; `.gitignore` already excludes the local database and uploaded files.
2. In Railway, create a project and add a service from that GitHub repository. It will detect the root `Dockerfile`.
3. Add a MySQL service to the same Railway project.
4. Add the following variables to the Spring Boot service. Replace `MySQL` with the exact name Railway shows for your database service if needed:

   ```text
   DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?serverTimezone=UTC
   DB_USERNAME=${{MySQL.MYSQLUSER}}
   DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
   COLLEGEHUB_STORAGE_LOCATION=/data/uploads
   ```

   To enable the demo admin on a new deployment, also set `COLLEGEHUB_ADMIN_EMAIL`, `COLLEGEHUB_ADMIN_PASSWORD`, and `COLLEGEHUB_ADMIN_NAME` to values you choose. Set the password in Railway's private variable settings; do not put it in GitHub or this file.

5. Add a Railway volume to the Spring service with mount path `/data`. Uploaded files are stored in `/data/uploads` and need this volume to persist across deployments.
6. Deploy the service, then use its Settings → Networking controls to generate a public domain. Visit that URL to check the site.

The database contains student account and usage records, so use a unique, strong admin password and keep database credentials private. Back up the database and file volume before relying on the deployment for important material. Uploaded resources are student-contributed and should be shared only with permission.

## How local development differs

For local testing, continue using the demo profile and local H2 database as described in `README.md`. The public deployment uses MySQL. Local files in `data/` and `uploads/` do not automatically copy to the public server.

## References

- [Railway Dockerfile builds](https://docs.railway.com/builds/dockerfiles)
- [Railway MySQL service](https://docs.railway.com/databases/mysql)
- [Railway persistent volumes](https://docs.railway.com/volumes)
- [Railway public networking](https://docs.railway.com/guides/docker-compose#public-networking)
