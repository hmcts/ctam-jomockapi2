# Feature Specification: Reference Data Lookup by Type and ID

**Feature Branch**: `002-reference-data-lookup`

**Created**: 2026-09-15

**Status**: Draft

**Input**: User description: "I want to come up with the api /api/v5/reference_data/{attribute_name}/{reference_id}. The description of this is present in the file joh-elinks-api/swagger-ui-elinks-api-v5.pdf. Reference data is present in the folder ReferenceData"

## Clarifications

### Session 2026-09-15

- Q: Should a lookup by ID still succeed and return the record when today's date falls outside that record's `start_date`/`end_date` validity window (i.e. an expired or not-yet-active reference entry)? → A: Always return the record regardless of `start_date`/`end_date` — no date filtering on this lookup.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Retrieve a single reference-data record by type and ID (Priority: P1)

A consumer that already holds a reference-data identifier (for example, a
`jurisdiction_id` or `ticket_id` returned as part of a person's or appointment's record)
needs to resolve that identifier into the full reference-data record it points to,
without downloading and searching through the entire reference-data collection for that
type.

**Why this priority**: This is the entire purpose of the endpoint and the only scenario
that must work for the feature to have any value. Every other scenario is a variation or
safeguard around this core lookup.

**Independent Test**: Can be fully tested by requesting a known, valid combination of
attribute type and record ID (e.g. `jurisdictions` / `30`) and verifying the response
contains exactly the one matching record with its correct fields, delivering value on
its own as a direct-lookup capability independent of any other endpoint.

**Acceptance Scenarios**:

1. **Given** a valid reference-data type (e.g. `jurisdictions`) and a record ID that
   exists within that type's dataset (e.g. `30`), **When** a client requests that
   type/ID combination, **Then** the system returns a success response containing the
   single matching record with all of its fields as held in the seed dataset (e.g. `id`,
   `name`, applicable dates, and any type-specific relationships).
2. **Given** a valid reference-data type, **When** a client requests a record ID that
   does not exist within that type's dataset, **Then** the system returns a "not found"
   response rather than an empty or partial success response.
3. **Given** a request that supplies valid authentication, **When** the client requests
   any supported reference-data type and a valid ID, **Then** the system returns the
   record without additional restriction.
4. **Given** a request that omits authentication or supplies invalid credentials,
   **When** the client requests any reference-data type and ID, **Then** the system
   returns an unauthorized response and does not disclose the record.

---

### User Story 2 - Look up a record using a deprecated (legacy) type name (Priority: P2)

A consumer that has not yet migrated to the current reference-data type names needs the
older, singular type names (e.g. `jurisdiction` instead of `jurisdictions`) to keep
working and return the same record as the current name, so its integration does not
break while it migrates at its own pace.

**Why this priority**: Backward compatibility is explicitly called out in the source
specification and protects existing integrations, but it is a compatibility safeguard
around the core lookup (User Story 1) rather than the primary value driver.

**Independent Test**: Can be fully tested by requesting the same record ID once using a
current type name and once using its deprecated equivalent, and verifying both requests
return an identical record, independent of any other scenario.

**Acceptance Scenarios**:

1. **Given** a record that can be retrieved using a current type name (e.g.
   `ticket_categories`), **When** the same record ID is requested using the
   corresponding deprecated type name (e.g. `ticket_category`), **Then** the system
   returns the identical record.
2. **Given** a deprecated type name that has no current equivalent supported by the
   system, **When** a client requests it, **Then** the system treats it the same as any
   other unsupported type name (see User Story 3).

---

### User Story 3 - Receive predictable errors for unsupported types or unknown IDs (Priority: P3)

An engineer integrating against the mock, or an automated test suite validating error
handling, needs the endpoint to fail predictably and distinguishably when given a
reference-data type that does not exist or a record ID that is not present, so that
error-handling code can be developed and tested with confidence.

**Why this priority**: Correct error behaviour matters for building trustworthy
integrations and tests, but it only has meaning once the core lookup (User Story 1)
exists to fail against.

**Independent Test**: Can be fully tested by issuing requests with a nonsensical type
name and with a well-formed but non-existent record ID, and verifying each produces a
clear, non-2xx response distinct from a successful lookup.

**Acceptance Scenarios**:

1. **Given** a type name that is neither a current nor a deprecated supported value,
   **When** a client requests it with any ID, **Then** the system returns a "not found"
   response.
2. **Given** a valid type name and a record ID that is not a well-formed identifier
   (e.g. non-numeric), **When** a client requests it, **Then** the system returns a
   response indicating the request itself was invalid.

---

### Edge Cases

- What happens when the same numeric ID exists in two different reference-data types
  (e.g. ID `1` in both `genders` and `locations`)? The system MUST scope the lookup to
  the requested type only and never return a record from a different type.
- What happens when a reference-data record has no `end_date` (i.e. it is still
  currently active)? The system MUST return the record with that field present and
  empty/absent, not omit the record or the field entirely.
- What happens when a reference-data record's `start_date`/`end_date` window does not
  cover today's date (i.e. it is expired or not yet active)? The system MUST still
  return the record on a direct ID lookup — this endpoint does not filter results by
  date validity.
- What happens when a client requests a deprecated type name that maps to a current type
  with no matching record for the given ID? The system MUST respond exactly as it would
  for the current type name in the same situation (a "not found" response).
