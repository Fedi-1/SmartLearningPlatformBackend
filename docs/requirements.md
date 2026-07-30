# LearnAI — Platform Requirements

> Generated from source code analysis of controllers, services, models, and enums.

---

## 1. BESOINS FONCTIONNELS

### Authentication

1. The system shall allow a visitor to register a new student account by providing first name, last name, email address, password, date of birth, and phone number. The password shall be hashed with BCrypt before storage.
2. The system shall reject registration attempts that use an email address already associated with an existing account, returning HTTP 409 Conflict.
3. Upon successful registration, the system shall generate a UUID verification token, persist it on the user record, and send an email containing a clickable verification link to the provided address.
4. The system shall allow a visitor to verify their email address by submitting the verification token via `GET /api/auth/verify-email?token=<token>`, after which the account `isVerified` flag is set to `true`.
5. The system shall allow a visitor to request a new verification email via `POST /api/auth/resend-verification`, subject to rate-limiting based on the `lastVerificationEmailSent` timestamp.
6. The system shall allow a verified and active student to log in by submitting their email and password. On success, a signed JWT access token is returned in the response body.
7. The system shall block login for accounts whose `isVerified` flag is `false`, returning HTTP 403.
8. The system shall block login for accounts whose `isActive` flag is `false` (suspended), returning HTTP 423.
9. The system shall record the `lastLogin` timestamp on every successful authentication.
10. The system shall allow a user to initiate a password reset by submitting their email via `POST /api/auth/forgot-password`. A time-limited reset token and its expiry timestamp are persisted and an email containing the reset link is dispatched.
11. The system shall allow a user to validate a password-reset token without consuming it via `GET /api/auth/validate-reset-token?token=<token>`.
12. The system shall allow a user to set a new password by submitting a valid reset token and the desired new password via `POST /api/auth/reset-password`. The new password is BCrypt-hashed before storage.

### Document Management

13. The system shall allow an authenticated student to upload a document via `POST /api/documents/upload` using a `multipart/form-data` request.
14. The system shall reject uploads for file types other than PDF, DOCX, PPTX, JPG, JPEG, PNG, and WEBP, returning an unsupported-file-type error.
15. The system shall reject uploads whose file size exceeds 10 MB.
16. The system shall compute a SHA-256 hash of every uploaded file's binary content and reject re-uploads of the same file by the same student (duplicate detection), returning HTTP 409 Conflict.
17. The system shall persist uploaded file binary content as a `bytea` column in PostgreSQL, along with the file name, file type, file size, SHA-256 hash, and an initial `UPLOADED` status.
18. The system shall allow an authenticated student to list all their uploaded documents via `GET /api/documents`.
19. The system shall allow an authenticated student to soft-delete a document and all its associated course data (lessons, quizzes, exams, certificates) via `DELETE /api/documents/{id}`.

### AI Course Generation

20. Upon successful document upload, the system shall immediately invoke the external FastAPI AI service (`POST {ai.service.base-url}/process-document`) by forwarding the file bytes and file type, and shall set the document status to `PROCESSING` during the call.
21. The AI service call shall be performed outside any database transaction to avoid connection-timeout issues during long-running LLM inference.
22. The system shall parse the AI service JSON response and persist a `Course` entity (title, category, description) together with its child `Lesson` entities (lessonNumber, title, content, summary, estimatedReadTime), per-lesson `Quiz` and `QuizQuestion` entities, and per-lesson `Flashcard` entities in a single programmatic transaction.
23. On successful course creation, the system shall set the document status to `COMPLETED` and log an `UPLOAD_DOCUMENT` and `GENERATE_COURSE` activity for the student.
24. On AI service failure, the system shall set the document status to `FAILED` and surface the error to the client.
25. The system shall reuse an existing course if the same document hash has already produced a completed course, copying lessons, quizzes, and flashcards to avoid redundant AI calls.

### Course Viewer & Lesson Progress

26. The system shall allow an authenticated student to retrieve the full details of a course (title, description, category, list of lessons with content and quiz metadata) via `GET /api/courses/{courseId}`.
27. The system shall allow an authenticated student to retrieve the aggregated lesson-progress state (locked, completed, quizPassed) for every lesson in a course via `GET /api/courses/{courseId}/my-progress`.
28. The system shall enforce lesson locking: the first lesson of a course is unlocked when the course is created; subsequent lessons are locked by default and are unlocked only after the preceding lesson's quiz is passed or all quiz attempts are exhausted.
29. The system shall track lesson access by recording `lastAccessedAt` when a student opens a lesson via `POST /api/lessons/{lessonId}/access`.
30. The system shall allow a student to retrieve the progress record for an individual lesson via `GET /api/lessons/{lessonId}/progress`.
31. The system shall emit a `COURSE_COMPLETE` in-app notification when a student completes the final lesson of a course.

