## 1. Data model and review workflow

- [x] 1.1 Extend `src/main/resources/db/schema.sql` with contract review, extracted field, risk item, and review task/rule definition tables.
- [x] 1.2 Add MyBatis entities, mappers, and enums for contract review status, field status, risk severity, and rule execution state.
- [x] 1.3 Implement a contract review application service that creates idempotent review runs bound to `documentId` and `docVersion`.
- [x] 1.4 Implement asynchronous review task dispatching and status transitions for pending, processing, completed, and failed runs.

## 2. Extraction and validation engine

- [x] 2.1 Implement a contract field extraction pipeline that outputs field code, normalized value, evidence, confidence, extractor type, and status.
- [x] 2.2 Support repeatable fields and non-success extraction states for missing or uncertain contract data.
- [x] 2.3 Implement pluggable enterprise rule validators for required fields, amount consistency, date ordering, and abnormal threshold checks.
- [x] 2.4 Persist rule hits and generate aggregated risk summaries, key warnings, and overall risk levels for each review.

## 3. APIs and frontend review experience

- [x] 3.1 Add backend endpoints to trigger a contract review, query review status/detail, and re-run analysis for a newer document version.
- [x] 3.2 Create a frontend contract risk review view that shows review status, extracted fields, evidence, and risk items.
- [x] 3.3 Add an entry from the existing knowledge/document management flow so users can start risk review for imported contracts.

## 4. Verification and rollout readiness

- [x] 4.1 Add backend tests for extraction status handling, validation rule hits, risk aggregation, and review lifecycle transitions.
- [x] 4.2 Add seed/default rule definitions plus failure logging or observability hooks for review execution.
- [x] 4.3 Validate the end-to-end flow with sample contracts covering missing fields, inconsistent amounts, and clause conflicts.

