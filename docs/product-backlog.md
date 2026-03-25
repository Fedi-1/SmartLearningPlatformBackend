# LearnAI — Complete Product Backlog

> Generated from the SmartLearningPlatformBackend codebase.  
> All stories, entities, endpoints, and acceptance criteria are derived solely from what exists in the actual source code.
>
> **Corrections applied (v2):**
> - **US-11** — fixed hash algorithm: code uses SHA-256 (not MD5).
> - **US-13** — fixed delete behaviour: the delete endpoint hard-deletes the document **and** the associated course and all its data (not a soft-delete, not cascade-free).
> - **US-17** — verified correct: quiz questions are indeed generated fresh by AI on every attempt.
> - **Epic 10** — expanded with visitor chatbot, student chatbot with lesson context, and landing-page stories.

---

## EPIC 1 — Authentication & Account Management

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-01 | As a **visitor**, I want to **register a new student account** with my first name, last name, email, password, date of birth, and phone number so that **I can access the learning platform**. | High | 5 | • Registration returns 201 with user details on valid input.<br>• A verification email is sent immediately after registration.<br>• A duplicate email returns an appropriate error response. |
| US-02 | As a **registered user**, I want to **verify my email address by clicking the link in the verification email** so that **my account is activated and I can log in**. | High | 3 | • `GET /api/auth/verify-email?token=…` sets `isVerified=true` for the matching user.<br>• Accessing a protected resource with an unverified account is rejected with a clear error.<br>• An invalid or already-used token returns an error response. |
| US-03 | As a **registered user**, I want to **request a new verification email** so that **I can complete email verification if the original email expired or was lost**. | Medium | 2 | • `POST /api/auth/resend-verification` sends a new token only when the rate-limit window has elapsed (`lastVerificationEmailSent`).<br>• Rate-limiting prevents abuse.<br>• Returns a success message regardless of whether the email exists (security best practice). |
| US-04 | As a **verified user**, I want to **log in with my email and password and receive a JWT token** so that **I can authenticate subsequent API requests**. | High | 3 | • Successful login returns an `AuthResponse` with `token`, `role`, `firstName`, `lastName`.<br>• Suspended (`isActive=false`) accounts receive a 403 error.<br>• `lastLogin` is updated on every successful login. |
| US-05 | As a **logged-in user**, I want to **request a password-reset email** so that **I can regain access to my account if I forget my password**. | High | 3 | • `POST /api/auth/forgot-password` generates a reset token with a 24-hour expiry and emails it.<br>• No information is leaked about whether the email exists in the system.<br>• The token is invalidated after a single use. |
| US-06 | As a **user with a reset link**, I want to **validate the reset token and submit a new password** so that **my password is changed securely**. | High | 3 | • `GET /api/auth/validate-reset-token` confirms token validity before the form is rendered.<br>• `POST /api/auth/reset-password` with a valid token updates the bcrypt-hashed password.<br>• Expired or reused tokens return an error. |
| US-07 | As a **logged-in student**, I want to **view and update my profile** (first name, last name, date of birth, phone number) so that **my personal information stays current**. | Medium | 2 | • `GET /api/students/profile` returns current profile data.<br>• `PUT /api/students/profile` with valid fields persists changes.<br>• Fields omitted from the request are not overwritten. |
| US-08 | As a **logged-in student**, I want to **change my password** so that **I can maintain account security**. | Medium | 2 | • `PUT /api/students/password` requires `currentPassword` to match the stored bcrypt hash.<br>• New password is hashed and persisted.<br>• Incorrect current password returns an error without changing anything. |
| US-09 | As an **admin**, I want to **view and update my own admin profile and change my password** so that **my account details remain accurate and secure**. | Low | 2 | • `GET /PUT /api/admin/profile` endpoints work identically to the student equivalents but are restricted to ADMIN role.<br>• `PUT /api/admin/password` requires current-password verification.<br>• Non-admin access returns 403. |

---

