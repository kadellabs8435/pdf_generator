# Bank Statement Generator Platform

Web platform for generating **synthetic, watermarked** bank statement PDFs for internal testing, demos, and workflow validation.

## Project Structure

```
pdfGenerator/
├── backend/     # Java Spring Boot REST API
├── frontend/    # React + TypeScript + shadcn/ui SPA
├── docs/        # Phase notes and future work
└── README.md
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| Frontend | React 19, TypeScript, Vite, Tailwind, shadcn-style UI |
| Backend | Java 17, Spring Boot 3.2, Spring Security, JWT |
| Database | MongoDB |
| PDF | Thymeleaf + OpenHTMLtoPDF |

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 18+
- MongoDB running locally (default: `mongodb://localhost:27017`)

## Quick Start

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

API: `http://localhost:8080`  
Swagger: `http://localhost:8080/swagger-ui.html`

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

App: `http://localhost:5173`

## Demo Users

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@bankdemo.com | Admin@123 |
| Staff | staff@bankdemo.com | Staff@123 |
| Viewer | viewer@bankdemo.com | Viewer@123 |

**OTP:** In dev mode, OTP is printed to the backend console after login.

## Phase Status

| Phase | Scope | Status |
|-------|-------|--------|
| 1 | Auth, dashboard, SBI template, statement form | Done |
| 2 | Transaction engine, PDF preview/download | Done |
| 3 | History, Excel bulk upload | Done |
| 4 | Admin panel, 6 bank templates, approval workflow | Done |
| 5 | AI narration + Android (stubs only) | Documented |

## Environment Variables

**Backend** (`application-dev.yml` or env):

- `MONGODB_URI` — MongoDB connection string
- `JWT_SECRET` — JWT signing key (min 256 bits recommended)
- `PDF_STORAGE_PATH` — local PDF storage path

**Frontend** (`.env`):

- `VITE_API_BASE_URL=http://localhost:8080/api`

## Key Features

- Email/mobile login with OTP verification
- Password reset flow
- Role-based access (Admin, Staff, Viewer)
- Statement form with customer/account/transaction settings
- Synthetic transaction generator (salary, UPI, ATM, EMI, interest)
- Watermarked A4 PDF preview and download
- Statement history with filters
- Excel bulk generation
- Admin: users, bank templates, activity logs
- 6 bank templates: SBI, HDFC, ICICI, Axis, Kotak, Canara

## Disclaimer

All generated data is **synthetic** and labeled as **SAMPLE / FOR DEMO ONLY**. For legitimate demo and testing purposes only.
