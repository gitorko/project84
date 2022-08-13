# Project61

Jenkins Pipeline + Data processing

[https://gitorko.github.io/jenkins-data-processing/](https://gitorko.github.io/jenkins-data-processing/)

### Version

Check version

```bash
$java --version
openjdk 17.0.3 2022-04-19 LTS
```

### Postgres DB

```
docker run -p 5432:5432 --name pg-container -e POSTGRES_PASSWORD=password -d postgres:9.6.10
docker ps
docker exec -it pg-container psql -U postgres -W postgres
CREATE USER test WITH PASSWORD 'test@123';
CREATE DATABASE "test-db" WITH OWNER "test" ENCODING UTF8 TEMPLATE template0;
grant all PRIVILEGES ON DATABASE "test-db" to test;

docker stop pg-container
docker start pg-container
```

### Dev

To run the backend in dev mode.

```bash
./gradlew clean build
./gradlew bootRun
```

To truncate the tables

```sql
truncate order_detail;
truncate material_detail;
truncate processed_detail;
truncate bonus_detail;
truncate factory_detail;
```