### Quiz System

32. The system shall allow an authenticated student to start a new quiz attempt via `POST /api/quizzes/{quizId}/start`, provided the student has not yet reached the `maxAttempts` limit for that quiz. On start, an `attemptNumber` is assigned and a `startedAt` timestamp is recorded.
33. The system shall allow a student to submit all answers for an active quiz attempt via `POST /api/quiz-attempts/{attemptId}/submit`. The service scores each answer, records `isCorrect` and `pointsAwarded` on each `QuizAnswer`, calculates the total score, sets `isPassed`, and records `submittedAt` with `FinishReason.SUBMITTED`.
34. The system shall allow a student to abandon an active quiz attempt via `POST /api/quiz-attempts/{attemptId}/abandon`, recording `FinishReason.ABANDONED`, score 0, and `isPassed = false`.
35. The system shall allow a student to retrieve all their previous attempts for a quiz via `GET /api/quizzes/{quizId}/my-attempts`.
36. The system shall trigger lesson-progress unlock processing after every quiz submission via the `LessonProgressService`.

### SM-2 Flashcard System

37. The system shall allow an authenticated student to retrieve their daily flashcard review session for a course via `GET /api/courses/{courseId}/flashcards/session`, returning all flashcards due today (ordered by next review date), the remaining due count, and the date of the next upcoming review.
38. The system shall allow a student to rate a flashcard as `AGAIN` (quality 0), `GOOD` (quality 3), or `EASY` (quality 5) via `POST /api/flashcards/{flashcardId}/review`.
39. The system shall compute the next review schedule for a flashcard using the SM-2 spaced-repetition algorithm: updating `easeFactor`, `interval` (days), `repetitionCount`, `consecutiveCorrectReviews`, `nextReviewDate`, `lastReviewedAt`, `lastRating`, and `qualityScore` on the `FlashcardReview` record.
40. The system shall derive and persist a `DifficultyLevel` (EASY, MEDIUM, HARD) for each flashcard based on the current ease factor (≥ 2.5 → EASY, ≥ 1.8 → MEDIUM, < 1.8 → HARD).
41. The system shall allow retrieval of all due flashcards for a course via `GET /api/courses/{courseId}/flashcards/due` for backward compatibility.

### Final Exam System

42. The system shall allow an authenticated student to trigger AI-based exam generation for a course via `POST /api/courses/{courseId}/generate-exam`. The service sends all lesson contents to the FastAPI AI endpoint and persists an `Exam` entity (title, passingScore, maxAttempts, totalPoints, timeLimitMinutes) with its `ExamQuestion` children (MCQ, TRUE_FALSE, FILL_BLANK types; difficulty levels; pointsWorth; sectionNumber).
43. The system shall return the existing exam if one has already been generated for the course, without invoking the AI service again.
44. The system shall allow a student to retrieve the exam for a course via `GET /api/courses/{courseId}/exam`.
45. The system shall allow a student to start an exam attempt via `POST /api/exams/{examId}/start`, enforcing the `maxAttempts` limit, assigning an `attemptNumber`, and recording `startedAt`.
46. The system shall allow a student to submit a completed exam attempt via `POST /api/exam-attempts/{attemptId}/submit`, scoring all answers, setting `score`, `isPassed`, `submittedAt`, and `FinishReason.SUBMITTED`.
47. On exam submission, if the student passes, the system shall automatically create a `Certificate` entity with status `PENDING`, a unique UUID, the exam score, and `issuedAt` timestamp.
48. On exam submission, the system shall dispatch an `EXAM_RESULT` notification (both in-app via SSE and email) informing the student of their score and pass/fail outcome.
49. On certificate creation, the system shall dispatch a `CERTIFICATE` notification (both in-app via SSE and email) informing the student that their certificate is pending admin approval.
50. The system shall allow a student to abandon an in-progress exam attempt via `POST /api/exam-attempts/{attemptId}/abandon`, recording `FinishReason.ABANDONED`.
51. The system shall allow a student to view all their previous exam attempts via `GET /api/exams/{examId}/my-attempts`.

### Anti-Cheat Monitoring