- What happens when an unsupported HTTP method is used against this path? The system
  MUST respond with a Method Not Allowed response, consistent with how the mock handles
  method mismatches elsewhere.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose an endpoint that returns a single reference-data
  record, identified by a reference-data type and a record ID, mirroring the path,
  method, and documented behaviour of the corresponding operation in the E-Links API
  specification (`joh-elinks-api/swagger-ui-elinks-api-v5.pdf`).
- **FR-002**: The system MUST support looking up records for every reference-data type
  documented in the source specification: appointment titles, base locations, contract
  types, genders, judiciary roles, jurisdictions, location types, locations, ticket
  categories, ticket category types, and tickets.
- **FR-003**: The system MUST also accept the deprecated (legacy) singular name for each
  reference-data type listed in FR-002 and treat it as equivalent to its current name for
  lookup purposes.
- **FR-004**: The system MUST require both the reference-data type and the record ID to
  be supplied; a request missing either MUST NOT be treated as a valid lookup.
- **FR-005**: On a successful lookup, the system MUST return exactly one record — the
  one whose ID matches the requested value within the requested type's dataset — including
  all fields held for that record in the seed reference data (not limited to a fixed,
  minimal set of fields), and MUST NOT return records from any other type.
- **FR-006**: When the requested record ID does not exist within the requested (or
  equivalent deprecated) type's dataset, the system MUST return a "not found" response
  rather than a success response with empty or missing data.
- **FR-007**: When the requested reference-data type is neither a current nor a
  deprecated supported value, the system MUST return a "not found" response.
- **FR-008**: When the requested record ID is not a well-formed identifier for this
  endpoint (e.g. not numeric), the system MUST return a response indicating the request
  was invalid, distinct from a "not found" response.
- **FR-009**: The system MUST require the caller to present valid authentication to use
  this endpoint; requests with missing or invalid credentials MUST receive an
  unauthorized response and MUST NOT receive record data, matching the source
  specification's authorization requirement for this operation.
- **FR-010**: The response for a given, valid type/ID combination MUST be identical in
  shape and content on every successful call for a given running instance — it MUST NOT
  vary between requests other than as a result of the underlying seed data changing.
- **FR-011**: Lookups against one reference-data type MUST have no effect on and MUST be
  independent of lookups against any other reference-data type or record.
- **FR-012**: The system MUST NOT filter a lookup's result based on the record's
  `start_date`/`end_date` validity window — a record whose window has expired or has not
  yet begun MUST still be returned successfully when its ID exists in the requested
  type's dataset.

### Key Entities

- **Reference Data Type**: A named category of reference data (e.g. jurisdictions,
  locations, tickets). Each type has one current (plural) name and one deprecated
  (singular) alias name that resolves to the same type. A type owns a collection of
  Reference Data Records.
- **Reference Data Record**: A single entry within one Reference Data Type, identified
  by a numeric ID unique within that type. Holds descriptive and relationship fields
  appropriate to its type (for example: a name, an applicable jurisdiction or parent
  reference, an effective start date, an optional end date, and creation/last-updated
  timestamps), sourced from the project's seed reference-data extracts. The start/end
  dates are informational fields on the record only — they are never used to filter
  whether a direct ID lookup succeeds (see Clarifications).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A consumer holding a known reference-data type and record ID can retrieve
  the exact matching record in a single request, for all 11 supported reference-data
  types, 100% of the time the ID exists in that type's dataset.
- **SC-002**: A consumer using a deprecated type name receives a result identical to
  using the corresponding current type name in 100% of cases, enabling migration with
  zero behavioural difference.
- **SC-003**: An automated test suite can reliably distinguish, from the response alone,
  between a successful lookup, an unknown type or ID, an invalid ID format, and an
  unauthorized request — with no overlap between these outcomes.
- **SC-004**: The endpoint responds quickly enough (well under one second) to be used
  for interactive, one-record-at-a-time lookups without a noticeable delay to the caller.

## Assumptions

- The reference data seeded into the mock is drawn from the extracts under
  `joh-elinks-api/ReferenceData/`, which cover exactly the 11 reference-data types listed
  in FR-002, confirming the source specification's documented list of supported
  `attribute_name` values is complete and matches the available seed data.
- The source specification's example response (`id`, `updated_at`, `created_at`,
  `start_date`, `end_date`) is illustrative rather than an exhaustive schema; different
  reference-data types carry additional fields in the seed data (e.g. a `name`, a
  `jurisdiction_id`, a `parent_id`, a `type_id`, or a `salaried` flag), and this feature
  returns all fields present for the requested record's type.
- Where the source specification does not explicitly document a status code (unknown
  type, unknown ID, malformed ID), this feature follows standard, predictable REST
  conventions: "not found" for an unknown type or unknown ID, and "invalid request" for
  a malformed ID — consistent with how a single-record lookup is expected to behave.
- Authentication for this endpoint is simulated by the mock rather than delegated to a
  real identity provider, consistent with this project's overall approach to
  authentication; this feature only specifies that valid credentials are required and
  enforced, not how credentials are issued or validated.
- Reference-data type names and their deprecated aliases are treated as fixed, known
  values (the 11 current names and their 11 deprecated equivalents); this feature does
  not need to support arbitrary or newly-introduced type names beyond those documented.
- Per project-wide governance (constitution v1.1.0), every error response this endpoint
  produces (400, 401, 404) uses one shared, project-wide error body shape (a message and
  a request-correlation identifier), and every response — successful or not — carries a
  correlation identifier as a response header. This does not add a new distinguishable
  outcome beyond those in SC-003 — it defines what the existing error bodies contain.
