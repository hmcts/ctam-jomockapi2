# Specification Quality Checklist: Reference Data API — Judiciary Roles

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-10-27
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

- Items marked incomplete require spec updates before `/speckit-clarify` or `/speckit-plan`
- No `[NEEDS CLARIFICATION]` markers were needed. The description excludes deprecated versions, so `judiciary_role` is not served (FR-001); it settles the ended-record question (002's rule applies, the extract's end dates don't); and it leaves the choice between `judiciary_role_id` and `judiciary_role_name_id` to the People feature (DF-4).
- Like 002–005, the spec names the project's own artefacts (routes, the type guard, the coverage gate), because the "users" are integrators and maintainers of an API mock and the constitution requires those artefacts. This is treated as scope, not implementation leakage.
- Every count and reference point was checked against the local extract and the checked-in fixtures: 164 rows, 164 distinct names, 7 with non-ASCII characters, none with leading or trailing whitespace, ids 90000–90163, 14 end-dated rows (only "Senior Family Liaison Judge" is also ended by 002's rule), 23 ended positions (70–1610), a longest name of 50 characters, and the 13 Reference Points rows. The unsupported-name count (16) is the six canonical types not yet served, their six singular forms, and `base_location`, `contract_type`, `gender` and `judiciary_role`.
- The description's example "…(King’s Bench) sorts before …(Queen's Bench)" is true but is decided by K before Q, not by `’`. No two neighbouring names in this dataset are ordered by `’` or `–`, so EC-008 says so and uses an upper/lower-case example to show where code-point order differs from alphabetical order.
