# Docuvra

Docuvra is a simple document viewer MVP for uploading, versioning, previewing, downloading, and printing documents.

## Features

- Upload documents up to 50 MB
- Store original files on the Windows local file system
- Store document metadata in PostgreSQL
- Store a PostgreSQL `BYTEA` backup copy of every uploaded file
- Maintain up to 5 versions per document
- View PDF files page-by-page using PDF.js
- View XLS, XLSX, and CSV files as read-only HTML spreadsheets
- Add comments to spreadsheet cells
- Download document versions
- Print PDF documents from the viewer
- Restore a missing local file from the PostgreSQL backup copy

## Tech Stack

- Java 21
- Spring Boot
- PostgreSQL
- Flyway
- Angular
- PDF.js
- Apache POI
- Apache Commons CSV

## Prerequisites

- Java 21
- Maven
- Node.js
- npm
- Angular CLI
- PostgreSQL
- Docker Desktop

## Run with Docker Compose

From the project root:

```powershell
docker compose -f docker/docker-compose.yml up --build
```

Detached:

```powershell
docker compose -f docker/docker-compose.yml up -d --build
```

Stop:

```powershell
docker compose -f docker/docker-compose.yml down
```

Stop and remove volumes:

```powershell
docker compose -f docker/docker-compose.yml down -v
```

View logs:

```powershell
docker compose -f docker/docker-compose.yml logs -f backend
docker compose -f docker/docker-compose.yml logs -f frontend
docker compose -f docker/docker-compose.yml logs -f postgres
```

Docker services:

- PostgreSQL: `postgres:16`
- Backend: Spring Boot on Java 21
- Frontend: Angular static build served by Nginx

Docker volumes:

- `docuvra_postgres_data:/var/lib/postgresql/data`
- `docuvra_uploads:/app/uploads`

Docker environment:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/docuvra
SPRING_DATASOURCE_USERNAME=docuvra
SPRING_DATASOURCE_PASSWORD=docuvra
DOCUVRA_STORAGE_BASE_PATH=/app/uploads
```

## PostgreSQL Setup

Create the database:

```sql
CREATE DATABASE docuvra;
```

Create or choose a PostgreSQL user that can connect to the `docuvra` database, then set the backend environment variables.

PowerShell example:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/docuvra"
$env:SPRING_DATASOURCE_USERNAME="docuvra"
$env:SPRING_DATASOURCE_PASSWORD="docuvra"
$env:DOCUVRA_STORAGE_BASE_PATH="C:/docuvra/uploads"
```

On Windows, start the PostgreSQL service if it is not already running:

```powershell
Start-Service postgresql-x64-18
```

If your local PostgreSQL service uses another port, update `SPRING_DATASOURCE_URL`. Example:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5433/docuvra"
```

## Backend Setup

```powershell
cd backend
mvn clean install
mvn spring-boot:run
```

Flyway runs automatically on startup and creates the required tables.

## Frontend Setup

```powershell
cd frontend
npm install
npm start
```

## Local URLs

- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080/api`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Health: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`
- PostgreSQL database: `docuvra`
- PostgreSQL username: `docuvra`
- PostgreSQL password: `docuvra`

## Windows File Storage

Default local storage path:

```text
C:/docuvra/uploads
```

Default converted preview cache path:

```text
C:/docuvra/converted
```

Both paths are configurable:

```powershell
$env:DOCUVRA_STORAGE_BASE_PATH="C:/docuvra/uploads"
$env:DOCUVRA_CONVERTED_BASE_PATH="C:/docuvra/converted"
```

Folder structure:

```text
C:/docuvra/uploads/{documentId}/{versionId}/{originalFileName}
```

Example:

```text
C:/docuvra/uploads/6f1d7c4e-5b6a-4e91-a71d-53d7c9c71d11/712fd4bb-24e3-4989-99de-f3d8c2e96b91/sample.pdf
```

## API Endpoints

Base path:

```text
http://localhost:8080/api/documents
```

Endpoints:

```text
POST   /api/documents/upload
GET    /api/documents
GET    /api/documents/{documentId}
DELETE /api/documents/{documentId}

POST   /api/documents/{documentId}/versions
GET    /api/documents/{documentId}/versions/{versionId}/view
GET    /api/documents/{documentId}/versions/{versionId}/download
GET    /api/documents/{documentId}/versions/{versionId}/thumbnail
DELETE /api/documents/{documentId}/versions/{versionId}
```

## Docker Validation Checklist

1. Start Docker Compose:

```powershell
docker compose -f docker/docker-compose.yml up --build
```

2. Check containers:

```powershell
docker ps
```

3. Check backend health:

```powershell
curl http://localhost:8080/actuator/health
```

4. Open frontend:

```text
http://localhost:4200
```

5. Upload a PDF under 50 MB.
6. Verify the file is stored in the Docker volume `docuvra_uploads`.
7. Verify metadata is stored in PostgreSQL.
8. Open the document viewer.
9. Check the thumbnail appears.
10. Download the file.
11. Print the file.

## Testing Flow

1. Start PostgreSQL.
2. Start the backend with `mvn spring-boot:run`.
3. Start the frontend with `npm start`.
4. Open `http://localhost:4200`.
5. Upload a PDF.
6. Open the PDF viewer and page through the document.
7. Upload a new version from the document details page.
8. Download a version.
9. Delete a version.
10. Delete the document.

