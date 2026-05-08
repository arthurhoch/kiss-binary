---
layout: default
---

# AI Usage Guide

This guide explains how to use AI coding agents safely with the KissBinary repository.

## Before You Start

AI agents working on this repository must read these files in order:

1. `CAVEMAN.md` — compact summary (read first).
2. `AGENTS.md` — authoritative rules.
3. `PRODUCT_SPEC.md` — product specification.
4. `IMPLEMENTATION_PLAN.md` — implementation phases.
5. `API_DESIGN.md` — public API shape.
6. `.github/architecture/index.md` — architecture reading order.

CAVEMAN.md is a summary only. If it conflicts with detailed docs, the detailed docs win.

## Current Status

**Initial implementation complete.** Java source code and tests now exist.

Do not implement new code unless explicitly instructed by a human.

## Rules for AI Agents

1. **Do not implement code** unless explicitly instructed.
2. **Do not add dependencies** without human approval.
3. **Do not create frameworks** or abstraction layers.
4. **Do not use reflection** for data mapping.
5. **Do not serialize Java objects.**
6. **Do not add schema engines** or IDL support.
7. **Do not skip documentation updates** when design changes.
8. **Do not claim features work** if they are not implemented.
9. **Prefer the simpler solution** when in doubt.
10. **Preserve Java 17 compatibility.**

## Safe Operations

These operations are safe for AI agents to perform without explicit instruction:

- Fix typos in documentation.
- Update cross-references between documents.
- Add missing content that is clearly implied by existing documents.

## Unsafe Operations

These operations require explicit human approval:

- Creating Java source files.
- Creating test files.
- Creating build configuration (`pom.xml`).
- Creating CI workflows.
- Adding any dependency.
- Changing the public API shape.
- Changing the project scope.

## Change Protocol

1. Read required docs before starting.
2. Make the smallest correct change.
3. Update documentation for any design change.
4. Report what changed and what remains.

## Architecture Authority

Architecture docs in `.github/architecture/` are authoritative for implementation. If code conflicts with architecture docs, the architecture docs win until they are explicitly updated by a human.
