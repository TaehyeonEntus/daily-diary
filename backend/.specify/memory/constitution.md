<!--
## Sync Impact Report
- Version change: N/A → 1.0.0 (initial population from template)
- Modified principles: All (template placeholders → concrete values)
- Added sections: Technology Constraints, Development Workflow
- Removed sections: None
- Templates requiring updates:
  - .specify/templates/plan-template.md ✅ No changes required; Constitution Check section is generic
  - .specify/templates/spec-template.md ✅ No changes required; structure is compatible
  - .specify/templates/tasks-template.md ✅ No changes required; structure is compatible
  - .specify/templates/agent-file-template.md ✅ No changes required; auto-generated from plans
- Follow-up TODOs: None — all placeholders resolved
-->

# daily-diary Constitution

## Core Principles

### I. Layered Architecture

All code MUST reside in one of five designated packages under `com.daily_diary.backend`:
`web/`, `infra/`, `service/`, `exception/`, or `domain/`. Dependencies MUST flow inward
only: `web` → `service` → `domain`; `infra` → `domain`. `service` MUST depend only on
repository interfaces, never on JPA implementation classes directly. Cross-layer violations
(e.g., a Controller importing a JPA Repository) are rejected in code review.

### II. Immutable Domain Model

Entity classes MUST declare `@Getter` and `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.
`@Setter` is forbidden on all entity classes without exception. State mutation MUST be
expressed via explicitly named `change*()` instance methods. Static factory methods MUST
follow the `of(...)` convention. All request/response DTOs MUST be Java `record` types
named `XxxRequest` / `XxxResponse`.

### III. Constructor Injection Only

All Spring-managed components MUST declare dependencies as `private final` fields and use
`@RequiredArgsConstructor` for injection. Field injection via `@Autowired` and setter
injection are forbidden project-wide. This ensures collaborator immutability and makes
unit-testing without a Spring context straightforward.

### IV. Centralized Exception Handling

All custom exceptions MUST extend `BusinessException`. Business logic MUST NOT throw raw
`RuntimeException` or `IllegalArgumentException`. A single `@ControllerAdvice` class MUST
handle all `BusinessException` subclasses and map them to appropriate HTTP responses.
Exception swallowing (catch block with no re-throw and no logging) is forbidden.

### V. Test-Layer Alignment

Each layer MUST be tested with the prescribed annotation:

- **Controller**: `@WebMvcTest` only — no full application context
- **Repository**: `@DataJpaTest` + H2 in-memory DB
- **Service**: `@ExtendWith(MockitoExtension.class)` — no Spring context loaded
- **Integration**: `@SpringBootTest` + `@ActiveProfiles("test")` + H2

Integration test configuration MUST live in
`src/test/resources/application-test.properties`. Tests MUST NOT load the full Spring
context when a slice annotation suffices.

## Technology Constraints

The following technology choices are fixed and MUST NOT be changed without a constitution
amendment:

- **Runtime**: Java 21, compiled with Java 17 toolchain
- **Framework**: Spring Boot 3.5, Spring MVC, Spring Security 6, Spring Data JPA
- **ORM**: Hibernate 6 via Spring Data JPA; HikariCP connection pool
- **Dynamic Queries**: QueryDSL 5.0 (jakarta); Q-classes generated at
  `build/generated/querydsl` via Gradle
- **Production DB**: MySQL 8+ via `mysql-connector-j`
- **Test DB**: H2 in-memory only — MySQL MUST NOT be used in tests
- **Boilerplate Reduction**: Lombok (`@Getter`, `@NoArgsConstructor`, `@RequiredArgsConstructor`)
- **Auth**: Spring Security + JWT; `SecurityFilterChain` bean configures all security rules;
  JWT filter registered via `addFilterBefore`

## Development Workflow

- `Service` classes MUST annotate the class with `@Transactional(readOnly = true)`.
  Write operations MUST annotate individual methods with `@Transactional`.
- Controller methods MUST return `ResponseEntity<T>`. No response wrapper class is used;
  data is returned directly.
- Every new domain feature MUST introduce sub-packages in each relevant layer:
  `web/{domain}/`, `service/{domain}/`, `domain/{domain}/`, `infra/{domain}/`.
- Database credentials MUST NOT be committed to the repository. Developers configure
  `src/main/resources/application.properties` locally per the CLAUDE.md template.
- CI runs on every push modifying `backend/**` via GitHub Actions (build + test).
  All tests MUST pass before a PR can be merged.

## Governance

This constitution supersedes all other development practices for the `daily-diary` backend.
Amendments MUST be proposed via a PR that includes:

1. Updated `constitution.md` with an incremented version number
2. A rationale comment explaining the change
3. A migration note if existing code must be updated to comply

All PRs MUST be verified for compliance with these principles during code review.
Complexity that violates a principle MUST be justified in the plan's Complexity Tracking
table. Runtime development guidance is maintained in `CLAUDE.md` at the backend root.

**Version**: 1.0.0 | **Ratified**: 2026-03-30 | **Last Amended**: 2026-03-30