## EPIC 2 — Document Management

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-10 | As a **student**, I want to **upload a document (PDF, DOCX, PPTX, JPG, PNG, WEBP)** so that **the platform can generate a learning course from my material**. | High | 5 | • `POST /api/documents/upload` accepts multipart files up to 10 MB; larger files throw `FileTooLargeException`.<br>• Unsupported types throw `UnsupportedFileTypeException`.<br>• On success, the response contains `courseId`, title, lesson count, quiz count, and flashcard count. |
| US-11 | As a **student**, I want the **platform to detect if I upload a duplicate document** so that **I am warned and redundant AI processing is avoided**. | Medium | 3 | • The **SHA-256** hash of the file bytes is computed and compared against existing records for the same student (`HashingUtil.computeSHA256`).<br>• A `DuplicateDocumentException` is thrown on a hash match; no new Document or Course record is created.<br>• If a matching hash exists from a previously soft-deleted document, the deleted record is hard-deleted before re-saving. |
| US-12 | As a **student**, I want to **view the list of all my uploaded documents** so that **I can track what materials I have submitted**. | Medium | 2 | • `GET /api/documents/` returns only documents owned by the authenticated student.<br>• Each entry includes `fileName`, `fileType`, `category`, `fileSize`, `uploadedAt`, and `status` (UPLOADED / PROCESSING / COMPLETED / FAILED).<br>• Deleted documents are excluded from the list. |
| US-13 | As a **student**, I want to **delete a document** so that **I can remove material I no longer need**. | Low | 3 | • `DELETE /api/documents/{id}` performs a **hard delete** — it permanently removes the document record and the entire associated course with all its data (lessons, quizzes, flashcards, exams, attempts, certificates).<br>• Only the document owner can trigger the deletion; other users receive 403.<br>• After deletion the document no longer appears in the student's document list. |

---

## EPIC 3 — AI Course Generation

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-14 | As a **student**, I want the **platform to automatically generate a complete course** (title, category, description) **from my uploaded document** so that **I can start learning immediately**. | High | 8 | • The AI service (`/process-document`) returns a course structure; `CourseService.generateAndSave()` persists it.<br>• A `Course` record with `title`, `category`, `description`, and `createdAt` is stored and linked to the document.<br>• If the AI service is unreachable, an `AiServiceException` is returned and the document status is set to FAILED. |
| US-15 | As a **student**, I want the **platform to generate multiple lessons** with content, summary, and estimated reading time **from my document** so that **the material is structured for progressive learning**. | High | 8 | • Each lesson is stored with `lessonNumber`, `title`, `content`, `summary`, and `estimatedReadTime`.<br>• The first lesson is unlocked on course creation; all subsequent lessons are locked.<br>• Lessons are returned in order when the course is fetched. |
| US-16 | As a **student**, I want the **platform to auto-generate flashcards with a difficulty level for each lesson** so that **I have ready-made study aids**. | High | 5 | • Flashcards (term, definition, difficulty: EASY / MEDIUM / HARD) are created for each lesson during course generation.<br>• Each flashcard is linked to its parent lesson.<br>• Flashcard count is returned in the upload response. |
| US-17 | As a **student**, I want the **platform to auto-generate a quiz for each lesson** so that **I can test my understanding before moving on**. | High | 5 | • A `Quiz` record is created per lesson with `passingScore`, `maxAttempts` (3), and `timeLimitMinutes`.<br>• Quiz questions are generated **fresh by AI on each attempt** (avoiding duplicates via `previousQuestions` list passed to the AI service); they are not stored at course-creation time.<br>• Quiz count is included in the upload response. |
| US-18 | As a **student**, I want to **trigger the generation of a recap image or video for a lesson** so that **I can quickly review key concepts visually**. | Medium | 5 | • `POST /api/lessons/{lessonId}/generate-recap` calls the AI service and stores the resulting file path in `recapVideoPath`.<br>• `GET /api/lessons/recap-image` streams the PNG image.<br>• `GET /api/lessons/recap-video` streams the MP4 file with HTTP range-request support. |

---