52. The system shall allow an authenticated student to report a suspicious browser event during an active exam attempt via `POST /api/exam-attempts/{attemptId}/suspicious-activity`. Supported event types are `TAB_SWITCH`, `COPY_PASTE`, `RIGHT_CLICK`, and `UNUSUAL_TIMING`.
53. Each reported event is persisted as a `SuspiciousActivity` record linked to the exam attempt, with a `count` and `detectedAt` timestamp.
54. A student shall be able to view the suspicious-activity log for their own attempt via `GET /api/exam-attempts/{attemptId}/suspicious-activity`.
55. An administrator shall be able to view the suspicious-activity log for any attempt via `GET /api/admin/exam-attempts/{attemptId}/suspicious-activity`.
56. An administrator shall be able to retrieve the list of all exam attempt IDs that contain at least one suspicious-activity record via `GET /api/admin/flagged-attempts`.

### Certificate System

57. The system shall allow an authenticated student to list all their non-revoked certificates via `GET /api/certificates/my-certificates`.
58. The system shall allow a student to retrieve the certificate for a specific course via `GET /api/certificates/course/{courseId}`.
59. The system shall allow a student to request PDF generation for a certificate via `POST /api/certificates/{certificateId}/generate`. The service calls the FastAPI PDF-generation endpoint, receives the PDF binary, and persists it as a `bytea` column.
60. The system shall provide a public (unauthenticated) endpoint `GET /api/certificates/{uuid}/download` that returns the PDF binary for an `APPROVED` certificate, an HTML status page for `PENDING` certificates, and an HTML revocation page for `REVOKED` certificates.
61. The system shall provide a public endpoint `GET /api/admin/certificates/verify/{uuid}` that returns certificate metadata (student name, course title, score, issued date, status) without requiring authentication.
62. An administrator shall be able to approve a pending certificate via `PATCH /api/admin/certificates/{id}/approve`, changing its status to `APPROVED`.
63. An administrator shall be able to revoke a certificate via `PATCH /api/admin/certificates/{id}/revoke`, changing its status to `REVOKED`.

### Lesson Recap Videos

64. The system shall allow a student to trigger recap generation for a lesson via `POST /api/lessons/{lessonId}/generate-recap`. The service calls the FastAPI AI service to produce a recap image (PNG) and video (MP4) and caches the result path on the `Lesson.recapVideoPath` field.
65. The system shall serve the recap image (PNG) for a lesson via `GET /api/lessons/recap-image?path=<path>`, validating that the resolved file path contains the `/recap-cards/` segment to prevent path traversal.
66. The system shall serve the recap video (MP4) for a lesson via `GET /api/lessons/recap-video?path=<path>`, supporting HTTP range requests for browser-native video streaming, validating that the resolved path contains `/recap-videos/`.

### Notification System

67. The system shall allow an authenticated user to open a long-lived Server-Sent Events (SSE) stream via `GET /api/notifications/subscribe` to receive real-time push notifications.
68. The system shall deliver in-app (SSE) notifications for the following categories: `COURSE_COMPLETE`, `EXAM_RESULT`, `CERTIFICATE`, and `SUSPICIOUS_ACTIVITY`.
69. The system shall deliver email notifications for the following categories: `EXAM_RESULT`, `CERTIFICATE`, and `REMINDER`.
70. The system shall allow a user to retrieve all their notifications via `GET /api/notifications`.
71. The system shall allow a user to retrieve the count of unread notifications via `GET /api/notifications/unread-count`.
72. The system shall allow a user to mark a notification as read via `PUT /api/notifications/{notificationId}/read`, recording the `readAt` timestamp.
73. The system shall send automated inactivity reminder emails (category `REMINDER`) once per day at 09:00 to students who have not accessed an in-progress course for 3 or more days. The same course reminder shall not be re-sent within a 7-day deduplication window.

### Admin Dashboard

