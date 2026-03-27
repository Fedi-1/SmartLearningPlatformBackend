# LearnAI — Complete Product Backlog

> Generated from the SmartLearningPlatformBackend codebase.  
> All stories, entities, endpoints, and acceptance criteria are derived solely from what exists in the actual source code.
>
> **Consolidation (v3):** 65 fine-grained stories merged into **25 higher-level user stories** (US-01 → US-25) to reflect sprint-level delivery units while preserving full functional coverage.

---

## EPIC 1 — Authentication & Account Management

*Sprint 1 · 5 stories (US-01 → US-05)*

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-01 | As a **visitor**, I want to **register a student account and verify my email** so that **my account is activated and I can log in to the platform**. | High | 8 | • `POST /api/auth/register` creates the account and sends a verification email immediately.<br>• `GET /api/auth/verify-email?token=…` activates the account; invalid or expired tokens return an error.<br>• `POST /api/auth/resend-verification` sends a new token, respecting the rate-limit window (`lastVerificationEmailSent`).<br>• Duplicate email addresses are rejected at registration. |
| US-02 | As a **user**, I want to **log in with my credentials, receive a JWT token, and recover my password via email** so that **I can securely access and regain access to my account**. | High | 8 | • `POST /api/auth/login` returns `AuthResponse` (token, role, firstName, lastName); suspended accounts receive 403.<br>• `POST /api/auth/forgot-password` generates a 24-hour token and emails it without leaking whether the address exists.<br>• `GET /api/auth/validate-reset-token` confirms token validity; `POST /api/auth/reset-password` persists the new bcrypt-hashed password and invalidates the token. |
| US-03 | As a **logged-in user**, I want to **view and update my profile and change my password** so that **my personal information and account security stay current**. | Medium | 5 | • Students: `GET/PUT /api/students/profile` (name, DOB, phone); `PUT /api/students/password` requires current-password verification.<br>• Admin: equivalent endpoints under `/api/admin/profile` and `/api/admin/password`, restricted to ADMIN role.<br>• Omitted fields are not overwritten; incorrect current password returns an error without changing anything. |
| US-04 | As a **student**, I want to **upload a document and have duplicates detected automatically** so that **the platform can generate my course without redundant AI processing**. | High | 8 | • `POST /api/documents/upload` accepts PDF, DOCX, PPTX, JPG, PNG, WEBP up to 10 MB; over-size files throw `FileTooLargeException`.<br>• SHA-256 hash comparison detects duplicates per student and throws `DuplicateDocumentException` without creating a new record.<br>• Successful upload triggers async AI course generation; response includes `courseId`, title, lesson count, quiz count, and flashcard count. |
| US-05 | As a **student**, I want to **browse my uploaded documents and delete any I no longer need** so that **I can manage my learning material**. | Medium | 5 | • `GET /api/documents/` returns only the authenticated student's documents with fileName, fileType, fileSize, status (UPLOADED/PROCESSING/COMPLETED/FAILED), and uploadedAt.<br>• `DELETE /api/documents/{id}` hard-deletes the document and the entire associated course cascade (lessons, quizzes, flashcards, exams, attempts, certificates).<br>• Deleted documents are excluded from the list; only the owner can delete. |

---

## EPIC 2 — AI Course Generation & Content

*Sprint 2 · 5 stories (US-06 → US-10)*

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-06 | As a **student**, I want the **platform to automatically generate a complete course with structured lessons from my uploaded document** so that **my material is organised for progressive learning**. | High | 13 | • The FastAPI AI service (`/process-document`) returns a course structure; `CourseService.generateAndSave()` persists it linked to the document.<br>• Each lesson is stored with `lessonNumber`, `title`, `content`, `summary`, and `estimatedReadTime`; the first lesson is unlocked and all others are locked.<br>• AI service unreachable → document status set to FAILED and `AiServiceException` returned. |
| US-07 | As a **student**, I want the **platform to auto-generate flashcards and a quiz for each lesson** so that **I have ready-made study aids and can test my understanding immediately**. | High | 8 | • Flashcards (term, definition, difficulty EASY/MEDIUM/HARD) are generated and linked to each lesson at course-creation time.<br>• A `Quiz` record (passingScore, maxAttempts=3, timeLimitMinutes) is created per lesson; questions are generated **fresh by AI on each attempt** using `previousQuestions` to prevent repetition.<br>• Both flashcard and quiz counts are included in the upload response. |
| US-08 | As a **student**, I want to **trigger the generation of an audio and video recap for a lesson** so that **I can review key concepts in multiple formats**. | Medium | 5 | • `POST /api/lessons/{lessonId}/generate-recap` calls the AI service and stores the resulting file paths in `recapVideoPath`.<br>• `GET /api/lessons/recap-image` streams the PNG; `GET /api/lessons/recap-video` streams the MP4 with HTTP range-request support (ElevenLabs TTS audio + Remotion-rendered video). |
| US-09 | As a **student**, I want to **view my full course with all its content and have lesson access automatically recorded** so that **I can study my material and track my activity**. | High | 5 | • `GET /api/courses/{courseId}` returns all lessons (with locked/unlocked status), quizzes, and flashcards; only the course owner can access it.<br>• `POST /api/lessons/{lessonId}/access` updates `lastAccessedAt` idempotently; only the owner's progress is affected. |
| US-10 | As a **student**, I want to **track my lesson-level progress, have the next lesson unlock automatically, and view a global learning dashboard** so that **I can follow my learning journey from start to finish**. | High | 13 | • `GET /api/courses/{courseId}/my-progress` returns per-lesson progress (isLocked, isCompleted, quizPassed, lastAccessedAt).<br>• `LessonProgressService.processQuizAttempt()` marks the current lesson complete and unlocks the next after a quiz pass or attempt exhaustion; a COURSE_COMPLETE notification fires when all lessons are done.<br>• `GET /api/dashboard/` returns globalProgressPercentage, completedCourses, per-course metrics, and flashcard stats (50 % lessons + 50 % quizzes formula). |