## EPIC 4 — Course Viewer & Lesson Progress

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-19 | As a **student**, I want to **view the full details of a course** including all lessons, quizzes, and flashcards so that **I know what the course contains**. | High | 3 | • `GET /api/courses/{courseId}` returns a `CourseDetailResponse` with all lessons, each containing its quiz and flashcards.<br>• Only the course owner can access it; others receive 403.<br>• Locked / unlocked status is included per lesson. |
| US-20 | As a **student**, I want to **see my progress across all lessons in a course** so that **I know how far I have advanced**. | High | 3 | • `GET /api/courses/{courseId}/my-progress` returns a `LessonProgressItem` for every lesson.<br>• Each item shows `isLocked`, `isCompleted`, `quizPassed`, and `lastAccessedAt`.<br>• Progress is scoped to the authenticated student. |
| US-21 | As a **student**, I want the **platform to record when I access a lesson** so that **my study activity is tracked**. | Medium | 2 | • `POST /api/lessons/{lessonId}/access` updates `lastAccessedAt` to the current timestamp.<br>• The call is idempotent — repeated calls just update the timestamp.<br>• Only the lesson owner receives the update. |
| US-22 | As a **student**, I want to **view my detailed progress for a specific lesson** so that **I can see completion status, time spent, and whether I can access the next lesson**. | Medium | 2 | • `GET /api/lessons/{lessonId}/progress` returns `isCompleted`, `quizPassed`, `isLocked`, `timeSpent`, `lastAccessedAt`, and `nextLessonId`.<br>• `nextLessonId` is null when the current lesson is the last in the course.<br>• Response is specific to the authenticated student. |
| US-23 | As a **student**, I want the **next lesson to unlock automatically after I pass a quiz or exhaust all attempts** so that **I can progress through the course sequentially**. | High | 5 | • `LessonProgressService.processQuizAttempt()` marks the current lesson `isCompleted=true` on pass, then unlocks the next lesson.<br>• If the student exhausts all attempts without passing, the next lesson is also unlocked.<br>• A COURSE_COMPLETE notification is triggered when all lessons are completed. |
| US-24 | As a **student**, I want to **view my overall dashboard** showing global progress, completed courses, and per-course statistics so that **I have a single view of my learning journey**. | High | 5 | • `GET /api/dashboard/` returns `globalProgressPercentage`, `completedCourses`, per-course metrics (lessons %, quizzes passed, exam status), and flashcard stats.<br>• Progress formula: 50 % lessons complete + 50 % quizzes passed per course.<br>• Dashboard reflects the most recent lesson and quiz completions. |

---

## EPIC 5 — Quiz & Flashcard System

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-25 | As a **student**, I want to **start a new quiz attempt for a lesson** so that **I can test my knowledge with fresh AI-generated questions**. | High | 5 | • `POST /api/quizzes/{quizId}/start` checks that the student has not reached `maxAttempts` (3); returns 403 if exceeded.<br>• Five new questions are generated by AI on each attempt (previous question texts are sent to avoid duplicates).<br>• Response contains the attempt ID, questions, and time limit. |
| US-26 | As a **student**, I want to **submit my quiz answers and receive an immediate score** with pass/fail status and per-question explanations so that **I understand where I went wrong**. | High | 5 | • `POST /api/quizzes/attempts/{attemptId}/submit` scores each answer using difficulty-weighted points.<br>• Response includes `score`, `isPassed`, `correctAnswersCount`, and per-question explanations.<br>• A passing result triggers a lesson-progress update via `LessonProgressService`. |
| US-27 | As a **student**, I want to **abandon a quiz attempt** so that **I can exit without submitting if I need to stop unexpectedly**. | Medium | 2 | • `POST /api/quizzes/attempts/{attemptId}/abandon` sets `finishReason=ABANDONED`, `score=0`, `isPassed=false`.<br>• The abandoned attempt counts toward the attempt limit.<br>• Subsequent start calls reflect the reduced remaining attempts. |
| US-28 | As a **student**, I want to **view all my past quiz attempts for a lesson** so that **I can track my quiz history and score trends**. | Medium | 2 | • `GET /api/quizzes/{quizId}/my-attempts` returns all attempts ordered by date for the authenticated student.<br>• Each attempt shows `attemptNumber`, `score`, `isPassed`, `startedAt`, `submittedAt`, and `finishReason`.<br>• Only the student's own attempts are returned. |
| US-29 | As a **student**, I want to **start a daily flashcard review session** based on SM-2 scheduling so that **I review cards at the optimal time for retention**. | High | 5 | • `GET /api/courses/{courseId}/flashcards/session` returns all flashcards where `nextReviewDate ≤ today`, sorted most-overdue first.<br>• Response includes `due` count and `nextUpcomingReviewDate` for cards not yet due.<br>• Only flashcards belonging to the specified course are returned. |
| US-30 | As a **student**, I want to **rate a flashcard as AGAIN, GOOD, or EASY** so that **the SM-2 algorithm adjusts its next review interval accordingly**. | High | 5 | • `POST /api/courses/{courseId}/flashcards/{flashcardId}/review` accepts a `FlashcardRateRequest` with rating AGAIN / GOOD / EASY.<br>• SM-2 logic applied: AGAIN → interval=1, EF decreases; GOOD → interval *= EF; EASY → interval *= EF × 1.2, EF increases by 0.1.<br>• Response returns the `nextCard` to review and `remainingDue` count. |

