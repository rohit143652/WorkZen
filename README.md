# Workforce Auth — Authentication, JWT, RBAC & Permission-Based Access Control

A modular, production-oriented authentication and authorization module built with
**Spring Boot 3 / Java 21** on the backend and **Angular (standalone components)** on the
frontend. Designed so that future modules (Employee, Client, Attendance, Payroll, Reports...)
can plug into the same auth/RBAC system without any redesign.

> **Build status honesty note:** this project was generated and manually reviewed for
> correctness (types, Spring/Spring Security 6 APIs, jjwt 0.12.x builder API, generic
> inference, transaction boundaries), but the sandbox this was built in has no access to
> Maven Central, so `mvn compile` was **not** run here. Before deploying, run:
> `cd backend && mvn clean verify` and fix anything your local Maven/JDK surfaces.

---

## 1. Architecture Overview

- **Stateless JWT authentication.** No server-side session. Each request carries a short-lived
  (15 min default) JWT access token in `Authorization: Bearer <token>`.
- **Refresh tokens** are opaque, random, stored in the `refresh_tokens` table, and rotated on
  every use (old token revoked, new one issued). They are delivered to the browser as an
  **HttpOnly, Secure, SameSite cookie** — never in JavaScript-readable storage.
- **RBAC is 100% database-driven.** `users -> user_roles -> roles -> role_permissions -> permissions`.
  Nothing like `if (role.equals("SUPER_ADMIN"))` exists in the Java code. `SUPER_ADMIN` has full
  access purely because Flyway seed data grants it every row in `permissions` via
  `role_permissions` — add a new permission later and you must explicitly grant it to a role.
- **Every protected endpoint is authorized with `@PreAuthorize("hasAuthority('X')")`,** where `X`
  is a permission name pulled from the database at login time and embedded as a Spring Security
  `GrantedAuthority`. Role checks (`hasRole('SUPER_ADMIN')`) are also available via the `ROLE_`
  prefix convention.
- **Modular by package**, not by layer: `login_module`, `role_module`, `permission_module`,
  `user_module`, `audit_module` each contain their own controller/service/repository/entity/dto.
  New business modules (Employee, Client...) follow the same pattern and reuse
  `CustomUserPrincipal`, `JwtAuthenticationFilter`, and `@PreAuthorize` without modification.

---

## 2. Complete Folder Structure

```
workforce-auth/
├── backend/
│   ├── pom.xml
│   ├── .env.example
│   └── src/main/java/com/example/application/
│       ├── Application.java
│       ├── config/                 SecurityConfig, JwtConfig, CorsConfig, OpenApiConfig
│       ├── common/
│       │   ├── exception/          GlobalExceptionHandler + typed exceptions
│       │   └── response/           ApiResponse<T>, ErrorResponse
│       ├── login_module/           auth flows, JWT, refresh tokens, login attempts
│       │   ├── controller/ service/ repository/ entity/ dto/ security/
│       ├── role_module/
│       ├── permission_module/
│       ├── user_module/
│       └── audit_module/
│   └── src/main/resources/
│       ├── application.yml / application-dev.yml / application-prod.yml
│       └── db/migration/           V1..V5 Flyway scripts
├── frontend/
│   ├── package.json / angular.json / tsconfig*.json
│   └── src/app/
│       ├── core/                   guards, interceptors, services, models
│       ├── shared/                 directives (appHasPermission)
│       ├── login_module/           LoginComponent, AuthService
│       ├── dashboard_module/       DashboardComponent
│       ├── user_module/ role_module/ permission_module/ audit_module/  (list screens)
│       └── app.routes.ts / app.config.ts
└── README.md
```

---

## 3. Technology Stack

**Backend:** Java 21, Spring Boot 3.3, Spring Security 6, Spring Data JPA/Hibernate, MySQL 8,
Flyway, jjwt 0.12.x, BCrypt, Bean Validation, springdoc-openapi (Swagger UI), JUnit 5, Mockito.

**Frontend:** Angular 18 (standalone components, signals), TypeScript, Reactive Forms, functional
Router guards, functional HTTP interceptor.

---

## 4. Database Setup

```sql
CREATE DATABASE workforce_auth CHARACTER SET utf8mb4;
```

Flyway runs automatically on application startup (`spring.flyway.enabled=true`) and applies, in
order:

| Migration | Purpose |
|---|---|
| `V1__create_auth_tables.sql` | `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens`, `login_attempts`, `audit_logs` + indexes/FKs |
| `V2__insert_roles.sql` | `SUPER_ADMIN, ADMIN, MANAGER, USER, CLIENT` |
| `V3__insert_permissions.sql` | All `*_CREATE/READ/UPDATE/DELETE`, `DASHBOARD_VIEW`, `AUDIT_LOG_READ`, `PASSWORD_CHANGE` |
| `V4__insert_super_admin.sql` | The `super_admin` user with a **real BCrypt hash**, assigned the `SUPER_ADMIN` role |
| `V5__insert_role_permissions.sql` | Grants — `SUPER_ADMIN` gets every permission row via SQL, other roles get a sensible subset |

---

## 5. Environment Variables

See `backend/.env.example`. Required:

| Variable | Purpose | Example |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL connection | `jdbc:mysql://localhost:3306/workforce_auth` |
| `JWT_SECRET` | HMAC signing key for access tokens (256+ bits) | `openssl rand -base64 64` |
| `JWT_ACCESS_EXPIRATION` | Access token TTL, ms | `900000` (15 min) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL, ms | `604800000` (7 days) |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:4200` |
| `COOKIE_SECURE` | `true` in production (HTTPS only) | `true` |
| `COOKIE_SAME_SITE` | `Strict` recommended | `Strict` |

**Never commit a populated `.env`** - `.gitignore` excludes `.env` (and any `.env.*` variant) while
still tracking `.env.example`. None of `DB_USERNAME`, `DB_PASSWORD`, or `JWT_SECRET` have a default
anywhere in `application.yml` or any profile — the app fails fast on startup if any is missing,
rather than silently using a weak key or a common credential like `root`/`root`.

---

## 6. Backend Setup & Run

**macOS / Linux:**
```bash
cd backend
cp .env.example .env        # then edit values
export $(grep -v '^#' .env | xargs)   # or use direnv / your IDE's env support
mvn clean verify
mvn spring-boot:run
```

**Windows (PowerShell):** `export $(cat .env)` doesn't exist on Windows, and `mvn spring-boot:run`
does **not** read a `.env` file for you — you must set the environment variables in the shell
session before running Maven, every time you open a new terminal (or set them once as permanent
Windows environment variables via System Properties → Environment Variables).
```powershell
cd backend
copy .env.example .env      # then edit values in a text editor
# Set env vars for THIS terminal session (repeat each time you open a new one):
$env:JWT_SECRET = "<paste your own generated secret here - see command below>"
$env:DB_URL = "jdbc:mysql://localhost:3306/workforce_auth?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:4200"
mvn clean verify
mvn spring-boot:run
```
Generate your own secret - never reuse an example/placeholder value as a real secret:
`openssl rand -base64 64` if you have Git Bash / WSL, or
`[Convert]::ToBase64String((1..64 | ForEach-Object {Get-Random -Max 256}) -as [byte[]])` in pure
PowerShell.

**No profile - including `dev` - has a hardcoded fallback for `DB_USERNAME`, `DB_PASSWORD`, or
`JWT_SECRET` anymore.** All three must come from a real environment variable in every environment,
local development included; the app fails to start with a clear error if any is missing, rather
than silently falling back to a weak/shared secret or a common credential like `root`/`root`.
This is intentional even though it means one extra step before your first local run - see the
PowerShell block above, or set them as permanent Windows environment variables so you don't have
to repeat this every session.

> **If you see `WeakKeyException: ... 0 bits which is not secure enough`**, `JWT_SECRET` is empty
> or unset. `JwtService` now fails with a clear `IllegalStateException` message instead of that raw
> jjwt error if this happens again - set `JWT_SECRET` as shown above and restart.

The API starts on `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`.
Click **Authorize** and paste `Bearer <accessToken>` from a login response to test protected
endpoints interactively.

## 7. Frontend Setup & Run

```bash
cd frontend
npm install
npm start        # ng serve, http://localhost:4200
```

`src/environments/environment.ts` points at `http://localhost:8080/api` for local dev.

---

## 8. Default Login Credentials

```
Username: super_admin
Password: admin123
```

**Change this password immediately after first login in any non-local environment.**
`POST /api/auth/change-password` revokes all existing refresh tokens once changed.

---

## 9. Token Storage Strategy (Section 52 decision)

