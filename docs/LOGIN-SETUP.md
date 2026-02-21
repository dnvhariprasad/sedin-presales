# Login Setup

## Current Status
- Registration is not implemented.
- User onboarding is admin-managed (`users` table / future admin module).

## Auth Modes

### 1) `dev` mode (default fallback)
- Used when `VITE_AUTH_MODE=dev` or Entra env vars are missing.
- Login page shows **Continue as Dev Admin**.
- Uses a local demo session for quick verification.
- Demo account identity:
  - Email: `dev.admin@sedin.com`
  - Role: `ADMIN`

### 2) `entra` mode
- Used when `VITE_AUTH_MODE=entra` (or Entra env vars are provided).
- Login page shows **Continue with Microsoft** via MSAL.
- Required frontend env vars:
  - `VITE_AZURE_TENANT_ID`
  - `VITE_AZURE_CLIENT_ID`
  - `VITE_AZURE_API_SCOPE` (optional but recommended)
  - `VITE_AZURE_REDIRECT_URI` (optional; defaults to current origin)

## Seeded Account
- Added migration: `src/backend/src/main/resources/db/migration/V14__seed_default_login_user.sql`
- Inserts `dev.admin@sedin.com` as `ADMIN` if missing.

