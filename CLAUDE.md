# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project: Sedin Pre-Sales Asset Management System

A Document Management System (DMS) for managing pre-sales assets (case studies, white papers), built on Azure. Replaces SharePoint with AI-powered search, automated case study formatting, and document intelligence.

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.4, Spring Data JPA, Spring Security, Spring AI
- **Frontend:** React 18+ with TypeScript, Vite, shadcn/ui, TailwindCSS, TanStack Query
- **Database:** Azure Database for PostgreSQL Flexible Server
- **File Storage:** Azure Blob Storage
- **AI/RAG:** Azure AI Search, Azure OpenAI (GPT-4o-mini, text-embedding-3-small), Azure Document Intelligence, Spring AI
- **Auth:** Microsoft Entra ID (Azure AD) with Spring Security OAuth2 + MSAL4J
- **PDF Rendition:** Aspose.Total for Java (license valid till Dec 2024, evaluation mode after)
- **PPT Generation:** Aspose.Slides for Java
- **DB Migrations:** Flyway
- **Hosting:** Azure App Service (API), Azure Functions (background jobs), Azure Static Web Apps (Frontend)
- **CI/CD:** GitHub Actions → Azure (JAR deploy, no Docker)

## Repository Structure

```
sedin-presales/
├── src/
│   ├── backend/                              # Maven multi-module project
│   │   ├── pom.xml                           # Parent POM
│   │   ├── presales-domain/                  # Entities, value objects, repository interfaces
│   │   ├── presales-application/             # Business logic, services, DTOs
│   │   ├── presales-infrastructure/          # JPA repos, Azure SDK clients, Aspose
│   │   ├── presales-api/                     # Spring Boot Web API (controllers, config)
│   │   └── presales-functions/               # Azure Functions (background jobs)
│   └── frontend/                             # React + Vite + shadcn/ui
├── infra/                                    # Azure provisioning scripts (az cli)
├── .github/workflows/                        # GitHub Actions CI/CD
├── docs/                                     # PRD, Tech Stack, Architecture
└── Sample Case Studies/                      # Reference PPT files
```

## Build & Run Commands

### Backend (Java 21 / Maven)
```bash
# Build all modules
mvn -B clean compile -f src/backend/pom.xml

# Run API locally
mvn -B spring-boot:run -pl presales-api -f src/backend/pom.xml

# Run tests
mvn -B verify -f src/backend/pom.xml

# Run single test class
mvn -B test -pl presales-api -Dtest=DocumentControllerTest -f src/backend/pom.xml

# Run single test method
mvn -B test -pl presales-api -Dtest=DocumentControllerTest#shouldUploadDocument -f src/backend/pom.xml

# Package JAR for deployment
mvn -B clean package -DskipTests -f src/backend/pom.xml
```

### Frontend (React + Vite)
```bash
cd src/frontend
npm install
npm run dev          # Dev server (port 5173)
npm run build        # Production build
npm run lint         # ESLint
```

## Azure Resources (Resource Group: rg-sedin-presales)

All Azure resources are in resource group `rg-sedin-presales` (East US region):
- Storage Account: `stsedinpresales`
- PostgreSQL: `psql-sedin-presales` (database: `presalesdb`)
- Azure AI Search: `search-sedin-presales`
- Azure OpenAI: `aoai-sedin-presales`
- Document Intelligence: `di-sedin-presales`
- App Service Plan: `asp-sedin-presales`
- App Service (API): `app-sedin-presales-api`
- Azure Functions: `func-sedin-presales`
- Static Web App: `swa-sedin-presales`

## Key Architecture Decisions

- **Clean Architecture:** Domain → Application → Infrastructure → Api (dependency flows inward)
- **Document-level ACLs:** Every document has explicit ACL entries. ACL checks enforced at the repository/service level, not in controllers.
- **Rendition pipeline:** Upload → Azure Function trigger → Aspose converts to PDF → Blob Storage. Summaries are text file renditions linked to the original document.
- **RAG with ACL filtering:** Azure AI Search indexes only flagged documents. Query results filtered by user's ACL permissions before being sent to Azure OpenAI via Spring AI.
- **Case Study Agent:** Admin-configured templates stored in DB. Active agent runs on upload to validate/reformat case studies via Aspose.Slides for Java.
- **No Docker:** All deployments are direct JAR/package deploys to Azure App Service and Azure Functions.

## Environment Variables

Backend expects these in `application.yml` or as environment variables:
- `SPRING_DATASOURCE_URL` — PostgreSQL JDBC connection string
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `AZURE_STORAGE_CONNECTION_STRING` — Blob Storage connection string
- `AZURE_OPENAI_ENDPOINT` / `AZURE_OPENAI_API_KEY`
- `AZURE_SEARCH_ENDPOINT` / `AZURE_SEARCH_API_KEY`
- `AZURE_DOC_INTELLIGENCE_ENDPOINT` / `AZURE_DOC_INTELLIGENCE_API_KEY`
- `AZURE_TENANT_ID` / `AZURE_CLIENT_ID`

## Conventions

- Backend follows Clean Architecture (Domain → Application → Infrastructure → Api)
- All service methods return DTOs, never entities
- DB migrations via Flyway in `presales-api/src/main/resources/db/migration/`
- API versioning via URL path (`/api/v1/...`)
- Aspose repo: `https://repository.aspose.com/repo/` (configured in parent POM)
- Frontend uses barrel exports from feature folders
- All API responses use consistent envelope: `{ data, error, metadata }`
- Git: feature branches (`feature/xxx`), conventional commits, PRs to `main`