- **Refresh token → HttpOnly, Secure, SameSite=Strict cookie**, set by the backend on
  `/api/auth/login` and `/api/auth/refresh`, scoped to path `/api/auth`. It is never exposed to
  JavaScript, which removes it from the XSS exfiltration surface entirely. `SameSite=Strict`
  (relaxed to `Lax` in the dev profile so `ng serve`'s different port doesn't break local login)
  mitigates CSRF for this cookie; combined with the API being stateless and JSON-only (no
  form-based CSRF-prone endpoints), this is a reasonable default. If you front the app with a
  different-origin SPA in production, keep `COOKIE_SAME_SITE=Strict` and route API calls through
  a same-site reverse proxy path rather than relaxing SameSite.
- **Access token → kept in memory** in Angular (`TokenService`, a plain class field), never
  written to `localStorage`/`sessionStorage`. It's short-lived (15 min) by design, so the blast
  radius of an in-memory leak is small, and it doesn't survive a full page reload — which is why
  `APP_INITIALIZER` calls `/api/auth/refresh` on bootstrap to silently re-establish it from the
  refresh cookie.

---

## 10. JWT Flow

1. `POST /api/auth/login` → Spring Security's `DaoAuthenticationProvider` + `BCryptPasswordEncoder`
   validate credentials against the DB. On success: `failed_login_attempts` resets, `last_login_at`
   updates, an access token is minted with claims `sub, uid, roles, permissions, iat, exp, jti`,
   a refresh token row is created, and the refresh token is set as an HttpOnly cookie.
2. Every subsequent request: `JwtAuthenticationFilter` reads `Authorization: Bearer <token>`,
   validates signature + expiry, **re-loads the user from the DB** (so a just-locked/deactivated
   account is rejected even with a still-valid JWT), and populates `SecurityContext`.
3. On `401` from an expired access token, the Angular `authInterceptor` calls
   `POST /api/auth/refresh` (cookie-based, single-flight — concurrent 401s share one refresh
   call), gets a new access token + rotated refresh cookie, and retries the original request
   transparently.

---

## 11. RBAC & Permission System

- `CustomUserPrincipal.getAuthorities()` builds Spring Security authorities directly from the
  user's DB-loaded roles/permissions: one `ROLE_<name>` per role, one `<PERMISSION_NAME>` per
  permission — nothing hardcoded.
- Controllers enforce this with `@PreAuthorize("hasAuthority('USER_CREATE')")` /
  `@PreAuthorize("hasRole('SUPER_ADMIN')")`.
- **Frontend guards (`roleGuard`, `permissionGuard`) and the `*appHasPermission` directive are
  UX-only.** They hide routes/buttons a user can't use, but the backend is the sole source of
  truth and re-validates every request regardless of what the UI shows.

---

## 12. Refresh Token Flow

`POST /api/auth/refresh` reads the token from the HttpOnly cookie (or JSON body, as a fallback
for non-browser clients), validates it's not revoked/expired and the owning user is
active/unlocked, **revokes it and issues a new one (rotation)**, and returns a new access token.
Rotation means a stolen, already-used refresh token becomes useless the moment the legitimate
client refreshes.

## 13. Logout Flow

`POST /api/auth/logout` revokes the presented refresh token server-side and clears the cookie.
The Angular `AuthService.logout()` always clears local state (access token + user), even if the
network call fails, so the client is never stuck appearing "logged in."

---

## 14. Testing

Backend tests live under `backend/src/test/java/.../login_module/{service,controller}` (H2 +
Spring Boot Test + Mockito wired via `pom.xml`). Recommended coverage to add, per the original
spec, beyond what's scaffolded:
login success/failure/locked/inactive, JWT validation/expiry, refresh (valid/invalid/rotated),
logout, password change, role/permission-based 403s, and CRUD for users/roles/permissions.

Run: `cd backend && mvn test`

Frontend: `cd frontend && npm test` (Karma/Jasmine, configured via `angular.json`). Add specs for
`LoginComponent`, `AuthService`, `TokenService`, the three guards, and `authInterceptor`'s
refresh/retry/single-flight behavior.

---

## 15. API Documentation

Base URL: `/api`. All responses use the `ApiResponse<T>` / `ErrorResponse` envelope described in
the spec (Section 31). Auth: **B**earer = requires `Authorization: Bearer <token>`.

| Method | Endpoint | Auth | Permission |
|---|---|---|---|
| POST | `/api/auth/login` | Public | — |
| POST | `/api/auth/refresh` | Public (cookie) | — |
| POST | `/api/auth/logout` | Public (cookie) | — |
| GET | `/api/auth/me` | Bearer | any authenticated user |
| POST | `/api/auth/change-password` | Bearer | any authenticated user |
| GET | `/api/users` | Bearer | `USER_READ` |
| GET | `/api/users/{id}` | Bearer | `USER_READ` |
| POST | `/api/users` | Bearer | `USER_CREATE` |
| PUT | `/api/users/{id}` | Bearer | `USER_UPDATE` |
| DELETE | `/api/users/{id}` | Bearer | `USER_DELETE` |
| PUT | `/api/users/{id}/roles` | Bearer | `USER_UPDATE` |
| PUT | `/api/users/{id}/activate` | Bearer | `USER_UPDATE` |
| PUT | `/api/users/{id}/deactivate` | Bearer | `USER_UPDATE` |
| PUT | `/api/users/{id}/unlock` | Bearer | `USER_UPDATE` |
| GET | `/api/roles` | Bearer | `ROLE_READ` |
| GET | `/api/roles/{id}` | Bearer | `ROLE_READ` |
| POST | `/api/roles` | Bearer | `ROLE_CREATE` |
| PUT | `/api/roles/{id}` | Bearer | `ROLE_UPDATE` |
| PUT | `/api/roles/{id}/permissions` | Bearer | `ROLE_UPDATE` |
| DELETE | `/api/roles/{id}` | Bearer | `ROLE_DELETE` |
| GET | `/api/permissions` | Bearer | `PERMISSION_READ` |
| GET | `/api/permissions/{id}` | Bearer | `PERMISSION_READ` |
| POST | `/api/permissions` | Bearer | `PERMISSION_CREATE` |
| PUT | `/api/permissions/{id}` | Bearer | `PERMISSION_UPDATE` |
| DELETE | `/api/permissions/{id}` | Bearer | `PERMISSION_DELETE` |
| GET | `/api/audit-logs` | Bearer | `AUDIT_LOG_READ` |

---

## 16. Production Deployment Notes

- Run behind HTTPS/TLS; `COOKIE_SECURE=true` requires it (browsers drop `Secure` cookies over
  plain HTTP).
- Set a strong, unique `JWT_SECRET` per environment via your secrets manager, not `.env` files
  committed anywhere.
- Set `CORS_ALLOWED_ORIGINS` to your real frontend origin(s) — never `*`.
- Use `spring.profiles.active=prod` (`application-prod.yml`), which disables `show-sql` and
  tightens logging.
- Put a connection pool size, statement timeout, and MySQL slow-query log in front of this in a
  real deployment — not included here as it's environment-specific.
- Rotate `JWT_SECRET` periodically; because access tokens are short-lived, rotation just requires
  a brief window where old tokens fail early (acceptable) rather than a hard cutover.

## 17. Security Notes Summary

BCrypt (strength 12) for password hashing · JWT secret via env var, fails fast if unset ·
stateless sessions · 15-min access tokens · rotating refresh tokens in HttpOnly/Secure/SameSite
cookies · refresh token revocation on logout and password change · account lockout after 5 failed
attempts (`login_attempts` + `users.failed_login_attempts`/`is_locked`) · CORS restricted to
configured origins · Bean Validation on all inputs · no password/JWT/refresh-token values ever
logged · `GlobalExceptionHandler` strips stack traces/SQL errors from every response · DTOs only,
entities never serialized directly · pagination on list endpoints · every write endpoint
authorized server-side via `@PreAuthorize` sourced from the database.

---

## 19. Employee Management, Login Access & Dashboard (Phase 2)

This phase adds Employee Management, integrated login-access management, a
role/permission-aware Dashboard, and an application-wide theme — **without**
touching the authentication/RBAC core described above. `AuthService`,
`TokenService`, `JwtService`, the guards, the interceptor, and
`SecurityConfig` are unchanged.

### Employee ↔ User relationship

`employees.user_id` is a **nullable, unique** foreign key to `users.id`:
**one Employee → zero or one User**. An employee can exist with no login
account at all. Employee Management (`/employees`) is the single place
employees are created; there is no separate "user registration" screen —
enabling login is an optional section of the same form, or a follow-up
action from the Employee Details page.

### Atomicity

`EmployeeService.create()` is `@Transactional`: when "Enable Login" is ON,
the Employee row, the User row, and its role assignment are all created (or
all rolled back) in one transaction. `enableLogin()` reuses an existing,
previously-disabled `User` row instead of ever creating a duplicate.

### New permissions (V8/V9 migrations)

`EMPLOYEE_CREATE/READ/UPDATE/DELETE/ACTIVATE/DEACTIVATE/ENABLE_LOGIN/
DISABLE_LOGIN/RESET_PASSWORD/ASSIGN_ROLE`, `DASHBOARD_ANALYTICS`.
`SUPER_ADMIN` receives all of them via `role_permissions` (not Java code);
`ADMIN` receives an operational subset. Existing permissions and grants are
untouched.

