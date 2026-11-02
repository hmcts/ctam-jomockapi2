# Specification Quality Checklist: People API — Search and Single Office Holder

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-11-02
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

- Three [NEEDS CLARIFICATION] markers remain, awaiting answers: the format `updated_since` accepts (FR-010), whether `include_previous_appointments` also filters authorisations and judiciary roles (FR-021), and what an authorisation's `jurisdiction_id`/`jurisdiction` refer to (FR-042).
- The description's other open points were settled with documented defaults (Assumptions): `400` for a malformed `id` and `404` for an unknown one, an inclusive `updated_since`, no `per_page` maximum, `judiciary_role_name_id` as the judiciary-role reference, `role_name_id` as the appointment-title reference, and `court_name`/`court_type`/`location`/`type` derived from the base location.
- As in 002 to 012, the spec names the API's own routes, field names and parameters, the shared error shape, the example request collection and the generated API document. These are the contract under test and constitution requirements (Principles XVII and XVIII), not implementation choices. FR-018's "one reusable component" restates Principles III and VI.
