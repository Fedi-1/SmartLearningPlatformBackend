# LearnAI — Sprint Planning Document

> **Project:** LearnAI — Smart Learning Platform  
> **Total User Stories:** 25 (US-01 → US-25)  
> **Total Story Points:** 202  
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
| US-01 | Student account registration and email verification | 8 | ✅ Done |
| US-02 | Login with JWT and full password-reset flow | 8 | ✅ Done |
| US-03 | Profile management and password change (student & admin) | 5 | ✅ Done |
| US-04 | Document upload with format/size validation and SHA-256 duplicate detection | 8 | ✅ Done |
| US-05 | Document listing and hard-delete with full course cascade | 5 | ✅ Done |

**Sprint 1 Total: 34 story points**

### Sprint Review

All authentication endpoints (`/api/auth/*`) and document management endpoints (`/api/documents/*`) were delivered and tested. Students can self-register, verify their email, recover their password, maintain their profile, and upload learning material. SHA-256 duplicate detection prevents redundant AI processing. Document deletion correctly cascades to the entire associated course.

### Sprint Retrospective

- ✅ **What went well:** Implementing Spring Security with a stateless JWT filter was straightforward thanks to the clean layered architecture; role-based access (STUDENT vs ADMIN) was established early and never needed revisiting.
- ✅ **What went well:** File-type validation and size enforcement at the service layer kept the upload endpoint robust and easy to unit-test in isolation.
- ✅ **What went well:** Rate-limiting the resend-verification flow with a `lastVerificationEmailSent` timestamp prevented abuse without requiring a third-party library.

---

## SPRINT 2 — AI Core

> **Duration:** 3 weeks  
> **Sprint Goal:** Integrate the FastAPI AI microservice so that uploaded documents are transformed into fully structured courses with lessons, flashcards, quizzes, recap media, and a working course viewer with lesson-progress tracking.

### Sprint Backlog

| ID | User Story | Story Points | Status |
|----|------------|:------------:|:------:|
| US-06 | AI-powered course and lesson generation from uploaded document | 13 | ✅ Done |
| US-07 | AI-powered flashcard and quiz generation per lesson | 8 | ✅ Done |
| US-08 | AI lesson recap generation (ElevenLabs audio + Remotion video) | 5 | ✅ Done |
| US-09 | Course viewer with full content and lesson access recording | 5 | ✅ Done |
| US-10 | Lesson-level progress tracking, sequential lesson unlock, and learning dashboard | 13 | ✅ Done |

**Sprint 2 Total: 44 story points**

### Sprint Review

The AI microservice (FastAPI + Groq Llama 3.3 70B) was integrated end-to-end. A single document upload now triggers asynchronous course generation producing lessons, flashcards, and quizzes. Lesson-progress tracking, sequential unlocking, and the student dashboard are fully operational. The recap feature streams ElevenLabs audio and Remotion-rendered video with HTTP range-request support.

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
| US-11 | Quiz attempt lifecycle — start, submit (scored), abandon, view history | 13 | ✅ Done |
| US-12 | SM-2 flashcard review — daily session and card rating | 8 | ✅ Done |
| US-13 | Final exam generation (AI, 45 questions, cached per course) and pre-attempt view | 8 | ✅ Done |
| US-14 | Exam attempt lifecycle — start (timed), submit, abandon, view history | 13 | ✅ Done |
| US-15 | Anti-cheat activity reporting and admin oversight of flagged attempts | 8 | ✅ Done |

**Sprint 3 Total: 50 story points**

### Sprint Review

The full assessment pipeline is live. Quizzes generate brand-new AI questions on every attempt, passing previous questions to avoid repetition. The SM-2 flashcard scheduler correctly adjusts intervals per rating. The 45-question AI-generated final exam is cached per course. The anti-cheat module upserts suspicious activity records and surfaces flagged attempts in the admin panel.

### Sprint Retrospective

- ✅ **What went well:** The SM-2 algorithm was encapsulated in a single method, making the three rating branches (AGAIN / GOOD / EASY) easy to unit-test with deterministic inputs and expected intervals.
- ✅ **What went well:** Using `previousQuestions` as a deduplication hint to the AI service on quiz-attempt start eliminated repeated questions across attempts without any complex question-pool management.
- ✅ **What went well:** The upsert pattern for suspicious activities (`SuspiciousActivityRepository`) kept the anti-cheat data tidy (one record per activity type per attempt) and made admin queries simple.

---

## SPRINT 4 — Certificates & Admin

