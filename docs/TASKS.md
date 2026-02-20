# Task Tracker — Pre-Sales Asset Management System

**Last Updated:** 2026-02-20
**Legend:** `[ ]` Pending | `[~]` In Progress | `[x]` Done | `[!]` Blocked

---

## Phase 0: Project Setup & Infrastructure

- [x] P0-01: Create PRD document (docs/PRD.md)
- [x] P0-02: Create Tech Stack document (docs/TECH-STACK.md)
- [x] P0-03: Scaffold backend (Spring Boot 3.4, Java 21, single Maven project)
- [x] P0-04: Scaffold frontend (React + Vite + TypeScript + shadcn/ui + TailwindCSS v4)
- [x] P0-05: Create Azure provisioning scripts (infra/*.sh)
- [x] P0-06: Provision Azure resources (Resource Group, Storage, PostgreSQL, OpenAI, AI Search, Doc Intelligence, App Service)
- [x] P0-07: Create GitHub repository and push initial code
- [x] P0-08: Create GitHub Actions CI/CD workflows
- [x] P0-09: Create CLAUDE.md
- [!] P0-10: Entra ID app registration (requires Azure AD admin permissions)

---

## Phase 1: Foundation (Backend Core + Auth + DB Schema)

### 1A: Database Schema & Migrations
- [x] P1-01: Design full database schema (ERD) for all entities
- [x] P1-02: Create Flyway migration V1 — `users` table
- [x] P1-03: Create Flyway migration V2 — `master_*` tables (domains, industries, technologies, document_types, business_units, sbus)
- [x] P1-04: Create Flyway migration V3 — `folders` table
- [x] P1-05: Create Flyway migration V4 — `documents` table
- [x] P1-06: Create Flyway migration V5 — `document_metadata` + `document_technologies` tables
- [x] P1-07: Create Flyway migration V6 — `document_versions` table
- [x] P1-08: Create Flyway migration V7 — `renditions` table
- [x] P1-09: Create Flyway migration V8 — `acl_entries` table
- [x] P1-10: Create Flyway migration V9 — `case_study_agents` table
- [x] P1-11: Create Flyway migration V10 — `audit_log` table
- [x] P1-12: Seed initial master data (V11 — domains, industries, technologies, etc.)

### 1B: Domain Entities & Repositories
- [x] P1-13: Create JPA entity — `User` (with Role enum: ADMIN, EDITOR, VIEWER)
- [x] P1-14: Create JPA entities — Master entities (Domain, Industry, Technology, DocumentType, BusinessUnit, Sbu)
- [x] P1-15: Create JPA entity — `Folder`
- [x] P1-16: Create JPA entity — `Document` with `DocumentMetadata` (one-to-one)
- [x] P1-17: Create JPA entity — `DocumentVersion`
- [x] P1-18: Create JPA entity — `Rendition` (with RenditionType/RenditionStatus enums)
- [x] P1-19: Create JPA entity — `AclEntry` (with ResourceType and Permission enums)
- [x] P1-20: Create JPA entity — `CaseStudyAgent`
- [x] P1-21: Create JPA entity — `AuditLog`
- [x] P1-22: Create Spring Data JPA repositories for all entities (15 repositories)

### 1C: Authentication & Authorization
- [x] P1-23: Configure Spring Security with Entra ID JWT validation + CORS + @EnableMethodSecurity
- [x] P1-24: Create `UserPrincipal` + `CurrentUserService` to extract current user from JWT
- [x] P1-25: Create ACL service — `AclService` (checkAccess, grantAccess, revokeAccess, getAccessibleDocumentIds)
- [x] P1-26: Create `@PreAuthorize` custom expressions or method-level security for role checks
- [x] P1-27: Create dev-mode auth bypass (profile=dev, DevUserPrincipal + DevCurrentUserService)

### 1D: Global Backend Infrastructure
- [x] P1-28: Create global exception handler (`@RestControllerAdvice`) with consistent API response envelope
- [x] P1-29: Create standard API response DTOs (`ApiResponse<T>`, `PagedResponse<T>`, `ErrorDetail`, `FieldError`)
- [x] P1-30: Create audit logging interceptor/aspect (auto-log controller actions)
- [x] P1-31: Configure Azure Blob Storage client bean (`BlobServiceClient` + `AzureBlobConfig`)
- [x] P1-32: Create `BlobStorageService` (upload, download, delete, generateSasUrl, exists)

---

## Phase 2: Document Management (DMS Core)

### 2A: Masters API (FR-047 to FR-049)
- [x] P2-01: Create CRUD service + controller for Domain master (`/api/v1/masters/domains`)
- [x] P2-02: Create CRUD service + controller for Industry master (`/api/v1/masters/industries`)
- [x] P2-03: Create CRUD service + controller for Technology master (`/api/v1/masters/technologies`)
- [x] P2-04: Create CRUD service + controller for BusinessUnit master (`/api/v1/masters/business-units`)
- [x] P2-05: Create CRUD service + controller for SBU master (`/api/v1/masters/sbus`)
- [x] P2-06: Create generic master service to reduce CRUD boilerplate

### 2B: Document Upload & Storage (FR-001 to FR-006)
- [x] P2-07: Create `DocumentService` — upload flow (validate file, store in Blob, create Document + Version + Metadata records)
- [x] P2-08: Create `DocumentController` — `POST /api/v1/documents/upload` (multipart file + metadata JSON)
- [x] P2-09: Create `DocumentController` — `GET /api/v1/documents` (paginated list with ACL filtering)
- [x] P2-10: Create `DocumentController` — `GET /api/v1/documents/{id}` (detail view with versions, renditions)
- [x] P2-11: Create `DocumentController` — `PUT /api/v1/documents/{id}` (update metadata)
- [x] P2-12: Create `DocumentController` — `DELETE /api/v1/documents/{id}` (soft delete)

### 2C: Versioning (FR-007 to FR-010)
- [x] P2-13: Create `VersionService` — re-upload creates new version, retains old
- [x] P2-14: Create `DocumentController` — `POST /api/v1/documents/{id}/versions` (upload new version)
- [x] P2-15: Create `DocumentController` — `GET /api/v1/documents/{id}/versions` (list versions)
- [x] P2-16: Create `DocumentController` — `GET /api/v1/documents/{id}/versions/{versionId}/download` (download specific version)

### 2D: Access Control API (FR-011 to FR-014)
- [x] P2-17: Create `AclController` — `GET /api/v1/acl/resource/{type}/{id}` (list permissions)
- [x] P2-18: Create `AclController` — `POST /api/v1/acl` (grant access) + `POST /api/v1/acl/bulk` (bulk grant)
- [x] P2-19: Create `AclController` — `DELETE /api/v1/acl/{id}` (revoke access) + `DELETE /api/v1/acl/resource/{type}/{id}` (revoke all)
- [x] P2-20: Integrate ACL filtering into document list query (only return docs user can access)

---

## Phase 3: Renditions & Document Processing

### 3A: PDF Rendition (FR-015 to FR-017)
- [x] P3-01: Create `AsposeConfig` — initialize Aspose license on app startup (evaluation mode if no license)
- [x] P3-02: Create `PdfRenditionService` — convert PPT/PPTX → PDF using Aspose.Slides
- [x] P3-03: Create `PdfRenditionService` — convert DOC/DOCX → PDF using Aspose.Words (+ Excel via Aspose.Cells)
- [x] P3-04: Wire rendition generation into upload pipeline (async, after document version created)
- [x] P3-05: Store PDF rendition in Blob Storage `renditions` container, link in `renditions` table
- [x] P3-06: Create Spring `@Async` rendition processing with ThreadPoolTaskExecutor (AsyncConfig)

### 3B: AI Summary Rendition (FR-018 to FR-021)
- [x] P3-07: Create `DocumentIntelligenceService` — extract text from uploaded documents using Azure Doc Intelligence
- [x] P3-08: Create `SummarizationService` — send extracted text to Azure OpenAI for summarization (REST-based)
- [x] P3-09: Store summary as text file in Blob Storage `summaries` container, link in `renditions` table
- [x] P3-10: Implement summary caching — serve from storage if exists, regenerate on new version
- [x] P3-11: Create `GET /api/v1/documents/{id}/summary` + `POST .../summary/regenerate` endpoints

### 3C: Document Viewer API (FR-022 to FR-025)
- [x] P3-12: Create `GET /api/v1/documents/{id}/view` — return PDF rendition URL (SAS token for Blob)
- [x] P3-13: Create `GET /api/v1/documents/{id}/versions/{v1}/compare/{v2}` — return two PDF URLs for side-by-side view
- [x] P3-14: Create `GET /api/v1/documents/{id}/download` — download native format (SAS URL)

---

## Phase 4: RAG & Intelligent Search (FR-026 to FR-031)

- [x] P4-01: Create `AzureSearchService` — configure Azure AI Search client
- [x] P4-02: Create search index schema for documents (title, content, domain, industry, technologies, customer, embedding vector)
- [x] P4-03: Create `IndexingService` — extract text → generate embeddings → push to AI Search index
- [x] P4-04: Wire indexing into upload pipeline (only if `rag_indexed` flag is true)
- [x] P4-05: Create `POST /api/v1/search` — natural language query → vector search → ACL filter → return ranked results
- [x] P4-06: Implement RAG response — send search results + user query to Azure OpenAI → return AI-generated answer with source citations
- [x] P4-07: Create `PUT /api/v1/documents/{id}/index-toggle` — toggle indexing on/off, add/remove from index
- [x] P4-08: Handle re-indexing on new version upload

---

## Phase 5: Case Study Agent (FR-032 to FR-043)

### 5A: Agent Configuration (Admin)
- [ ] P5-01: Create `CaseStudyAgentService` — CRUD + set active agent
- [ ] P5-02: Create `AgentController` — `POST/GET/PUT/DELETE /api/v1/admin/agents`
- [ ] P5-03: Create `AgentController` — `POST /api/v1/admin/agents/{id}/activate`
- [ ] P5-04: Design agent template config JSON schema (slide structure, sections, branding rules, required fields)

### 5B: Auto-Validation on Upload
- [ ] P5-05: Create `CaseStudyValidationService` — validate uploaded PPT against active agent's template
- [ ] P5-06: Create `CaseStudyFormattingService` — reformat non-conforming PPT using Aspose.Slides
- [ ] P5-07: Wire validation into case study upload pipeline (run after upload, before rendition)
- [ ] P5-08: Store formatted rendition in `renditions` table (type: FORMATTED)
- [ ] P5-09: Log validation results, expose via `GET /api/v1/documents/{id}/validation-results`

### 5C: Case Study Creation Wizard
- [ ] P5-10: Create `CaseStudyGenerationService` — generate PPT from structured input using Aspose.Slides + active template
- [ ] P5-11: Create `POST /api/v1/case-studies/generate` — accept structured input, return generated document
- [ ] P5-12: Use Azure OpenAI to enhance/polish user input before generating slides

---

## Phase 6: Admin Module (FR-044 to FR-052)

- [ ] P6-01: Create `UserService` — CRUD (create, update, deactivate, list users)
- [ ] P6-02: Create `UserController` — `POST/GET/PUT/DELETE /api/v1/admin/users`
- [ ] P6-03: Create `AuditLogService` — query audit logs with filters
- [ ] P6-04: Create `AuditLogController` — `GET /api/v1/admin/audit-logs`

---

## Phase 7: Frontend — Layout & Navigation

- [ ] P7-01: Set up React Router with route structure (/, /documents, /documents/:id, /admin/*, /search)
- [ ] P7-02: Create app shell layout (sidebar nav, header with user info, main content area)
- [ ] P7-03: Set up MSAL authentication provider (@azure/msal-react)
- [ ] P7-04: Create auth guard (ProtectedRoute) — redirect unauthenticated users
- [ ] P7-05: Set up Axios instance with auth token interceptor
- [ ] P7-06: Set up TanStack Query provider with global config
- [ ] P7-07: Create global error boundary and toast notifications (sonner)

---

## Phase 8: Frontend — Document Grid & Browse (FR-053 to FR-059)

- [ ] P8-01: Create `DocumentsPage` — main data grid using TanStack Table + shadcn DataTable
- [ ] P8-02: Implement server-side pagination (page, size, sort params)
- [ ] P8-03: Implement column filtering (domain, industry, technology, customer, BU, SBU dropdowns)
- [ ] P8-04: Implement multi-column sorting
- [ ] P8-05: Implement full-text search bar (metadata search)
- [ ] P8-06: Create technology tags display (multi-value column with badges)
- [ ] P8-07: Create document row actions menu (view, download, edit, delete, manage ACL)

---

## Phase 9: Frontend — Document Upload & Detail

- [ ] P9-01: Create `UploadDialog` — drag-and-drop file upload (react-dropzone) + metadata form
- [ ] P9-02: Create metadata form with typeahead/dropdowns from masters API
- [ ] P9-03: Create RAG indexing toggle in upload form
- [ ] P9-04: Create `DocumentDetailPage` — show document info, metadata, versions list, renditions
- [ ] P9-05: Create version history panel (list versions, download, compare actions)
- [ ] P9-06: Create "Upload New Version" dialog
- [ ] P9-07: Create metadata edit form (inline or dialog)

---

## Phase 10: Frontend — Document Viewer

- [ ] P10-01: Create `DocumentViewerPage` — in-browser PDF viewer using @react-pdf-viewer/core
- [ ] P10-02: Implement zoom, scroll, page navigation controls
- [ ] P10-03: Add download buttons (PDF rendition + native format)
- [ ] P10-04: Create version comparison view — side-by-side PDFs using react-resizable-panels
- [ ] P10-05: Create AI summary panel (sidebar or tab)

---

## Phase 11: Frontend — RAG Search

- [ ] P11-01: Create `SearchPage` — natural language search input
- [ ] P11-02: Display search results with document cards (title, excerpt, relevance score, metadata tags)
- [ ] P11-03: Display AI-generated answer at the top with source citations
- [ ] P11-04: Click result → navigate to document viewer

---

## Phase 12: Frontend — Case Study Agent

- [ ] P12-01: Create `CaseStudyWizardPage` — multi-step form for creating case studies from scratch
- [ ] P12-02: Step 1: Customer & project info
- [ ] P12-03: Step 2: Challenges (list builder)
- [ ] P12-04: Step 3: Solution description
- [ ] P12-05: Step 4: Technologies used (multi-select from masters)
- [ ] P12-06: Step 5: Benefits & metrics
- [ ] P12-07: Step 6: Review & generate → call API → download/save generated PPT
- [ ] P12-08: Show validation results after upload (pass/fail, changes made)

---

## Phase 13: Frontend — Admin Module

- [ ] P13-01: Create `AdminLayout` with tabs: Users, Masters, Agents, Audit Log
- [ ] P13-02: Create `UsersManagement` — data table with create/edit/deactivate actions
- [ ] P13-03: Create `MastersManagement` — tabbed CRUD for each master type
- [ ] P13-04: Create `AgentsManagement` — list agents, create/edit form, activate/deactivate toggle
- [ ] P13-05: Create `AuditLogViewer` — paginated log table with filters (user, action, date range)

---

## Phase 14: Testing & Quality

- [x] P14-01: Backend unit tests — services (MasterService, FolderService, DocumentService, AclService, PermissionEvaluator) — 48 tests
- [x] P14-02: Backend controller tests — @WebMvcTest (MasterController, FolderController, DocumentController, AclController) — 29 tests
- [x] P14-02b: Backend infra/util tests — GlobalExceptionHandler, DocumentMapper, BlobStorageService — 19 tests
- [ ] P14-03: Backend integration tests — repository layer with Testcontainers (PostgreSQL)
- [ ] P14-04: Frontend unit tests — React Testing Library for key components
- [ ] P14-05: E2E tests — Playwright for critical flows (login → upload → view → search)
- [ ] P14-06: API documentation — Swagger/OpenAPI annotations on all controllers

---

## Phase 15: Deployment & Go-Live

- [ ] P15-01: Configure backend application-prod.yml with Azure resource env vars
- [ ] P15-02: Deploy backend JAR to Azure App Service (app-sedin-presales-api)
- [ ] P15-03: Deploy frontend build to Azure App Service (sedin-presales-web)
- [ ] P15-04: Deploy Azure Functions (rendition processing)
- [ ] P15-05: Verify end-to-end flow in production
- [ ] P15-06: Migrate existing 5 case studies into the system
- [ ] P15-07: Configure custom domain (optional)

---

## Dependency Graph (Recommended Build Order)

```
Phase 0 (Done) → Phase 1A (DB Schema) → Phase 1B (Entities)
                                       ↘
                Phase 1C (Auth) ────────→ Phase 1D (Global Infra)
                                                     ↓
                                              Phase 2A (Masters)
                                              Phase 2B (Upload)
                                              Phase 2C (Versioning)
                                              Phase 2D (ACLs)
                                                     ↓
                                    ┌────────────────┼────────────────┐
                                Phase 3           Phase 4          Phase 7
                              (Renditions)    (RAG/Search)     (FE Layout)
                                    ↓              ↓                ↓
                                Phase 5        Phase 11        Phase 8-10
                              (CS Agent)      (FE Search)    (FE Grid/Viewer)
                                    ↓                              ↓
                                Phase 6                        Phase 12-13
                              (Admin BE)                     (FE Agent/Admin)
                                    ↓                              ↓
                                              Phase 14 (Testing)
                                                     ↓
                                              Phase 15 (Deploy)
```

---

## Progress Summary

| Phase | Total Tasks | Done | Pending | % Complete |
|-------|------------|------|---------|------------|
| P0: Setup & Infra | 10 | 9 | 1 (blocked) | 90% |
| P1: Foundation | 32 | 32 | 0 | 100% |
| P2: DMS Core | 20 | 20 | 0 | 100% |
| P3: Renditions | 14 | 14 | 0 | 100% |
| P4: RAG & Search | 8 | 8 | 0 | 100% |
| P5: CS Agent | 12 | 0 | 12 | 0% |
| P6: Admin BE | 4 | 0 | 4 | 0% |
| P7: FE Layout | 7 | 0 | 7 | 0% |
| P8: FE Grid | 7 | 0 | 7 | 0% |
| P9: FE Upload/Detail | 7 | 0 | 7 | 0% |
| P10: FE Viewer | 5 | 0 | 5 | 0% |
| P11: FE Search | 4 | 0 | 4 | 0% |
| P12: FE CS Agent | 8 | 0 | 8 | 0% |
| P13: FE Admin | 5 | 0 | 5 | 0% |
| P14: Testing | 7 | 3 | 4 | 43% |
| P15: Deployment | 7 | 0 | 7 | 0% |
| **TOTAL** | **157** | **86** | **71** | **55%** |
