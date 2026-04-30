# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Stack
- Backend: Spring Boot 3.5, Java 17, MyBatis-Plus, Sa-Token, Redis, MySQL
- Frontend: Vue 3, Vue Router, Vite, TypeScript
- Test setup: Spring Boot test starter with H2 for tests

## Common commands

### Backend
- Start backend: `mvn spring-boot:run`
- Run backend tests: `mvn test`
- Run a single backend test class: `mvn -Dtest=LegalApplicationTests test`
- Package backend: `mvn package`

### Frontend
Run frontend commands from `web/`.
- Install dependencies: `npm install`
- Start dev server: `npm run dev`
- Build frontend: `npm run build`
- Preview production build: `npm run preview`

## Runtime configuration
- The backend default profile is `dev`, selected by `LEGAL_PROFILE` in `src/main/resources/application.yaml`.
- The backend listens on port `8081` by default.
- Frontend API requests default to `http://localhost:8081` unless `VITE_API_BASE_URL` is set in the frontend environment.
- The checked-in dev config points at external MySQL and Redis instances via `LEGAL_DB_*` and `LEGAL_REDIS_*` environment variables, with fallback defaults in `application.yaml`.

## High-level architecture

### Overall shape
This repo is split into two applications:
- `src/main/java` + `src/main/resources`: Spring Boot JSON API and persistence layer
- `web/src`: Vue single-page app consuming that API

The current backend is a baseline legal Q&A platform: auth, chat sessions/messages, knowledge-base document metadata, retrieval logging, and feedback are all wired end-to-end, but the actual answer generation is still placeholder logic in `src/main/java/com/legal/chat/service/ChatService.java`.

### Backend structure
Backend code is organized by domain package rather than by layer across the whole app:
- `auth`: login, logout, current-user APIs and user persistence
- `chat`: session creation, message history, ask flow
- `knowledge`: knowledge document CRUD-ish flows plus index outbox task creation
- `feedback`: thumbs-up/down style answer feedback
- `retrieval`: retrieval log persistence
- `security`, `common`, `config`: cross-cutting concerns

Within each domain, the common pattern is:
- `controller`: REST endpoints under `/api/**`
- `service`: business logic and transaction boundaries
- `mapper`: MyBatis-Plus mapper interfaces
- `entity`: database-mapped persistence objects
- `dto`: request/response payloads

`com.legal.LegalApplication` enables component scanning and uses `@MapperScan("com.legal.**.mapper")`, so new MyBatis mappers should stay under a `mapper` package.

### Request/response conventions
- API responses are wrapped in `ApiResponse<T>` from `src/main/java/com/legal/common/ApiResponse.java` with `{ code, message, data, traceId }`.
- Success responses use `code = 0`; errors are normalized through `GlobalExceptionHandler`.
- `TraceIdFilter` assigns or propagates `X-Trace-Id`, stores it in MDC, and every API response includes that trace ID.

### Authentication and authorization
- Authentication is handled by Sa-Token, not Spring Security.
- `WebMvcConfig` applies a `SaInterceptor` to `/api/**` and leaves only `/api/auth/login` public.
- Login stores tenant/user/role data in the Sa-Token session; `AuthContextHolder.getRequired()` reconstructs an `AuthPrincipal` from that session state for controllers/services.
- Role checks are annotation-based where needed; for example, the knowledge-base controller is guarded with `@SaCheckRole("ADMIN")`.
- The frontend mirrors auth state in `localStorage` and uses router guards in `web/src/router/index.ts` to enforce login and admin-only navigation.

### Multi-tenant and idempotent write flow
- Most domain data is tenant-scoped and user-scoped. Services typically query by both `tenantId` and `ownerUserId`, so preserve that pattern when adding reads or writes.
- Write endpoints expect a client-generated `requestId`.
- `IdempotencyService` stores keys in Redis for 10 minutes using the tuple `(userId, scene, requestId)`, so new mutating operations should follow the same scene-based idempotency pattern.

### Chat flow
- `ChatController` exposes session creation, session listing, message listing, and ask endpoints.
- `ChatService.ask(...)` currently performs the full baseline flow: enforce idempotency, verify session ownership, persist the user message, generate a placeholder assistant answer, persist the assistant message, log a retrieval record, and bump `lastActiveAt` on the session.
- The important architectural point is that chat already has persistence, auditing, and response envelope shape in place; replacing the placeholder answer should preserve those side effects.

### Knowledge-base flow
- Knowledge documents are metadata records in `kb_document`; this version does not yet upload or parse source files.
- Triggering index or delete does not perform indexing inline. Instead, `KnowledgeService` increments `docVersion`, updates document status, and writes an outbox row to `kb_index_outbox` with `UPSERT` or `DELETE`.
- That means future indexing workers should consume the outbox table rather than bolting indexing directly into the controller path.

### Database model
`src/main/resources/db/schema.sql` is the clearest overview of the current data model. Core tables are:
- `legal_user`: tenant-scoped users with `ADMIN` / `USER` roles
- `chat_session` and `chat_message`: conversation state
- `kb_document`, `kb_chunk`, `kb_index_outbox`: knowledge-base metadata, chunk storage, and asynchronous indexing handoff
- `qa_feedback`: answer feedback
- `retrieval_log`: per-request retrieval/audit trail

The schema also seeds two dev accounts:
- `admin / Admin@123`
- `user / User@123`

### Frontend structure
The frontend is a small SPA with route-per-screen views:
- `LoginView.vue`: username/password + tenant login
- `ChatView.vue`: active conversation UI and feedback submission
- `SessionsView.vue`: session list and reopen flow
- `KnowledgeView.vue`: admin-only knowledge metadata management

`web/src/api/client.ts` is the central API wrapper:
- injects bearer token from `localStorage`
- unwraps the backend `ApiResponse<T>` envelope
- clears auth state on `401`
- provides the `randomRequestId(...)` helper used by all mutating calls

When changing frontend write flows, preserve request-id generation unless the backend contract changes too.

## Testing notes
- There is currently only a minimal Spring Boot context-load test in `src/test/java/com/legal/LegalApplicationTests.java`.
- The test profile is `test`; if you add backend tests that hit persistence, keep in mind the production/dev profile depends on MySQL + Redis while tests already include H2.

## Important implementation constraints
- Do not assume the chat answer path is connected to a real LLM or vector retrieval system yet; the current behavior is intentionally placeholder.
- Do not remove `requestId` from mutating APIs unless you also redesign the Redis-backed idempotency checks.
- Knowledge-base indexing is modeled as an outbox workflow, not synchronous indexing.
- Auth/session logic depends on Sa-Token session attributes (`tenantId`, `userId`, `roleCode`) being populated consistently at login time.

## Authentication  
- Use JWT tokens, not sessions  
- Store in httpOnly cookies  

## Testing  
- Write tests for all API endpoints  
- Use Jest, not Mocha  

## Error Handling  
- Return structured errors: { error: string, code: number }

