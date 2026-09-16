# Trust Agro Consulting & Farming Management System

A modular monolith ERP-style management system for agro-consulting and poultry/farm management.

**Production deployment:** see [docs/production-deployment.md](docs/production-deployment.md) (profiles, required env vars, Docker, backups, JWT rotation).

---

## Tech Stack

| Layer     | Technology                              |
|-----------|----------------------------------------|
| Frontend  | React.js, React Router, Bootstrap 5, Axios|
| Backend   | Spring Boot 3, Spring Security, JWT, Actuator |
| Database  | H2 for local dev, PostgreSQL for production |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 18+

---

## Recommended Local Startup

Start everything from the project root with one command:

```powershell
cd "C:\Users\Hp\Desktop\Trust ERP"
.\start-dev.ps1
```

If PowerShell blocks script execution, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-dev.ps1
```

What this does:
- Starts the backend first using the Spring `local` profile
- Uses the built-in H2 local database automatically
- Waits until the backend is reachable on `http://localhost:8081/actuator/health`
- Starts the frontend only after the backend is ready

Expected local URLs:
- Backend: `http://localhost:8081`
- Frontend: `http://localhost:3000`
- Frontend fallback: `http://localhost:3001` if port `3000` is already busy

### Linux / macOS

```bash
cd "/path/to/Trust ERP"
./start-dev.sh
```

Or start the backend manually with the embedded H2 database (no PostgreSQL required):

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

**Important:** The backend defaults to the `local` H2 profile. For production PostgreSQL, use `SPRING_PROFILES_ACTIVE=prod` with the required env vars documented in [docs/production-deployment.md](docs/production-deployment.md).

### Demo credentials (local profile only)

On first successful **local** backend startup (`app.seed.demo-users=true`), demo users are seeded. **These accounts are not created in production.**

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@trustagro.com` | `Admin@1234` |
| Farm Manager | `farmmanager@trustagro.com` | `Trust@1234` |
| Inventory | `inventory@trustagro.com` | `Trust@1234` |
| Pharmacy | `pharmacy@trustagro.com` | `Trust@1234` |
| Veterinarian | `vet@trustagro.com` | `Trust@1234` |
| Finance | `finance@trustagro.com` | `Trust@1234` |
| CRM | `crm@trustagro.com` | `Trust@1234` |
| Marketing | `marketing@trustagro.com` | `Trust@1234` |
| Training | `training@trustagro.com` | `Trust@1234` |

Restart the backend once after updating to create any missing role accounts.

---

## Advanced Manual Startup

These commands are for troubleshooting only. The normal day-to-day workflow is `.\start-dev.ps1`.

### Backend Only

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The backend starts on **http://localhost:8081** in local mode.

### Frontend Only

```bash
cd frontend
npm install
npm start
```

The frontend expects the backend to already be running on **http://localhost:8081**.

---

## PostgreSQL / Production

Do **not** hardcode DB passwords or JWT secrets in properties files. Use environment variables:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://localhost:5432/trust_agro_db
export DB_USERNAME=trustagro
export DB_PASSWORD='your-strong-password'
export APP_JWT_SECRET='your-long-random-secret-at-least-32-chars'
export APP_CORS_ALLOWED_ORIGINS=https://app.example.com
```

Or use Docker Compose (see `.env.example` and [docs/production-deployment.md](docs/production-deployment.md)):

```bash
cp .env.example .env
docker compose up --build -d
```

Create the database first if running Postgres outside Compose:

```sql
CREATE DATABASE trust_agro_db;
```

---

## Project Structure

```
Trust ERP/
├── backend/
│   └── src/main/java/com/trustagro/
│       ├── auth/          # JWT authentication
│       ├── config/        # Security, CORS, seeder, dashboard, scheduler
│       ├── user/          # User management & roles
│       ├── farm/          # Farm, Flock, Daily Records
│       ├── inventory/     # Stock management (FEFO)
│       ├── veterinary/    # Vaccinations, Disease, Treatments, Prescriptions
│       ├── pharmacy/      # Sales, Customers, Receipts
│       ├── finance/       # Income, Expenses, Profit/Loss
│       ├── crm/           # Clients, Farm Visits, Follow-ups
│       ├── notification/  # In-app alerts
│       ├── audit/         # Audit logs
│       └── common/        # Shared exceptions, responses
├── docs/
│   └── production-deployment.md
└── frontend/
    └── src/
        ├── api/           # Axios API calls
        ├── context/       # Auth context
        ├── layouts/       # Sidebar, TopBar, MainLayout
        ├── components/    # Reusable components
        ├── pages/         # All page components
        └── utils/         # Helpers, constants
```

