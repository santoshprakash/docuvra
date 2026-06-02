# Docuvra

<p align="center">
  <b>Modern Document Viewer & Collaboration Platform</b><br/>
  Upload • Preview • Compare • Annotate • Collaborate
</p>

<p align="center">

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![Angular](https://img.shields.io/badge/Angular-20-red)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![PDF.js](https://img.shields.io/badge/PDF.js-Viewer-orange)
![License](https://img.shields.io/badge/license-MIT-green)

</p>

---

# Overview

**Docuvra** is a modern browser-based document viewer and collaboration platform built to manage, preview, compare, annotate, and collaborate on enterprise documents directly from a web application.

It supports secure file storage, document versioning, PDF preview, Office conversion, Excel spreadsheet viewing, annotations, threaded comments, mentions, role-based access, and document collaboration workflows.

---

# Features

## Document Upload & Versioning

* Upload documents up to **50 MB**
* Maintain up to **5 versions** per document
* Upload new version against existing document
* Download any version
* Delete document/version
* File metadata stored in PostgreSQL
* Original file backup stored in PostgreSQL `BYTEA`

---

## Universal Document Viewer

Supports:

### PDF

* Page-by-page viewing using **PDF.js**
* Zoom in / Zoom out
* Fit width
* Pagination
* Print
* Download

---

## Office Document Preview

Supported:

* DOC
* DOCX
* PPT
* PPTX
* TXT
* RTF
* ODT
* ODP

Converted on-demand to PDF using:

* **LibreOffice / OpenOffice** (Windows/Linux)

---

## Image Preview

Supported:

* JPG
* JPEG
* PNG
* BMP
* GIF
* TIFF
* WEBP

Converted to PDF using:

* **ImageMagick**

---

## Excel Spreadsheet Viewer

Excel files are rendered directly in browser (not converted to PDF).

Supported:

* XLS
* XLSX
* CSV

Features:

* read-only HTML spreadsheet view
* multiple sheet tabs
* full workbook view
* scrollable rows/columns
* cell comments
* comment threads per selected cell

---

# Annotation & Markup

Supported annotation tools:

* Highlight
* Comment
* Rectangle
* Freehand Draw
* Underline
* Strike-through

Features:

* create annotation on PDF
* threaded comments
* reply to comment
* @mention users
* notification badge
* comment to annotation linking
* render saved annotations on reload

---
## Default App Users

Flyway seeds these users for the login/RBAC workflow:

| Role | Email / Username | Password |
| --- | --- | --- |
| Normal User | `normal@docuvra.local` | `password` |
| Staff | `staff@docuvra.local` | `password` |
| Supervisor | `supervisor@docuvra.local` | `password` |

Security configuration:

```text
DOCUVRA_LOGIN_ENABLED=true
DOCUVRA_DEFAULT_ROLE_WHEN_LOGIN_DISABLED=STAFF
DOCUVRA_DEFAULT_USERNAME_WHEN_LOGIN_DISABLED=Staff
DOCUVRA_JWT_SECRET=change-this-local-dev-secret-please
DOCUVRA_JWT_EXPIRATION_MINUTES=720
```

User rules:

- Normal users can sign up from the login page.
- Signup captures username, email, mobile, and password.
- Supervisors can open `Users` and create Staff or Supervisor accounts.
- Supervisors can assign documents to staff users from document details.
- Staff accounts created by a supervisor must change password on first login.
- Normal users and staff users can change their own password from the header.
- Normal users see only documents they uploaded.
- Staff users see only documents assigned to them.
- Supervisors see all documents.
- Username is globally unique.
- Email and mobile are unique per role, so the same email/mobile can exist once as Staff and once as Normal User with different usernames.


---

# Document Compare

Compare:

* document vs document
* version vs version

Features:

* side-by-side compare
* independent scroll
* responsive compare layout
* read-only annotation visibility during compare mode

---

# Role-Based Access

Docuvra supports **configuration-based login ON/OFF**.

---

## Roles

### NORMAL_USER

Can:

* upload document
* view document
* view only comments where user is mentioned
* reply only on mentioned thread

Cannot:

* annotate
* markup
* create root comment
* delete annotation
* delete comment

---

### STAFF

Can:

* upload document
* view assigned documents
* view all unassigned documents
* annotate
* comment
* reply
* mention users
* request unassigned document to own bucket

---

### SUPERVISOR

Can:

* all STAFF actions
* assign documents to staff
* approve/reject document assignment requests
* access all documents

---

# Notifications

Supported notification types:

* @mention in comment
* document assigned
* document request created
* request approved
* request rejected

Features:

* unread badge
* mark as read
* mark all as read

---

# Secure File Handling

## Original File Always Safe

Docuvra **never modifies the original uploaded file**.

Original file is stored in:

* Local file system
* PostgreSQL `BYTEA` backup

---

## Converted File = Temporary Cache

Converted preview PDFs are stored as temporary cache:

```text
{convertedBasePath}/{documentId}/{versionId}/viewer.pdf
```

This folder can be deleted anytime.

If deleted:

* Docuvra automatically regenerates preview from original file

Benefits:

* original file protection
* fast preview load
* easy cleanup
* auto recovery

---

# Tech Stack

## Backend

* Java 21
* Spring Boot
* PostgreSQL
* Flyway

## Frontend

* Angular
* TypeScript
* SCSS
* PDF.js

## File Handling

* Apache POI
* Apache Commons CSV
* LibreOffice / OpenOffice
* ImageMagick

---

# Project Structure

```bash
docuvra/
├── backend/
├── frontend/
├── docker/
├── docs/
└── README.md
```

---

# Local Setup

## 1. Clone Repository

```bash
git clone <repo-url>
cd docuvra
```

---

- Excel editing is not supported.
- Excel compare is read-only.
- Cell range selection is minimal; comments are added from the selected cell in the UI.

## Run backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

---

# Frontend Setup

```bash
cd frontend
npm install
npm start
```

---

# Local URLs

Frontend:

```text
http://localhost:4200
```

Backend:

```text
http://localhost:8080/api
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

Health:

```text
http://localhost:8080/actuator/health
```

---

# Environment Variables

```bash
DOCUVRA_STORAGE_BASE_PATH
DOCUVRA_CONVERTED_BASE_PATH
DOCUVRA_OFFICE_PATH
DOCUVRA_IMAGEMAGICK_PATH
DOCUVRA_CONVERSION_TIMEOUT_SECONDS
DOCUVRA_LOGIN_ENABLED
DOCUVRA_DEFAULT_ROLE_WHEN_LOGIN_DISABLED
DOCUVRA_DEFAULT_USERNAME_WHEN_LOGIN_DISABLED
DOCUVRA_EXCEL_MAX_ROWS
DOCUVRA_EXCEL_MAX_COLUMNS
```

---

# Default Storage Paths

## Windows

```text
C:/docuvra/uploads
C:/docuvra/converted
```

---

## Linux

```text
/opt/docuvra/uploads
/opt/docuvra/converted
```

---

# Docker Support

Run:

```bash
docker compose up --build
```

Stop:

```bash
docker compose down
```

---

# Testing & Reports

Backend:

```bash
cd backend
mvn test
mvn verify
mvn surefire-report:report
mvn jacoco:report
```

Allure backend report:

```bash
cd backend
mvn allure:report
allure serve target/allure-results
```

Frontend:

```bash
cd frontend
npm run test:ci
```

E2E:

```bash
cd frontend
npx playwright install
npm run e2e
npm run e2e:report
```

Full regression:

```bash
./scripts/run-regression-with-report.sh
```

Windows:

```powershell
./scripts/run-regression-with-report.ps1
```

Report locations:

* Backend Surefire XML: `backend/target/surefire-reports/`
* Backend Failsafe XML: `backend/target/failsafe-reports/`
* Backend Surefire HTML: `backend/target/site/surefire-report.html`
* Backend JaCoCo coverage: `backend/target/site/jacoco/index.html`
* Backend Allure results: `backend/target/allure-results/`
* Backend Allure HTML: `backend/target/allure-report/index.html`
* Frontend Karma JUnit: `frontend/test-results/karma/test-results.xml`
* Frontend coverage: `frontend/coverage/`
* Playwright HTML: `frontend/playwright-report/index.html`
* Playwright screenshots: `frontend/test-results/screenshots/`
* Combined reports: `reports/`

---

# Roadmap

Upcoming:

* OCR support
* Digital signature
* Redaction
* Audit trail
* S3/Cloud storage
* Real-time collaboration
* Workflow integration

---

# Known Limitations

* OCR not implemented yet
* Digital signature not implemented yet
* Redaction not implemented yet
* Excel compare not supported yet
* Real-time collaboration not available yet

---

# License

MIT License

---

## Future Enhancements

- Convert DOCX, PPT, XLS, and image files to PDF
- Annotation support
- OCR
- Access control
- S3-compatible storage

## Sample screen
<img width="1884" height="837" alt="image" src="https://github.com/user-attachments/assets/cb5f680a-3d18-4c7e-961b-7231c454105b" />

<img width="1822" height="978" alt="image" src="https://github.com/user-attachments/assets/6fda5233-d4e8-4348-8299-4e8a1bcac666" />
<img width="1884" height="752" alt="image" src="https://github.com/user-attachments/assets/778d7d29-b498-4fd5-b016-144d6d4ea3b7" />
<img width="1906" height="838" alt="image" src="https://github.com/user-attachments/assets/6ce2f66d-a527-4fe0-a7e7-d9558e989dd6" />
<img width="1893" height="961" alt="image" src="https://github.com/user-attachments/assets/8d7b5a4b-2931-4668-83c1-bc46a09a99a0" />

## OCR Support - Search Text on image and scan document
<img width="1898" height="923" alt="image" src="https://github.com/user-attachments/assets/635144b8-4640-4c21-8f4d-9051cc05590f" />


# Author

Built with ❤️ as **Docuvra** – a modern document viewing and collaboration platform.



