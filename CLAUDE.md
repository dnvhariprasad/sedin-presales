# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project: Sedin Pre-Sales Asset Management System

A Document Management System (DMS) for managing pre-sales assets (case studies, white papers), built on Azure. Replaces SharePoint with AI-powered search, automated case study formatting, and document intelligence.

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.4, Spring Data JPA, Spring Security, Spring AI
- **Frontend:** React 18+ with TypeScript, Vite, shadcn/ui, TailwindCSS v4, TanStack Query
- **Database:** Azure Database for PostgreSQL Flexible Server (B1ms, v16)
- **File Storage:** Azure Blob Storage (Standard LRS, Hot tier)
- **AI/RAG:** Azure AI Search, Azure OpenAI (GPT-4o-mini, text-embedding-3-small), Azure Document Intelligence, Spring AI
- **Auth:** Microsoft Entra ID (Azure AD) with Spring Security OAuth2 + MSAL4J
- **PDF Rendition:** Aspose.Total for Java (license valid till Dec 2024, evaluation mode after)
- **PPT Generation:** Aspose.Slides for Java
- **DB Migrations:** Flyway
- **Hosting:** Azure App Service (API + Frontend), Azure Functions (background jobs) — all in `centralus`
- **CI/CD:** GitHub Actions → Azure (JAR deploy, no Docker)

## Repository Structure

```
sedin-presales/
├── src/
│   ├── backend/                              # Single Maven project (package-based layering)
│   │   ├── pom.xml                           # Single POM with all dependencies
│   │   └── src/main/java/com/sedin/presales/
│   │       ├── PresalesApplication.java      # Spring Boot entry point
│   │       ├── domain/entity/                # JPA entities (BaseEntity, etc.)
│   │       ├── domain/repository/            # Repository interfaces
│   │       ├── application/service/          # Business logic, DTOs
│   │       ├── infrastructure/               # Azure SDK clients, Aspose integration
│   │       ├── api/controller/               # REST controllers
│   │       └── config/                       # SecurityConfig, etc.
│   └── frontend/                             # React + Vite + shadcn/ui
├── infra/                                    # Azure provisioning scripts (az cli)
│   ├── 01-resource-group.sh                  # rg-sedin-presales (eastus)
│   ├── 02-storage.sh                         # stsedinpresales (eastus)
│   ├── 03-database.sh                        # psql-sedin-presales (centralus)
│   ├── 04-ai-services.sh                     # OpenAI + AI Search + Doc Intelligence
│   ├── 05-app-service.sh                     # App Service Plan + API + Frontend + Functions (centralus)
│   ├── 07-entra-app-registration.sh          # Entra ID (requires admin permissions)
│   ├── provision-all.sh                      # Master script
│   ├── teardown.sh                           # Deletes resource group
│   └── output/azure-resources.env            # Generated connection strings/keys
├── .github/workflows/                        # GitHub Actions CI/CD
├── docs/                                     # PRD, Tech Stack, Architecture
└── Sample Case Studies/                      # Reference PPT files (5 samples)
```

## Build & Run Commands

### Backend (Java 21 / Maven)
```bash
# Build
mvn -B clean compile -f src/backend/pom.xml

# Run API locally (dev profile: ddl-auto=update, flyway disabled)
mvn -B spring-boot:run -Dspring-boot.run.profiles=dev -f src/backend/pom.xml

# Run all tests (96 tests)
mvn -B test -f src/backend/pom.xml

# Run single test class
mvn -B test -Dtest=MasterServiceTest -f src/backend/pom.xml

# Run single test method
mvn -B test -Dtest=MasterServiceTest#create_shouldSaveAndReturnDto -f src/backend/pom.xml

# Run tests by category (service tests only)
mvn -B test -Dtest="MasterServiceTest,FolderServiceTest,DocumentServiceTest,AclServiceTest,PermissionEvaluatorTest" -f src/backend/pom.xml

# Run tests by category (controller tests only)
mvn -B test -Dtest="MasterControllerTest,FolderControllerTest,DocumentControllerTest,AclControllerTest" -f src/backend/pom.xml

# Package JAR for deployment
mvn -B clean package -DskipTests -f src/backend/pom.xml
```

### Frontend (React + Vite)
```bash
cd src/frontend
npm install
npm run dev          # Dev server (port 5173, proxies /api to localhost:8080)
npm run build        # Production build
npm run lint         # ESLint
```

## Azure Resources (Resource Group: rg-sedin-presales)

