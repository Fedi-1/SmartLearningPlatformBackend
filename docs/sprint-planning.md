# LearnAI — Sprint Planning Document

> **Project:** LearnAI — Smart Learning Platform  
> **Total User Stories:** 65 (US-01 → US-65)  
> **Total Story Points:** 227 (functional) + 13 (testing / deployment)  
> **Total Sprints:** 5  
> **Total Duration:** ~11 weeks

---

## SCRUM ROLES

| Role | Name |
|------|------|
| **Product Owner** | Fedi Mhenni |
| **Scrum Master** | Fedi Mhenni |
| **Development Team** | Fedi Mhenni |

> *Single-developer project — a common and accepted configuration for a PFE (Projet de Fin d'Études). All Scrum ceremonies (planning, daily stand-up, review, retrospective) were conducted as self-managed checkpoints.*

---

## TECHNIQUES UTILISÉES

| Category | Tools & Methodologies |
|----------|-----------------------|
| **Methodology** | Scrum framework (5 sprints, incremental delivery) |
| **Version Control** | Git / GitHub (feature branches, pull requests) |
| **Backend** | Spring Boot 3 (Java 21), REST API, Spring Security |
| **Frontend** | Angular 18, TypeScript, TailwindCSS |
| **AI Microservice** | FastAPI (Python), Groq API / Llama 3.3 70B |
| **Database** | PostgreSQL, Spring Data JPA / Hibernate |
| **Authentication** | JWT (JSON Web Tokens), BCrypt password hashing |
| **AI / LLM** | Groq API — Llama 3.3 70B (course generation, quiz, exam, chatbot) |
| **Text-to-Speech** | ElevenLabs TTS (lesson audio recap) |
| **Video Rendering** | Remotion (animated lesson recap video) |
| **Real-time** | Server-Sent Events (SSE) for in-app notifications |
| **Email** | Gmail SMTP via Spring Mail (async HTML emails) |
| **Unit Testing** | JUnit 5 + Mockito |
| **API Testing** | Postman (manual & collection-based) |
| **IDEs** | IntelliJ IDEA (backend), VS Code (frontend & AI service) |
| **UI Prototyping** | v0.dev (component scaffolding) |
| **Spaced Repetition** | SM-2 algorithm (flashcard scheduling) |

---

## SPRINT 1 — Foundation

> **Duration:** 2 weeks  
> **Sprint Goal:** Deliver a fully working authentication system and document-upload pipeline so that students can register, log in, and submit learning material.

### Sprint Backlog

| ID | User Story | Story Points | Status |
|----|------------|:------------:|:------:|
| US-01 | Register a new student account (name, email, password, DOB, phone) | 5 | ✅ Done |
| US-02 | Verify email address via verification link | 3 | ✅ Done |
| US-03 | Request a new verification email (rate-limited) | 2 | ✅ Done |
| US-04 | Log in with email & password and receive a JWT token | 3 | ✅ Done |
| US-05 | Request a password-reset email | 3 | ✅ Done |
| US-06 | Validate reset token and submit a new password | 3 | ✅ Done |
| US-07 | View and update student profile (name, DOB, phone) | 2 | ✅ Done |
| US-08 | Change password (requires current-password verification) | 2 | ✅ Done |
| US-09 | Admin: view and update admin profile + change password | 2 | ✅ Done |
| US-10 | Upload a document (PDF, DOCX, PPTX, JPG, PNG, WEBP ≤ 10 MB) | 5 | ✅ Done |
| US-11 | Detect duplicate document via SHA-256 hash comparison | 3 | ✅ Done |
| US-12 | View list of all uploaded documents | 2 | ✅ Done |
| US-13 | Delete a document (hard delete including associated course) | 3 | ✅ Done |

**Sprint 1 Total: 38 story points**

### Sprint Review

All authentication endpoints (`/api/auth/*`) and document management endpoints (`/api/documents/*`) were delivered and tested. Students can self-register, verify their email, recover their password, maintain their profile, and upload learning material. Duplicate detection using SHA-256 prevents redundant AI processing. The document delete operation correctly removes the document and the entire associated course cascade.

### Sprint Retrospective

- ✅ **What went well:** Implementing Spring Security with a stateless JWT filter was straightforward thanks to the clean layered architecture; role-based access (STUDENT vs ADMIN) was established early and never needed revisiting.
- ✅ **What went well:** File-type validation and size enforcement at the service layer kept the upload endpoint robust and easy to unit-test in isolation.
- ✅ **What went well:** Rate-limiting the resend-verification flow with a `lastVerificationEmailSent` timestamp prevented abuse without requiring a third-party library.

---

## SPRINT 2 — AI Core

> **Duration:** 3 weeks  
> **Sprint Goal:** Integrate the FastAPI AI microservice so that uploaded documents are transformed into fully structured courses with lessons, flashcards, quizzes, and a working course-viewer with lesson-progress tracking.

### Sprint Backlog

| ID | User Story | Story Points | Status |
|----|------------|:------------:|:------:|
| US-14 | AI-powered course generation from uploaded document (title, category, description) | 8 | ✅ Done |
| US-15 | AI-powered lesson generation (content, summary, estimated reading time) | 8 | ✅ Done |
| US-16 | AI-powered flashcard generation with difficulty levels per lesson | 5 | ✅ Done |
| US-17 | AI-powered quiz generation — questions generated fresh by AI on each attempt | 5 | ✅ Done |
| US-18 | Trigger AI recap image / video generation for a lesson | 5 | ✅ Done |
| US-19 | View full course details (lessons, quizzes, flashcards) | 3 | ✅ Done |
| US-20 | View lesson progress across an entire course | 3 | ✅ Done |
| US-21 | Record lesson access timestamp | 2 | ✅ Done |
| US-22 | View detailed progress for a specific lesson | 2 | ✅ Done |
| US-23 | Auto-unlock next lesson after quiz pass or attempt exhaustion | 5 | ✅ Done |
| US-24 | View overall learning dashboard (global %, per-course metrics) | 5 | ✅ Done |

**Sprint 2 Total: 51 story points**

### Sprint Review

The AI microservice (FastAPI + Groq Llama 3.3 70B) was integrated end-to-end. A single document upload now triggers asynchronous course generation producing lessons, flashcards, and quizzes. The `CourseService.generateAndSave()` pipeline persists all generated content. Lesson-progress tracking, sequential unlocking, and the student dashboard are fully operational. The recap feature streams ElevenLabs audio and Remotion-rendered video with HTTP range-request support.

### Sprint Retrospective

- ✅ **What went well:** Isolating all AI calls inside `AiServiceClient` (with typed exception `AiServiceException`) made error handling predictable and kept controllers thin.
- ✅ **What went well:** The hash-match course-cloning path (`CourseService.cloneFrom()`) eliminated redundant AI API calls for duplicate documents and reduced generation time to near-zero for re-uploads.
- ✅ **What went well:** Separating `LessonProgressService` from `CourseService` kept the unlock logic independently testable and reusable from both quiz and exam flows.

---

## SPRINT 3 — Assessment

> **Duration:** 2 weeks  
> **Sprint Goal:** Deliver a complete assessment system — per-lesson quizzes with AI-generated questions, SM-2 spaced-repetition flashcard reviews, and a final AI-powered exam with anti-cheat activity logging.

### Sprint Backlog

| ID | User Story | Story Points | Status |
|----|------------|:------------:|:------:|
| US-25 | Start a new quiz attempt — fresh AI-generated questions each time | 5 | ✅ Done |
| US-26 | Submit quiz answers and receive score, pass/fail, per-question explanations | 5 | ✅ Done |
| US-27 | Abandon a quiz attempt | 2 | ✅ Done |
| US-28 | View all past quiz attempts for a lesson | 2 | ✅ Done |
| US-29 | Start a daily SM-2 flashcard review session (due cards only) | 5 | ✅ Done |
| US-30 | Rate a flashcard AGAIN / GOOD / EASY (SM-2 interval update) | 5 | ✅ Done |
| US-31 | Generate a final exam (AI, 45 questions, one per course) | 8 | ✅ Done |
| US-32 | View existing final exam details before starting | 2 | ✅ Done |
| US-33 | Start a final exam attempt (max 3 attempts, 50-minute timer) | 5 | ✅ Done |
| US-34 | Submit exam answers — receive score, pass/fail, trigger certificate creation | 5 | ✅ Done |
| US-35 | Abandon an exam attempt | 2 | ✅ Done |
| US-36 | View all past exam attempts for a course | 2 | ✅ Done |
| US-37 | Report suspicious exam activity (tab-switch, copy-paste, right-click, timing) | 5 | ✅ Done |
| US-38 | Admin: view suspicious activity log and flagged exam attempts | 3 | ✅ Done |

**Sprint 3 Total: 56 story points**

### Sprint Review

The full assessment pipeline is live. Quizzes generate brand-new AI questions on every attempt, passing previous questions to avoid repetition. The SM-2 flashcard scheduler correctly adjusts intervals for each rating. The 45-question AI-generated final exam is cached per course (no duplicate generation). The anti-cheat module upserts suspicious activity records and surfaces flagged attempts in the admin panel.

### Sprint Retrospective

- ✅ **What went well:** The SM-2 algorithm was encapsulated in a single method, making the three rating branches (AGAIN / GOOD / EASY) easy to unit-test with deterministic inputs and expected intervals.
- ✅ **What went well:** Using `previousQuestions` as a deduplication hint to the AI service on quiz-attempt start eliminated repeated questions across attempts without requiring any complex question-pool management.
- ✅ **What went well:** The upsert pattern for suspicious activities (`SuspiciousActivityRepository`) kept the anti-cheat data tidy (one record per activity type per attempt) and made admin queries simple.

---

## SPRINT 4 — Certificates & Admin

> **Duration:** 2 weeks  
> **Sprint Goal:** Complete the certificate issuance workflow, deliver real-time and email notifications, and provide admins with a full-featured dashboard covering platform statistics, student management, and audit logs.

### Sprint Backlog

| ID | User Story | Story Points | Status |
|----|------------|:------------:|:------:|
| US-39 | Auto-create certificate (status PENDING) when exam is passed | 5 | ✅ Done |
| US-40 | Student: view all own certificates | 2 | ✅ Done |
| US-41 | Request AI-generated certificate PDF (base64 → BYTEA) | 5 | ✅ Done |
| US-42 | Download certificate PDF by public UUID (no auth required) | 3 | ✅ Done |
| US-43 | Admin: approve or revoke a certificate | 3 | ✅ Done |
| US-44 | Public certificate authenticity verification by UUID | 2 | ✅ Done |
| US-45 | Subscribe to real-time in-app notifications via SSE | 5 | ✅ Done |
| US-46 | View full notification history | 2 | ✅ Done |
| US-47 | View unread notification count | 1 | ✅ Done |
| US-48 | Mark a notification as read | 1 | ✅ Done |
| US-49 | Receive email notifications for course completion, exam results, certificate changes | 5 | ✅ Done |
| US-50 | Admin: view platform-wide statistics (students, courses, pass rate, etc.) | 3 | ✅ Done |
| US-51 | Admin: view activity chart for the last N days | 3 | ✅ Done |
| US-52 | Admin: view course category distribution | 2 | ✅ Done |
| US-53 | Admin: view recent platform activity entries | 2 | ✅ Done |
| US-54 | Admin: view all students with summary metrics | 3 | ✅ Done |
| US-55 | Admin: view detailed student info (courses, certs, attempts, suspicious counts) | 5 | ✅ Done |
| US-56 | Admin: enable / disable a student account | 3 | ✅ Done |
| US-57 | Admin: view all exam attempts with suspicious activity counts | 3 | ✅ Done |
| US-58 | Admin: view all certificates with approval status | 3 | ✅ Done |
| US-59 | Admin: browse paginated activity logs with filters | 3 | ✅ Done |
| US-60 | Admin: view a specific student's exam attempts | 2 | ✅ Done |

**Sprint 4 Total: 66 story points**

### Sprint Review

The certificate lifecycle (auto-creation → PDF generation → admin approval → public download → public verification) is fully functional. Notifications are delivered both in-app via SSE and via async HTML emails. The admin dashboard provides comprehensive visibility: platform-wide KPIs, activity trends, category distribution, student management (with account toggling), certificate approval queue, exam attempt review, suspicious-activity flagging, and paginated audit logs.

### Sprint Retrospective

- ✅ **What went well:** Routing notifications through a single `NotificationService` that dispatches to both SSE and email channels kept the notification logic in one place and avoided duplication across exam, quiz, and certificate flows.
- ✅ **What went well:** Storing the certificate PDF as BYTEA in PostgreSQL (rather than on disk) simplified deployment by eliminating any file-system dependency on the server.
- ✅ **What went well:** Building the admin dashboard as a pure aggregation layer over existing repositories meant zero new domain logic was needed — all queries leveraged Spring Data JPA projections and existing entity relationships.

---

## SPRINT 5 — Polish & Testing

> **Duration:** 2 weeks  
> **Sprint Goal:** Complete the landing page, deliver the chatbot feature for both visitors and authenticated students, then close out the project with full regression testing, bug fixing, and deployment preparation.

### Sprint Backlog

#### Functional Stories

| ID | User Story | Story Points | Status |
|----|------------|:------------:|:------:|
| US-61 | Visitor chatbot via public `/api/chat` endpoint (no auth required) | 3 | ✅ Done |
| US-62 | Landing page — hero section with registration call-to-action | 3 | ✅ Done |
| US-63 | Landing page — smooth scroll-based animations | 2 | ✅ Done |
| US-64 | Landing page — multi-section layout (features, how-it-works, testimonials, footer) | 3 | ✅ Done |
| US-65 | Student chatbot with lesson context (JWT-protected, lesson-aware AI response) | 5 | ✅ Done |

#### Non-Functional / Quality Tasks

| Task | Description | Points | Status |
|------|-------------|:------:|:------:|
| TEST-01 | Write and run JUnit 5 + Mockito unit tests for all service-layer methods | 5 | ✅ Done |
| TEST-02 | Postman collection: full API regression run across all 65 stories | 3 | ✅ Done |
| DEPLOY-01 | Finalise environment configuration (application.yml, env variables) and deployment prep | 2 | ✅ Done |
| DOC-01 | Complete `docs/product-backlog.md` and `docs/sprint-planning.md` | 3 | ✅ Done |

**Sprint 5 Total (functional): 16 story points**  
**Sprint 5 Total (quality tasks): 13 story points**  
**Sprint 5 Grand Total: 29 story points**

### Sprint Review

The Angular 18 landing page is complete with a hero section, scroll-triggered animations, and multi-section layout. The chatbot feature is available to both unauthenticated visitors (public `/api/chat`) and authenticated students (lesson-context-aware responses). Unit tests covering service-layer business logic were authored with JUnit 5 and Mockito. A Postman collection was used to run a full API regression pass. The project is ready for deployment.

### Sprint Retrospective

- ✅ **What went well:** Prototyping the landing-page UI components with v0.dev dramatically accelerated the Angular layout work, allowing more time to focus on lesson-context integration in the chatbot.
- ✅ **What went well:** Declaring `/api/chat` as `permitAll()` in `SecurityConfig` from the start (Sprint 1 security setup) meant adding visitor chatbot access in Sprint 5 required no security changes — just the endpoint implementation.
- ✅ **What went well:** Completing the documentation (product backlog and sprint planning) as part of the final sprint kept it accurate and grounded in the actual delivered code rather than being written speculatively upfront.

---

## PROJECT SUMMARY

| Sprint | Theme | Duration | Story Points | Cumulative |
|--------|-------|:--------:|:------------:|:----------:|
| Sprint 1 | Foundation | 2 weeks | 38 | 38 |
| Sprint 2 | AI Core | 3 weeks | 51 | 89 |
| Sprint 3 | Assessment | 2 weeks | 56 | 145 |
| Sprint 4 | Certificates & Admin | 2 weeks | 66 | 211 |
| Sprint 5 | Polish & Testing | 2 weeks | 29 | 240 |
| **Total** | | **11 weeks** | **240** | |

> **Functional story points:** 227 (US-01 → US-65)  
> **Quality / non-functional task points:** 13 (tests, deployment, documentation)  
> **Grand total:** 240 story points
