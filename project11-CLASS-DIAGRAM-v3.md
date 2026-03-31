# Smart Learning Platform — Class Diagram v3

> Extracted directly from the actual Java source code (entities + services).  
> Accurate as of **2026-03-06**.  
> Methods are sourced from the corresponding `@Service` class for each entity.

---

## Enumerations

```
+--------------------------+    +--------------------------+    +--------------------------+
|    <<enumeration>>       |    |    <<enumeration>>       |    |    <<enumeration>>       |
|        UserRole          |    |        FileType          |    |     DocumentStatus       |
+--------------------------+    +--------------------------+    +--------------------------+
|  STUDENT                 |    |  PDF                     |    |  UPLOADED                |
|  ADMIN                   |    |  DOCX                    |    |  PROCESSING              |
+--------------------------+    |  PPTX                    |    |  COMPLETED               |
                                |  IMAGE                   |    |  FAILED                  |
                                +--------------------------+    +--------------------------+

+--------------------------+    +--------------------------+    +--------------------------+
|    <<enumeration>>       |    |    <<enumeration>>       |    |    <<enumeration>>       |
|      QuestionType        |    |     DifficultyLevel      |    |     FlashcardRating      |
+--------------------------+    +--------------------------+    +--------------------------+
|  MCQ                     |    |  EASY                    |    |  AGAIN                   |
|  TRUE_FALSE              |    |  MEDIUM                  |    |  GOOD                    |
|  FILL_BLANK              |    |  HARD                    |    |  EASY                    |
+--------------------------+    +--------------------------+    +--------------------------+

+--------------------------+    +--------------------------+    +--------------------------+
|    <<enumeration>>       |    |    <<enumeration>>       |    |    <<enumeration>>       |
|     FinishReason         |    |   NotificationType       |    | NotificationCategory     |
+--------------------------+    +--------------------------+    +--------------------------+
|  SUBMITTED               |    |  EMAIL                   |    |  COURSE_COMPLETE         |
|  TIME_EXPIRED            |    |  IN_APP                  |    |  EXAM_RESULT             |
|  ABANDONED               |    +--------------------------+    |  CERTIFICATE             |
+--------------------------+                                    |  REMINDER                |
                                                                |  SUSPICIOUS_ACTIVITY     |
                                                                +--------------------------+

+--------------------------+    +---------------------------+
|    <<enumeration>>       |    |    <<enumeration>>        |
|      ActionType          |    |  SuspiciousActivityType   |
+--------------------------+    +---------------------------+
|  UPLOAD_DOCUMENT         |    |  TAB_SWITCH               |
|  GENERATE_COURSE         |    |  COPY_PASTE               |
|  PASS_EXAM               |    |  RIGHT_CLICK              |
|  FAIL_EXAM               |    |  UNUSUAL_TIMING           |
|  DOWNLOAD_CERTIFICATE    |    +---------------------------+
+--------------------------+
```

---

## Classes

### User (base class)

```
+----------------------------------------------------------------+
|                            User                                |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - email: String {unique}                                       |
| - password: String                                             |
| - firstName: String                                            |
| - lastName: String                                             |
| - role: UserRole                                               |
| - isActive: Boolean {default true}                             |
| - isVerified: Boolean {default false}                          |
| - verificationToken: String [0..1]                             |
| - resetToken: String [0..1]                                    |
| - resetTokenExpiry: Timestamp [0..1]                           |
| - lastVerificationEmailSent: Timestamp [0..1]                  |
| - createdAt: Timestamp {auto}                                  |
| - lastLogin: Timestamp [0..1]                                  |
+----------------------------------------------------------------+
| + register(request): AuthResponse {no token — redirects to login} |
| + login(request): AuthResponse {JWT token}                     |
| + verifyEmail(token): void                                     |
| + resendVerificationEmail(email): void                         |
| + forgotPassword(email): void                                  |
| + resetPassword(token, newPassword): void                      |
| + getProfile(userId): StudentProfileResponse                   |
| + updateProfile(userId, request): StudentProfileResponse       |
| + changePassword(userId, request): void                        |
+----------------------------------------------------------------+
                    ^                            ^
                    | extends                    | extends
         +----------+                            +----------+

+----------------------------------------+  +------------------------------------+
|              Student                   |  |         Administrator              |
+----------------------------------------+  +------------------------------------+
| - dateOfBirth: Date [0..1]             |  | (no additional fields)             |
| - phoneNumber: String [0..1]           |  +------------------------------------+
+----------------------------------------+
```