---

## EPIC 3 — Assessment System

*Sprint 3 · 5 stories (US-11 → US-15)*

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-11 | As a **student**, I want to **start quiz attempts, submit answers for scored feedback, abandon an attempt, and review my attempt history** so that **I can test and track my lesson-level knowledge**. | High | 13 | • `POST /api/quizzes/{quizId}/start` enforces maxAttempts (3); generates fresh AI questions (difficulty-weighted); returns attemptId, questions, and time limit.<br>• `POST /api/quizzes/attempts/{attemptId}/submit` returns score, isPassed, correctAnswersCount, and per-question explanations; a pass triggers `LessonProgressService`.<br>• `POST …/abandon` sets finishReason=ABANDONED, score=0, and counts toward the attempt limit.<br>• `GET /api/quizzes/{quizId}/my-attempts` returns the full ordered history. |
| US-12 | As a **student**, I want to **start a daily SM-2 flashcard review session and rate each card so the algorithm adjusts future intervals** so that **I review material at the optimal time for long-term retention**. | High | 8 | • `GET /api/courses/{courseId}/flashcards/session` returns all cards with nextReviewDate ≤ today, sorted most-overdue first, plus nextUpcomingReviewDate.<br>• `POST …/{flashcardId}/review` with AGAIN/GOOD/EASY applies SM-2: AGAIN → interval=1, EF decreases; GOOD → interval×=EF; EASY → interval×=EF×1.2, EF+=0.1.<br>• Response contains nextCard and remainingDue count. |
| US-13 | As a **student**, I want to **generate a final AI exam for my course and review its details before sitting it** so that **I can formally assess my overall course knowledge**. | High | 8 | • `POST /api/exams/generate/{courseId}` calls the AI service to create 45 questions; idempotent — returns the existing exam on subsequent calls.<br>• `GET /api/exams/{courseId}` returns exam details (title, question count, time limit, remaining attempts) without starting an attempt. |
| US-14 | As a **student**, I want to **start a timed exam attempt, submit my answers, abandon if needed, and view my attempt history** so that **I can sit the final assessment under controlled conditions and track my results**. | High | 13 | • `POST /api/exams/{examId}/start` checks remaining attempts (max 3) and opens a 50-minute timed session.<br>• `POST /api/exams/attempts/{attemptId}/submit` returns score and isPassed; a pass auto-triggers certificate creation.<br>• `POST …/abandon` exits without submitting and counts toward the attempt limit.<br>• `GET /api/exams/{examId}/my-attempts` returns full attempt history with scores and statuses. |
| US-15 | As a **student and admin**, I want the **frontend to report suspicious exam activity and the admin to review flagged attempts** so that **academic integrity is monitored and enforced**. | High | 8 | • The frontend reports tab-switch, copy-paste, right-click, and unusual-timing events to `POST /api/exams/attempts/{attemptId}/suspicious-activity`; records are upserted (one per activity type per attempt).<br>• `GET /api/admin/exam-attempts/suspicious` returns a paginated list of flagged attempts with suspicious event counts.<br>• `GET /api/admin/exam-attempts/{attemptId}/suspicious` returns all events for a specific attempt. |

---

## EPIC 4 — Certificates, Notifications & Admin Dashboard

