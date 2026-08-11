# CLAUDE.md

Instructions for Claude Code when working in this repository. Read this file in full before making any change.

## Project

Meal Planner Agent — a portfolio project demonstrating agentic system design in Java, not just LLM usage. Full requirements live in `docs/PRD.md`. When a requirement is unclear, check the PRD before guessing — it is the source of truth for product behavior.

## Stack

- Java 25, Spring Boot 4.1, Maven
- PostgreSQL, Flyway for schema migrations (no Hibernate `ddl-auto`, ever)
- Spring Security + JWT (jjwt 0.12.6) for auth
- Testcontainers for integration tests
- Claude Haiku 4.5 via Anthropic API for the agent layer

## Architecture rules

- Standard layering: `controller` → `service` → `repository`, with `dto` and `mapper` packages. Controllers never touch repositories directly.
- DTOs never expose JPA entities directly in API responses or requests. Always map.
- `BigDecimal` for all quantities, prices, and nutrient values (kcal, protein, fat, carbs). Never `double` or `float` for anything that gets summed or compared.
- `java.time` types only (`LocalDate`, `Instant`) — never `java.util.Date`.
- Bean Validation annotations on all incoming DTOs.
- Package-private where possible; keep public API surface minimal.

Business logic (pricing/scoring rules, the tool-calling loop, portion scaling, recipe validation, anything in `docs/PRD.md` sections 3–4) is implemented directly, same as everything else — no stub-and-TODO handoff. Explain non-obvious decisions in your response so they're easy to walk through later, and cite the PRD requirement a piece of logic implements.

## Agent layer — non-negotiable rules (see PRD section 4 and 6.1)

- **The LLM is never the source of truth for persistent state.** Every fact used in a business operation (pantry contents, quantities, nutrition values) is confirmed by a tool result or DB query — never taken from the model's own claim.
- **Generated recipes are validated in Java before being shown to the user.** Allergies and hard exclusions, pantry availability, units, equipment, cook time — a dedicated validator, not a prompt instruction. See INV-04, INV-08.
- **Available tools are scoped by scenario**, not given to the agent wholesale. Onboarding, cooking, and shopping scenarios each expose a different tool subset (AI-13).
- **All state-changing tools are idempotent**, keyed by agent run ID + call sequence number (AI-15).
- **Errors are split into two classes**: recoverable domain errors (not enough pantry, invalid unit, infeasible recipe) go back to the model as an observation; infrastructure errors (DB down, timeout) are handled at the application level and never enter the reasoning loop (AI-21a/AI-21b).
- **System prompts and tool descriptions live in external resource files**, not as string literals in Java classes. They're versioned with the code, and the version/hash used is stored on the `agent_run` record.
- **No PII in prompts.** Email, name, account ID, Telegram binding never go to the model. Free-text user input is treated as potentially identifying and normalized before being forwarded.

## Domain invariants (PRD section 6.1)

These must never be violated. Each one needs both a service-level check and a DB constraint or test — don't rely on just one:

- Pantry quantity is never negative (`CHECK` constraint + optimistic locking on normal ops, pessimistic locking on the cooking transaction)
- A consumed/discarded pantry item is never consumed again
- Cooking confirmation is idempotent
- Historical nutrition values never change retroactively after a meal is confirmed
- Target daily calories never go below `MIN_ALLOWED_DAILY_CALORIES` (a config constant, not a computed "safe minimum")

If a change touches any of these, flag it explicitly in your response, don't just make the change silently.

## Conventions

- Code comments, commit messages, class/method/variable names: **English only**. UI copy and bot responses will be localized later — that's a separate concern.
- Commit after each working, reviewed task — not after a whole feature. Small commits are how I keep the ability to roll back.
- Ask for a plan and my go-ahead only before touching architecture decisions or anything covered by the "Domain invariants" section above. For everything else — including business logic — just do it and summarize what you changed afterward. Don't wait for approval on routine work.

## What NOT to do

- Don't add dependencies not already in `pom.xml` without asking.
- Don't invent API endpoints or DB columns not in `docs/PRD.md` — flag the gap instead.
- Don't silently work around a failing test or constraint — surface it.