### Document and Course

```
+----------------------------------------------------------------+
|                         Document                               |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - studentId: Long (FK -> students)                             |
| - fileName: String                                             |
| - fileType: FileType                                           |
| - category: String [0..1]                                      |
| - fileSize: Long                                               |
| - fileContent: byte[]                                          |
| - uploadedAt: Timestamp {auto}                                 |
| - status: DocumentStatus {default UPLOADED}                    |
| - fileHash: String [0..1]                                      |
+----------------------------------------------------------------+
| + uploadAndGenerate(file, student): UploadResponse             |
| + getStudentDocuments(studentId): List<DocumentResponse>       |
| + softDeleteDocument(documentId, studentId): void              |
+----------------------------------------------------------------+
              |
              | 1 generates
              v 1
+----------------------------------------------------------------+
|                          Course                                |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - documentId: Long (FK -> documents)                           |
| - studentId: Long (FK -> students)                             |
| - title: String                                                |
| - category: String [0..1]                                      |
| - description: Text [0..1]                                     |
| - createdAt: Timestamp {auto}                                  |
+----------------------------------------------------------------+
| + generateAndSave(aiResponse, document, student): Course       |
| + getCourseById(courseId, studentId): CourseDetailResponse     |
| + getCourseProgress(courseId, studentId): List<...>            |
| + cloneCourseForStudent(source, newDoc, studentId): Course     |
+----------------------------------------------------------------+
```

### Lesson, Quiz, Flashcard, LessonProgress

```
+----------------------------------------------------------------+
|                          Lesson                                |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - courseId: Long (FK -> courses)                               |
| - lessonNumber: Integer                                        |
| - title: String                                                |
| - content: Text                                                |
| - summary: Text [0..1]                                         |
| - estimatedReadTime: Integer [0..1]                            |
| - recapVideoPath: String [0..1]                                |
| - createdAt: Timestamp {auto}                                  |
+----------------------------------------------------------------+
| + trackLessonAccess(lessonId, studentId): void                 |
| + getLessonProgress(lessonId, studentId): LessonProgressResponse|
| + generateLessonRecap(lessonId): Map<String,String>            |
+----------------------------------------------------------------+
              |
    +---------+-----------+
    | 0..1    | 0..*      | 0..*
    v         v           v

+------------------+  +-------------------+  +----------------------+
|      Quiz        |  |    Flashcard      |  |   LessonProgress     |
+------------------+  +-------------------+  +----------------------+
| - id: Long       |  | - id: Long        |  | - id: Long           |
| - lessonId: Long |  | - lessonId: Long  |  | - studentId: Long    |
| - title: String  |  | - term: String    |  | - lessonId: Long     |
| - passingScore:  |  | - definition: Text|  | - isCompleted: Bool  |
|   Integer        |  | - difficulty:     |  | - completedAt:       |
| - maxAttempts:   |  |   DifficultyLevel |  |   Timestamp [0..1]   |
|   Integer        |  | - createdAt:      |  | - timeSpent:         |
| - timeLimitMinutes  |   Timestamp {auto}|  |   Integer [0..1]     |
|   : Integer      |  +-------------------+  | - lastAccessedAt:    |
| - createdAt:     |  | + getSession():   |  |   Timestamp [0..1]   |
|   Timestamp {auto}  |   FlashcardSession|  | - isLocked: Boolean  |
+------------------+  | + reviewFlashcard |  |   {default true}     |
| + startAttempt() |  |   (): ReviewResp  |  | - quizPassed: Boolean|
| + submitAttempt()|  | + getDueFlashcards|  |   {default false}    |
| + abandonAttempt |  |   (): List<...>   |  +----------------------+
| + getMyAttempts()|  +-------------------+  | + processQuizAttempt |
+------------------+                         |   (): ProgressResp   |
                                             +----------------------+

+----------------------------------------------------------------+
|                        QuizQuestion                            |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - quizId: Long (FK -> quizzes)                                 |
| - questionNumber: Integer                                      |
| - questionText: Text                                           |
| - questionType: QuestionType                                   |
| - correctAnswer: String                                        |
| - option1: String                                              |
| - option2: String                                              |
| - option3: String                                              |
| - option4: String [0..1]                                       |
| - explanation: Text [0..1]                                     |
| - difficulty: DifficultyLevel                                  |
| - pointsWorth: Integer                                         |
+----------------------------------------------------------------+
```