---

## EPIC 6 — Final Exam & Anti-Cheat

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-31 | As a **student**, I want to **generate a final exam for my course** (once, AI-powered, 45 questions) so that **I can formally assess my overall knowledge**. | High | 8 | • `POST /api/courses/{courseId}/generate-exam` calls the AI service with all lesson contents and returns 45 mixed-type questions (MCQ, TRUE_FALSE, FILL_BLANK).<br>• If an exam already exists for the course, the cached version is returned — no duplicate generation.<br>• Only the course owner can trigger generation; others receive 403. |
| US-32 | As a **student**, I want to **view the existing final exam for my course** so that **I can see its details before starting**. | Medium | 2 | • `GET /api/courses/{courseId}/exam` returns `ExamResponse` with title, `passingScore`, `maxAttempts`, `totalPoints` (45), `timeLimitMinutes` (50), and all questions.<br>• Returns 404 if no exam has been generated yet.<br>• Only the course owner can access it. |
| US-33 | As a **student**, I want to **start a final exam attempt** so that **I can sit the exam under timed conditions**. | High | 5 | • `POST /api/courses/exams/{examId}/start` creates an `ExamAttempt` and returns questions plus attempt metadata.<br>• Max 3 attempts enforced; 403 returned if exceeded.<br>• `startedAt` is recorded for time-limit enforcement. |
| US-34 | As a **student**, I want to **submit my exam answers and receive my score and pass/fail result** so that **I know whether I have earned a certificate**. | High | 5 | • `POST /api/courses/exam-attempts/{attemptId}/submit` scores the exam, persists answers, and returns `score`, `isPassed`, and `finishReason`.<br>• On pass, a `Certificate` record (status=PENDING) is created and its `certificateId` is returned.<br>• An EXAM_RESULT notification is sent via in-app SSE and email. |
| US-35 | As a **student**, I want to **abandon an exam attempt** so that **I can exit the exam without submitting**. | Medium | 2 | • `POST /api/courses/exam-attempts/{attemptId}/abandon` sets `finishReason=ABANDONED`, `score=0`, `isPassed=false`.<br>• The abandoned attempt still counts toward the 3-attempt maximum.<br>• No certificate is created for an abandoned attempt. |
| US-36 | As a **student**, I want to **view all my past exam attempts for a course** so that **I can track my exam history**. | Medium | 2 | • `GET /api/courses/exams/{examId}/my-attempts` returns all attempts for the authenticated student.<br>• Each attempt shows `score`, `isPassed`, `attemptNumber`, and `finishReason`.<br>• Results are ordered by date, most recent first. |
| US-37 | As a **student taking an exam**, I want the **frontend to report suspicious activities** (tab switching, copy-paste, right-click, unusual timing) so that **academic integrity is maintained**. | High | 5 | • `POST /api/exam-attempts/{attemptId}/suspicious-activity` accepts a `LogSuspiciousActivityRequest` with `activityType`.<br>• Records are upserted: if the same type is reported again, `count` is incremented rather than a new row inserted.<br>• The student can view their own activity log via `GET` on the same path. |
| US-38 | As an **admin**, I want to **view the suspicious activity log for any exam attempt and see a list of all flagged attempts** so that **I can investigate potential academic dishonesty**. | High | 3 | • `GET /api/admin/exam-attempts/{attemptId}/suspicious-activity` returns the full activity log for any attempt.<br>• `GET /api/admin/flagged-attempts` returns all attempt IDs that have at least one suspicious activity record.<br>• Both endpoints are restricted to ADMIN role. |