74. The system shall provide a protected admin endpoint `GET /api/admin/stats` that returns aggregate platform statistics: total students, total courses, total certificates, total documents, and overall exam pass rate (percentage).
75. The system shall provide an endpoint `GET /api/admin/activity-chart?days=<n>` returning daily activity counts for the past n days for chart visualization.
76. The system shall provide an endpoint `GET /api/admin/category-distribution` returning the distribution of courses by category.
77. The system shall provide an endpoint `GET /api/admin/recent-activity?limit=<n>` returning the most recent activity log entries.
78. The system shall provide an endpoint `GET /api/admin/students` returning a summary list of all students (name, email, registration date, active status, course count, certificate count).
79. The system shall provide an endpoint `GET /api/admin/students/{studentId}/detail` returning full details for a student: profile, enrolled courses with progress, certificates, and recent activity.
80. The system shall provide an endpoint `GET /api/admin/students/{studentId}/exam-attempts` returning all exam attempts for a specific student.
81. The system shall allow an administrator to toggle a student's account active/suspended status via `PATCH /api/admin/students/{studentId}/toggle-status`.
82. The system shall provide an endpoint `GET /api/admin/certificates` returning all certificates on the platform with their status.
83. The system shall provide an endpoint `GET /api/admin/exam-attempts` returning all exam attempts across the platform.
84. The system shall provide a paginated, filterable activity-log endpoint `GET /api/admin/activity-logs?page=<n>&size=<n>&action=<action>&studentId=<id>` tracking the actions `UPLOAD_DOCUMENT`, `GENERATE_COURSE`, `PASS_EXAM`, `FAIL_EXAM`, and `DOWNLOAD_CERTIFICATE`.
85. The system shall allow an administrator to view and update their own profile and change their password via dedicated admin profile endpoints.

### Student Dashboard & Profile

86. The system shall provide an authenticated student endpoint `GET /api/dashboard` that returns a consolidated view including: per-course lesson progress (completed/total), quiz statistics (attempts, pass rate), upcoming flashcard review count, pending exam status, and recent activity entries.
87. The system shall allow an authenticated student to retrieve their profile (first name, last name, email, date of birth, phone number) via `GET /api/students/profile`.
88. The system shall allow an authenticated student to update their first name, last name, date of birth, and phone number via `PUT /api/students/profile`.
89. The system shall allow an authenticated student to change their password via `PUT /api/students/password` by providing the current password for verification and the new desired password.

---

## 2. BESOINS NON FONCTIONNELS

### Performance

1. The system shall serve all standard REST API endpoints (not including AI-generation calls) with a response time under 500 ms under normal load conditions.
2. AI course-generation and exam-generation calls to the FastAPI service are expected to take 30–60 seconds; these calls shall be executed outside any database transaction to prevent connection-pool exhaustion and JPA timeout errors.
3. The system shall support HTTP range requests on MP4 lesson recap video delivery (`GET /api/lessons/recap-video`), enabling efficient browser-native video streaming without full file transfer.
4. The SSE notification service shall maintain per-user emitter connections using a `ConcurrentHashMap` to support concurrent users without blocking.
5. The inactivity reminder scheduler shall run in a dedicated scheduled thread; the startup catch-up run shall execute 10 seconds after `ApplicationReadyEvent` to avoid interfering with application boot.

### Security

6. All API endpoints, except registration, login, email verification, password reset, certificate download (`/api/certificates/{uuid}/download`), and certificate verification (`/api/admin/certificates/verify/{uuid}`), shall require a valid JWT Bearer token in the `Authorization` header.
7. JWT tokens shall be signed with a secret key configured via the `${jwt.secret}` environment variable and shall carry the user's role claim for authorization decisions.
8. Role-based access control shall be enforced at the method level using Spring Security's `@PreAuthorize("hasRole('ADMIN')")` annotation on all admin endpoints.
9. All user passwords shall be hashed using BCrypt before storage; plain-text passwords shall never be persisted.
10. SHA-256 hashing shall be applied to every uploaded file's binary content for duplicate detection; the hash is stored in the `file_hash` column of the `documents` table.
11. CORS shall be configured to allow requests only from `http://localhost:4200`, with allowed methods `GET, POST, PUT, PATCH, DELETE, OPTIONS`, all headers permitted, and credentials enabled.
12. File path resolution in the lesson recap endpoints shall validate that the normalized path contains the expected directory segment (`/recap-cards/` or `/recap-videos/`) to prevent path-traversal attacks.
13. Certificate download and verification endpoints are intentionally public (no JWT required) to support direct URL sharing; the UUID acts as a non-guessable access token.
14. All sensitive configuration values (JWT secret, database credentials, mail server credentials, AI service URL, frontend URL) shall be supplied via environment variables or application properties, never hardcoded in source code.

### Reliability