### FlashcardReview (SM-2 Spaced Repetition)

```
+----------------------------------------------------------------+
|                      FlashcardReview                           |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - studentId: Long (FK -> students)                             |
| - flashcardId: Long (FK -> flashcards)                         |
| - easeFactor: Float {default 2.5}                              |
| - interval: Integer {default 1}                                |
| - repetitionCount: Integer {default 0}                         |
| - nextReviewDate: Date                                         |
| - lastReviewedAt: Timestamp [0..1]                             |
| - lastRating: FlashcardRating [0..1]                           |
| - qualityScore: Integer {0..5} [0..1]                          |
| - consecutiveCorrectReviews: Integer {default 0}               |
+----------------------------------------------------------------+
```

### Exam

```
+----------------------------------------------------------------+
|                           Exam                                 |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - courseId: Long (FK -> courses)                               |
| - title: String                                                |
| - passingScore: Integer                                        |
| - maxAttempts: Integer                                         |
| - totalPoints: Integer                                         |
| - timeLimitMinutes: Integer [0..1]                             |
| - createdAt: Timestamp {auto}                                  |
+----------------------------------------------------------------+
| + generateExamForCourse(courseId, studentId): ExamResponse     |
| + getExamForCourse(courseId, studentId): ExamResponse          |
| + startAttempt(examId, studentId): ExamAttemptResponse         |
| + submitAttempt(attemptId, studentId, req): SubmitExamResponse |
| + abandonAttempt(attemptId, studentId): ExamAttemptResponse    |
| + getMyAttempts(examId, studentId): List<ExamAttemptResponse>  |
+----------------------------------------------------------------+
    |
    | 1 contains
    v 1..*
+----------------------------------------------------------------+
|                       ExamQuestion                             |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - examId: Long (FK -> exams)                                   |
| - questionNumber: Integer                                      |
| - questionText: Text                                           |
| - questionType: QuestionType                                   |
| - correctAnswer: String                                        |
| - option1: String [0..1]                                       |
| - option2: String [0..1]                                       |
| - option3: String [0..1]                                       |
| - option4: String [0..1]                                       |
| - explanation: Text [0..1]                                     |
| - difficulty: DifficultyLevel                                  |
| - sectionNumber: Integer                                       |
| - pointsWorth: Integer                                         |
+----------------------------------------------------------------+
```

### Quiz Attempt and Answers

