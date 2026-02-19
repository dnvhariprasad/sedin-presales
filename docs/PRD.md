# Product Requirements Document (PRD)

## Pre-Sales Asset Management System

**Version:** 1.0
**Date:** 2026-02-20
**Status:** Draft

---

## 1. Overview

### 1.1 Problem Statement

The organization has delivered 100+ projects but lacks a centralized, structured system for managing pre-sales assets. Case studies and related documents are scattered across SharePoint in PPT format with no standardization, version control, or intelligent search capabilities. Finding relevant case studies during pre-sales activities is time-consuming and inconsistent.

### 1.2 Solution

Build an enterprise-grade Document Management System (DMS) purpose-built for pre-sales assets, starting with case studies. The system will provide centralized storage, AI-powered search (RAG), automated case study formatting, PDF renditions, version management, and fine-grained access control.

### 1.3 MVP Scope

The MVP focuses exclusively on **Case Studies**. Future phases will extend to white papers, contract documents, and other pre-sales assets.

---

## 2. User Roles

| Role | Description |
|------|-------------|
| **Admin** | Full system access. Manages users, masters data, case study agents, and system settings. |
| **Editor** | Can upload, create, edit, and manage documents they have access to. |
| **Viewer** | Read-only access to documents they are permitted to view. |

- No self-registration. Admins create and manage all user accounts.

---

## 3. Functional Requirements

### 3.1 Document Management (DMS Core)

#### 3.1.1 Upload & Storage
- **FR-001**: Users can upload documents (PPT, PPTX, DOC, DOCX, PDF).
- **FR-002**: Files are stored on cloud file storage (not in the database).
- **FR-003**: Metadata is stored in a relational database with a reference link to the file in storage.
- **FR-004**: Upon upload, user fills in metadata: Domain, Industry, Technology (multi-value), Customer Name, Document Type, Document Date, Business Unit, SBU.
- **FR-005**: Metadata fields provide value assistance (typeahead/dropdown) from admin-managed master lists.
- **FR-006**: Upon upload, user is prompted with an option to index the document for RAG (yes/no flag).

#### 3.1.2 Versioning
- **FR-007**: Every re-upload of a document creates a new version; previous versions are retained.
- **FR-008**: Users can view version history of any document.
- **FR-009**: Users can download any specific version (PDF or native format).
- **FR-010**: Users can select any two versions for side-by-side comparison (rendered as PDFs).

#### 3.1.3 Access Control (ACLs)
- **FR-011**: Document-level ACLs — each document has explicit access permissions for users/roles.
- **FR-012**: Folder-level ACLs — permissions can be set at folder/category level and inherited by documents within.
- **FR-013**: ACL enforcement: users only see and access documents they are permitted to.
- **FR-014**: Admin can grant/revoke access per document or per folder for any user.

### 3.2 Renditions

#### 3.2.1 PDF Renditions
- **FR-015**: When an MSOffice file (PPT, PPTX, DOC, DOCX) is uploaded, a PDF rendition is automatically generated.
- **FR-016**: PDF rendition is stored in cloud storage alongside the native file with a relationship link in the database.
- **FR-017**: PDF rendition is regenerated when a new version is uploaded.

#### 3.2.2 Summarization Rendition
- **FR-018**: Upon upload (or on-demand), the system generates an AI summary of the document.
- **FR-019**: Summary is stored as a plain text file in cloud storage, linked as a rendition of the original document.
- **FR-020**: If a summary rendition already exists, the system serves it from storage (no repeat AI call).
- **FR-021**: When a new version is uploaded, the summary is regenerated and the old summary rendition is versioned.

### 3.3 Document Viewer

- **FR-022**: In-browser document viewer displays the PDF rendition of any document.
- **FR-023**: Viewer supports zoom, scroll, and page navigation.
- **FR-024**: Download option available for both PDF rendition and native format.
- **FR-025**: Version comparison mode: select two versions → display side-by-side in PDF format within the browser.

### 3.4 RAG & Intelligent Search

- **FR-026**: Documents flagged for indexing are processed and indexed into a vector store for RAG.
- **FR-027**: Users can query in natural language (e.g., "Show me case studies involving Salesforce implementation in healthcare").
- **FR-028**: RAG results return relevant documents with excerpts and relevance scores.
- **FR-029**: Index flag is editable — admin or editor can toggle indexing on/off per document.
- **FR-030**: When indexing is toggled off, the document's vectors are removed from the index.
- **FR-031**: New version upload re-indexes the document if the index flag is on.

### 3.5 Case Study Agent

#### 3.5.1 Agent Configuration (Admin)
- **FR-032**: Admin can create one or more "Case Study Agents" via a configuration UI.
- **FR-033**: Agent configuration includes:
  - Template pattern/layout to enforce (slide structure, sections, branding)
  - Required content sections (e.g., Customer Overview, Challenges, Solution, Technologies, Benefits)
  - Branding rules (logo placement, color scheme, fonts)
- **FR-034**: Admin selects one agent as the **Active Agent**. Only one agent can be active at a time.
- **FR-035**: Active agent selection can be changed at any time by admin.

#### 3.5.2 Auto-Validation on Upload
- **FR-036**: When a case study is uploaded, the Active Agent automatically validates it against the configured format.
- **FR-037**: If the uploaded case study does not conform to the template:
  - The agent automatically reformats it into the standard template.
  - The first slide contains the full summary (matching current single-slide format).
  - Any additional content from the original is added as subsequent slides.