## Known Limitations

- Non-PDF preview requires LibreOffice/OpenOffice for office files and ImageMagick for images.
- No login or authentication.
- No OCR.

## Document Preview Conversion

Docuvra keeps the original uploaded file unchanged. When a non-PDF file is viewed, the backend creates a cached PDF at:

```text
{convertedBasePath}/{documentId}/{versionId}/viewer.pdf
```

If the cached PDF is deleted, Docuvra recreates it from the original file. If the original file is missing from local storage, Docuvra restores it from the PostgreSQL `BYTEA` backup first.

Supported conversion types:

```text
doc, docx, ppt, pptx, txt, rtf, odt, odp
jpg, jpeg, png, bmp, gif, tiff, tif, webp
```

PDF uploads are streamed directly without conversion. Downloads always return the original file.

Excel files are not converted to PDF. XLS, XLSX, and CSV files open in the dedicated Excel viewer.

## Excel Viewing

XLS, XLSX, and CSV files are rendered as read-only HTML tables.

- All workbook sheets are shown as tabs.
- Users can add and delete comments on cells.
- Comments are stored in PostgreSQL and do not modify the original spreadsheet.
- Original Excel files remain unchanged.
- Download still returns the original file.
- Very large sheets are limited by configured row and column limits.

Excel configuration:

```text
DOCUVRA_EXCEL_MAX_ROWS=1000
DOCUVRA_EXCEL_MAX_COLUMNS=100
```

Current limitations:

- Excel editing is not supported.
- Excel compare is not supported in the current MVP.
- Cell range selection is minimal; comments are added from the selected cell in the UI.

Configuration:

```text
DOCUVRA_STORAGE_BASE_PATH
DOCUVRA_CONVERTED_BASE_PATH
DOCUVRA_OFFICE_PATH
DOCUVRA_IMAGEMAGICK_PATH
DOCUVRA_CONVERSION_TIMEOUT_SECONDS
```

If storage paths are empty, Docuvra picks OS-aware defaults:

```text
Windows uploads:   C:/docuvra/uploads
Windows converted: C:/docuvra/converted
Linux uploads:     /opt/docuvra/uploads
Linux converted:   /opt/docuvra/converted
```

### Windows Converter Setup

Install LibreOffice or OpenOffice, plus ImageMagick.

Verify:

```powershell
"C:/Program Files/LibreOffice/program/soffice.exe" --version
magick --version
```

Create folders:

```powershell
mkdir C:\docuvra\uploads
mkdir C:\docuvra\converted
```

Environment example:

```powershell
$env:DOCUVRA_STORAGE_BASE_PATH="C:/docuvra/uploads"
$env:DOCUVRA_CONVERTED_BASE_PATH="C:/docuvra/converted"
$env:DOCUVRA_OFFICE_PATH="C:/Program Files/LibreOffice/program/soffice.exe"
$env:DOCUVRA_IMAGEMAGICK_PATH="magick"
```

### Linux Converter Setup

Ubuntu/Debian:

```bash
sudo apt update
sudo apt install -y libreoffice imagemagick poppler-utils fonts-dejavu fonts-liberation
```

Verify:

```bash
soffice --version
libreoffice --version
magick --version
convert --version
```

Create folders:

```bash
sudo mkdir -p /opt/docuvra/uploads
sudo mkdir -p /opt/docuvra/converted
sudo chown -R $USER:$USER /opt/docuvra
```

Run backend:

```bash
export DOCUVRA_STORAGE_BASE_PATH=/opt/docuvra/uploads
export DOCUVRA_CONVERTED_BASE_PATH=/opt/docuvra/converted
export DOCUVRA_OFFICE_PATH=soffice
export DOCUVRA_IMAGEMAGICK_PATH=magick

mvn spring-boot:run
```

If you use OpenOffice instead of LibreOffice:

```bash
export DOCUVRA_OFFICE_PATH=/path/to/soffice
```

### Docker Converter Note

The backend Docker image installs:

```text
libreoffice
imagemagick
font-dejavu
font-liberation
```

Docker paths:

```text
/app/uploads
/opt/docuvra/converted
```

## Future Enhancements

- Convert DOCX, PPT, XLS, and image files to PDF
- Annotation support
- OCR
- Access control
- S3-compatible storage