```
+----------------------------------------------------------------+
|                        QuizAttempt                             |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - studentId: Long (FK -> students)                             |
| - quizId: Long (FK -> quizzes)                                 |
| - attemptNumber: Integer                                       |
| - score: Integer [0..1]                                        |
| - isPassed: Boolean {default false}                            |
| - startedAt: Timestamp {auto}                                  |
| - submittedAt: Timestamp [0..1]                                |
| - finishReason: FinishReason [0..1]                            |
+----------------------------------------------------------------+
    |
    | 1 records
    v 1..*
+------------------------------------------+
|             QuizAnswer                   |
+------------------------------------------+
| - id: Long                               |
| - quizAttemptId: Long (FK->quiz_attempts)|
| - questionId: Long (FK->quiz_questions)  |
| - studentAnswer: String                  |
| - isCorrect: Boolean {default false}     |
| - pointsAwarded: Integer {default 0}     |
| - answeredAt: Timestamp {auto}           |
+------------------------------------------+
```

### Exam Attempt and Answers

```
+----------------------------------------------------------------+
|                        ExamAttempt                             |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - studentId: Long (FK -> students)                             |
| - examId: Long (FK -> exams)                                   |
| - attemptNumber: Integer                                       |
| - score: Integer [0..1]                                        |
| - isPassed: Boolean {default false}                            |
| - startedAt: Timestamp {auto}                                  |
| - submittedAt: Timestamp [0..1]                                |
| - finishReason: FinishReason [0..1]                            |
+----------------------------------------------------------------+
    |                    |                    |
    | 1 records          | 1 selects          | 1 triggers
    v 1..*               v 1..*               v 0..*

+------------------------------+  +------------------------------+  +----------------------------------+
|         ExamAnswer           |  |   ExamAttemptQuestion        |  |       SuspiciousActivity         |
+------------------------------+  +------------------------------+  +----------------------------------+
| - id: Long                   |  | - id: Long                   |  | - id: Long                       |
| - examAttemptId: Long        |  | - examAttemptId: Long        |  | - examAttemptId: Long            |
| - questionId: Long           |  | - examQuestionId: Long       |  | - activityType:                  |
| - studentAnswer: String      |  +------------------------------+  |   SuspiciousActivityType         |
| - isCorrect: Boolean         |                                    | - count: Integer {default 1}     |
| - pointsAwarded: Integer     |                                    | - detectedAt: Timestamp {auto}   |
| - answeredAt: Timestamp {auto}|                                   +----------------------------------+
+------------------------------+                                    | + logActivity(): SuspiciousActivityDTO |
                                                                    | + getForAttempt(): List<...>     |
                                                                    | + getFlaggedAttemptIds(): List<> |
                                                                    +----------------------------------+
```

### Certificate

```
+----------------------------------------------------------------+
|                        Certificate                             |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - certificateUuid: String {unique}                             |
| - studentId: Long (FK -> students)                             |
| - courseId: Long (FK -> courses)                               |
| - examAttemptId: Long (FK -> exam_attempts)                    |
| - score: Integer                                               |
| - issuedAt: Timestamp {auto}                                   |
| - pdfContent: byte[]                                           |
| - status: CertificateStatus                                    |
+----------------------------------------------------------------+
| + getMyCertificates(studentId): List<CertificateDTO>           |
| + getCourseCertificate(studentId, courseId): CertificateDTO    |
| + generatePdf(certificateId, studentId): CertificateDTO        |
| + downloadPdf(certificateId, studentId): byte[]                |
+----------------------------------------------------------------+
```

### Notification

```
+----------------------------------------------------------------+
|                       Notification                             |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - userId: Long (FK -> users)                                   |
| - type: NotificationType                                       |
| - category: NotificationCategory                               |
| - title: String                                                |
| - message: Text                                                |
| - referenceId: Long [0..1]                                     |
| - actionUrl: String [0..1]                                     |
| - isRead: Boolean {default false}                              |
| - sentAt: Timestamp {auto}                                     |
| - readAt: Timestamp [0..1]                                     |
+----------------------------------------------------------------+
| + notify(userId, category, title, message): void               |
| + sendEmailNotification(userId, title, msg, url): void         |
| + markAsRead(notificationId): NotificationDTO                  |
| + getUserNotifications(userId): List<NotificationDTO>          |
| + getUnreadCount(userId): long                                 |
+----------------------------------------------------------------+
```

