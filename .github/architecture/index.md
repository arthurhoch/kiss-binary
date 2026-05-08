# Architecture Index

This directory contains the authoritative architecture documentation for KissBinary. These documents define the rules and boundaries for all future implementation.

## Reading Order

AI agents and contributors must read these files in order:

1. [01-product-boundaries.md](01-product-boundaries.md) — What KissBinary is and is not.
2. [02-core-abstractions.md](02-core-abstractions.md) — Planned public API types and why they exist.
3. [03-binary-format-rules.md](03-binary-format-rules.md) — Rules for designing and reading binary formats.
4. [04-performance-rules.md](04-performance-rules.md) — Hot path principles and benchmark requirements.
5. [05-error-handling-rules.md](05-error-handling-rules.md) — Error message rules and exception hierarchy.
6. [06-testing-and-benchmarking.md](06-testing-and-benchmarking.md) — Test categories and JMH benchmark plan.
7. [07-release-readiness.md](07-release-readiness.md) — Release readiness gates and checklists.

## Authority Rule

Architecture docs are authoritative for implementation. If code conflicts with architecture docs, the architecture docs win until they are explicitly updated by a human.

## Cross-References

- Product specification: `PRODUCT_SPEC.md`
- API design: `API_DESIGN.md`
- Implementation plan: `IMPLEMENTATION_PLAN.md`
- Performance goals: `PERFORMANCE.md`
- Error handling model: `ERROR_HANDLING.md`
- AI agent instructions: `AGENTS.md`
- Compact summary: `CAVEMAN.md`

## Maintenance

When adding a new architecture document:

1. Add it to this index with the correct number and description.
2. Follow the naming convention: `NN-topic.md` where `NN` is a two-digit sequence number.
3. Keep each document focused on one topic.