---

## EPIC 7 — Certificate System

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-39 | As a **student**, I want a **certificate to be automatically created when I pass the final exam** so that **my achievement is immediately recorded**. | High | 5 | • On exam submission with `isPassed=true`, `CertificateService` creates a `Certificate` record with status PENDING and a unique UUID.<br>• `certificateId` is returned in the exam submission response.<br>• A CERTIFICATE notification is sent to the student. |
| US-40 | As a **student**, I want to **view all my certificates** so that **I can see which courses I have completed**. | Medium | 2 | • `GET /api/certificates/my-certificates` returns all non-REVOKED certificates for the authenticated student.<br>• Each entry shows `courseTitle`, `score`, `issuedAt`, `status`, and `uuid`.<br>• REVOKED certificates are excluded. |
| US-41 | As a **student**, I want to **request PDF generation for my certificate** so that **I have an official document to download**. | High | 5 | • `POST /api/certificates/{certificateId}/generate` calls the AI service with student name, course title, and score; receives a base64-encoded PDF back.<br>• PDF is decoded and stored as BYTEA in the database.<br>• The certificate must be in APPROVED status before it can be downloaded. |
| US-42 | As a **student or external visitor**, I want to **download a certificate PDF by its public UUID** so that **I can share proof of completion without logging in**. | High | 3 | • `GET /api/certificates/{uuid}/download` is a public endpoint requiring no authentication.<br>• Returns the PDF binary with `Content-Type: application/pdf`.<br>• Only APPROVED certificates can be downloaded; PENDING or REVOKED returns an error. |
| US-43 | As an **admin**, I want to **approve or revoke student certificates** so that **I can control the validity of issued credentials**. | High | 3 | • `PATCH /api/admin/certificates/{id}/approve` sets status to APPROVED and sends a CERTIFICATE notification to the student.<br>• `PATCH /api/admin/certificates/{id}/revoke` sets status to REVOKED and notifies the student.<br>• Both actions are restricted to ADMIN role. |
| US-44 | As an **external verifier**, I want to **check the authenticity of a certificate by UUID** so that **I can confirm a credential is valid without logging in**. | High | 2 | • `GET /api/admin/certificates/verify/{uuid}` is a public endpoint returning `isValid`, `studentName`, `courseTitle`, `score`, and `issuedAt`.<br>• A REVOKED or non-existent UUID returns `isValid=false`.<br>• No private student data beyond name and course is exposed. |

---

## EPIC 8 — Notifications

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-45 | As a **student**, I want to **subscribe to real-time in-app notifications via Server-Sent Events** so that **I receive instant updates without polling**. | High | 5 | • `GET /api/notifications/subscribe` opens an SSE stream tied to the authenticated user.<br>• Events are pushed immediately when triggered (exam result, certificate status change, course complete).<br>• The SSE emitter is cleaned up server-side on client disconnect. |
| US-46 | As a **student**, I want to **view all my notifications** so that **I can review past alerts I may have missed**. | Medium | 2 | • `GET /api/notifications/` returns all notifications for the authenticated user ordered by `sentAt` descending.<br>• Each item includes `type`, `category`, `title`, `message`, `isRead`, `sentAt`, and `actionUrl`.<br>• Both EMAIL and IN_APP type records are returned. |
| US-47 | As a **student**, I want to **see the count of unread notifications** so that **I know whether there are alerts I haven't reviewed**. | Medium | 1 | • `GET /api/notifications/unread-count` returns the integer count of unread notifications.<br>• Count decreases when notifications are marked as read.<br>• Returns 0 when all notifications are read. |
| US-48 | As a **student**, I want to **mark a notification as read** so that **the unread indicator is cleared after I review it**. | Medium | 1 | • `PUT /api/notifications/{notificationId}/read` sets `isRead=true` and records `readAt` timestamp.<br>• Only the notification owner can mark it read; others receive 403.<br>• Marking an already-read notification is idempotent. |
| US-49 | As a **student**, I want to **receive an email notification when I complete a course, pass or fail an exam, or when my certificate is approved or revoked** so that **I am informed of important events even when offline**. | High | 5 | • `NotificationService` routes notifications by category: COURSE_COMPLETE, EXAM_RESULT, and CERTIFICATE categories trigger both IN_APP (SSE) and EMAIL channels.<br>• Emails are sent asynchronously via `@Async` through Gmail SMTP and formatted as HTML.<br>• An IN_APP `Notification` record is always persisted in the database. |

