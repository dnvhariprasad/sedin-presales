# Session Handoff — Pre-Sales Asset Management System

**Date:** 2026-02-20
**Last Commit:** `c5652bc` — Phase 2: DMS Core APIs + 96 unit/controller tests
**Branch:** `main`
**Repository:** https://github.com/dnvhariprasad/sedin-presales

---

## What Was Completed

### Phase 0: Project Setup & Infrastructure (90%)
- Azure resources fully provisioned (PostgreSQL, Blob Storage, OpenAI, AI Search, Doc Intelligence, App Service)
- GitHub repo, CI/CD workflows, CLAUDE.md, PRD, Tech Stack docs
- **Blocked:** P0-10 Entra ID app registration (requires Azure AD admin permissions)

### Phase 1: Foundation (97%)
- 11 Flyway migrations (V1-V11) covering all tables + seed data
- 15 JPA entities with enums, 15 Spring Data repositories
- Spring Security + OAuth2 config, UserPrincipal, CurrentUserService, dev auth bypass
- Global exception handler, API response DTOs, BlobStorageService
- **Remaining:** P1-26 (`@PreAuthorize` custom expressions), P1-30 (audit logging interceptor)

### Phase 2: DMS Core (95%)
- **Masters API:** Generic `MasterService` + `MasterController` at `/api/v1/masters/{type}` handling all 6 master types (domains, industries, technologies, document-types, business-units, sbus)
- **Document API:** Full CRUD + multipart upload at `/api/v1/documents` with blob storage integration, JPA Specification filters
- **Versioning API:** Upload new versions, list versions, download specific version at `/api/v1/documents/{id}/versions`
- **Folder API:** Hierarchical CRUD at `/api/v1/folders` with parent/child support
- **ACL API:** Grant/revoke/bulk/check permissions at `/api/v1/acl` with permission hierarchy (ADMIN > WRITE > READ)
- **Remaining:** P2-20 (integrate ACL filtering into document list query)

### Phase 14: Testing (43%)
- **96 tests, 0 failures**
- 48 service unit tests (Mockito): MasterService(8), FolderService(11), DocumentService(12), AclService(13), PermissionEvaluator(4)
- 29 controller @WebMvcTest tests: MasterController(7), FolderController(7), DocumentController(8), AclController(7)
- 19 infra/utility tests: GlobalExceptionHandler(7), DocumentMapper(6), BlobStorageService(6)
- TestSecurityConfig in test root disables OAuth2 for controller tests

---

## Overall Progress: 39% (62/157 tasks)

---

## What's Next (Recommended Order)

### Immediate (Phase 3: Renditions & Document Processing)
1. **P3-01:** Create `AsposeService` — initialize Aspose license on startup
2. **P3-02/03:** PDF rendition services (PPT→PDF via Aspose.Slides, DOC→PDF via Aspose.Words)
3. **P3-04/05/06:** Wire rendition into upload pipeline, store in blob `renditions` container
4. **P3-07/08/09:** AI summary pipeline — Azure Doc Intelligence text extraction → OpenAI summarization
5. **P3-10/11:** Summary caching + `/api/v1/documents/{id}/summary` endpoint
6. **P3-12/13/14:** Document viewer API (PDF URL with SAS token, version compare, download)

### Then (Phase 4: RAG & Intelligent Search)
1. Azure AI Search client + index schema
2. Indexing service (text extraction → embeddings → push to index)
3. `/api/v1/search` endpoint with RAG response + ACL filtering
4. Index toggle + re-indexing on new version

### Or (Phase 7: Frontend Layout — can run in parallel with Phase 3/4)
1. React Router setup, app shell layout
2. MSAL auth provider + auth guard
3. Axios instance with token interceptor, TanStack Query provider

---

## Key Technical Notes for Next Session

### Build & Test
```bash
mvn -B compile -f src/backend/pom.xml          # Compile
mvn -B test -f src/backend/pom.xml              # Run all 96 tests
mvn -B spring-boot:run -Dspring-boot.run.profiles=dev -f src/backend/pom.xml  # Run locally
```

### Architecture
- **Single Maven project** (NOT multi-module) — package-based layering
- **No Docker** — hard requirement, all deployments are direct JAR/package
- Packages: `domain.entity`, `domain.repository`, `domain.enums`, `application.service`, `application.dto`, `application.mapper`, `application.exception`, `infrastructure.storage`, `api.controller`, `config`
- All responses wrapped in `ApiResponse<T>`, paged responses in `PagedResponse<T>`

### Azure Resources
- Connection strings in `infra/output/azure-resources.env` (gitignored)
- PostgreSQL in **centralus**, Storage/AI in **eastus**, App Service in **centralus**
- OpenAI uses **GlobalStandard** SKU (Standard had zero quota)

### Testing Patterns
- Service tests: `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- Controller tests: `@WebMvcTest` + `@MockitoBean` (Spring Boot 3.4+, NOT old `@MockBean`)
- Must add `@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})` to controller tests
- Must add `excludeAutoConfiguration = {OAuth2ResourceServerAutoConfiguration.class, OAuth2ClientAutoConfiguration.class}` to `@WebMvcTest`

### Pending Items (Low Priority)
- P0-10: Entra ID app registration (blocked on Azure AD admin)
- P1-26: `@PreAuthorize` custom expressions
- P1-30: Audit logging interceptor/aspect
- P2-20: ACL filtering in document list query

### Key Files to Read First
- `CLAUDE.md` — Full project context
- `docs/TASKS.md` — All 157 tasks with progress tracking
- `docs/PRD.md` — Product requirements
- `docs/TECH-STACK.md` — Technology decisions
- `src/backend/pom.xml` — Dependencies
- `src/backend/src/main/resources/application.yml` — App config