### New/changed API endpoints

| Method | Endpoint | Permission |
|---|---|---|
| GET | `/api/employees?search=&status=&department=&loginEnabled=&page=&size=&sort=` | `EMPLOYEE_READ` |
| GET | `/api/employees/{id}` | `EMPLOYEE_READ` |
| POST | `/api/employees` | `EMPLOYEE_CREATE` |
| PUT | `/api/employees/{id}` | `EMPLOYEE_UPDATE` |
| PUT | `/api/employees/{id}/activate` | `EMPLOYEE_ACTIVATE` |
| PUT | `/api/employees/{id}/deactivate` | `EMPLOYEE_DEACTIVATE` |
| DELETE | `/api/employees/{id}` (soft-delete → deactivate) | `EMPLOYEE_DELETE` |
| POST | `/api/employees/{id}/enable-login` | `EMPLOYEE_ENABLE_LOGIN` |
| POST | `/api/employees/{id}/disable-login` | `EMPLOYEE_DISABLE_LOGIN` |
| PUT | `/api/employees/{id}/role` | `EMPLOYEE_ASSIGN_ROLE` |
| POST | `/api/employees/{id}/reset-password` | `EMPLOYEE_RESET_PASSWORD` |
| POST | `/api/users/{id}/reset-password` (generic) | `USER_UPDATE` |
| GET | `/api/dashboard/summary` | `DASHBOARD_ANALYTICS` |

### Login enable/disable flow

- **Enable Login = OFF at creation:** only the Employee row is created. No
  User, no role assignment, no login possible.
- **Enable Login = ON at creation:** Employee + User + role assignment
  created atomically; `must_change_password=true` is set so the new user is
  forced to `/change-password` on first login.
- **Disable Login:** sets `users.is_active=false` and revokes all refresh
  tokens for that user. The Employee row and its status are untouched — the
  employee still appears in Employee Management as ACTIVE.
- **Re-enable Login:** if a `User` already exists (previously disabled),
  it's reactivated (`is_active=true`, unlocked, failed-attempt counter
  reset) — never duplicated. If no `User` exists yet, one is created.
- **Deactivate Employee:** sets the employee to INACTIVE and, if a login
  exists, also disables it and revokes its refresh tokens in the same
  transaction.

### Dashboard

`GET /api/dashboard/summary` computes all seven statistics server-side
(count queries, not loaded-and-filtered-in-Angular). The Angular dashboard
renders skeleton cards while loading and hides the summary section entirely
for users without `DASHBOARD_ANALYTICS`, while still showing them a
welcome/role header — the sidebar itself is filtered permission-by-permission
in `AppShellComponent`, so unauthorized modules never appear as links (the
backend enforces the real boundary regardless).

### Frontend structure additions

```
frontend/src/app/
├── theme.css                          CSS custom properties, shared component classes
├── shared/
│   ├── components/
│   │   ├── app-shell/                 sidebar + topbar + profile menu, wraps all authenticated routes
│   │   ├── toast/                     ToastContainerComponent (success/error/info/warning)
│   │   ├── confirm-dialog/            Promise-based confirmation modal
│   │   └── status-badge/              reusable colored status pill
│   └── services/
│       ├── toast.service.ts
│       └── confirm-dialog.service.ts
├── employee_module/
│   ├── components/employee-list, employee-form, employee-details
│   ├── services/employee.service.ts
│   └── models/employee.model.ts
├── dashboard_module/
│   ├── components/dashboard/          redesigned with summary cards + skeletons
│   └── services/dashboard.service.ts
├── user_module/
│   ├── components/user-list/          now a view over employee-linked accounts
│   └── services/user-management.service.ts
├── login_module/
│   └── components/change-password/    forced-password-change flow after admin-issued temp passwords
└── role_module, permission_module/
    └── services/                      role.service.ts, permission.service.ts (roles/permissions always fetched, never hardcoded)
```

### Test credentials & validation scenarios

Continue using `super_admin` / `admin123` (unchanged). To exercise the new
flow end to end:

1. Log in as `super_admin` → Dashboard loads with summary cards.
2. **Employees → Add Employee**, Enable Login OFF, e.g. `EMP001 / Rohit Patil`
   → employee appears in the list with a "⚪ Login Disabled" badge; no row
   appears for them under User Management.
3. **Employees → Add Employee**, Enable Login ON, e.g. `EMP002 / Amit Sharma`,
   role `USER` → employee shows "🟢 Login Enabled"; a row appears under User
   Management; the new user can log in and is redirected to
   `/change-password` on first login (temporary-password flow).
4. From Employee Details, **Disable Login** for Amit → badge flips to
   disabled, employee stays ACTIVE, Amit can no longer log in (401 on next
   request, refresh token revoked).
5. **Enable Login** again for Amit (no new username/password needed) → the
   same `User` row is reactivated, not duplicated; Amit can log in again.
6. **Deactivate** Rohit's employee record → status becomes INACTIVE; if he'd
   had a login it would be disabled automatically in the same action.
7. Log in as a `USER`-role account → sidebar shows only permitted modules;
   attempting a direct API call to an unpermitted endpoint (e.g.
   `/api/employees` without `EMPLOYEE_READ`) returns `403`, and the Angular
   guards/`*appHasPermission` directive keep the corresponding UI hidden.

### Security checklist for this phase

