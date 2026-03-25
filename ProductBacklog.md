📦 LearnAI — Complete Product Backlog
EPIC 1 — Authentication & Account Management
ID	User Story	Priority	Story Points	Acceptance Criteria
US-01	As a visitor, I want to register a new student account with my first name, last name, email, password, date of birth and phone number so that I can access the learning platform.	High	5	- Registration endpoint returns 201 with user details on valid input
- A verification email is sent immediately
- Duplicate email returns 409
US-02	As a registered user, I want to verify my email so that my account is activated.	High	3	- GET /api/auth/verify-email?token=… sets isVerified=true
- Unverified users cannot access protected resources
- Invalid token returns error
US-03	As a user, I want to resend verification email.	Medium	2	- Rate-limited resend endpoint
- Uses lastVerificationEmailSent
- Always returns success message
US-04	As a verified user, I want to log in and receive a JWT.	High	3	- Returns token + user info
- Suspended users get 403
- lastLogin updated
US-05	As a user, I want to reset my password.	High	3	- Reset email with token (24h expiry)
- Token is single-use
US-06	As a user, I want to set a new password.	High	3	- Valid token updates password (bcrypt)
- Expired token rejected
US-07	As a student, I want to update my profile.	Medium	2	- GET/PUT profile works
- Partial updates allowed
US-08	As a student, I want to change my password.	Medium	2	- Requires current password
- New password hashed
US-09	As an admin, I want to manage my profile.	Low	2	- Admin-only endpoints
- Same behavior as student
EPIC 2 — Document Management
ID	User Story	Priority	Story Points	Acceptance Criteria
US-10	Upload document for course generation	High	5	- Accepts files ≤10MB
- Valid formats only
- Returns course details
US-11	Detect duplicate uploads	Medium	3	- Uses MD5 hash
- Prevents duplicate records
US-12	View uploaded documents	Medium	2	- Returns only user's docs
- Includes status
US-13	Soft delete document	Low	2	- Marks deleted
- Course remains
EPIC 3 — AI Course Generation
ID	User Story	Priority	Story Points	Acceptance Criteria
US-14	Generate course from document	High	8	- AI generates structure
- Stored in DB
US-15	Generate lessons	High	8	- Lessons ordered
- First unlocked
US-16	Generate flashcards	High	5	- Linked to lessons
US-17	Generate quizzes	High	5	- Quiz per lesson
US-18	Generate recap media	Medium	5	- Image/video endpoints
EPIC 4 — Course Viewer & Progress
ID	User Story	Priority	Story Points	Acceptance Criteria
US-19	View course details	High	3	- Includes lessons/quizzes
US-20	View progress	High	3	- Shows completion
US-21	Track lesson access	Medium	2	- Updates timestamp
US-22	View lesson progress	Medium	2	- Includes next lesson
US-23	Unlock next lesson	High	5	- Unlock on pass/fail
US-24	Dashboard overview	High	5	- Global stats
EPIC 5 — Quiz & Flashcards
ID	User Story	Priority	Story Points	Acceptance Criteria
US-25	Start quiz attempt	High	5	- Max 3 attempts
US-26	Submit quiz	High	5	- Score + explanations
US-27	Abandon quiz	Medium	2	- Counts toward attempts
US-28	View attempts	Medium	2	- History list
US-29	Flashcard session	High	5	- SM-2 scheduling
US-30	Rate flashcards	High	5	- Updates interval
EPIC 6 — Final Exam
ID	User Story	Priority	Story Points	Acceptance Criteria
US-31	Generate final exam	High	8	- 45 questions
US-32	View exam	Medium	2	- Includes metadata
US-33	Start exam	High	5	- Max 3 attempts
US-34	Submit exam	High	5	- Score + certificate
US-35	Abandon exam	Medium	2	- Counts attempt
US-36	View attempts	Medium	2	- Attempt history
US-37	Log suspicious activity	High	5	- Tracks behavior
US-38	Admin review logs	High	3	- Admin-only
EPIC 7 — Certificates
ID	User Story	Priority	Story Points	Acceptance Criteria
US-39	Auto-create certificate	High	5	- On pass
US-40	View certificates	Medium	2	- Excludes revoked
US-41	Generate PDF	High	5	- Stored in DB
US-42	Public download	High	3	- UUID access
US-43	Approve/Revoke	High	3	- Admin only
US-44	Verify certificate	High	2	- Public endpoint
EPIC 8 — Notifications
ID	User Story	Priority	Story Points	Acceptance Criteria
US-45	Real-time notifications	High	5	- SSE stream
US-46	View notifications	Medium	2	- Ordered list
US-47	Unread count	Medium	1	- Returns count
US-48	Mark as read	Medium	1	- Idempotent
US-49	Email notifications	High	5	- Async email
EPIC 9 — Admin Dashboard
ID	User Story	Priority	Story Points	Acceptance Criteria
US-50	Platform stats	High	3	- Key metrics
US-51	Activity chart	Medium	3	- Trend data
US-52	Category distribution	Medium	2	- Course categories
US-53	Recent activity	Medium	2	- Latest logs
US-54	Student list	High	3	- Summary metrics
US-55	Student details	High	5	- Full data
US-56	Enable/disable user	High	3	- Toggle status
US-57	Suspicious attempts	High	3	- Admin view
US-58	Manage certificates	High	3	- Status list
US-59	Activity logs	Medium	3	- Filters + pagination
US-60	Student exam history	Medium	2	- Attempts list
EPIC 10 — Landing Page & Chatbot
ID	User Story	Priority	Story Points	Acceptance Criteria
US-61	Public chatbot endpoint	Low	2	- No auth required