> **Duration:** 2 weeks  
> **Sprint Goal:** Complete the certificate issuance workflow, deliver real-time and email notifications, and provide admins with a full-featured dashboard covering platform statistics, student management, and audit logs.

### Sprint Backlog

| ID | User Story | Story Points | Status |
|----|------------|:------------:|:------:|
| US-16 | Certificate lifecycle — auto-creation on exam pass, student listing, admin approval/revocation | 8 | ✅ Done |
| US-17 | Certificate PDF generation, public download by UUID, and authenticity verification | 8 | ✅ Done |
| US-18 | Notification system — real-time SSE in-app notifications and async email notifications | 13 | ✅ Done |
| US-19 | Admin statistics dashboard — KPIs, activity chart, category distribution, recent activity | 8 | ✅ Done |
| US-20 | Admin student and exam management — list, details, toggle-active, certificates, audit logs | 13 | ✅ Done |

**Sprint 4 Total: 50 story points**

### Sprint Review

The certificate lifecycle (auto-creation → PDF generation → admin approval → public download → public verification) is fully functional. Notifications are delivered both in-app via SSE and via async HTML emails. The admin dashboard provides comprehensive visibility: platform-wide KPIs, activity trends, category distribution, student management (with account toggling), certificate approval queue, exam attempt review, suspicious-activity flagging, and paginated audit logs.

### Sprint Retrospective

- ✅ **What went well:** Routing notifications through a single `NotificationService` that dispatches to both SSE and email channels kept the notification logic in one place and avoided duplication across exam, quiz, and certificate flows.
- ✅ **What went well:** Storing the certificate PDF as BYTEA in PostgreSQL (rather than on disk) simplified deployment by eliminating any file-system dependency on the server.
- ✅ **What went well:** Building the admin dashboard as a pure aggregation layer over existing repositories meant zero new domain logic was needed — all queries leveraged Spring Data JPA projections and existing entity relationships.

---

## SPRINT 5 — Polish & Testing

> **Duration:** 2 weeks  
> **Sprint Goal:** Complete the landing page and chatbot features, close out the project with full regression testing, and prepare the application for production deployment.

### Sprint Backlog

| ID | User Story | Story Points | Status |
|----|------------|:------------:|:------:|
| US-21 | Public visitor chatbot via `permitAll()` `/api/chat` endpoint | 3 | ✅ Done |
| US-22 | Authenticated student chatbot with lesson context (JWT-protected) | 5 | ✅ Done |
| US-23 | Landing page — hero, features, how-it-works, testimonials, footer with scroll animations | 8 | ✅ Done |
| US-24 | Unit testing (JUnit 5 + Mockito) and API regression testing (Postman) | 5 | ✅ Done |
| US-25 | Deployment preparation — environment configuration and production readiness | 3 | ✅ Done |

**Sprint 5 Total: 24 story points**

### Sprint Review

The Angular 18 landing page is complete with a hero section, scroll-triggered animations, and multi-section layout. The chatbot is available to both unauthenticated visitors (public `/api/chat`) and authenticated students (lesson-context-aware responses). Unit tests covering service-layer business logic were authored with JUnit 5 and Mockito. A Postman collection validated all 25 user stories. The project is ready for deployment.

### Sprint Retrospective

- ✅ **What went well:** Prototyping the landing-page UI components with v0.dev dramatically accelerated the Angular layout work, allowing more time to focus on lesson-context integration in the chatbot.
- ✅ **What went well:** Declaring `/api/chat` as `permitAll()` in `SecurityConfig` from the start (Sprint 1 security setup) meant adding visitor chatbot access in Sprint 5 required no security changes — just the endpoint implementation.
- ✅ **What went well:** Completing the documentation (product backlog and sprint planning) as part of the final sprint kept it accurate and grounded in the actual delivered code rather than being written speculatively upfront.

---

## PROJECT SUMMARY

| Sprint | Theme | Duration | Story Points | Cumulative |
|--------|-------|:--------:|:------------:|:----------:|
| Sprint 1 | Foundation | 2 weeks | 34 | 34 |
| Sprint 2 | AI Core | 3 weeks | 44 | 78 |
| Sprint 3 | Assessment | 2 weeks | 50 | 128 |
| Sprint 4 | Certificates & Admin | 2 weeks | 50 | 178 |
| Sprint 5 | Polish & Testing | 2 weeks | 24 | 202 |
| **Total** | | **11 weeks** | **202** | |

> **Total user stories:** 25 (US-01 → US-25)  
> **Total story points:** 202