- [x] All new/changed endpoints permission-gated via `@PreAuthorize`, not role-name checks
- [x] Employee creation with login is one `@Transactional` unit — no partial state on failure
- [x] Passwords never returned in any Employee/User API response
- [x] Temporary passwords returned exactly once, from the endpoint that generated them, never logged
- [x] Disabling login revokes refresh tokens immediately (can't ride out an existing session)
- [x] Deactivating an employee cascades to disabling their login, not the reverse
- [x] Frontend guards/directives are UX only; every action re-validated server-side
- [x] No duplicate `User` rows possible on repeated enable/disable/enable cycles (unique `employees.user_id`, existing-row reuse logic)

---

## 21. Multi-Tenant Client Company / Sub-Client / Site (Phase 3)

This phase turns the application into a true multi-tenant system:
**SUPER_ADMIN → Client Company → Sub-Client → Site → Employee Assignment**,
with every tenant completely isolated from every other tenant at the
database-query level, not just hidden in the UI. Phase 1/2 authentication,
RBAC, and Employee Management are unmodified.

### The tenant boundary: `TenantContextService`

`common/tenant/TenantContextService` is the single, authoritative source for
"which tenant is this request operating as." Every tenant-scoped service goes
through it instead of trusting a `clientCompanyId` on a request DTO — most
tenant-scoped request DTOs (`EmployeeRequest`, `SiteRequest`,
`SubClientRequest`) have **no such field at all**, so there is nothing for a
malicious client to override in the first place.

- `currentTenantIdOrNull()` — the caller's own tenant, or `null` for
  `SUPER_ADMIN` (and the pre-existing internal `ADMIN` role, which keeps its
  original unrestricted visibility for backward compatibility).
- `requireCurrentTenantId()` — same, but throws a 400 if the caller has no
  tenant (used by endpoints that only make sense inside one tenant, e.g.
  creating a site).
- `resolveEffectiveTenantId(requestedId)` — for `SUPER_ADMIN`, honors an
  explicit filter; for a tenant-scoped user, always returns their own tenant
  and **ignores** anything the client sent.

Every tenant-scoped repository lookup uses `findByIdAndClientCompanyId(...)`
or `findAllByClientCompanyId(...)` — never a bare `findById`/`findAll` — so a
Client A admin can never fetch Client B's employee/site/sub-client/assignment
by guessing or incrementing an ID.

### Cross-tenant access never leaks existence

`TenantAccessDeniedException` is mapped to a **generic 404** ("Resource not
found"), identical to a real not-found, and logged server-side as
`TENANT_ACCESS_DENIED`. A cross-tenant probe against `/api/employees/999`
cannot distinguish "doesn't exist" from "exists but isn't yours."

### Schema (V10–V16)

| Migration | Adds |
|---|---|
| V10 | `client_companies` |
| V11 | `sub_clients`, `sites` (site has `required_employee_count`, `allow_over_allocation`) |
| V12 | `employees.client_company_id`, `employee_site_assignments` (assignment history: `start_date`/`end_date`/`status`/`is_primary`) |
| V13 | `users.client_company_id` (nullable — `NULL` for SUPER_ADMIN/house users) |
| V14 | `CLIENT_ADMIN` role |
| V15 | `CLIENT_COMPANY_*`, `SITE_*`, `SUBCLIENT_*`, `EMPLOYEE_ASSIGN*`, `CLIENT_DASHBOARD_VIEW`, `CLIENT_PROFILE_*` permissions |
| V16 | Grants — `SUPER_ADMIN` gets everything (same idempotent "all current permissions" pattern as V5/V9); `CLIENT_ADMIN` gets the operational subset from spec section 27, **deliberately excluding** `CLIENT_COMPANY_CREATE/ACTIVATE/DEACTIVATE` — only `SUPER_ADMIN` manages tenants themselves |

### How a tenant gets its data

- **Client Company creation** (`SUPER_ADMIN` only): `ClientCompanyService.create()` optionally creates a `CLIENT_ADMIN` `User` atomically alongside the company, mirroring the Employee "Enable Login" pattern. Deactivating a company cascades to disabling every login under it and revoking their refresh tokens, in one transaction.
- **Employee creation**: `employee.clientCompanyId` is always `tenantContextService.currentTenantIdOrNull()` — there is no field on `EmployeeRequest` for it. A login account created alongside an employee inherits the same tenant.
- **Site creation**: validates the chosen sub-client both exists *and* belongs to the current tenant before attaching (`SubClientService.getEntityForCurrentTenant`).
- **Employee Assignment**: `EmployeeAssignmentService` resolves both the employee and the site through tenant-scoped lookups before creating the link — an assignment can never mix an employee and a site from different tenants, even if both IDs are individually valid within their own tenant. Over-allocation is blocked unless `site.allowOverAllocation` is set. **Transfer** ends the current active assignment and creates a new one in the same transaction (history preserved, `EMPLOYEE_TRANSFERRED` audited). **Bulk assign** validates every employee individually and returns a per-employee rejection list rather than failing the whole batch.
- **Dashboard**: `SUPER_ADMIN` sees a global rollup (`clientCompanyRepository.count()`, etc.); everyone else sees `tenantSummary(currentTenantId)` — counts always computed with an explicit tenant filter, never by loading rows and filtering in Angular.

### New/changed API endpoints

| Method | Endpoint | Permission |
|---|---|---|
| GET/POST | `/api/client-companies` | `CLIENT_COMPANY_READ` / `CLIENT_COMPANY_CREATE` |
| PUT | `/api/client-companies/{id}`, `/{id}/activate`, `/{id}/deactivate` | `CLIENT_COMPANY_UPDATE` / `_ACTIVATE` / `_DEACTIVATE` |
| GET/POST | `/api/sub-clients` | `SUBCLIENT_READ` / `SUBCLIENT_CREATE` |
| PUT | `/api/sub-clients/{id}`, `/{id}/activate`, `/{id}/deactivate` | `SUBCLIENT_UPDATE` / `_ACTIVATE` / `_DEACTIVATE` |
| GET/POST | `/api/sites` | `SITE_READ` / `SITE_CREATE` |
| PUT | `/api/sites/{id}`, `/{id}/activate`, `/{id}/deactivate` | `SITE_UPDATE` / `_ACTIVATE` / `_DEACTIVATE` |
| GET | `/api/sites/{id}/employees` | `EMPLOYEE_ASSIGNMENT_READ` |
| GET/POST | `/api/employee-assignments` | `EMPLOYEE_ASSIGNMENT_READ` / `EMPLOYEE_ASSIGN` |
| POST | `/api/employee-assignments/bulk` | `EMPLOYEE_ASSIGN` |
| GET | `/api/employee-assignments/active` | `EMPLOYEE_ASSIGNMENT_READ` |
| POST | `/api/employee-assignments/bulk-end` | `EMPLOYEE_ASSIGN` |
| POST | `/api/employee-assignments/{id}/end` | `EMPLOYEE_ASSIGN` |
| POST | `/api/employees/{id}/transfer` | `EMPLOYEE_TRANSFER` |
| GET | `/api/employees/{id}/assignments` | `EMPLOYEE_ASSIGNMENT_READ` |
| GET | `/api/dashboard/summary` | `DASHBOARD_ANALYTICS` (global) or `CLIENT_DASHBOARD_VIEW`/`DASHBOARD_VIEW` (tenant) |

**Reassignment behavior:** `POST /api/employee-assignments` (single assign) automatically ends an
employee's existing active assignment if they're assigned elsewhere, then creates the new one —
functionally identical to Transfer, so moving someone to a different site is a single action from
the UI. Assigning to the *same* site they're already on is rejected as a duplicate. Bulk assignment
(`POST /api/employee-assignments/bulk`) intentionally keeps the stricter behavior — it's meant for
filling new headcount, so an already-assigned employee is skipped and reported in the response
rather than silently reassigned; use the single-assign endpoint or explicit Transfer for moving
existing staff.

**Assignment picker UI:** the Employee Assignment board (`GET /api/employee-assignments/active`
backs this) cross-references *every* active assignment across the tenant, not just the selected
site, so an employee already working somewhere else shows up in the picker **disabled**, with a
badge naming their current site — you can see them, you just can't double-book them from here.
**Unassigning is multi-select**: the "Currently Assigned" panel has per-row checkboxes plus a
"Select all" toggle and an "End Selected" button, backed by `POST /api/employee-assignments/bulk-end`,
which tenant-checks and ends each assignment and reports any that couldn't be ended (already-ended,
wrong tenant) without failing the whole batch.

**Deactivating a site cascades to unassigning its employees:** `PUT /api/sites/{id}/deactivate`
ends every active assignment at that site in the same transaction (mirroring how deactivating an
Employee or Client Company cascades to disabling logins) — the employees themselves are untouched
and become available to assign elsewhere immediately. A deactivated site also **disappears from the
"Select site" dropdown** on the Assignment board and loses its "Assign" action on the Site
Management list, so it can never be picked as a new assignment target while inactive; reactivating
it makes it selectable again (existing employees are *not* automatically re-assigned - that's a
deliberate choice, since "who should work there now" may have changed while the site was inactive).

### Frontend additions

`client_company_module`, `subclient_module`, `site_module`,
`employee_assignment_module` — each with models/services/list/form
components following the same pattern as `employee_module`. The Assignment
Board (`/employee-assignments`) is the main interactive screen: pick a site,
see required/assigned counts live, multi-select unassigned employees, set a
start date, bulk-assign, and end individual assignments. The Employee
Details page gained a **Site Assignment** panel (current site, transfer
form, assignment history). The sidebar (`AppShellComponent`) is unchanged in
mechanism — it already filters purely by permission, so a `CLIENT_ADMIN`
naturally never sees "Client Companies" without needing any role-specific
branching in the template.

### Validation scenarios (spec section 78)

1–9: `SUPER_ADMIN` creates Client A and Client B, each with a `CLIENT_ADMIN`
login; each admin creates employees; neither can see the other's employees
(`GET /api/employees/{otherTenantsId}` → 404).
10–15: Client A creates a sub-client, a site, assigns an employee to it;
attempting to assign that employee to a Client B site fails (the site lookup
itself returns 404 for a cross-tenant ID, so the assignment can't even be
attempted); transferring within Client A's own sites works and preserves
history.
16–18: Client A's dashboard shows only Client A's counts; Client B's shows
only Client B's; `SUPER_ADMIN`'s shows the global rollup.
19–20: `SUPER_ADMIN` can open any Client Company's detail/edit page (no
tenant restriction applies to `SUPER_ADMIN`'s own lookups).
21–23: A `CLIENT_ADMIN` gets 403 on `POST /api/client-companies` (lacks
`CLIENT_COMPANY_CREATE`); there is no field on any employee endpoint that
accepts a `clientCompanyId`, so there's nothing to tamper with; any
cross-tenant ID substitution returns 404 via `TenantAccessDeniedException`.
24–25: every unauthorized request returns the correct status, and
`CLIENT_CREATED/UPDATED/ACTIVATED/DEACTIVATED`, `SITE_*`,
`SUBCLIENT_*`, `EMPLOYEE_ASSIGNED/TRANSFERRED/UNASSIGNED`,
`BULK_EMPLOYEES_ASSIGNED` are all audited.

### Security checklist for this phase

- [x] Every tenant-scoped repository call uses `findByIdAndClientCompanyId`/`findAllByClientCompanyId`, never a bare lookup
- [x] No request DTO for a tenant-scoped create/update endpoint has a `clientCompanyId` field — nothing for a client to override
- [x] Cross-tenant access returns a generic 404, not a distinguishable 403/error message
- [x] `CLIENT_ADMIN` cannot create, activate, or deactivate Client Companies (SUPER_ADMIN-only by permission grant, not by role-name check)
- [x] Assignment creation validates employee and site belong to the *same* tenant before linking them, not just that each individually belongs to *a* tenant
- [x] Deactivating a Client Company cascades to disabling every login under it and revoking refresh tokens, in one transaction
- [x] Dashboard counts are computed server-side with an explicit tenant filter, never client-side filtered from an unscoped fetch

### Auto-generated codes (V17+)

Company Code, Employee Code, Sub-Client Code, and Site Code are now
**optional** on every create request. If left blank, the backend generates
the next sequential code itself:

- `ClientCompanyService`, `EmployeeService`, `SubClientService`,
  `SiteService` each fetch the **highest existing code with the relevant
  prefix** (`CLI`, `EMP`, `SUB`, `SITE`) — scoped **per tenant** for
  Employee/Sub-Client/Site codes, globally for Company codes since tenants
  have no parent scope — via a `findTopBy...OrderBy...Desc` repository
  query, then hand it to `CodeGeneratorService.nextCode(prefix, lastCode,
  padWidth)`, which parses the trailing digits and increments them
  (`CLI0007` → `CLI0008`; width grows automatically past 9999 rather than
  wrapping or failing).
- If a code **is** supplied, it's still validated for uniqueness exactly as
  before — auto-generation only fills the gap when the field is blank, it
  never overrides an explicit value.
- The Angular Employee form's Employee Code field is now optional with a
  placeholder hint; the same pattern applies to Client Company/Sub-Client/Site
  forms once built out on the frontend — they only need to omit the code
  field from the request to get an auto-generated one.

### Sample seed data (V17)

For local development, `V17__seed_sample_tenant_data.sql` creates a ready-to-explore tenant:

| What | Details |
|---|---|
| 1 Client Company | `CLI0001` — ABC Facility Management |
| 1 Client Admin login | username `client_admin`, password `admin123`, forced to change password on first login (`must_change_password=true`) |
| 5 Sub-Clients | `SUB0001`–`SUB0005` — XYZ IT Park, ABC Mall, DEF Hospital, PQR Tower, LMN School |
| 5 Sites | `SITE0001`–`SITE0005`, one per sub-client, with varied `required_employee_count` (20/15/10/8/5) for exercising the manpower-allocation dashboard |
| 10 Employees | `EMP0001`–`EMP0010`, all belonging to `CLI0001`, spread across Operations/Security/Maintenance/Housekeeping — none have login accounts yet, so you can exercise **Enable Login** from Employee Management immediately |

Log in as `client_admin` / `admin123` to see this tenant's data end to end
(after the forced password change); log in as `super_admin` / `admin123` to
see it from the platform-owner side via the Client Companies list.

### Sub-Client layer removed (V18)

The hierarchy was originally **Client Company → Sub-Client → Site**. It has
since been simplified to **Client Company → Site directly** — Sites no
longer have a `subClientId`; they belong straight to their Client Company.

`V18__remove_subclient_layer.sql` is an additive forward migration (nothing
in V11/V17 was edited in place, so this is safe to apply even against a
database where those already ran): it drops the `sub_client_id` FK/index/
column from `sites`, drops the `sub_clients` table outright, removes every
`SUBCLIENT_*` permission and its role grants, and renames the 5 seeded sites
so they read naturally as the client's own sites (e.g. what was "Main
Building" under sub-client "XYZ IT Park" is now simply the site "XYZ IT
Park"). The `subclient_module` (backend) and `subclient_module` (frontend)
were deleted entirely, along with the `/sub-clients` route, its sidebar
entry, and every `totalSubClients`/`subClientName` field across
`ClientCompanyResponse`, `SiteResponse`, `DashboardSummaryResponse`, and
their frontend models.

---

## 22. Department & Designation Master Data

Employee creation's Department and Designation fields are now **dropdowns**
backed by admin-managed, tenant-scoped master lists, instead of free text.

### Why this exists

Free-text fields let two employees end up with "Operations" and "operations"
as separate, effectively-duplicate departments. A managed list keeps the
dropdown authoritative and lets `CLIENT_ADMIN` add new options themselves —
e.g. adding "Site Manager" or "Site Supervisor" as new **designations** for a
housekeeping/facility-management business — without SUPER_ADMIN involvement.

**Note on terminology:** these are job-title/organizational categories, not
to be confused with the existing RBAC **Role** system (`SUPER_ADMIN`,
`CLIENT_ADMIN`, `USER`, etc. under Roles & Permissions), which controls
*login and API access*, not job titles. "Site Manager" is a Designation an
employee holds; it is not a role that grants them permissions. Keeping these
separate avoids a confusing, insecure mixing of "what job you do" with "what
you're allowed to click."

### Backend

- New `department_module`/`designation_module`: `Department`/`Designation`
  entities (`client_company_id`, `name`, `status`), tenant-scoped repositories,
  services, and controllers — the same shape as `site_module`.
- **Deactivating never deletes**: existing employees keep a valid value even
  after their department/designation is deactivated; it just stops appearing
  as a selectable option for *new* selections going forward.
- **`EmployeeService.create()`/`update()`** now validate that the submitted
  department/designation exists in the current tenant's master list (any
  status — active or deactivated — so editing an employee who already has a
  since-deactivated value doesn't break), rejecting anything else with a
  clear `400` pointing at where to add it. For SUPER_ADMIN/house context
  (no tenant), this validation is a no-op.
- New permissions: `DEPARTMENT_READ`, `DEPARTMENT_MANAGE`, `DESIGNATION_READ`,
  `DESIGNATION_MANAGE` — granted to `SUPER_ADMIN` (all) and `CLIENT_ADMIN`
  (both READ+MANAGE for each) via V21.
- V22 seeds `CLI0001`'s departments (Operations, Security, Maintenance,
  Housekeeping) and designations (Supervisor, Housekeeping Staff, Security
  Guard, Technician, Team Lead, Electrician, **Site Manager**, **Site
  Supervisor**) matching the 10 sample employees plus your requested examples.

