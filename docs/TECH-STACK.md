# Pre-Sales Asset Management System -- Tech Stack Recommendation

**Document Version:** 2.0
**Date:** 2026-02-20
**Purpose:** Technology stack recommendation for an enterprise Document Management System (DMS) on Azure, replacing SharePoint for a team of ~50 users.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [File Storage](#1-file-storage--azure-blob-storage)
3. [Database](#2-database--azure-database-for-postgresql-flexible-server)
4. [Backend](#3-backend--java-21-with-spring-boot-3x)
5. [Frontend](#4-frontend--reactjs)
6. [PDF Rendition from MSOffice](#5-pdf-rendition-from-msoffice--aspose)
7. [RAG / AI Services](#6-rag--ai-services)
8. [Document Summarization](#7-document-summarization)
9. [Case Study PPT Generation](#8-case-study-ppt-generation)
10. [Authentication](#9-authentication--microsoft-entra-id)
11. [Hosting](#10-hosting--azure-app-service-for-java)
12. [CI/CD Pipeline](#11-cicd-pipeline)
13. [Cost Summary](#cost-summary)

---

## Architecture Overview

```
Users (Browser)
    |
    v
[React SPA] -- hosted on Azure Static Web Apps
    |
    v
[Spring Boot 3.x API] -- hosted on Azure App Service (Java 21 runtime)
    |
    +---> Azure Blob Storage (documents, renditions)
    +---> Azure PostgreSQL Flexible Server (metadata, ACLs, versions)
    +---> Azure AI Search (full-text + vector index)
    +---> Azure OpenAI (embeddings, summarization, chat)
    +---> Azure Document Intelligence (OCR / text extraction)
    +---> Aspose Libraries for Java (PDF rendition from Office files)
    +---> Microsoft Entra ID (authentication)
    |
[Azure Functions (Java)] -- background processing
    +---> PDF renditions, text extraction, AI indexing, summarization
```

---

## 1. File Storage -- Azure Blob Storage

### Recommendation: Azure Blob Storage (Standard, LRS, Hot tier)

**SKU:** Standard general-purpose v2, Locally Redundant Storage (LRS)

### Why Blob Storage over alternatives

| Criteria | Azure Blob Storage | Azure Files | Azure Data Lake Gen2 |
|---|---|---|---|
| Cost per GB (Hot) | ~$0.018/GB | ~$0.06/GB | ~$0.018/GB |
| API Access | REST API native | SMB/NFS protocol | REST + HDFS |
| Tiering (Hot/Cool/Archive) | Yes | Limited | Yes |
| Max file size | 190.7 TB (block blob) | 4 TB | 190.7 TB |
| Best for | Unstructured docs, app-driven access | File share replacement | Big data analytics |

**Decision:** Azure Blob Storage is the clear choice for a DMS. It provides REST API-driven access (no SMB needed since users interact through the web app, not a mapped drive), lifecycle management with automatic tiering from Hot to Cool to Archive as documents age, and the lowest cost per GB. Azure Files is designed for lift-and-shift file share scenarios, which does not apply here. Azure Data Lake Gen2 adds unnecessary complexity for analytics-oriented features we do not need.

### Container Structure

```
presales-documents/
  ├── originals/{year}/{month}/{document-id}/        # Original uploaded files
  ├── renditions/{document-id}/pdf/                  # PDF renditions
  ├── renditions/{document-id}/thumbnail/             # Thumbnail images
  ├── summaries/{document-id}/summary.txt             # AI-generated summaries
  └── temp/                                           # Processing workspace
```

### Key Configuration

- **Access tier policy:** Hot for recent documents (< 90 days), Cool for older, Archive for > 1 year
- **Soft delete:** Enabled with 30-day retention
- **Versioning:** Enabled at the blob level for recovery scenarios
- **Encryption:** Azure-managed keys (SSE) enabled by default

### SDKs

- `com.azure:azure-storage-blob` (Java SDK)
- `@azure/storage-blob` (JavaScript SDK for SAS-based direct uploads from browser)

### Estimated Monthly Cost

| Item | Estimate |
|---|---|
| Storage (500 GB Hot) | ~$9/month |
| Storage (1 TB Cool) | ~$10/month |
| Operations (100K read/write) | ~$5/month |
| Data transfer (outbound 50 GB) | ~$4/month |
| **Total** | **~$28/month** |

---

## 2. Database -- Azure Database for PostgreSQL Flexible Server

### Recommendation: PostgreSQL Flexible Server (Burstable B1ms tier)

**SKU:** Burstable B1ms (1 vCore, 2 GB RAM) -- can scale up as needed

### Why PostgreSQL over alternatives

| Criteria | PostgreSQL Flexible Server | Azure SQL Database (Basic/S0) | Cosmos DB |
|---|---|---|---|
| Monthly cost (entry) | ~$12/month (B1ms) + storage | ~$5/month (Basic 5 DTU) | ~$25/month (400 RU/s) |
| Relational support | Full SQL, JSONB, CTEs | Full T-SQL | Limited (NoSQL) |
| ACL/RBAC modeling | Excellent (row-level security) | Excellent | Poor fit |
| JSONB for flexible metadata | Native | JSON support but weaker | Native |
| Full-text search built-in | Yes (tsvector) | Yes | No |
| Stop/Start for cost savings | Yes | No | No (serverless has cold start) |
| pgvector for embeddings | Yes | No | Vector search available |
| Open source ecosystem | Broad ORM/tooling support | Microsoft-specific | SDK-specific |

**Decision:** PostgreSQL Flexible Server wins on three critical fronts:

1. **Cost control:** The Burstable tier with stop/start capability means you pay nothing for compute during off-hours. For a 50-user team, this is significant. Azure SQL Basic tier at 5 DTU would be too constrained, and the S0 tier at ~$15/month is comparable but lacks the flexibility.

2. **Relational + JSON hybrid:** The JSONB column type lets you store flexible document metadata (custom attributes per document type) alongside strict relational schemas for ACLs, versioning chains, and user management. This avoids the rigidity of pure relational or the chaos of pure NoSQL.

3. **pgvector extension:** If you ever want a secondary vector store alongside Azure AI Search, PostgreSQL supports it natively. This avoids vendor lock-in for your RAG pipeline.

### Schema Design Highlights

```sql
-- Core tables
documents          -- id, title, description, content_type, blob_path, created_by, created_at
document_versions  -- id, document_id, version_number, blob_path, created_by, created_at
document_metadata  -- id, document_id, metadata JSONB (flexible key-value)
document_acls      -- id, document_id, principal_id, principal_type, permission_level
renditions         -- id, document_id, rendition_type, blob_path, status
tags               -- id, name
document_tags      -- document_id, tag_id

-- Users and groups (synced from Entra ID)
users              -- id, entra_object_id, email, display_name
groups             -- id, entra_object_id, name
group_members      -- group_id, user_id
```

### SDKs

- `org.postgresql:postgresql` (JDBC driver)
- `org.springframework.boot:spring-boot-starter-data-jpa` (Spring Data JPA with Hibernate)
- `org.flywaydb:flyway-core` (database migrations)

### Estimated Monthly Cost

| Item | Estimate |
|---|---|
| Compute (B1ms, running 12h/day) | ~$6/month |
| Storage (32 GB) | ~$4/month |
| Backup (7 days) | Included |
| **Total** | **~$10/month** |

*Scale to B2s ($24/month) or General Purpose D2s_v3 ($100/month) when production load demands it.*

---

## 3. Backend -- Java 21 with Spring Boot 3.x

### Recommendation: Java 21 (LTS) with Spring Boot 3.x Web API

### Why Java with Spring Boot

| Criteria | Java 21 + Spring Boot 3.x | Python (FastAPI) |
|---|---|---|
| Aspose library support | **Excellent** -- Aspose for Java is a native port, fully featured | Wrapper over Java/.NET (thin interop layer) |
| Azure SDK quality | **First-class** -- Azure SDK for Java is well-maintained | Good |
| PDF rendition from Office | **Native Aspose for Java, no interop layer** | Requires Java/.NET runtime underneath |
| REST API development speed | Fast (Spring Boot auto-config, annotations) | Fastest (but weaker typing) |
| RAG pipeline integration | **Spring AI** (official Spring project for AI) | LangChain (most mature) |
| Performance | **Excellent** (Virtual threads in Java 21, GraalVM-ready) | Moderate (GIL limitations) |
| Azure App Service support | **First-class** (built-in Java 21 runtime) | Good |
| Enterprise team familiarity | **Very common** in enterprise | Growing but less common |
| Type safety | **Strong** (compile-time checks) | Optional (type hints) |

**Decision:** Java 21 with Spring Boot 3.x is the strongest choice for this project for several reasons:

1. **Aspose for Java:** The Aspose Java libraries are native Java ports (not wrappers), providing full-featured MSOffice-to-PDF conversion. The user holds a valid Aspose.Total for Java license (expiring December 2024; evaluation mode applies after expiry until renewal). The Java versions of Aspose.Words, Aspose.Slides, and Aspose.Cells are feature-complete and well-documented.

2. **Spring AI for RAG:** Spring AI is the official Spring project for AI integration, providing first-class support for Azure OpenAI, embedding models, vector stores, and RAG pipelines. It follows familiar Spring conventions (auto-configuration, dependency injection) and integrates seamlessly with the rest of the Spring ecosystem.

3. **Java 21 virtual threads:** Java 21 LTS introduces virtual threads (Project Loom), enabling highly concurrent I/O operations (Blob Storage reads, Azure OpenAI calls, database queries) without the complexity of reactive programming.

4. **Azure App Service native support:** Azure App Service provides a built-in Java 21 runtime. Deploy a JAR file directly -- no containers, no custom images, no registry management.

### Project Structure (Maven Multi-Module)

```
src/backend/ (Maven multi-module project)
├── pom.xml (parent)
├── presales-api/              # Spring Boot Web API (controllers, middleware, security config)
│   └── pom.xml
├── presales-application/      # Business logic, services, use cases
│   └── pom.xml
├── presales-domain/           # Entities, value objects, domain events
│   └── pom.xml
├── presales-infrastructure/   # JPA repositories, Azure SDK clients, Aspose integration
│   └── pom.xml
└── presales-functions/        # Azure Functions (Java) for background jobs
    └── pom.xml
```

**Module dependency flow:** `api` -> `application` -> `domain` <- `infrastructure`

### Key Maven Dependencies

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API with embedded Tomcat |
| `spring-boot-starter-data-jpa` | JPA/Hibernate ORM |
| `spring-boot-starter-security` | Security framework |
| `spring-boot-starter-oauth2-resource-server` | Entra ID JWT validation |
| `spring-ai-azure-openai-spring-boot-starter` | Spring AI with Azure OpenAI (RAG, embeddings, chat) |
| `com.azure:azure-storage-blob` | Azure Blob Storage SDK |
| `com.azure:azure-search-documents` | Azure AI Search SDK |
| `com.azure:azure-ai-formrecognizer` | Azure Document Intelligence SDK |
| `com.azure:azure-identity` | Managed Identity / DefaultAzureCredential |
| `org.postgresql:postgresql` | PostgreSQL JDBC driver |
| `org.flywaydb:flyway-core` | Database migrations |
| `com.aspose:aspose-words` | Word/DOCX to PDF conversion (Java) |
| `com.aspose:aspose-slides` | PowerPoint to PDF conversion (Java) |
| `com.aspose:aspose-cells` | Excel to PDF conversion (Java) |
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | Swagger/OpenAPI documentation |
| `com.microsoft.graph:microsoft-graph` | Microsoft Graph SDK for Entra ID group sync |

### Estimated Monthly Cost

Backend hosting cost is covered under the Hosting section (Section 10).

---

## 4. Frontend -- ReactJS

### Recommendation: React 18+ with TypeScript

### UI Component Library

**Recommendation: shadcn/ui**

| Library | License | Components | Customization | Bundle Size |
|---|---|---|---|---|
| **shadcn/ui** | MIT | 50+ (copy-paste) | **Full control** -- you own the code | **Minimal** (only what you use) |
| MUI | MIT (core) + Commercial (X) | 60+ core | Theme-based, limited override | Moderate |
| Ant Design | MIT | 70+ | Theme-based, CJK defaults | Large |

**Why shadcn/ui:** shadcn/ui provides beautifully designed, accessible components built on Radix UI primitives and styled with Tailwind CSS. Unlike traditional component libraries, components are copied into your project as source code, giving full ownership and customization control. There are no version lock-in concerns or opaque abstractions. The component quality is excellent for enterprise UIs, and the Tailwind CSS foundation ensures consistent, maintainable styling.

### PDF Viewer

**Recommendation: `@react-pdf-viewer/core` (free) for basic viewing, or PSPDFKit/Nutrient Web SDK (commercial) for advanced needs**

| Library | License | Features | Cost |
|---|---|---|---|
| `@react-pdf-viewer/core` | Apache 2.0 | View, zoom, search, thumbnails | Free |
| `react-pdf` (wojtekmaj) | MIT | Basic rendering via PDF.js | Free |
| **PSPDFKit / Nutrient** | Commercial | Annotations, forms, redaction, comparison | ~$3,200/year |

**Recommendation:** Start with `@react-pdf-viewer/core` for viewing PDF renditions. It wraps PDF.js with a clean React API, supports search, thumbnails, zoom, and page navigation. If annotation or in-browser editing becomes a requirement later, upgrade to PSPDFKit/Nutrient.

### Side-by-Side Document Comparison

**Recommendation: Two-pronged approach**

1. **Text/metadata comparison:** Use `react-diff-viewer-continued` (active fork of react-diff-viewer, MIT license) for comparing document text content, metadata changes, and version diffs. Supports split (side-by-side) and unified views with syntax highlighting.

2. **Visual PDF comparison:** Render two PDF viewers side-by-side using `@react-pdf-viewer/core` in a split-pane layout with `react-resizable-panels`. For pixel-level comparison, process on the backend: render both PDFs to images, diff the images, and overlay highlights.

```tsx
// Side-by-side PDF comparison layout
<PanelGroup direction="horizontal">
  <Panel defaultSize={50}>
    <PdfViewer fileUrl={versionA.url} />
  </Panel>
  <PanelResizeHandle />
  <Panel defaultSize={50}>
    <PdfViewer fileUrl={versionB.url} />
  </Panel>
</PanelGroup>
```

### Data Grid

**Recommendation: TanStack Table with shadcn/ui DataTable**

shadcn/ui provides a DataTable component built on TanStack Table (headless), offering:

- Server-side filtering, sorting, and pagination
- Column pinning, visibility toggles, and reordering
- Full customization since you own the source code
- No commercial license required
- Row virtualization via TanStack Virtual for large datasets

### Additional Frontend Libraries

| Library | Purpose |
|---|---|
| `@tanstack/react-query` | Server state management, caching |
| `@tanstack/react-table` | Headless data table (powers shadcn DataTable) |
| `react-router-dom` v6 | Client-side routing |
| `react-hook-form` + `zod` | Form handling with validation |
| `@azure/msal-react` | Entra ID authentication in SPA |
| `axios` | HTTP client |
| `react-dropzone` | Drag-and-drop file upload |
| `react-resizable-panels` | Split-pane layouts for comparison |
| `tailwindcss` | Utility-first CSS (foundation for shadcn/ui) |
| `date-fns` | Date formatting |

### Frontend Hosting

**Azure Static Web Apps (Free tier)** -- serves the React SPA with global CDN, custom domain, and automatic HTTPS. Free for up to 2 custom domains and 100 GB bandwidth/month.

### Estimated Monthly Cost

| Item | Estimate |
|---|---|
| Azure Static Web Apps (Free tier) | $0/month |
| shadcn/ui + TanStack Table | $0 (MIT license) |
| **Total** | **$0/month** |

---

## 5. PDF Rendition from MSOffice -- Aspose

### Recommendation: Aspose.Words for Java + Aspose.Slides for Java + Aspose.Cells for Java

This is the most critical technical requirement. The system must convert uploaded Word, PowerPoint, and Excel files into PDF renditions for in-browser viewing.

### Options Evaluated

| Approach | Fidelity | Deployment | Cost | Concurrency | Azure Compat |
|---|---|---|---|---|---|
| **Aspose for Java** | **Excellent** -- near-identical to Office | Maven dependency, no external dependencies | Existing license (see note) | Unlimited (in-process) | **Native** |
| LibreOffice Headless | Good -- occasional layout drift | Requires 500MB+ runtime installation | Free (GPLv3) | Limited (1 process per conversion) | Complex setup |
| Microsoft Graph API | Excellent | Cloud API call | Per-user M365 license | API rate limits | Native |
| GrapeCity/GcDocs | Good | Maven dependency | ~$3,000/year | Unlimited | Native |

### Why Aspose over alternatives

1. **Fidelity:** Aspose renders documents using its own layout engine that replicates Microsoft Office rendering. Fonts, tables, charts, SmartArt, and embedded objects are preserved with the highest fidelity of any non-Office solution. LibreOffice often misaligns complex layouts, especially with PowerPoint SmartArt and Excel charts.

2. **No external dependencies:** Aspose runs entirely in-process as a Java library (JAR dependency via Maven). LibreOffice requires a separate headless process, adding operational complexity, memory overhead (~500MB per process), and concurrency limitations (one conversion at a time per process).

3. **Azure-native deployment:** Aspose for Java works in Azure App Service and Azure Functions without any special configuration -- it is just a JAR dependency. No custom runtime installations, no OS-level packages, no special image builds.

4. **Microsoft Graph API limitation:** While Graph API can convert files using the Office Online engine, it requires each document to be stored in OneDrive/SharePoint and requires per-user M365 licensing. This defeats the purpose of building a custom DMS.

### Aspose License Status

The user holds an existing **Aspose.Total for Java** license (valid through December 2024). After expiry, the libraries operate in **evaluation mode** with watermarks and page/feature limitations until the license is renewed.

| Product | License Status |
|---|---|
| Aspose.Words for Java | Covered under Aspose.Total for Java (expires Dec 2024) |
| Aspose.Slides for Java | Covered under Aspose.Total for Java (expires Dec 2024) |
| Aspose.Cells for Java | Covered under Aspose.Total for Java (expires Dec 2024) |

*Renewal of Aspose.Total for Java is ~$2,999/year for a Developer Small Business license (1 developer, 1 deployment). Budget for renewal before production launch.*

### Rendition Pipeline Architecture

```
Upload Event (Blob trigger via Azure Function or Service Bus message)
    |
    v
[Azure Functions (Java) -- Background Processing]
    |
    +---> Detect file type (DOCX, PPTX, XLSX, PDF)
    |
    +---> If Office format:
    |       Aspose.Words / Slides / Cells for Java --> Convert to PDF
    |       Save PDF rendition to Blob Storage
    |       Generate thumbnail (first page as JPEG)
    |
    +---> If already PDF:
    |       Copy as-is to renditions container
    |       Generate thumbnail
    |
    +---> Update rendition status in PostgreSQL
    +---> Trigger text extraction (Document Intelligence)
    +---> Trigger AI indexing (Azure AI Search)
```

### Estimated Monthly Cost

| Item | Estimate |
|---|---|
| Aspose.Total for Java renewal (amortized) | ~$250/month |
| **Total** | **~$250/month amortized** |

---

## 6. RAG / AI Services

### 6.1 Document Text Extraction / OCR

**Recommendation: Azure Document Intelligence (formerly Form Recognizer)**

**SKU:** S0 (Pay-as-you-go)

- **Read model:** Extracts text, tables, and structure from PDFs and images
- **Layout model:** Preserves document structure (headings, paragraphs, tables) for better chunking
- **Pricing:** $1.50 per 1,000 pages (Read), $10.00 per 1,000 pages (Layout)
- **Why this over raw OCR:** Document Intelligence understands document structure, not just characters. It outputs text with semantic sections, which produces better chunks for RAG.

**SDK:** `com.azure:azure-ai-formrecognizer` (Java)

### 6.2 Document Indexing and Search

**Recommendation: Azure AI Search**

**SKU:** Basic tier ($75.14/month)

| Tier | Indexes | Storage | Replicas | Cost |
|---|---|---|---|---|
| Free | 3 | 50 MB | 1 | $0 |
| **Basic** | **15** | **2 GB** | **3** | **~$75/month** |
| Standard S1 | 50 | 25 GB | 12 | ~$249/month |

**Why Basic tier:** For 50 users and an initial document corpus, Basic provides 2 GB of index storage (sufficient for ~200K documents with metadata), 15 indexes, and up to 3 replicas for availability. Start here and upgrade to S1 when index storage exceeds 2 GB.

**Features to use:**
- **Hybrid search:** Combines keyword (BM25) and vector search for best recall
- **Semantic ranking:** Re-ranks results using a Microsoft-trained language model
- **Integrated vectorization:** Azure AI Search can call Azure OpenAI embeddings during indexing, eliminating custom embedding pipeline code
- **Skillsets:** Built-in OCR, key phrase extraction, and entity recognition during indexing

**SDK:** `com.azure:azure-search-documents` (Java)

### 6.3 Embeddings Generation

**Recommendation: Azure OpenAI -- text-embedding-3-small**

| Model | Dimensions | Cost per 1M tokens | Quality | Use Case |
|---|---|---|---|---|
| text-embedding-3-small | 1,536 | ~$0.02 | Good | Cost-effective, general use |
| text-embedding-3-large | 3,072 | ~$0.13 | Best | When quality matters most |
| text-embedding-ada-002 | 1,536 | ~$0.10 | Good | Legacy, avoid for new projects |

**Decision:** Use `text-embedding-3-small` for cost efficiency. At ~$0.02 per 1M tokens, embedding a 10-page document (~3K tokens) costs fractions of a cent. The quality difference versus `text-embedding-3-large` is minimal for document search use cases.

### 6.4 LLM for Querying

**Recommendation: Azure OpenAI -- GPT-4o-mini (primary) + GPT-4o (complex queries)**

| Model | Input Cost (1M tokens) | Output Cost (1M tokens) | Use Case |
|---|---|---|---|
| **GPT-4o-mini** | ~$0.15 | ~$0.60 | Summarization, simple Q&A |
| GPT-4o | ~$2.50 | ~$10.00 | Complex reasoning, comparison |
| GPT-4.1 | ~$2.00 | ~$8.00 | Latest capabilities (when available) |

**Decision:** Route most queries to GPT-4o-mini for cost efficiency. Use GPT-4o for complex multi-document queries or when GPT-4o-mini produces low-confidence responses. Spring AI's chat client abstraction and prompt templating handle this routing cleanly.

### RAG Pipeline Architecture

```
User Query
    |
    v
[Spring AI Chat Client / Advisor Chain]
    |
    +---> Embed query (text-embedding-3-small via Spring AI)
    +---> Hybrid search (Azure AI Search: vector + keyword + semantic ranking)
    +---> Retrieve top-K document chunks with metadata
    +---> Apply ACL filter (only return chunks from documents the user can access)
    +---> Construct prompt with retrieved context (Spring AI PromptTemplate)
    +---> Call GPT-4o-mini (or GPT-4o for complex queries)
    +---> Return grounded answer with source citations
```

### Estimated Monthly Cost (AI Services)

| Service | Estimate (50 users, moderate usage) |
|---|---|
| Azure AI Search (Basic) | ~$75/month |
| Azure OpenAI Embeddings (text-embedding-3-small) | ~$5/month |
| Azure OpenAI LLM (GPT-4o-mini primary) | ~$30/month |
| Azure Document Intelligence (Read model, ~5K pages/month) | ~$8/month |
| **Total** | **~$118/month** |

---

## 7. Document Summarization

### Recommendation: Azure OpenAI GPT-4o-mini with Spring AI

**Approach:**
1. When a document is uploaded, the text extraction pipeline (Document Intelligence) produces structured text.
2. An Azure Function (Java) sends the extracted text to GPT-4o-mini via Spring AI with a summarization prompt.
3. The summary is stored as a text file in Azure Blob Storage under `summaries/{document-id}/summary.txt`.
4. The summary is also stored in the PostgreSQL metadata table for quick retrieval without Blob access.

**Prompt Strategy:**
- For short documents (< 4K tokens): Single-pass summarization
- For long documents (> 4K tokens): Map-reduce pattern -- summarize each chunk, then summarize the summaries
- Output: 3-5 bullet points + one-paragraph executive summary

**Key Library:** `spring-ai-azure-openai-spring-boot-starter` -- provides chat client abstraction, prompt templating, token counting, and output parsing utilities.

**Cost:** Included in the Azure OpenAI estimates above (Section 6.4).

---

## 8. Case Study PPT Generation

### Recommendation: Aspose.Slides for Java (covered under existing Aspose.Total for Java license)

### Why Aspose.Slides over alternatives

| Library | Language | Template Support | Chart/Table | License |
|---|---|---|---|---|
| **Aspose.Slides for Java** | Java | **Full PPTX template manipulation** | Full | Commercial (existing license) |
| python-pptx | Python | Good template support | Limited charts | MIT (Free) |
| Indico PPTX API | SaaS | WYSIWYG template editor | Good | SaaS pricing |
| pptxgenjs | JavaScript | Create from scratch only | Basic | MIT (Free) |

**Decision:** Since we already have an Aspose.Total for Java license, Aspose.Slides for Java is included at no additional cost. It provides the most comprehensive PowerPoint manipulation API available: clone slides from template, replace placeholders, populate tables, insert charts, and apply master slide formatting.

### PPT Generation Workflow

```
1. Designer creates PPTX template with named placeholders
   - {{client_name}}, {{project_title}}, {{challenge}}, {{solution}}, {{results}}
   - Table placeholders for metrics
   - Image placeholders for screenshots/diagrams

2. User fills case study form in the web UI

3. Backend (Aspose.Slides for Java):
   - Load template PPTX from Blob Storage
   - Replace text placeholders with form data
   - Populate tables with structured data
   - Optionally call GPT-4o-mini to polish/expand text
   - Save generated PPTX to Blob Storage
   - Generate PDF rendition of the PPTX

4. User downloads PPTX or views PDF in browser
```

### Key Aspose.Slides Operations

```java
Presentation presentation = new Presentation("template.pptx");

// Replace text placeholders
for (ISlide slide : presentation.getSlides()) {
    for (IShape shape : slide.getShapes()) {
        if (shape instanceof IAutoShape) {
            IAutoShape autoShape = (IAutoShape) shape;
            String text = autoShape.getTextFrame().getText();
            if (text.contains("{{client_name}}")) {
                for (IParagraph paragraph : autoShape.getTextFrame().getParagraphs()) {
                    for (IPortion portion : paragraph.getPortions()) {
                        String portionText = portion.getText();
                        if (portionText.contains("{{client_name}}")) {
                            portion.setText(portionText.replace("{{client_name}}", caseStudy.getClientName()));
                        }
                    }
                }
            }
        }
    }
}

// Add table row
ITable table = (ITable) slide.getShapes().get_Item(0);
table.getRows().addClone(table.getRows().get_Item(1), false);
IRow row = table.getRows().get_Item(table.getRows().size() - 1);
row.get_Item(0).getTextFrame().setText(metric.getName());
row.get_Item(1).getTextFrame().setText(metric.getValue());

presentation.save("output.pptx", com.aspose.slides.SaveFormat.Pptx);
```

**Cost:** Included in Aspose.Total for Java license (Section 5).

---

## 9. Authentication -- Microsoft Entra ID

### Recommendation: Microsoft Entra ID (formerly Azure AD)

**SKU:** Entra ID Free tier (included with any Azure subscription) or P1 ($6/user/month) if Conditional Access is needed.

### Why Entra ID

This is a non-decision for an enterprise Azure application. Entra ID provides:

- **Single Sign-On (SSO):** Users authenticate with their corporate Microsoft 365 credentials. No separate password.
- **Group-based access:** Map Entra ID security groups to document ACLs (e.g., "Sales Team" group gets read access to all proposals).
- **Multi-Factor Authentication (MFA):** Included in the Free tier.
- **Conditional Access (P1):** Block access from unmanaged devices, require MFA from outside corporate network.
- **MSAL libraries:** First-class support in React (`@azure/msal-react`) and Java (`com.azure:msal4j`).

### Authentication Flow

```
React SPA
  |
  +---> MSAL.js redirects to Entra ID login
  +---> User authenticates (SSO with M365)
  +---> Receives ID token + Access token (JWT)
  +---> Access token sent to Spring Boot API in Authorization header
  |
Spring Boot API
  +---> Validates JWT (Spring Security OAuth2 Resource Server)
  +---> Extracts user identity, group memberships from JWT claims
  +---> Checks document ACLs against user/group
```

### Key SDKs

| SDK | Usage |
|---|---|
| `@azure/msal-react` | React SPA authentication |
| `@azure/msal-browser` | Token acquisition and caching |
| `spring-boot-starter-oauth2-resource-server` | Spring Boot JWT validation |
| `com.azure:msal4j` | MSAL for Java (token acquisition on backend) |
| `com.microsoft.graph:microsoft-graph` | Sync users and groups from Entra ID |

### Estimated Monthly Cost

| Item | Estimate |
|---|---|
| Entra ID Free tier (50 users) | $0/month |
| Entra ID P1 (if Conditional Access needed) | $300/month ($6/user) |
| **Total (starting)** | **$0/month** |

---

## 10. Hosting -- Azure App Service for Java

### Recommendation: Azure App Service (B1 or S1 tier) with built-in Java 21 runtime

### Why App Service over alternatives

| Criteria | App Service (B1/S1) | Azure Kubernetes Service (AKS) |
|---|---|---|
| Monthly cost (low traffic) | **~$13/month (B1)** or ~$55/month (S1) | ~$150+/month |
| Java 21 runtime | **Built-in** (no setup) | Manual container configuration |
| Deployment model | **Deploy JAR directly** | Container images required |
| Kubernetes complexity | **None** | High |
| Auto-scaling | Yes (S1+, rule-based) | Yes (KEDA, HPA) |
| Built-in TLS/custom domain | Yes | Requires ingress controller |
| Deployment slots (staging) | Yes (S1+) | Namespace-based |
| Managed Identity | Built-in | Built-in |
| Monitoring | Built-in App Insights integration | Manual setup |

**Decision:** Azure App Service is the simplest and most cost-effective hosting option for this project. It provides a built-in Java 21 runtime, so you deploy a fat JAR directly -- no container images, no container registry, no Dockerfile to maintain. The B1 tier at ~$13/month is sufficient for a 50-user team during initial rollout. Scale to S1 (~$55/month) when you need deployment slots, auto-scaling, or more compute.

For background processing (renditions, indexing, summarization), Azure Functions (Java) on the Consumption plan handles event-driven workloads with pay-per-execution pricing.

### Deployment Architecture

```
Azure App Service (Java 21 runtime)
  |
  +---> [presales-api.jar]         # Spring Boot 3.x Web API
          B1 tier (1 vCPU, 1.75 GB RAM)
          Custom domain + TLS
          Managed Identity for Azure resource access

Azure Functions (Java, Consumption Plan)
  |
  +---> [presales-functions.jar]   # Background processing
          Triggered by: Blob events, Service Bus messages, Timer
          Handles: PDF rendition, text extraction, AI indexing, summarization
          Pay-per-execution (first 1M executions free/month)

Azure Static Web Apps (Free tier)
  |
  +---> [React SPA]                # Frontend
          Global CDN, automatic HTTPS
```

### Key Configuration

- **App Service Plan:** B1 (1 vCPU, 1.75 GB RAM) -- upgrade to S1 for deployment slots and auto-scaling
- **Java runtime:** Java 21 (built-in, selected via App Service configuration)
- **Deployment:** JAR file deployed via GitHub Actions (`azure/webapps-deploy` action)
- **Health probes:** Spring Boot Actuator endpoints (`/actuator/health`)
- **Secrets:** Managed Identity for Azure resources (no connection strings in config), App Service Configuration for app settings
- **Logging:** Application Insights for distributed tracing and log aggregation

### Estimated Monthly Cost

| Item | Estimate |
|---|---|
| App Service B1 (1 vCPU, 1.75 GB RAM, always on) | ~$13/month |
| Azure Functions Consumption (background jobs) | ~$2/month (pay-per-execution) |
| **Total** | **~$15/month** |

*Scale to S1 (~$55/month) for deployment slots, auto-scaling, and more compute when production load demands it.*

---

## 11. CI/CD Pipeline

### Recommendation: GitHub Actions with Maven

### Pipeline Architecture

```yaml
# .github/workflows/deploy.yml
name: Build and Deploy

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean package -DskipTests
        working-directory: src/backend
      - name: Run tests
        run: mvn test
        working-directory: src/backend
      - uses: azure/webapps-deploy@v3
        with:
          app-name: presales-api
          package: src/backend/presales-api/target/*.jar

  deploy-functions:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build Functions
        run: mvn clean package -DskipTests -pl presales-functions
        working-directory: src/backend
      - uses: azure/functions-action@v1
        with:
          app-name: presales-functions
          package: src/backend/presales-functions/target/azure-functions/
```

### Key Points

- **Build tool:** Maven (multi-module, parent POM manages dependency versions)
- **Java version:** 21 (Temurin distribution)
- **App Service deployment:** JAR deployed directly via `azure/webapps-deploy` GitHub Action
- **Functions deployment:** Deployed via `azure/functions-action` GitHub Action
- **Frontend deployment:** Azure Static Web Apps has its own GitHub Action for auto-deploy on push
- **No containers anywhere:** No Dockerfile, no container registry, no image builds

### Estimated Monthly Cost

| Item | Estimate |
|---|---|
| GitHub Actions (2,000 free minutes/month for public repos) | $0/month |
| **Total** | **$0/month** |

---

## Cost Summary

### Monthly Operating Cost (50 users, moderate usage)

| Layer | Service | Monthly Cost |
|---|---|---|
| File Storage | Azure Blob Storage | ~$28 |
| Database | PostgreSQL Flexible Server (B1ms) | ~$10 |
| AI Search | Azure AI Search (Basic) | ~$75 |
| AI Services | Azure OpenAI + Document Intelligence | ~$43 |
| Hosting | Azure App Service (B1) + Azure Functions | ~$15 |
| Frontend Hosting | Azure Static Web Apps | $0 |
| Authentication | Entra ID (Free tier) | $0 |
| CI/CD | GitHub Actions | $0 |
| **Infrastructure Total** | | **~$171/month** |

### Annual License Costs (Amortized Monthly)

| License | Annual | Monthly Amortized |
|---|---|---|
| Aspose.Total for Java (renewal) | ~$2,999/year | ~$250/month |
| shadcn/ui + TanStack Table | $0 (MIT) | $0/month |
| **License Total** | | **~$250/month** |

### Grand Total

| | Monthly | Annual |
|---|---|---|
| **Infrastructure** | ~$171 | ~$2,052 |
| **Licenses** | ~$250 | ~$2,999 |
| **Total** | **~$421** | **~$5,051** |

*These estimates assume a 50-user team with moderate document upload volume (~500 documents/month). Costs will increase with usage but remain well within enterprise budgets. For comparison, SharePoint Online Plan 2 costs $12.50/user/month = $625/month for 50 users, without any AI capabilities.*

---

## Appendix: Key Decision Log

| Decision | Chosen | Rejected | Reason |
|---|---|---|---|
| File Storage | Azure Blob Storage | Azure Files, Data Lake | REST-native, cheapest, lifecycle tiering |
| Database | PostgreSQL Flexible | Azure SQL, Cosmos DB | JSONB + relational, stop/start, pgvector |
| Backend | Java 21 + Spring Boot 3.x | Python | Aspose for Java native support, Spring AI, virtual threads |
| PDF Rendition | Aspose for Java | LibreOffice, Graph API | Fidelity, no dependencies, existing license |
| RAG Framework | Spring AI | LangChain4j | Official Spring project, Azure OpenAI native, Spring ecosystem |
| UI Components | shadcn/ui | MUI, Ant Design | Full code ownership, Tailwind CSS, zero license cost |
| Data Grid | TanStack Table + shadcn | MUI DataGrid Pro | Free, headless, fully customizable |
| PDF Viewer | @react-pdf-viewer | PSPDFKit | Free, sufficient for viewing renditions |
| Search | Azure AI Search | Elasticsearch | Managed, integrated vectorization, skillsets |
| LLM | GPT-4o-mini + GPT-4o | Single model | Cost optimization with quality routing |
| Hosting | App Service (JAR deploy) | AKS | Simplest deployment, built-in Java runtime, lowest cost |
| Background Jobs | Azure Functions (Java) | Spring Batch in App Service | Pay-per-execution, event-driven, scales independently |
| CI/CD | GitHub Actions + Maven | Azure DevOps | Free tier sufficient, simpler YAML config |
| Auth | Entra ID + Spring Security OAuth2 | Auth0, Custom | Enterprise standard, SSO, included free |
| PPT Generation | Aspose.Slides for Java | python-pptx | Existing license, full template support |