---

## EPIC 9 — Admin Dashboard

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-50 | As an **admin**, I want to **view platform-wide statistics** (total students, courses, certificates, documents, exam pass rate) so that **I have a high-level overview of platform health**. | High | 3 | • `GET /api/admin/stats` returns an `AdminStatsResponse` with all five metrics.<br>• `examPassRate` is the percentage of passed attempts over total submitted attempts.<br>• Restricted to ADMIN role. |
| US-51 | As an **admin**, I want to **view an activity chart for the last N days** so that **I can identify usage trends**. | Medium | 3 | • `GET /api/admin/activity-chart` accepts an optional `days` parameter and returns a list of `ActivityChartPoint` (date + count).<br>• Data is sourced from `ActivityLog`, grouped by date.<br>• Missing dates return count=0 to keep the chart continuous. |
| US-52 | As an **admin**, I want to **see the distribution of courses by category** so that **I understand what subjects students are learning**. | Medium | 2 | • `GET /api/admin/category-distribution` returns each distinct course category and its count.<br>• All categories present in the `Course` table are included.<br>• Restricted to ADMIN role. |
| US-53 | As an **admin**, I want to **view recent platform activity entries** so that **I can quickly see what actions users have performed**. | Medium | 2 | • `GET /api/admin/recent-activity` returns the most recent `ActivityLog` entries as `RecentActivityEntry` objects.<br>• Each entry includes `studentName`, `action`, and `timestamp`.<br>• Restricted to ADMIN role. |
| US-54 | As an **admin**, I want to **view a list of all students with summary metrics** (course count, certificate count, pass rate) so that **I can monitor student engagement**. | High | 3 | • `GET /api/admin/students` returns a list of `StudentSummaryResponse` objects.<br>• Summary includes `courseCount`, `certificateCount`, and `passRate` per student.<br>• Restricted to ADMIN role. |
| US-55 | As an **admin**, I want to **view detailed information about a specific student** (courses, certificates, exam attempts, suspicious activity counts) so that **I can investigate performance or integrity issues**. | High | 5 | • `GET /api/admin/students/{studentId}/detail` returns a `StudentDetailResponse` with nested lists of courses, certificates, and exam attempts.<br>• Each exam attempt includes `suspiciousActivityCount`.<br>• Restricted to ADMIN role. |
| US-56 | As an **admin**, I want to **enable or disable a student account** so that **I can suspend users who violate platform policies**. | High | 3 | • `PATCH /api/admin/students/{studentId}/toggle-status` flips `isActive` and returns the updated status.<br>• A disabled student receives 403 on the next login attempt.<br>• Restricted to ADMIN role. |
| US-57 | As an **admin**, I want to **view a list of all exam attempts with suspicious activity counts** so that **I can identify potentially dishonest submissions at a glance**. | High | 3 | • `GET /api/admin/exam-attempts` returns a list of `AdminExamAttemptItem` objects including `suspiciousActivityCount`.<br>• Results include all students' attempts.<br>• Restricted to ADMIN role. |
| US-58 | As an **admin**, I want to **view all certificates and their approval status** so that **I can manage certificate issuance**. | High | 3 | • `GET /api/admin/certificates` returns a list of `AdminCertificateItem` with `status` (PENDING / APPROVED / REVOKED), `studentName`, `courseTitle`, and `score`.<br>• PENDING certificates are surfaced first to prioritise the review queue.<br>• Restricted to ADMIN role. |
| US-59 | As an **admin**, I want to **browse paginated activity logs with filters by action type and student** so that **I can audit specific user actions**. | Medium | 3 | • `GET /api/admin/activity-logs` supports pagination (`page`, `size`) and optional `action` + `studentId` query parameters.<br>• Returns `ActivityLogPageResponse` with `totalElements`, `totalPages`, and `currentPage`.<br>• Restricted to ADMIN role. |
| US-60 | As an **admin**, I want to **view a specific student's exam attempts** so that **I can review their performance history in detail**. | Medium | 2 | • `GET /api/admin/students/{studentId}/exam-attempts` returns all exam attempts for the specified student.<br>• Each attempt shows `score`, `isPassed`, `finishReason`, and `suspiciousActivityCount`.<br>• Restricted to ADMIN role; wrong role returns 403. |

