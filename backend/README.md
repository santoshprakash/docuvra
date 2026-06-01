# Docuvra Backend

Spring Boot backend for the Docuvra MVP.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL

## Database Environment Variables

PowerShell:

```powershell
$env:DOCUVRA_DB_URL="jdbc:postgresql://localhost:5432/docuvra"
$env:DOCUVRA_DB_USERNAME="docuvra"
$env:DOCUVRA_DB_PASSWORD="docuvra"
```

Optional overrides:

```powershell
$env:DOCUVRA_STORAGE_BASE_PATH="D:/Workspace/files-store"
$env:DOCUVRA_CORS_ALLOWED_ORIGIN="http://localhost:4200"
```

## Run

```powershell
mvn spring-boot:run
```

## Test

```powershell
mvn test
```

## API Docs

After the app starts:

```text
http://localhost:8080/swagger-ui/index.html
```
