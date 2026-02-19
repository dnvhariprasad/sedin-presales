# Skills, Plugins & Tools Needed

## Claude Code Skills (Available in This Session)

| Skill | Usage |
|-------|-------|
| `vercel-react-best-practices` | React/Next.js performance patterns — use when building frontend components |
| `web-design-guidelines` | UI accessibility and UX audits |
| `figma:implement-design` | Convert Figma mockups to code (if using Figma for UI design) |
| `figma:create-design-system-rules` | Generate design system rules from Figma |
| `document-skills:pptx` | Read/create/edit PowerPoint files — useful for case study template work |
| `document-skills:xlsx` | Spreadsheet operations if needed for data exports |
| `document-skills:pdf` | PDF operations — reading, merging, watermarking |
| `document-skills:docx` | Word document operations |
| `document-skills:webapp-testing` | Playwright-based testing of the web app |
| `document-skills:frontend-design` | Production-grade frontend interface design |
| `code-review:code-review` | Code review pull requests |
| `claude-md-management:revise-claude-md` | Keep CLAUDE.md updated |

---

## IDE / Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| **IntelliJ IDEA** (or VS Code + Java Extension Pack) | Java/Spring Boot backend development | IntelliJ recommended for enterprise Java |
| **VS Code** | Frontend React/TypeScript development | With ESLint, Prettier, Tailwind CSS IntelliSense extensions |
| **Postman / Bruno** | API testing during development | Import OpenAPI spec for auto-generated collections |
| **DBeaver / pgAdmin** | PostgreSQL database management | Connect to Azure PostgreSQL for schema inspection |
| **Azure Data Studio** | Alternative DB client with Azure integration | Built-in PostgreSQL support |

---

## VS Code Extensions (Frontend)

| Extension | Purpose |
|-----------|---------|
| ESLint | Linting |
| Prettier | Code formatting |
| Tailwind CSS IntelliSense | TailwindCSS autocomplete |
| ES7+ React/Redux/React-Native snippets | React snippets |
| Auto Rename Tag | HTML/JSX tag renaming |
| Error Lens | Inline error display |

---

## IntelliJ Plugins (Backend)

| Plugin | Purpose |
|--------|---------|
| Lombok | `@Getter`, `@Setter`, `@Builder` support |
| Spring Boot Assistant | Spring Boot run configs, property autocomplete |
| Database Tools (built-in) | Connect to PostgreSQL directly |
| JPA Buddy | Entity generation, Flyway migration helpers |
| SonarLint | Code quality analysis |

---

## CLI Tools Required

| Tool | Purpose | Install |
|------|---------|---------|
| **Azure CLI (`az`)** | Azure resource management | `brew install azure-cli` |
| **GitHub CLI (`gh`)** | PR creation, issue management | `brew install gh` |
| **Java 21 (Temurin)** | Backend runtime | `brew install --cask temurin@21` |
| **Maven 3.9+** | Backend build tool | `brew install maven` |
| **Node.js 20 LTS** | Frontend runtime | `brew install node@20` |
| **npm** | Frontend package manager | Comes with Node.js |

---

## Azure Services & SDKs (Already Provisioned)

| Service | Java SDK | Purpose |
|---------|----------|---------|
| Azure Blob Storage | `azure-storage-blob` (12.x) | File storage for documents, renditions, summaries |
| Azure OpenAI | Spring AI (`spring-ai-azure-openai`) | Chat completion (summarization, RAG answers), embeddings |
| Azure AI Search | `azure-search-documents` (11.x) | Vector search index for RAG |
| Azure Document Intelligence | `azure-ai-formrecognizer` (4.x) | Text extraction from uploaded documents |
| Azure App Service | N/A (deploy target) | Hosting API + Frontend |
| Azure Functions | `azure-functions-java-library` | Background job processing (renditions) |

---

## Key Libraries (Backend — Already in pom.xml)

| Library | Purpose |
|---------|---------|
| Spring Boot Starter Web | REST API |
| Spring Boot Starter Data JPA | ORM / Database |
| Spring Boot Starter Security | Authentication/Authorization |
| Spring Boot Starter OAuth2 Resource Server | Entra ID JWT validation |
| Spring Boot Starter Validation | Request validation |
| Spring Boot Starter Actuator | Health checks, metrics |
| Flyway | Database migrations |
| PostgreSQL Driver | Database connectivity |
| Aspose.Words for Java | DOC/DOCX → PDF conversion |
| Aspose.Slides for Java | PPT/PPTX → PDF conversion + PPT generation |
| Aspose.Cells for Java | Excel processing (future) |
| SpringDoc OpenAPI | Swagger/API docs |
| MSAL4J | Entra ID token handling |
| Lombok | Boilerplate reduction |

---

## Key Libraries (Frontend — Already in package.json)

| Library | Purpose |
|---------|---------|
| React 18 + TypeScript | UI framework |
| Vite | Build tool |
| TailwindCSS v4 | Styling |
| shadcn/ui | Component library (button, table, dialog, form, etc.) |
| TanStack Query | Server state management, caching |
| TanStack Table | Data grid with sorting, filtering, pagination |
| React Router DOM | Client-side routing |
| Axios | HTTP client |
| React Hook Form + Zod | Form management + validation |
| @azure/msal-browser + @azure/msal-react | Entra ID authentication |
| @react-pdf-viewer/core | In-browser PDF viewing |
| react-resizable-panels | Side-by-side version comparison |
| react-dropzone | Drag-and-drop file upload |
| Lucide React | Icons |
| Sonner | Toast notifications |

---

## External Services / Accounts Needed

| Service | Status | Action Required |
|---------|--------|-----------------|
| Azure Subscription | Active | Already authenticated |
| GitHub Account | Active | Repo created (dnvhariprasad/sedin-presales) |
| Aspose License | Expired (Dec 2024) | Running in evaluation mode; renew for production |
| Entra ID (Azure AD) | Blocked | Admin needs to grant "Application Developer" role or create app registration |
| Figma | Optional | For UI mockups before building frontend |

---

## Recommended Development Workflow

1. **Each session**: Pull latest, check `docs/TASKS.md` for next phase
2. **Backend first**: Build API endpoints, test with Postman/Swagger
3. **Frontend follows**: Build UI consuming the API
4. **Per milestone**: Commit, push, update TASKS.md progress
5. **Testing**: Write tests alongside feature code, not after
6. **Code review**: Use `/code-review` skill on PRs before merge