*Sprint 4 · 5 stories (US-16 → US-20)*

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-16 | As a **student and admin**, I want **certificates to be automatically created when an exam is passed, listed for the student, and manageable by an admin** so that **achievements are formally recognised and controlled**. | High | 8 | • A PENDING certificate is created automatically when an exam attempt is marked as passed.<br>• `GET /api/certificates/my` returns all the authenticated student's certificates with status (PENDING/APPROVED/REVOKED).<br>• `POST /api/admin/certificates/{id}/approve` and `…/revoke` update certificate status and trigger the corresponding in-app notification and email. |
| US-17 | As a **student or visitor**, I want to **request a certificate PDF, download it publicly by UUID, and verify its authenticity** so that **I can share and prove my credential without requiring the recipient to log in**. | High | 8 | • `POST /api/certificates/{id}/generate-pdf` triggers AI-generated PDF creation; content is stored as BYTEA in PostgreSQL.<br>• `GET /api/certificates/download/{uuid}` returns the PDF for any caller without authentication.<br>• `GET /api/certificates/verify/{uuid}` returns certificate status and student name for public authenticity checks. |
| US-18 | As a **student**, I want to **receive real-time in-app notifications and email alerts for key platform events** so that **I am informed immediately and even when offline**. | High | 13 | • `GET /api/notifications/stream` establishes an SSE connection per student for real-time in-app delivery.<br>• `GET /api/notifications/` returns full notification history; `GET …/unread-count` returns the badge count; `PUT …/{id}/read` clears the unread flag.<br>• Async HTML emails are sent via Gmail SMTP for course completion, exam pass/fail, and certificate approval/revocation. |
| US-19 | As an **admin**, I want to **view platform-wide KPIs, activity trends, course category distribution, and recent activity** so that **I have a high-level overview of platform health**. | High | 8 | • `GET /api/admin/dashboard/stats` returns total students, courses, certificates, documents, and exam pass rate.<br>• `GET /api/admin/dashboard/activity-chart?days=N` returns daily activity counts for the last N days.<br>• `GET /api/admin/dashboard/category-distribution` returns course counts per category; `GET …/recent-activity` returns the latest activity log entries. |
| US-20 | As an **admin**, I want to **manage students, review exam attempts with suspicious activity, approve certificates, and browse audit logs** so that **I can administer the platform and ensure academic integrity**. | High | 13 | • `GET /api/admin/students` returns all students with course count, certificate count, and exam pass rate; `POST …/{id}/toggle-active` enables or disables the account.<br>• `GET /api/admin/students/{id}` returns full student detail (courses, certificates, exam attempts, suspicious activity counts).<br>• `GET /api/admin/exam-attempts` returns all attempts with suspicious event counts; `GET /api/admin/certificates` lists all certificates with approval status.<br>• `GET /api/admin/activity-logs` supports pagination and filters by action type and student. |

---

## EPIC 5 — Landing Page, Chatbot & Quality

*Sprint 5 · 5 stories (US-21 → US-25)*

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-21 | As a **visitor**, I want to **send messages to a public AI chatbot** so that **I can learn about the LearnAI platform without creating an account**. | Medium | 3 | • `POST /api/chat` is declared `permitAll()` in `SecurityConfig` — no JWT is required.<br>• The request is forwarded to the FastAPI AI service (Groq Llama 3.3 70B) with a platform-context system prompt.<br>• Returns a plain-text AI response. |
| US-22 | As a **logged-in student**, I want to **chat with the AI using my current lesson as context** so that **I can ask questions about my study material and get relevant answers**. | High | 5 | • `POST /api/chat/lesson/{lessonId}` is JWT-protected and verifies that the lesson belongs to the authenticated student.<br>• Lesson content is injected into the AI system prompt as context before calling the Groq API.<br>• Out-of-context questions are handled gracefully with a relevant response. |
| US-23 | As a **visitor on the landing page**, I want to **explore the platform's sections with smooth scroll animations** so that **I understand the value of LearnAI and am prompted to sign up**. | Medium | 8 | • Angular 18 landing page includes a hero section with a registration CTA, features section, how-it-works timeline, testimonials, and footer.<br>• Scroll-triggered animations are applied to section entries using Angular animations or CSS Intersection Observer.<br>• All sections are responsive and navigable via smooth scrolling. |
| US-24 | As a **developer**, I want to **have comprehensive unit tests and a full API test collection** so that **regressions are caught and all endpoints are validated before release**. | High | 5 | • JUnit 5 + Mockito unit tests cover all service-layer business logic (auth, quiz scoring, SM-2 algorithm, certificate lifecycle).<br>• A Postman collection covers all 25 user stories end-to-end with assertions on response status and body.<br>• All tests pass cleanly on the final build. |
| US-25 | As a **developer**, I want to **finalise the deployment configuration** so that **the application is production-ready and can be started reliably from environment variables**. | Medium | 3 | • `application.yml` externalises all secrets (DB credentials, JWT secret, Groq API key, ElevenLabs key) as environment variables.<br>• A `.env.example` or README section documents all required variables.<br>• The Spring Boot application and FastAPI microservice start cleanly from the configured environment. |

---

## Story Point Summary

| Epic | Stories | Total Points |
|------|:-------:|:------------:|
| Epic 1 — Authentication & Account Management | US-01 → US-05 | 34 |
| Epic 2 — AI Course Generation & Content | US-06 → US-10 | 44 |
| Epic 3 — Assessment System | US-11 → US-15 | 50 |
| Epic 4 — Certificates, Notifications & Admin | US-16 → US-20 | 50 |
| Epic 5 — Landing Page, Chatbot & Quality | US-21 → US-25 | 24 |
| **Total** | **25 stories** | **202** |