### ActivityLog

```
+----------------------------------------------------------------+
|                        ActivityLog                             |
+----------------------------------------------------------------+
| - id: Long                                                     |
| - userId: Long (FK -> users)                                   |
| - action: ActionType                                           |
| - entityType: String [0..1]                                    |
| - entityId: Long [0..1]                                        |
| - timestamp: Timestamp {auto}                                  |
+----------------------------------------------------------------+
```

---

## Relationships Summary

### Inheritance
- `User` <|-- `Student`
- `User` <|-- `Administrator`

### Composition (cascade delete)
- `Course` *---- 1..* `Lesson`
- `Quiz` *---- 1..* `QuizQuestion`
- `Exam` *---- 1..* `ExamQuestion`
- `QuizAttempt` *---- 1..* `QuizAnswer`
- `ExamAttempt` *---- 1..* `ExamAnswer`

### Association
- `Student` 1 -- 0..* `Document`
- `Document` 1 -- 1 `Course`
- `Student` 1 -- 0..* `Course`
- `Course` 1 -- 0..1 `Exam`
- `Lesson` 1 -- 0..1 `Quiz`
- `Lesson` 1 -- 0..* `Flashcard`
- `Student` 1 -- 0..* `QuizAttempt`
- `Quiz` 1 -- 0..* `QuizAttempt`
- `Student` 1 -- 0..* `ExamAttempt`
- `Exam` 1 -- 0..* `ExamAttempt`
- `ExamAttempt` 1 -- 0..* `ExamAttemptQuestion`
- `ExamAttempt` 1 -- 0..* `SuspiciousActivity`
- `Student` 1 -- 0..* `Certificate`
- `Course` 1 -- 0..* `Certificate`
- `ExamAttempt` 1 -- 0..1 `Certificate`
- `User` 1 -- 0..* `Notification`
- `User` 1 -- 0..* `ActivityLog`

### Association Classes (Join Tables with own attributes)
- `Student` -- `LessonProgress` -- `Lesson`
- `Student` -- `FlashcardReview` -- `Flashcard`

### Pure Join Tables (no extra attributes)
- `ExamAttempt` -- `ExamAttemptQuestion` -- `ExamQuestion`

---

## Entity to Database Table Mapping (22 tables)

| # | Class | Database Table | PK | Main FKs |
|---|---|---|---|---|
| 1 | User | `users` | id | - |
| 2 | Student | `students` | id | -> users(id) |
| 3 | Administrator | `administrators` | id | -> users(id) |
| 4 | Document | `documents` | id | -> students(id) |
| 5 | Course | `courses` | id | -> documents(id), -> students(id) |
| 6 | Lesson | `lessons` | id | -> courses(id) |
| 7 | Quiz | `quizzes` | id | -> lessons(id) |
| 8 | QuizQuestion | `quiz_questions` | id | -> quizzes(id) |
| 9 | Flashcard | `flashcards` | id | -> lessons(id) |
| 10 | FlashcardReview | `flashcard_reviews` | id | -> students(id), -> flashcards(id) |
| 11 | LessonProgress | `lesson_progress` | id | -> students(id), -> lessons(id) |
| 12 | Exam | `exams` | id | -> courses(id) |
| 13 | ExamQuestion | `exam_questions` | id | -> exams(id) |
| 14 | QuizAttempt | `quiz_attempts` | id | -> students(id), -> quizzes(id) |
| 15 | QuizAnswer | `quiz_answers` | id | -> quiz_attempts(id), -> quiz_questions(id) |
| 16 | ExamAttempt | `exam_attempts` | id | -> students(id), -> exams(id) |
| 17 | ExamAnswer | `exam_answers` | id | -> exam_attempts(id), -> exam_questions(id) |
| 18 | ExamAttemptQuestion | `exam_attempt_questions` | id | -> exam_attempts(id), -> exam_questions(id) |
| 19 | SuspiciousActivity | `suspicious_activities` | id | -> exam_attempts(id) |
| 20 | Certificate | `certificates` | id | -> students(id), -> courses(id), -> exam_attempts(id) |
| 21 | Notification | `notifications` | id | -> users(id) |
| 22 | ActivityLog | `activity_logs` | id | -> users(id) |