### API

| Method | Endpoint | Permission |
|---|---|---|
| GET | `/api/departments?includeInactive=` | `DEPARTMENT_READ` |
| POST | `/api/departments` | `DEPARTMENT_MANAGE` |
| PUT | `/api/departments/{id}`, `/{id}/activate`, `/{id}/deactivate` | `DEPARTMENT_MANAGE` |
| GET | `/api/designations?includeInactive=` | `DESIGNATION_READ` |
| POST | `/api/designations` | `DESIGNATION_MANAGE` |
| PUT | `/api/designations/{id}`, `/{id}/activate`, `/{id}/deactivate` | `DESIGNATION_MANAGE` |

### Frontend

- New `department_module`/`designation_module` services + models.
- **`OrgSettingsComponent`** (`/org-settings`, sidebar "Departments &
  Designations") — one page managing both lists side by side: add new entries
  inline, see each one's current employee count, activate/deactivate —
  gated by `*appHasPermission="'DEPARTMENT_MANAGE'"` /
  `'DESIGNATION_MANAGE'` so a read-only user sees the lists without the
  add/edit controls.
- The Employee form's Department/Designation fields are now `<select>`
  dropdowns populated from these lists, with a "Don't see it? Add it from
  Departments & Designations" link. If an employee being edited has a
  since-deactivated value, it's shown in the dropdown suffixed `(inactive)`
  rather than silently disappearing.

---

## 24. Tenant-Scoped Custom Roles + Edit-Employee Enable Login

### Edit Employee now supports Enable Login directly

Previously, editing an employee who had no login account just showed a
message pointing to the Employee Details page. The Edit form now has the
same inline "Enable Login" mini-form Employee Details has (username,
password, role) — one less click, no page navigation required.

### Roles became tenant-scoped, and `CLIENT_ADMIN` can now create them

**What changed:**
- `roles.client_company_id` (nullable) was added (`V23`). `NULL` = system/house
  role (`SUPER_ADMIN`, `ADMIN`, `MANAGER`, `USER`, `CLIENT`, `CLIENT_ADMIN`,
  `CLIENT_USER` — all untouched). Non-null = a custom role created by that
  tenant's own Client Admin. The old *global* `UNIQUE(name)` constraint was
  replaced with `UNIQUE(client_company_id, name)`, so **Client A and Client
  B can each independently have a role named "Site Manager"** without
  colliding. (MySQL treats each `NULL` in a unique index as distinct, so
  this doesn't *database-level*-guarantee uniqueness among system roles
  themselves — but those are only ever created by trusted migrations or by
  `SUPER_ADMIN` through `RoleService`, which already checks for duplicates
  before insert, so this is a reasonable trade-off for a simpler, more
  portable migration.)
- `CLIENT_ADMIN` was granted `ROLE_READ`, `ROLE_CREATE`, `ROLE_UPDATE`,
  `ROLE_DELETE`, and `PERMISSION_READ` (`V24`). **This also fixes a
  pre-existing bug**: the employee-login role dropdown was already calling
  `GET /api/roles`, but `CLIENT_ADMIN` never actually had `ROLE_READ` —
  that call would have 403'd for every Client Admin before this change.
- Five example tenant-scoped roles were seeded for `CLI0001` (`V25`):
  **`ADMIN`, `HR_ADMIN`, `SITE_ADMIN`, `SITE_SUPERVISOR`, `ACCOUNTANT`**,
  each with a differentiated starter permission set (see the migration for
  the exact grants — e.g. `HR_ADMIN` gets employee/login management but not
  site management; `ACCOUNTANT` is read-mostly). `CLIENT_ADMIN` can add
  more roles like this themselves at any time from **Roles** → **+ Add
  Role**, or adjust these five, going forward — no `SUPER_ADMIN`
  involvement needed.

**On terminology** (per your question): these are **job-title/business
roles** an employee is *assigned*, not to be confused with the platform's
own authentication roles. A role named "ADMIN" created by Client A here is
a completely separate database row from the global system "ADMIN" role —
same name, different scope, no relationship.

### Security fix found while implementing this

Nothing previously stopped a `CLIENT_ADMIN` with `EMPLOYEE_ENABLE_LOGIN`
from assigning the global `SUPER_ADMIN` role to an employee's new login —
instantly creating a second super-admin account. Two guardrails now close
this, enforced centrally in `RoleService` (never bypassable via a different
code path, since `EmployeeService` now resolves every role ID through
`RoleService.resolveAssignableRoleForCurrentTenant()`):

1. **Ownership** — a non-`SUPER_ADMIN` can only create, edit, delete, or
   assign roles that belong to their **own tenant**, plus a small safelist
   (`CLIENT_ADMIN`, `CLIENT_USER`) offered as defaults to a brand-new tenant
   that hasn't created custom roles yet. All other global/house roles
   (`SUPER_ADMIN`, `ADMIN`, `MANAGER`, `USER`, `CLIENT`) are invisible and
   unassignable to tenant users.
2. **Permission ceiling** — a non-`SUPER_ADMIN` can **never attach a
   permission to any role that they do not themselves currently hold**,
   full stop — regardless of which tenant "owns" that role. This is the
   real fix: tenant-scoping the *role* row doesn't tenant-scope the
   *permission's* effect, since `@PreAuthorize` only ever checks the
   authority name. Attempting to grant a permission you don't have returns
   a clear `400` listing exactly which ones were rejected. The Angular "Add
   Role" form mirrors this by only showing checkboxes for permissions the
   logged-in user's own account currently has, so the UI can't even attempt
   what the backend would reject.

A related correctness fix: `ClientCompanyService`'s lookup of the global
`CLIENT_ADMIN` role by name was ambiguous once role names could repeat
across tenant scopes — it now uses a dedicated
`findByClientCompanyIdIsNullAndName` query that can only ever match the
one true global role.

### Frontend

Role Management (`/roles`) went from a read-only table to a full **Add
Role** form: name, description, and a permission checklist scoped to the
current user's own permissions, plus a "Custom"/"System" badge per row.

---

## 25. Troubleshooting: Flyway "Detected failed migration"

If `mvn spring-boot:run` fails with something like:

```
FlywayValidateException: Validate failed: Migrations have failed validation
Detected failed migration to version 23 (tenant scoped roles).
Please remove any half-completed changes then run repair to fix the schema history.
```

**What this means:** a *previous* run applied that migration's SQL and it
errored out partway through (or the app was killed mid-migration). Flyway
recorded that attempt as failed in its `flyway_schema_history` table and
now refuses to run anything else — including a corrected version of the
same file — until that failed record is cleared. This is Flyway protecting
you from silently retrying against a half-altered schema; it is not
something a code fix alone can resolve, since the problem is state that
already exists in *your* database, not in the migration file itself.

**Fastest fix for local development** (this environment only has sample/seed
data, so there's nothing worth preserving):

```sql
DROP DATABASE workforce_auth;
CREATE DATABASE workforce_auth CHARACTER SET utf8mb4;
```

Then run `mvn spring-boot:run` again — Flyway applies all migrations from
`V1` forward, cleanly, with no history to conflict with.

**Known root cause we hit during development:** the original `V23` tried to
add a `STORED` generated column to the `roles` table right after adding a
foreign key to it. MySQL/InnoDB has a documented limitation where this
combination fails with `Error 1215: Cannot add foreign key constraint` —
even though the failing statement doesn't touch any FK itself. The fix
(already applied in this repo's `V23`) was to drop the generated-column
approach entirely in favor of a plain `UNIQUE(client_company_id, name)`
constraint. If you're seeing this exact error on version 23, resetting your
database (above) with the current `V23` file resolves it.

**If you need to keep existing data**, repair instead of dropping:

1. Manually inspect the `roles` table and undo whatever partially applied
   (e.g. `ALTER TABLE roles DROP COLUMN client_company_id;` if that column
   exists but the new unique constraint doesn't) so the
   table matches its state *before* `V23` ran at all.
2. Delete the failed row from history: `DELETE FROM flyway_schema_history
   WHERE version = '23';` (or run `mvn flyway:repair` if the Flyway Maven
   plugin is configured).
3. Re-run `mvn spring-boot:run`.

Option 1 (drop & recreate) is almost always simpler and is what's
recommended here unless you've already entered real data you can't lose.

---

## 26. Attendance Module

### Business rules implemented

- **Who can mark attendance:** `SITE_ADMIN` and `SITE_SUPERVISOR` (both
  granted `ATTENDANCE_CREATE` in `V28`), plus `CLIENT_ADMIN` and
  `SUPER_ADMIN`.
- **Which employees:** only employees with a currently **active site
  assignment** — attendance is inherently site-scoped, so an unassigned
  employee has nothing to mark attendance against. The site recorded on
  each attendance row is taken from the employee's active assignment at
  the moment attendance is marked, not asked for separately.
- **Which dates:** today or any previous date (`@PastOrPresent` validation
  server-side) — never a future date.
- **Mark once, then locked:** `POST /api/attendance` flatly refuses to
  create a second row for the same employee+date — there is no "overwrite"
  branch in `AttendanceService.mark()` at all, backed by a database-level
  `UNIQUE(client_company_id, employee_id, attendance_date)` constraint so
  even a race between two concurrent requests can't double-mark a day.
- **Who can correct a mistake:** only holders of `ATTENDANCE_UPDATE`
  (`CLIENT_ADMIN` and the tenant-scoped `ADMIN` role by default grant) can
  reach `PUT /api/attendance/{id}` at all — it's a separate,
  permission-gated endpoint, not a special case inside `mark()`. `SITE_ADMIN`
  and `SITE_SUPERVISOR` get a clean `403` if they somehow call it directly.
- **Viewing history:** both roles can view any employee's attendance over a
  date range via `GET /api/attendance/employee/{id}?from=&to=` — no site
  restriction on *reading*, only on *marking* the current day's employees
  (the "markable" list is what's scoped to actively-assigned employees).

### A scoped simplification, stated plainly

The spec described these roles as tied to *their own* site
("tya site la jo employee ahe tyachi attendance bharu shaktat"). This
codebase has no existing concept of "which site does this login belong
to" — `SITE_ADMIN`/`SITE_SUPERVISOR` are tenant-scoped roles, not
site-scoped ones, and there's no user-to-site binding table. Rather than
invent one without being asked, **attendance marking is currently scoped to
"any actively-assigned employee in the tenant"**, filterable by site in the
UI for convenience, not enforced as a hard per-user site boundary. If you
want a real per-supervisor site restriction (e.g. a `site_supervisors`
mapping table + a tenant-context check mirroring the tenant-isolation
pattern used throughout this app), that's a natural follow-up — say the
word and it can be added the same way `TenantContextService` scopes
everything else.

### API

| Method | Endpoint | Permission |
|---|---|---|
| GET | `/api/attendance/markable?date=&siteId=` | `ATTENDANCE_CREATE` |
| POST | `/api/attendance` | `ATTENDANCE_CREATE` |
| PUT | `/api/attendance/{id}` | `ATTENDANCE_UPDATE` |
| GET | `/api/attendance/employee/{id}?from=&to=` | `ATTENDANCE_READ` |
| GET | `/api/attendance?from=&to=&siteId=&page=&size=` | `ATTENDANCE_READ` |

### Frontend

- **Mark Attendance** (`/attendance`): date picker (max = today) + optional
  site filter, showing every actively-assigned employee for that filter.
  Already-marked employees show a locked status badge instead of a
  dropdown; unmarked ones get a status selector + optional remarks + Save.
- **Attendance History** (`/attendance/history`): employee picker + date
  range, listing that employee's records. Rows include an inline **Edit**
  action, but only when the record's `editable` flag (computed server-side
  from the *current caller's* own `ATTENDANCE_UPDATE` permission) is true —
  a `SITE_SUPERVISOR` viewing the exact same history sees no edit option at
  all, matching the backend rule exactly rather than just hiding a button
  that would 403 anyway.

---

## 27. Grouped Sidebar (Menu / Submenu)

The sidebar had grown to 12 flat links, so it's now organized into a
dashboard link plus four collapsible groups:

- **Organization** — Client Companies, Sites
- **Workforce** — Employees, Departments & Designations, Employee Assignments
- **Attendance** — Mark Attendance, Attendance History
- **Administration** — User Management, Roles, Permissions, Audit Logs

A group only appears at all if at least one of its children is visible to
the current user's permissions (same per-item permission filtering as
before, just applied per-group now). Whichever group contains the page
you're currently on auto-expands on load and on navigation, so you're never
looking at a collapsed group hiding the page you're already viewing.
Clicking a group header toggles it; multiple groups can be open at once.

---

## 28. Dummy Attendance Data (V29 + V30)

Two additive seed migrations make the Attendance module immediately
demoable without marking anything by hand first:

- **`V29`** assigns each of the 10 sample employees to one of the 5 sample
  sites (2 employees per site) as an `ACTIVE` assignment — the sample data
  from `V17` had no site assignments at all, so "Mark Attendance" would
  otherwise show zero eligible employees.
- **`V30`** backfills the **last 7 days** (not including today, so there's
  still something to try marking live) of attendance for all 10 employees,
  mostly `PRESENT` with a handful of `ABSENT`/`HALF_DAY`/`ON_LEAVE` days
  mixed in for a realistic-looking history. Dates are computed from
  `CURDATE()` at migration-run time (not hardcoded), so "the last week"
  stays accurate no matter when you actually apply the migration.

**One honest caveat:** these are one-time `INSERT` migrations. If you'd
already manually marked attendance for one of these employees on one of
these same relative dates before running `V30`, you'd hit the
`UNIQUE(employee, date)` constraint and the migration would fail — a
non-issue on a fresh database (the normal case for pulling this update),
but worth knowing if you're layering this onto a database where you'd
already started using the feature.

---

## 29. Bulk Attendance Saving + Sidebar Highlight Fix

### "100 employees = 100 saves" fixed

Marking attendance previously required clicking Save once per employee row
— genuinely unworkable at real headcount. It's now:

- Each unmarked row gets a status dropdown as before, but **no per-row save
  button** — picking a status just stages it locally.
- **"Mark all unmarked as Present"** fills every still-unmarked row with
  `PRESENT` in one click (the common case on most days), and any employee
  who actually needs a different status can still change their own
  dropdown afterward.
- **One "Save All (N)" button** submits every staged row in a single
  request via the new `POST /api/attendance/bulk` endpoint.

The backend refactored the single-employee `mark()` logic into a shared
`markOne()` helper, so `mark()` and `bulkMark()` enforce the exact same
rules (active-assignment check, one-time-only, status validation) — bulk
marking isn't a separate, looser code path. Each entry in a bulk request
succeeds or fails independently: one employee already marked or lacking an
active assignment doesn't block the other 99 from saving. The response
reports exactly how many were marked and, for any that weren't, why —
mirroring the same pattern already used for `POST /api/employee-assignments/bulk`.

### Sidebar highlight bug fixed

Clicking **Attendance History** left **Mark Attendance** visually
highlighted too, because Angular's `routerLinkActive` defaults to
*prefix* matching — `/attendance` is technically a prefix of
`/attendance/history`, so both looked "active" at once even though
they're sibling pages, not a parent/child pair. Every sidebar link now
sets `[routerLinkActiveOptions]="{exact: true}"`, so a link only
highlights when its own path is the one actually open. The *group*
auto-expand logic (which group's submenu should be open) intentionally
keeps prefix matching — you still want the whole "Attendance" group open
on both of its sub-pages — only the individual link highlight changed.

---

## 31. Payroll Structure: Basic Salary + PF % + Other Deductions

### What it does

- Every **Designation** now carries a real payroll structure, not a single
  number: **Basic Salary**, **PF %** (Provident Fund deduction, defaults to
  the standard 12%), and **Other Deductions** (a fixed amount — uniform,
  canteen, etc.). This applies to every employee holding that designation
  by default. `DesignationResponse` also returns a computed **Net Salary**
  (`Basic − PF amount − Other Deductions`) for quick reference.
- Every **Employee** can optionally override **any combination** of the
  three components from **Edit Employee** — each of the three is
  *independently* nullable. The common case matches the original request
  exactly: a designation pays ₹22,000 basic to everyone, but one specific
  employee got raised to ₹25,000 — only their `basicSalaryOverride` is set;
  their PF % and other deductions keep inheriting from the designation, so
  a future change to the designation's PF %/deductions still reaches them
  too.
- The employee's **effective pay** is computed field-by-field (override if
  set, else the designation's current value for that one field), then PF
  amount and net salary are derived from the effective basic + effective
  PF %. Nothing is duplicated onto every employee row — raising a whole
  designation's basic salary is one edit on the designation, and every
  non-overridden employee picks it up automatically; only genuinely
  overridden employees are decoupled from a specific field going forward.

### Salary is more sensitive than general employee data, so it's separately permissioned

Two permissions, deliberately **not** folded into `EMPLOYEE_READ`/
`EMPLOYEE_UPDATE`:

- **`EMPLOYEE_SALARY_READ`** — see any pay figures at all.
- **`EMPLOYEE_SALARY_UPDATE`** — actually set/change any of the three overrides.

Default grants (`V33`): `SUPER_ADMIN` (everything), `CLIENT_ADMIN` (global)
and the tenant-scoped `ADMIN`/`HR_ADMIN` roles get both; `ACCOUNTANT` gets
read-only (their job is visibility, not changing pay); `SITE_ADMIN` and
`SITE_SUPERVISOR` get **neither** — they manage site operations and
attendance, not payroll.

**Two enforcement points, not one:**
1. `EmployeeResponse` only populates any of the eleven salary-related
   fields when the caller holds `EMPLOYEE_SALARY_READ` — otherwise they
   all stay unset with `salaryVisible: false`, so the frontend can tell
   "no permission to see this" apart from "the amount happens to be zero"
   and hide the whole section rather than show a misleading blank.
2. `EmployeeService.create()`/`update()` reject any *actual change* to
   **any of the three** override fields from a caller without
   `EMPLOYEE_SALARY_UPDATE` — compared against what's already stored using
   `BigDecimal.compareTo()`, not `.equals()` (a real bug caught during
   implementation: `.equals()` is scale-sensitive, so `15000` vs
   `15000.00` would have wrongly counted as "changed" and blocked
   legitimate saves). Someone editing a name or department on a form that
   never showed them the salary section at all isn't blocked just because
   their client echoed back the same unseen values.

### Sample data (`V36`)

All 8 sample designations from `V22` now have realistic monthly INR
figures (₹12,000–₹35,000 basic, 12% PF, ₹200–₹500 other deductions,
scaled roughly with seniority — see the migration for exact numbers per
designation). `EMP0001` (Rohit Patil, a Supervisor — designation basic
₹22,000) has a personal `basicSalaryOverride` of ₹25,000 set, with PF %/
deductions left as `NULL` so they keep inheriting from the Supervisor
designation — a live, working example of exactly the scenario described in
the request.

### Frontend

- **Departments & Designations** (`/org-settings`): the Designations panel
  has three inputs when adding (Basic Salary, PF %, Other Deductions), a
  wider table showing all four figures (including computed Net Salary),
  and an inline **Edit Pay** action that edits all three fields together
  per row.
- **Employee Add/Edit form**: a Salary section appears only for users with
  `EMPLOYEE_SALARY_READ`, showing the selected designation's default
  structure as reference text (looked up live as you change the
  Designation dropdown), three independent override inputs — editable only
  with `EMPLOYEE_SALARY_UPDATE`, read-only text otherwise — and a live
  client-side net-salary preview so the form isn't a black box while
  filling it in (the backend recomputes the authoritative value the same
  way on save).
- **Employee Details**: shows the designation's structure, this employee's
  own overrides (or "None" per field), and a highlighted effective-pay
  summary (Basic − PF amount − Other Deductions = Net Salary) — only
  rendered at all when `salaryVisible` is true.

---

## 31a. Scope Note: Full Payroll Engine Request (Salary Structure only, by explicit choice)

A follow-up request asked for a full production payroll engine — Salary
Structures as a standalone entity with configurable components, Payroll
Rules, Payroll Runs/Processing, Bonus & Deduction workflows, Employee
Advances/Loans with recovery schedules, PDF Payslips, a payroll dashboard,
CSV/Excel export, and roughly 30 new database tables in total.

Given the scope difference between "tweak what exists" and "build a second
major subsystem," I asked which was intended rather than guess wrong in
either direction. The answer: **keep the existing Designation-based
structure exactly as it is architecturally** (no new `salary_structures`,
`payroll_runs`, `bonuses`, `advances`, `loans`, or `payslips` tables), and
only align **terminology** with the standard payroll formula the larger
spec used throughout: **Gross Earnings − Total Deductions = Net Salary**.

**What actually changed:** `DesignationResponse` and `EmployeeResponse`
gained two new computed fields each — `grossEarnings`/`effectiveGrossEarnings`
(currently equal to Basic Salary, since Basic is still the only earning
component) and `totalDeductions`/`effectiveTotalDeductions` (PF amount +
Other Deductions, combined into one figure instead of asking the reader to
add two numbers). The Employee form's designation-reference text and the
Employee Details pay summary now read as Gross → Total Deductions → Net,
matching that formula shape. Nothing else from the full payroll spec
(separate Salary Structure entity, Payroll Rules, Payroll Processing,
Bonus/Deduction, Advance/Loan, Payslip generation, payroll dashboard,
export) was implemented — those remain a clearly separate, much larger
piece of work if wanted later, and this note exists so that scope decision
is traceable rather than silently dropped.

**Update:** the user came back and asked for the full scope after all. The
Salary Structure module (section 32 below) is the first piece of that
larger build — Payroll Processing, Bonus/Deduction, Advance/Loan, and
Payslip are the natural next pieces, in that dependency order, since each
one builds on Salary Structure's output.

---

## 32. Salary Structure Module (Full Payroll Build, Part 1)

This is the foundation the rest of the payroll engine (Payroll Processing,
Bonus/Deduction, Advance/Loan, Payslip) will build on. It's additive to,
and separate from, the simpler Designation-based Basic/PF%/Deductions
fields from section 31 — both currently coexist; nothing from section 31
was removed.

### Data model

- **`salary_components`** (`V37`) — database-driven line items (BASIC, HRA,
  PF, ESI...), never hardcoded. Each has a `component_type` (`EARNING`,
  `DEDUCTION`, `EMPLOYER_CONTRIBUTION`, `REIMBURSEMENT`) and a
  `calculation_type` (`FIXED`, `PERCENTAGE_OF_BASIC`, `PERCENTAGE_OF_GROSS`,
  `PER_DAY`, `PER_HOUR`, `MANUAL`) plus its own default value/percentage.
- **`salary_structures`** + **`salary_structure_components`** (`V38`) — a
  named, reusable template (e.g. "Housekeeping Staff Grade A") built from a
  set of components, each with its OWN calculation type/amount/percentage
  *within that structure* (so the same "HRA" component can be a fixed 3000
  in one structure and 10% of basic in another).
- **`employee_salary_structures`** (`V39`) — assignment history, mirroring
  the exact "never overwrite, always end the old row and start a new one"
  pattern already used for `employee_site_assignments`: the current
  structure is the row with `status=ACTIVE` and `effective_to IS NULL`;
  assigning a new one ends the previous row rather than mutating it, so a
  past payroll period can always resolve exactly which structure applied on
  that date.

### The calculation engine (no unsafe formula evaluation)

Per spec section 11, **`FORMULA` calculation is deliberately not
supported** — there is no dynamic expression evaluator anywhere in this
codebase. `SalaryStructureService` resolves the four calculation types that
don't require attendance data (`FIXED`, `PERCENTAGE_OF_BASIC`,
`PERCENTAGE_OF_GROSS`, `MANUAL`; `PER_DAY`/`PER_HOUR` resolve as a flat
reference amount here, since real per-day/hour figures need attendance
data that belongs to the not-yet-built Payroll Processing module) in a
fixed, documented order to avoid circular dependencies:

1. `FIXED`/`MANUAL`/`PER_DAY`/`PER_HOUR` resolve to their own amount.
2. The resolved amount of the `EARNING` component coded `BASIC` becomes "basic".
3. `PERCENTAGE_OF_BASIC` components resolve against that basic figure.
4. "Gross Earnings so far" = sum of all resolved `EARNING` components.
5. `PERCENTAGE_OF_GROSS` components resolve against that gross figure (not
   recursively against a gross that includes themselves).
6. Total Deductions = sum of all `DEDUCTION` components.
7. `REIMBURSEMENT` components are paid to the employee but aren't part of
   taxable Gross Earnings — added to Net separately.
8. `EMPLOYER_CONTRIBUTION` components are cost to the employer only and
   never reduce the employee's Net Salary.
9. **Net Salary = Gross Earnings − Total Deductions + Reimbursements.**

All monetary math uses `BigDecimal` with `HALF_UP` rounding to 2 decimal
places — never `double`/`float` — per spec sections 27-28.

### Sample data (`V42`)

Reproduces spec section 7's exact worked example: **Housekeeping Staff
Grade A** — Basic ₹15,000 (`FIXED`) + HRA ₹3,000 + Conveyance ₹1,500 +
Special Allowance ₹1,000 = **Gross ₹20,500** (matches the spec exactly),
plus PF at 12% of basic (`PERCENTAGE_OF_BASIC`, ₹1,800), ESI at 0.75% of
gross (`PERCENTAGE_OF_GROSS`, ₹153.75), and a fixed ₹200 Professional Tax —
**Net ₹18,346.25**. Assigned to `EMP0002` effective from their joining
date, demonstrating the full employee-assignment flow with real data.

### Permissions

`SALARY_STRUCTURE_CREATE/READ/UPDATE/DELETE` and `SALARY_ASSIGN` (`V40`),
granted (`V41`) to `SUPER_ADMIN` (all), `CLIENT_ADMIN`/tenant
`ADMIN`/`HR_ADMIN` (full CRUD + assign), `ACCOUNTANT` (read-only);
`SITE_ADMIN`/`SITE_SUPERVISOR` get neither. Component management shares the
same permissions as structures (components are a sub-concern of
structures, not separately permissioned — matches the spec's permission
list, which doesn't name component-level permissions separately).

### API

| Method | Endpoint | Permission |
|---|---|---|
| GET/POST | `/api/salary-components` | `SALARY_STRUCTURE_READ` / `_CREATE` |
| PUT | `/api/salary-components/{id}`, `/{id}/activate`, `/{id}/deactivate` | `SALARY_STRUCTURE_UPDATE` |
| GET/POST | `/api/salary-structures` | `SALARY_STRUCTURE_READ` / `_CREATE` |
| PUT/DELETE | `/api/salary-structures/{id}` | `SALARY_STRUCTURE_UPDATE` / `_DELETE` |
| PUT | `/api/salary-structures/{id}/activate`, `/{id}/deactivate` | `SALARY_STRUCTURE_UPDATE` |
| POST | `/api/salary-structures/{id}/duplicate` | `SALARY_STRUCTURE_CREATE` |
| GET/POST | `/api/employees/{id}/salary-structure` | `SALARY_STRUCTURE_READ` / `SALARY_ASSIGN` |
| GET | `/api/employees/{id}/salary-history` | `SALARY_STRUCTURE_READ` |

A structure can't be deleted once it's ever been assigned to an employee
(`SalaryStructureService.delete()` checks this explicitly) — deactivate it
instead, matching the same "deactivate, never delete something referenced
elsewhere" pattern used throughout this codebase (Department, Designation,
Site).

### Frontend

- **Salary Components** (`/salary-components`) and **Salary Structures**
  (`/salary-structures`) join a new **Payroll** sidebar group (more payroll
  modules will land here as they're built).
- The Salary Structure form has dynamic component line items (add/remove
  rows, per-row calculation-type-dependent fields) and a **live client-side
  preview** mirroring the backend's exact resolution order, so the form
  isn't a black box while building a structure — the backend recalculates
  authoritatively on save regardless.
- **Employee Details** gained a "Salary Structure" card (spec section 16):
  current structure, effective-from date, Gross/Deductions/Net, an
  **Assign/Change Salary Structure** action, and a togglable **Salary
  History** list — all gated behind `SALARY_STRUCTURE_READ`, with the
  assign action further gated behind `SALARY_ASSIGN`.

---

## 33. Final Verification Checklist

- [x] Maven dependencies present for Security 6 / JPA / MySQL / Flyway / jjwt 0.12.x / springdoc
- [x] Java 21 language features used (records not required, text blocks avoided for portability)
- [x] Entity relationships (`User↔Role↔Permission` many-to-many, `RefreshToken→User`,
      `AuditLog→User` nullable FK) match the Flyway schema
- [x] Flyway scripts run in numeric order (`V1`→`V5`) with FK-safe ordering
- [x] `SUPER_ADMIN` permissions come from `role_permissions` rows, not Java conditionals
- [x] Angular standalone bootstrap (`main.ts` → `appConfig` → `app.routes.ts`)
- [x] Guards registered functionally (`authGuard`, `roleGuard`, `permissionGuard`) on routes
- [x] Interceptor registered via `provideHttpClient(withInterceptors([authInterceptor]))`
- [x] Login request/response shapes match `LoginRequest`/`LoginResponse` on both sides
- [x] Refresh rotation implemented on both `/refresh` and re-used by the interceptor
- [x] Logout revokes token + clears cookie + clears client state
- [x] Employee/login-enable atomicity: single `@Transactional` boundary, verified by code review of `EmployeeService`
- [x] No duplicate `User` creation on repeated enable/disable cycles (unique `employees.user_id` + reuse-if-present logic)
- [x] Tenant isolation verified by code review: every tenant-scoped repository method takes a `clientCompanyId`, no tenant-scoped DTO accepts one as input, `TenantAccessDeniedException` maps to a generic 404
- [x] Frontend Angular signal/ngModel binding audited app-wide: `[(ngModel)]` never targets a signal directly (caught and fixed one instance in the Assignment Board)
- [ ] **Not verified by an actual compiler run** — see the honesty note at the top. Run
      `mvn clean verify` and `npm run build` before deploying.