- **FR-038**: The reformatted version is stored as a new rendition (not replacing the original native file).
- **FR-039**: Validation results (pass/fail, changes made) are logged and visible to the uploader.

#### 3.5.3 Case Study Creation Wizard
- **FR-040**: Users can create a new case study from scratch within the system.
- **FR-041**: System prompts the user for structured input:
  - Customer Name
  - Industry / Domain
  - Project Overview
  - Challenges (list)
  - Solution Description
  - Technologies Used
  - Key Benefits / Metrics
  - Engagement Model
- **FR-042**: Using the Active Agent's template, the system generates a PPT file with:
  - Slide 1: Single-slide summary (matching the standard format)
  - Subsequent slides: Detailed content per section
- **FR-043**: Generated PPT is stored as a new document with all metadata pre-filled.

### 3.6 Case Study Template Specification

Based on analysis of 5 existing case studies, the standard single-slide format follows this structure:

| Section | Description | Slide Area |
|---------|-------------|------------|
| **Title** | "[Customer]: [Project/Tagline]" | Top 25-30% |
| **Customer Overview** | 1-3 sentences about the customer | 10-15% |
| **Challenges** | 4-7 bullet points | 45-50% (shared) |
| **Solution** | Narrative paragraph on Sedin's approach | 45-50% (shared) |
| **Technologies** | Technology names with logos | 15-20% |
| **Benefits/Results** | Quantified metrics (e.g., "85% accuracy improvement") | Integrated with solution |
| **Branding** | Company logo, consistent color scheme | Fixed positions |

- Aspect ratio: 16:9
- Layout: Professional, corporate, white/light backgrounds
- All case studies use grouped shapes for layout organization

### 3.7 Admin Module

#### 3.7.1 User Management
- **FR-044**: Admin can create, edit, deactivate, and delete user accounts.
- **FR-045**: Admin assigns roles (Admin, Editor, Viewer) to users.
- **FR-046**: No self-registration or public signup.

#### 3.7.2 Masters Management
- **FR-047**: Admin can manage master lists for the following fields:
  - Domain
  - Industry
  - Technology
  - Document Type (Case Study, White Paper — for future use)
  - Business Unit
  - SBU
- **FR-048**: Masters page is accessible only to Admin role.
- **FR-049**: Master values are used for dropdown/typeahead in upload forms and grid filters.

#### 3.7.3 Agent Settings
- **FR-050**: Admin can create, edit, and delete Case Study Agents.
- **FR-051**: Admin can set one agent as Active.
- **FR-052**: Settings page consolidates: User Management, Masters, Agent Configuration.

### 3.8 Grid & Browse UI

- **FR-053**: Main view is a flat data grid displaying all case studies the user has access to.
- **FR-054**: Grid columns:
  - Document Title
  - Domain
  - Industry
  - Technology (multi-value, displayed as tags)
  - Customer Name
  - Document Type
  - Document Date
  - Business Unit
  - SBU
  - Version (latest)
- **FR-055**: Grid supports filtering by any metadata column.
- **FR-056**: Grid supports multi-column sorting.
- **FR-057**: Grid supports full-text search across metadata fields.
- **FR-058**: Clicking a row opens the document viewer.
- **FR-059**: Grid supports pagination for large datasets.

---

## 4. Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| **NFR-001** | System must support 50+ concurrent users. |
| **NFR-002** | Document upload size limit: up to 100 MB per file. |
| **NFR-003** | PDF rendition generation must complete within 60 seconds of upload. |
| **NFR-004** | In-browser PDF viewer must load within 3 seconds for files under 20 MB. |
| **NFR-005** | RAG query response time: under 5 seconds. |
| **NFR-006** | Enterprise-grade UI with consistent look and feel across all pages. |
| **NFR-007** | All data in transit encrypted (HTTPS/TLS). All data at rest encrypted. |
| **NFR-008** | Audit logging for document uploads, access, and admin operations. |
| **NFR-009** | System should be horizontally scalable for storage and compute. |

---

## 5. Future Scope (Out of MVP)

- Support for additional document types: White Papers, Contract Documents, Proposals
- Workflow/approval chains for document publishing
- Email notifications on document events
- Integration with external CRM for customer data
- Advanced analytics dashboard (most-viewed case studies, search trends)
- Bulk upload and migration tool (for SharePoint content)
- Multi-language support
- Mobile-responsive views

---

## 6. Assumptions & Constraints

1. All users are internal employees — no external/public access.
2. Azure cloud services will be used for all infrastructure.
3. Existing 5 case studies (single-slide PPTs) serve as the reference template for the Case Study Agent.
4. Initial data migration from SharePoint is manual (admin uploads).
5. AI/LLM services are Azure-based (Azure OpenAI).

---

## 7. Glossary

| Term | Definition |
|------|------------|
| **Rendition** | A derived representation of a document (e.g., PDF version of a PPT, text summary) |
| **ACL** | Access Control List — permissions defining who can access a document |
| **RAG** | Retrieval-Augmented Generation — AI technique that searches document content to answer queries |
| **Active Agent** | The currently selected Case Study Agent configuration used for auto-validation |
| **Masters** | Admin-managed lookup lists for metadata fields |
| **SBU** | Strategic Business Unit |