---

## Design Decisions

| Decision | Reason |
|---|---|
| **Separate QuizQuestion / ExamQuestion** | Different parent tables (Quiz vs Exam) — separate tables for clean FK constraints. |
| **Separate QuizAnswer / ExamAnswer** | Each points to its own Attempt and Question table. |
| **ExamAttemptQuestion** | Snapshot of which questions were served for a given attempt — decouples attempt from live question pool. |
| **FK attributes inside classes** | Design-level diagram for direct Spring Boot entity generation. |
| **`sectionNumber` on ExamQuestion** | Exam is divided into 3 difficulty sections (easy/medium/hard) — section determines UI grouping. |
| **`totalPoints`, `sectionEasyCount/MediumCount/HardCount` on Exam** | Stored at exam creation time to avoid recomputing on every attempt. |
| **`recapVideoPath` on Lesson** | Path to AI-generated Remotion video recap — stored as nullable string. |
| **`category` on Course** | Optional classification field derived from uploaded document context. |
| **`SuspiciousActivity.count`** | Upsert pattern — one row per (examAttemptId, activityType), count incremented per event. |
| **Removed `totalLessons` from Course** | Redundant — `lessons.size()` in JPA. |
| **Removed `totalQuestions` from Exam/Quiz** | Redundant — `questions.size()` in JPA. |
| **Enums extracted as separate enumerations** | Proper UML. Maps directly to Java enums. |
| **LessonProgress / FlashcardReview** | Association classes (join tables with own attributes). |
| **`isVerified` + `verificationToken` on User** | Registration flow requires email verification before login is permitted. Token is single-use and cleared after use. |
| **`resetToken` + `resetTokenExpiry` on User** | Password-reset flow — UUID token with 15-minute TTL, cleared after successful reset. |
| **`lastVerificationEmailSent` on User** | Rate-limits resend-verification requests to one per 45 seconds per account. |
| **Methods pulled from `@Service` classes** | Entities are pure data models. Business logic lives entirely in service classes. |
| **`Notification.userId` (not `studentId`)** | Notifications are sent to any `User` (student or admin) — FK points to `users(id)`. |
| **`referenceId` + `actionUrl` on Notification** | Allow deep-linking from a notification to a specific resource in the frontend. |
| **`NotificationType`: EMAIL, IN_APP only** | `PUSH` removed — no push-notification infrastructure is implemented. |
| **`NotificationCategory`: 5 values** | `ALERT` replaced by `SUSPICIOUS_ACTIVITY` to accurately describe the proctoring system category. |
| **`option4` nullable on QuizQuestion** | Some questions (TRUE_FALSE) only need 2 options. |
| **`fileHash` on Document** | SHA-256 hash of the uploaded file bytes — used to detect duplicate uploads across students. Nullable, no unique constraint (multiple students can have the same hash). |
| **`cloneCourseForStudent()` on CourseService** | When a duplicate hash is detected, the existing course content (Lessons, Flashcards, Quiz, QuizQuestions) is deep-cloned for the new student — each student still gets their own Course, LessonProgress, and FlashcardReview records. AI service is not called again. |
| **`register()` returns no JWT token** | After registration the account is unverified — issuing a token would allow the user to bypass email verification. The response contains user info only; the frontend redirects to `/login`. |
| **`option1–4` all nullable on ExamQuestion** | Exam supports FILL_BLANK type which requires no options. |