| Resource | Name | Region |
|----------|------|--------|
| Resource Group | `rg-sedin-presales` | eastus |
| Storage Account | `stsedinpresales` (5 containers: originals, renditions, summaries, templates, temp) | eastus |
| PostgreSQL Flexible Server | `psql-sedin-presales` (DB: `presalesdb`, User: `presalesadmin`) | centralus |
| Azure OpenAI | `aoai-sedin-presales` (GPT-4o-mini + text-embedding-3-small, GlobalStandard SKU) | eastus |
| Azure AI Search | `search-sedin-presales` (Basic tier) | eastus |
| Document Intelligence | `di-sedin-presales` (S0) | eastus |
| App Service Plan | `asp-sedin-presales` (B1, Linux) | centralus |
| App Service (API) | `app-sedin-presales-api` (Java 21) — `https://app-sedin-presales-api.azurewebsites.net` | centralus |
| App Service (Frontend) | `sedin-presales-web` (Node 20) — `https://sedin-presales-web.azurewebsites.net` | centralus |
| Azure Functions | `func-sedin-presales` (Java 21, on App Service plan) | centralus |

**Note:** Entra ID app registration is pending — requires Azure AD admin permissions.

**Note:** PostgreSQL, App Service, and Functions are in `centralus` because `eastus` has provisioning restrictions for these resource types on this subscription.

## Key Architecture Decisions

- **Single Maven Project:** One `pom.xml`, package-based layering (not multi-module). Packages: `domain` → `application` → `infrastructure` → `api` → `config`.
- **Clean Architecture:** Dependency flows inward. Controllers never access repositories directly; always go through services.
- **Document-level ACLs:** Every document has explicit ACL entries. ACL checks enforced at the repository/service level, not in controllers.
- **Rendition pipeline:** Upload → Azure Function trigger → Aspose converts to PDF → Blob Storage. Summaries are text file renditions linked to the original document.
- **RAG with ACL filtering:** Azure AI Search indexes only flagged documents. Query results filtered by user's ACL permissions before being sent to Azure OpenAI via Spring AI.
- **Case Study Agent:** Admin-configured templates stored in DB. Active agent runs on upload to validate/reformat case studies via Aspose.Slides for Java.
- **No Docker:** All deployments are direct JAR/package deploys to Azure App Service. This is a hard requirement.
- **Profiles:** `dev` profile disables Flyway, uses `ddl-auto: update`, enables DEBUG logging. Production uses Flyway with `ddl-auto: validate`.

## Environment Variables

Backend expects these in `application.yml` or as environment variables:
- `SPRING_DATASOURCE_URL` — PostgreSQL JDBC connection string (with `?sslmode=require` for Azure)
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `AZURE_STORAGE_CONNECTION_STRING` — Blob Storage connection string
- `AZURE_OPENAI_ENDPOINT` / `AZURE_OPENAI_API_KEY`
- `AZURE_SEARCH_ENDPOINT` / `AZURE_SEARCH_API_KEY`
- `AZURE_DOC_INTELLIGENCE_ENDPOINT` / `AZURE_DOC_INTELLIGENCE_API_KEY`
- `AZURE_TENANT_ID` — Entra ID tenant for JWT validation

All Azure connection strings/keys are saved in `infra/output/azure-resources.env` (gitignored).

## Testing Conventions

- **Service tests:** Pure unit tests with `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`. No Spring context.
- **Controller tests:** `@WebMvcTest` with `@MockitoBean` (Spring Boot 3.4+, NOT `@MockBean`). Import `TestSecurityConfig.class` + `GlobalExceptionHandler.class`.
- **Infrastructure tests:** Mockito-based unit tests (e.g., BlobStorageServiceTest mocks BlobServiceClient chain).
- **Assertions:** AssertJ (`assertThat`, `assertThatThrownBy`).
- **Test security:** `TestSecurityConfig` in test config package disables CSRF and permits all. Also `excludeAutoConfiguration` for OAuth2 in `@WebMvcTest`.
- Test file location mirrors main: `src/test/java/com/sedin/presales/{same package structure}`

## Conventions

- All service methods return DTOs, never entities
- DB migrations via Flyway in `src/backend/src/main/resources/db/migration/`
- API versioning via URL path (`/api/v1/...`)
- Aspose repo: `https://releases.aspose.com/java/repo/` (configured in POM)
- Aspose license file at `src/backend/src/main/resources/license/Aspose.Total.Java.lic` (gitignored)
- Frontend uses barrel exports from feature folders
- All API responses use consistent envelope: `{ data, error, metadata }`
- Git: feature branches (`feature/xxx`), conventional commits, PRs to `main`
- Code quality: FAANG standards, modular architecture, Vercel best practices for React, Java industry standards
