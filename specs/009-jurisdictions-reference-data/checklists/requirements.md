# Specification Quality Checklist: Reference Data API — Jurisdictions

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-10-29
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- As in 002–008, the spec names HTTP routes, status codes, field names and the generated API documentation: these are the contract being mocked, not implementation choices. The feature description named specific test classes; the spec states the required checks as behaviour (FR-011, FR-012, FR-017, FR-018) and leaves where they live to the plan.
- The description's facts were checked against the local extract and the shipped fixtures before writing: a JSON object with a `results` array of 5 entries, ASCII, no byte-order mark; ids 30, 34, 84, 86, 87; 5 distinct names; no end dates. Every `jurisdiction_id` in the shipped locations (2,000), base locations (1,462) and location types (16) is one of 30, 34, 84 and 86, none null; 87 is unreferenced. Ids 1, 31, 88 and 999 are unknown under `jurisdictions`.
- Main code mentions "jurisdiction" in 12 places: 11 are spellings of the generic field `jurisdiction_id` (`JURISDICTION_ID`, `jurisdictionId`, `getJurisdictionId`), and one is the field's OpenAPI description ("Id of the record's jurisdiction"). That is why FR-018 needs a decision.
- FR-018's clarification is resolved (2026-10-29, option A): the guard gains the plural fragment `jurisdictions`, which matches nothing in main code today (checked: 0 matches, against 14 for the singular), with no exemption and no main-code change.