---

## Roles & Access (Department Isolation)

Each role sees **only its department workspace** in the sidebar. Cross-department menus are hidden unless the role has explicit read permission (Admin supervisor only).

| Role | Email | Password | Workspace |
|------|-------|----------|-----------|
| ADMIN | admin@trustagro.com | Admin@1234 | Users, Reports, Settings, Executive Dashboard (read-only ops) |
| FARM_MANAGER | farmmanager@trustagro.com | Trust@1234 | Farms, Flocks, Daily Records, Chicken Sales, Feed |
| INVENTORY_MANAGER | inventory@trustagro.com | Trust@1234 | Stock In/Out, POs, Suppliers, Forecasting |
| PHARMACIST | pharmacy@trustagro.com | Trust@1234 | Prescription Queue, POS, Customers |
| VETERINARIAN | vet@trustagro.com | Trust@1234 | Health Records, Diagnosis, Vaccination, Prescriptions |
| FINANCE | finance@trustagro.com | Trust@1234 | Revenue, Expenses, COA, Invoices, Reports |
| CRM_MANAGER | crm@trustagro.com | Trust@1234 | Leads, Pipeline, Customers, Orders, Consultancy |
| MARKETING | marketing@trustagro.com | Trust@1234 | Campaigns, Lead Generation, ROI Analytics |
| TRAINING_OFFICER | training@trustagro.com | Trust@1234 | Training Sessions, Students, Certificates |

> Passwords above apply to **local demo seeding only**. Production must create admins manually with strong credentials.

Legacy accounts (`store@`, `extension@`, `gmanager@`, `ops@`) are auto-migrated to the new roles on backend startup.

---

## Key API Endpoints

| Method | Endpoint                          | Description              |
|--------|-----------------------------------|--------------------------|
| POST   | /api/auth/login                   | Login                    |
| GET    | /api/auth/me                      | Current user             |
| GET    | /actuator/health                  | Liveness/readiness       |
| GET    | /api/farms                        | List farms               |
| POST   | /api/farms                        | Create farm              |
| GET    | /api/flocks                       | List flocks              |
| POST   | /api/daily-farm-records           | Create daily record      |
| POST   | /api/inventory/stock-in           | Record stock in          |
| POST   | /api/inventory/stock-out          | Record stock out         |
| GET    | /api/inventory/low-stock          | Low stock items          |
| GET    | /api/inventory/expiry-alerts      | Expiring items           |
| POST   | /api/vet/vaccinations             | Schedule vaccination     |
| POST   | /api/pharmacy/sales               | Create pharmacy sale     |
| GET    | /api/finance/profit-loss          | Profit/loss summary      |
| GET    | /api/dashboard/admin              | Admin dashboard data     |
| GET    | /api/notifications                | All notifications        |

---

## Business Rules Implemented

- Daily farm records cannot be duplicated for same farm + flock + date
- Mortality rate > threshold triggers automatic alert to Vet Officer & Operations Manager
- Stock out uses FEFO (First Expiry First Out) for drugs/vaccines
- Expired stock cannot be issued or sold
- Low stock triggers notification when below minimum level
- Pharmacy sale automatically reduces inventory stock
- Pharmacy sale automatically creates a Finance income transaction
- **NEW**: Veterinary prescriptions trigger a real-time fulfillment queue for pharmacists
- **NEW**: Dispensing clinical stock automatically creates a Finance `EXPENSE` transaction tagged to the specific flock for accurate COGS
- Missed vaccinations are auto-detected by daily scheduled job (6 AM)
- CRM follow-ups due today or overdue are highlighted

---

## Configuration

Key settings in `application.properties` / profiles:

```properties
app.alerts.mortality-threshold=5.0      # % mortality rate to trigger alert
app.alerts.expiry-warning-days=30       # Days before expiry to warn
app.jwt.expiration=86400000             # JWT expiry: 24 hours
# Secrets via env: APP_JWT_SECRET, DB_PASSWORD, APP_CORS_ALLOWED_ORIGINS
```