15. Document upload processing shall follow a status lifecycle (`UPLOADED → PROCESSING → COMPLETED | FAILED`); a failed AI call shall set the document to `FAILED` so the student can retry.
16. Exam and quiz attempt records shall be preserved even after submission to maintain a complete audit trail; deletion is only performed as part of cascaded document deletion.
17. The `@Transactional` annotation shall be applied to all service methods that perform multiple database writes to ensure atomicity; AI service calls shall use a `TransactionTemplate` for programmatic transaction boundaries.
18. The SSE emitter for a disconnected user shall be automatically removed from the emitter map on completion, timeout, or error to prevent memory leaks.
19. The reminder scheduler includes a startup catch-up run to compensate for any downtime that may have occurred during the scheduled 09:00 daily window.

### Scalability

20. The REST API is stateless; no server-side HTTP session state is maintained. All authentication context is carried in the JWT, enabling horizontal scaling behind a load balancer.
21. Database entities use auto-incremented `BIGINT` primary keys and PostgreSQL `bytea` columns for binary data (file content, PDF content), supporting large-scale binary storage natively within the database.
22. The `ConcurrentHashMap` used by `SseService` for emitter management is thread-safe, supporting multi-threaded Spring request handling without explicit synchronization.

### Usability

23. The API shall return structured JSON error responses with a `message` field for all expected failure cases (validation errors, not found, access denied, duplicates) to enable consistent frontend error handling.
24. All admin endpoints are organized under the `/api/admin` path prefix with a uniform `@PreAuthorize("hasRole('ADMIN')")` guard, providing a consistent and discoverable API surface.
25. Certificate status pages for PENDING and REVOKED states shall be rendered as styled HTML pages (dark theme, LearnAI branding) so that end users accessing a certificate URL directly receive a human-readable response.

### Maintainability

26. The application follows a strict layered architecture: `controller → service → repository`, with all cross-cutting concerns (security, scheduling, CORS) isolated in dedicated `config` and `security` packages.
27. All JPA entities are annotated with Lombok `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` to eliminate boilerplate; `@SuperBuilder` is used for the `User` inheritance hierarchy.
28. Database table and column names follow `snake_case` conventions; Java class and variable names follow `PascalCase` and `camelCase` conventions respectively.
29. Enum values are persisted as strings (`@Enumerated(EnumType.STRING)`) for readability and safe schema evolution.
30. The AI service integration is encapsulated entirely within `AiServiceClient`, decoupling all service classes from HTTP-level concerns.

---

## 3. ACTEURS ET RÔLES

| Actor | Description | Main Actions |
|---|---|---|
| **Visitor** (unauthenticated) | Any user who has not yet logged in or created an account. No JWT token is present in requests. | Register (`POST /api/auth/register`); verify email (`GET /api/auth/verify-email`); resend verification email; initiate and complete password reset; validate reset token; view/download a certificate by public UUID (`GET /api/certificates/{uuid}/download`); verify a certificate by UUID (`GET /api/admin/certificates/verify/{uuid}`) |
| **Student** (authenticated, role `STUDENT`) | A registered and email-verified learner. Identified by a JWT bearing the `STUDENT` role. | Log in; view and update own profile; change own password; upload and delete documents; view generated courses and lesson content; track lesson access and progress; attempt and submit quizzes; rate flashcards using SM-2; generate and take final exams; report anti-cheat events during exams; download own certificates; request PDF generation for own certificates; view own exam attempts and suspicious-activity logs; receive and read in-app and email notifications; subscribe to SSE notification stream; view student dashboard |
| **Administrator** (authenticated, role `ADMIN`) | A privileged platform operator. Identified by a JWT bearing the `ADMIN` role. All admin endpoints require this role via `@PreAuthorize("hasRole('ADMIN')")`. | Log in; view and update own admin profile; change own password; view platform-wide statistics and charts; list, view details of, and toggle active/suspended status of students; list all certificates and approve or revoke individual certificates; list all exam attempts; view paginated and filtered activity logs; view flagged exam attempts and their suspicious-activity detail; verify any certificate by UUID |
| **AI Service** (FastAPI, internal actor) | An external FastAPI microservice that performs LLM-based content generation using Llama 3.3 70B. Invoked by `AiServiceClient` over HTTP. | Process an uploaded document and return structured course data (lessons, quizzes, flashcards) (`POST /process-document`); generate exam questions from lesson content (`POST /generate-exam-questions`); generate a lesson recap image and video (`POST /generate-recap`); generate a certificate PDF (`POST /generate-certificate-pdf`) |
| **Email Service** (SMTP, internal actor) | A JavaMailSender-backed SMTP service used by `NotificationService` to dispatch transactional emails. Invoked asynchronously via `@Async`. | Send account email-verification links; send password-reset links; send exam-result notification emails; send certificate-status notification emails; send course-inactivity reminder emails |