---

## EPIC 10 — Landing Page & Chatbot

| ID | User Story | Priority | Story Points | Acceptance Criteria |
|----|------------|:--------:|:------------:|---------------------|
| US-61 | As a **visitor**, I want to **send a message to a public chatbot endpoint** so that **I can get information about the LearnAI platform without creating an account**. | Medium | 3 | • `POST /api/chat` (or `GET /api/chat`) is declared as a `permitAll()` public route in `SecurityConfig` — no JWT required.<br>• The endpoint accepts a request and returns a response without authentication.<br>• CORS allows cross-origin requests from the configured frontend URL. |
| US-62 | As a **visitor on the landing page**, I want to **see a hero section with a call-to-action** so that **I understand the platform's value and am prompted to sign up**. | Medium | 3 | • The landing page renders a prominent hero section visible above the fold.<br>• A clear call-to-action button links to the registration flow (`/api/auth/register`).<br>• The section is visually distinct and loads without authentication. |
| US-63 | As a **visitor on the landing page**, I want to **experience smooth scroll-based animations as I navigate through page sections** so that **the page feels engaging and modern**. | Low | 2 | • Scroll-triggered animations activate as each section enters the viewport.<br>• All animated sections are reachable via standard keyboard and mouse navigation.<br>• Animations degrade gracefully on devices that prefer reduced motion. |
| US-64 | As a **visitor on the landing page**, I want to **browse multiple content sections** (features, how-it-works, testimonials, footer) so that **I can learn about the platform before registering**. | Medium | 3 | • The landing page contains at least the following sections: hero, features, how-it-works, and footer.<br>• Each section is accessible via direct scroll or anchor navigation.<br>• All sections render correctly without a logged-in session. |
| US-65 | As a **logged-in student**, I want to **send a message to the chatbot with the context of a specific lesson** so that **I can ask questions about my current study material and get relevant answers**. | High | 5 | • The chatbot endpoint (`/api/chat`) accepts a request payload that includes lesson context (lesson ID or content).<br>• The response is relevant to the provided lesson content.<br>• The endpoint is protected by JWT — only authenticated students can pass lesson context. |

---

## Summary

| Epic | Stories | Total Story Points |
|------|---------:|-------------------:|
| EPIC 1 — Authentication & Account Management | 9 | 25 |
| EPIC 2 — Document Management | 4 | 13 |
| EPIC 3 — AI Course Generation | 5 | 31 |
| EPIC 4 — Course Viewer & Lesson Progress | 6 | 20 |
| EPIC 5 — Quiz & Flashcard System | 6 | 20 |
| EPIC 6 — Final Exam & Anti-Cheat | 8 | 32 |
| EPIC 7 — Certificate System | 6 | 20 |
| EPIC 8 — Notifications | 5 | 14 |
| EPIC 9 — Admin Dashboard | 11 | 29 |
| EPIC 10 — Landing Page & Chatbot | 5 | 16 |
| **Total** | **65** | **220** |
