# Research: People API — Search and Single Office Holder

**Feature**: `013-people-api` | **Date**: 2026-11-02 | **Spec**: [spec.md](./spec.md)

The spec has no open markers after `/speckit-clarify`. Each decision below settles a design question the spec leaves to planning. Most follow from the existing code (002 to 012) and the constitution; R1 records your answer to the one question raised during planning.

---

## R1. Scope of the type-name guard's People exception

**Decision**: `NoTypeSpecificCodeTest` gains a reviewed list of People main-source files (`PEOPLE_SOURCES`, the files tagged *guard-listed* in plan.md § Source Code) and an allowlist of contract names (`PEOPLE_CONTRACT_NAMES`, FR-052). For a listed file the guard:

1. **Fails on non-contract type names first.** It fails if the file contains, in any spelling the existing `spellingsOf` matches, a canonical type name that is not a contract name: `appointment_titles`, `base_locations`, `contract_types`, `genders`, `locations`, `location_types`, `jurisdictions`, `ticket_categories`, `ticket_category_types`. `judiciary_roles` and `tickets` are contract field names as well as type names, so they're allowed.
2. **Then masks contract names.** It splits each identifier and comment into words, on snake_case, camelCase/PascalCase humps, spaces and hyphens. Any run of consecutive words equal to an allowlisted contract name is blanked, wherever it sits in the identifier. So `getAppointments`, `ResolvedAppointment`, `AppointmentStatus`, `currentAppointments`, `baseLocationTypeId`, `AppointmentsResponse`, `JudiciaryRoleResponse` and "an appointment's end date" are masked. This is safe because step 1 has already failed any non-contract type name (`AppointmentTitles`, `baseLocations`, `locationTypes`, …) before masking.
3. **Runs the existing fragment scan on what remains.** Any other match fails, for example `AppointmentTitle`, `myLocation` or `GenderHelper`.

Every other main source is scanned exactly as today. The guard also fails if a listed file doesn't exist. Its own tests show that each kind of spelling is masked, that every non-contract type name in a listed file still fails, and that a file outside the list gets no exemption.

Which reference-data type each People field refers to lives in `application.yml` (`jo.people.reference-types`, R4). Java code never names a type.

**Rationale**: The People contract's own field names (`appointments`, `base_location_id`, `gender`, `tickets`, …) contain the guard's fragments. They are needed in the controller (the `include_previous_appointments` parameter), the service (filtering appointments, joining authorisations on `appointment_id`), the fixture entities, the start-up validator and the properties, not only in the DTOs. Masking contract-name word runs, after the non-contract type names have been rejected, keeps the guard's purpose: no People source can quietly start serving or naming a reference-data type. Checking the non-contract type names before masking closes the gap where masking `appointment` alone would let "appointment titles" through. The singular `judiciary_role` is a contract name because the Swagger names the schema `JudiciaryRoleResponse`.

**Alternatives considered**: An exception for the response models only (the spec's first wording); this would have forced neutral internal names (for example "posts") and an exempt mapper, which hurts Principle XII. Not scanning People code at all; this loses the protection. A per-file allowlist of exact identifiers; there are too many accessor forms (Lombok getters, builders) to list by hand.

---

## R2. Fixture shape: ids in the fixture, names derived at start-up

**Decision**: The People fixture stores only the **reference ids** and the person's own synthetic values. Every name taken from reference data is derived at start-up from the served reference data, once, through the configured types (R4). That covers `sex`, `gender`, `role_name`, `contract_type`, `court_name`, `base_location`, `court_type`, `location`, `type`, judiciary-role `name`, `ticket`, and the authorisation's `jurisdiction` and `jurisdiction_id`. The summaries (`authorisations`, `authorisations_current`) are computed per request from the dated authorisations the response carries (FR-044, R7).

Derivations (data-model.md § 3):

| Response field | Derived from |
|---|---|
| `sex`, `gender` | `gender_id` → gender `name` (`gender` is written only when the record's `show_gender` is `true`) |
| `role_name` | `role_name_id` → appointment title `name` |
| `contract_type` | `contract_type_id` → contract type `name` |
| `base_location`, `court_name` | `base_location_id` → base location `name` |
| `court_type` | that base location's `type_id` → location type `name` |
| `location` | that base location's `parent_id` → location `name` |
| `type` | that base location's `jurisdiction_id` → jurisdiction `name` |
| judiciary role `name` | `judiciary_role_name_id` → judiciary role `name` |
| `ticket` | `ticket_id` → ticket `name` |
| authorisation `jurisdiction_id`, `jurisdiction` | that ticket's `ticket_category_id` → ticket category's `jurisdiction_id` → jurisdiction `id` and `name` (FR-042) |

**Rationale**: A name in the response can't disagree with the record it names, because it's read from that record (SC-004). The fixture can't drift if a reference name is ever corrected. The validator then only has to check that ids resolve, not that names match. The independent references test (R10) still compares every served name with the reference-data records, so FR-045 is checked from outside the code that derives them.

**Alternatives considered**: A denormalised fixture that looks like the response, with the validator checking every name. That means the same join logic plus a check for each field, and a larger fixture where 13 names per appointment could drift.

---

## R3. Generating the People fixture

**Decision**: `src/main/resources/people/office_holders.json` (120 records) is generated **once** during implementation by a throwaway script, following data-model.md § 4, and checked in. The script is not committed, as in 002 to 012. A new unit test, `PeopleFixtureRulesTest`, checks every generation rule against the checked-in file: the counts and coverage of FR-031, FR-035 and FR-036, the ranges of FR-032 and FR-033, the date rules of FR-034 and the formats of data-model.md § 4. So the file is verified on every build however it was produced.

Synthetic personal values come from small fixed lists: 40 forenames and 60 surnames, everyday words chosen so no combination is meant to resemble a real office holder (data-model.md § 4). `per_id` values run from 900001 and `personal_code` values from 9100001. Both ranges are distinctive and stable.

**Rationale**: This matches how every earlier fixture was produced and verified (fixture rules test plus checked-in data; Principle V). No extract exists for People (FR-030), so there's nothing to keep out of the repository.

**Alternatives considered**: Generating at start-up with a seeded generator. This is deterministic, but the data is invisible in review, the golden files would depend on generator code, and a swapped fixture (Principle X) would mean swapping code. Committing the generator: it would be run once, and the rules test is what guards the data.

---

## R4. Configuration

**Decision**: A new `jo.people` block, bound by `config/PeopleProperties` (`@Validated`):

```yaml
jo:
  people:
    fixture: classpath:people/office_holders.json
    # The date that decides "current" (D-8). Explicit placeholder: relaxed binding would expect JO_PEOPLE_REFERENCEDATE.
    reference-date: ${JO_PEOPLE_REFERENCE_DATE:2026-01-01}
    # Which served reference-data type each People reference names (FR-052: never in Java).
    reference-types:
      gender-id: genders
      role-name-id: appointment_titles
      contract-type-id: contract_types
      base-location-id: base_locations
      base-location-type-id: location_types
      base-location-parent-id: locations
      jurisdiction-id: jurisdictions
      judiciary-role-name-id: judiciary_roles
      ticket-id: tickets
      ticket-category-id: ticket_categories
```

Each `reference-types` value must be a configured canonical type name. The type that a chained field is read from must declare the field: `base_locations` must declare `type_id`, `parent_id` and `jurisdiction_id`, `tickets` must declare `ticket_category_id`, and `ticket_categories` must declare `jurisdiction_id`. Both rules are checked at start-up (R6). The properties' Java field names (`genderId`, `baseLocationTypeId`, …) are masked contract names under R1. `fixture` and `reference-date` are required.

**Rationale**: Principle X (fixtures and scenarios configurable) and FR-052 (type names never in Java). The explicit placeholder follows the bearer-token precedent in `application.yml`.

**Alternatives considered**: Hard-coding the type names in a People-specific Java constant class; FR-052 forbids it. A `Map<String, String>` of field to type: untyped, so a misspelled key would be ignored silently.

---

## R5. Query-parameter handling and check order

**Decision**:

- **Unknown and repeated parameters**: a new generic `filters/DeclaredQueryParametersInterceptor` reads the matched `HandlerMethod`'s `@RequestParam` names. It rejects any query parameter the method doesn't declare (`UndefinedQueryParameterException`, "Unsupported query parameter.") and any parameter given more than once (`RepeatedQueryParameterException`, "Query parameters must not be repeated."). The repeated-parameter check runs first, so `foo=1&foo=2` reports the repeat. `WebConfig` registers it for `ApiPaths.PEOPLE` and `ApiPaths.PEOPLE + "/**"`. It names no route itself, so the leavers and deleted features only add their path. The reference-data `NoQueryParametersInterceptor` and its message are unchanged (FR-050).
- **Values**: the controller binds every parameter as an optional `String` (`@RequestParam(required = false)`), so Spring's own conversion and missing-parameter errors, which have their own messages, never fire. A new generic `services/QueryParameterParser` turns them into values with fixed messages (`InvalidQueryParameterException`; the message is built from the parameter's name constant, never from caller input):
  - `requiredDate(name, raw)`: absent or empty gives "`<name>` is required."; anything not matching `^[0-9]{4}-[0-9]{2}-[0-9]{2}$`, or not a real date under `ResolverStyle.STRICT`, gives "`<name>` must be a valid date.";
  - `positiveWholeNumber(name, raw, default)`: absent gives the default; otherwise the value must match `^[0-9]+$`, parse as `long` (a longer number fails) and be greater than 0, else "`<name>` must be a positive whole number.";
  - `flag(name, raw)`: absent gives `false`; exactly `true` or `false`, else "`<name>` must be true or false.".
  
  `updated_since` is documented as required with `@Parameter(required = true)` even though it's bound as optional; the OpenAPI contract test asserts this.
- **Order**: authentication (filter) → route and method (Spring MVC) → repeated, then undefined, parameters (interceptor) → values in the contract's listing order: `updated_since`, `page`, `per_page`, `include_previous_appointments` (service) → `id` format → lookup. This is EC-010; the first failure decides the response.
- **`id`**: a new `services/PersonIdParser` accepts `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$` and lower-cases it (EC-005). Anything else gives `InvalidPersonIdException` ("id must be a UUID."). `UUID.fromString` is not used because it accepts non-canonical forms such as `1-1-1-1-1`.

**Rationale**: Principles III and VI. The parser and the interceptor are the shared validation component the leavers (`left_since`) and deleted (`deleted_since`) features will reuse unchanged. Binding as strings keeps every message fixed and the check order under our control.

**Alternatives considered**: Spring `@RequestParam` with typed binding and `@Positive`: it gives framework messages, a fixed order we can't control, and silently accepts repeats (joined with commas). Extending `NoQueryParametersInterceptor` with per-route sets: its message is part of 002's contract and must not change.

---

## R6. Start-up validation (FR-046)

**Decision**: `repository/FixturePeopleRepository` loads the fixture in its constructor, as `FixtureReferenceDataRepository` does, so an invalid fixture stops the context. Loading has three steps:

1. **Strict JSON**: the strict-coercion `JsonMapper` that `FixtureReferenceDataRepository` builds inline moves to a shared `repository/StrictFixtureJson` factory. The reference repository uses it unchanged, with byte-for-byte the same behaviour, which its existing tests prove. Unknown properties fail. A required key that is missing fails, even when its value may be `null`; this is checked on the JSON tree, as 007 does for nullable reference fields.
2. **Rules**: `repository/PeopleDatasetValidator` checks the rules in data-model.md § 5, which covers FR-023's shape, FR-032, FR-033, FR-034 and FR-037 to FR-044. It stops at the first failure and throws `IllegalStateException("Invalid People fixture <location>: <rule> (office holder <id> | record at position <n>)")`. Before any record is checked it confirms the configured reference types (R4). With an empty fixture there is nothing to resolve, so no type is required (EC-013).
3. **Assembly**: the resolved names (R2) are attached once, and every list is put in its FR-025 order. The repository then holds an immutable list of office holders in FR-012 order, plus a map from `id` to office holder.

FR-035 and FR-036 describe the default dataset only, so `PeopleFixtureRulesTest` checks them and start-up doesn't.

**Rationale**: This is the same fail-fast pattern and failure-message style as reference data. Moving the mapper into one shared factory is required by Principle III and changes nothing that can be observed.

**Alternatives considered**: An `ApplicationRunner` that validates after start-up. The service would already be listening, so requests could be served from an unchecked dataset. Bean Validation annotations on the entities: they can't express references between records, so a second mechanism would still be needed.

---

## R7. "Current", the flag, and the summaries

**Decision**: A new `services/AppointmentStatus` component holds the configured reference date. An appointment or a dated authorisation is current when its `end_date` is `null` or on or after that date (FR-020). Per request, `PeopleService` builds each office holder's view with `PeopleMapper`:

- flag `false`: `appointments` holds the current ones, and `authorisations_with_dates` holds those whose `appointment_id` names a current appointment;
- flag `true`: both lists are complete;
- `authorisations` is computed from the `authorisations_with_dates` written in this response, and `authorisations_current` from the office holder's current dated authorisations, whatever the flag's value (FR-021, FR-044);
- the summary grouping (`services/AuthorisationSummaries`): one entry per distinct `jurisdiction`, with its ticket names de-duplicated; entries in ascending jurisdiction name, and tickets in ascending name, both by `String.compareTo` (FR-025);
- `judiciary_roles` doesn't depend on the flag.

Because an authorisation's dates lie within its appointment's (FR-043), an authorisation under a previous appointment is never current. So `authorisations_current` never names a ticket that is missing from the response; data-model.md § 5 adds a rule that makes this hold for any fixture.

**Rationale**: The clarified FR-021. The reference date is never read from `Clock` (D-8). A test starts the context with a `Clock` set years ahead and gets identical responses (SC-003).

**Alternatives considered**: Building both flag variants at start-up: twice the memory for no measurable gain at 120 people (R11).

---

## R8. Pagination component

**Decision**: These are generic and never mention People:

- `domain/PaginationResponse` (record): `results`, `pages`, `current_page`, `results_per_page` (all `long`, OpenAPI `int64`) and `more_pages` (`boolean`), all required, in that order.
- `services/PageRequest` (record): `page` and `perPage`, both ≥ 1. The defaults (`1`, `50`) are constants here, so leavers and deleted share them.
- `services/Paginator`: `<T> Page<T> paginate(List<T> ordered, PageRequest request)`, where `services/Page<T>` holds `List<T> results` and `PaginationResponse pagination`.
  - `results` is the size of the list;
  - `pages` is ⌈results ÷ perPage⌉, computed as `results / perPage + (results % perPage == 0 ? 0 : 1)` so it can't overflow;
  - `more_pages` is `page < pages`;
  - the slice is empty when `page - 1 >= pages`, otherwise it starts at `(page - 1) * perPage`. That product is only computed when `page ≤ pages`, so it's below the list size and can't overflow.

`PeopleApiResponse(results, pagination)` is the People-specific wrapper; the later features add their own wrappers around the same `PaginationResponse`.

**Rationale**: FR-017 to FR-019 and Principle VI ("a single reusable pagination component"). Using `long` throughout matches FR-013's 64-bit bound with no overflow paths.

**Alternatives considered**: Spring Data's `Pageable` and `Page`: zero-based pages, a different JSON shape, and a new dependency. An `int` page size, which would need a lower bound the spec doesn't set.

---

## R9. Response models and OpenAPI

**Decision**: These are Java records in `domain/`, with `@JsonPropertyOrder` in the Swagger's field order and `@Schema` for descriptions, types, formats and `requiredMode`/`nullable`, as `ReferenceDataResponse` does:

- the five People DTOs: `CurrentOfficeHolderResponse`, `AppointmentsResponse`, `JudiciaryRoleResponse`, `AuthorisationSummaryResponse` and `AuthorisationsResponse`;
- `PeopleApiResponse` and `PaginationResponse`.

Field rules:
- Optional fields (`gender`, `email_personal`, `circuit`, `bench`, `advisory_committee_area`) use `@JsonInclude(NON_NULL)` and are left out when unset (EC-009).
- Required nullable fields are always written, `null` included: `post_nominals`, `work_phone`, `retirement_date`, `leaving_on`, and every `end_date`.
- Dates are `LocalDate`, written as `YYYY-MM-DD` with schema `format: date`. `id` is a `String` with `format: uuid`.

`PeopleController` documents both operations with their own `400` and `404` responses and examples, under a new "People" tag. `OpenApiConfig`'s customiser, which today adds the correlation header, the response headers and the shared `401`/`406`/`500` to reference-data operations only, now also applies to operations under `ApiPaths.PEOPLE`. The reference-data part of the document stays byte-for-byte the same, which the existing contract test checks.

**Rationale**: Principles VIII and XVIII, using the conventions already established. One customiser keeps the shared error documentation in one place (Principle III).

**Alternatives considered**: A second customiser just for People, which would duplicate the shared error responses.

---

## R10. Test plan

**Decision**: These go in the existing four suites. "New" marks a new class; the rest extend existing ones.

**Unit** (`src/test`):
- `PaginatorTest` (new): property-style table over N in {0, 1, 49, 50, 51, 120} × P in {1, 7, 50, 120, `Long.MAX_VALUE`} × every page plus one past the last, so AC-007, AC-008, AC-009 and SC-002 are checked at component level.
- `QueryParameterParserTest` (new): every valid and invalid class in FR-010, FR-013 and FR-014, including `2025-02-30`, `2025-06-01T00:00:00Z`, `007`, `+1`, `1.0`, ` 1`, the empty value and 2^63.
- `PersonIdParserTest` (new).
- `DeclaredQueryParametersInterceptorTest` (new).
- `AppointmentStatusTest` (new): the boundary day.
- `AuthorisationSummariesTest` (new).
- `PeopleServiceTest` (new): the filter, the order, ties, the flag matrix, and that the flag doesn't change the pagination (AC-017).
- `PeopleMapperTest` (new).
- `FixturePeopleRepositoryTest` (new): every data-model.md § 5 rule fails start-up with its message, using small fixtures in `src/test/resources/people/`, plus the empty fixture.
- `PeopleControllerTest` (new, `@WebMvcTest`): routing, binding, `405`, `406` and trailing slashes.
- `GlobalExceptionHandlerTest`: the new exceptions.
- `PeopleFixtureRulesTest` (new): R3.
- `PeopleReferencesTest` (new): FR-045. For every office holder and every flag value, each served name and id equals the reference-data record found through `ReferenceDataRepository`, a path independent of the derivation code.
- `NoTypeSpecificCodeTest`: R1.
- `OpenApiConfigTest`: People paths get the shared responses.

**Integration** (`src/integrationTest`):
- `PeopleOpenApiContractTest` (new): every parameter, required flag, type, format, nullable flag and status of the spec; and that actual responses match the document's schemas, checked by a hand-rolled walk (no validation library).
- `PeopleStartupIntegrationTest` (new): an invalid configured fixture stops the context with the documented message; an empty fixture starts; a missing reference type fails.
- `PeopleClockIndependenceIntegrationTest` (new): SC-003, with a `Clock` set years ahead.
- `InternalFailureIntegrationTest`: a People route returns `500` (EC-011).
- `ReferenceDataExtensionIntegrationTest`: sets `jo.people.fixture` to an empty fixture, because it replaces the reference types (EC-013).
- `ReferenceDataOpenApiContractTest`: unchanged, and must still pass.

**Functional** (`src/functionalTest`):
- `PeopleFunctionalTest` (new): AC-001 to AC-030 over real HTTP, using a `PeopleHttp` helper next to `ReferenceDataHttp`.
- Golden files: `golden/people_all_with_previous.json` (`updated_since=2000-01-01&per_page=120&include_previous_appointments=true`), `golden/people_default_page1.json` and `golden/person_single.json`.
- The reference-data golden files are unchanged (AC-031).

**Smoke** (`src/smokeTest`): `PeopleSmokeTest` (new): the three SC-009 requests, each asserting `200`, with the same p95 harness as `ReferenceDataSmokeTest` (default bound 1 s; 100 ms with `-Dperf.strict=true`).

**Rationale**: Principle XI. Each acceptance scenario runs at least once over HTTP, and the components are checked exhaustively where that's cheap.

---

## R11. Performance

**Decision**: There is no special handling. Searches run over an in-memory, pre-sorted list of 120 office holders, filtered by a linear scan and mapped per request. At `per_page=120` with the flag on, the response is roughly 300 to 400 KB of JSON, which fits the smoke suite's 100 ms strict p95 on a laptop JVM. If measurement says otherwise, the first fix is caching the two mapped views per office holder, which changes no observable behaviour.

**Rationale**: Principle XII. The data is small and fixed.

---

## R12. Postman, documentation and closing deferred items

**Decision**:
- **Postman**: a "People" folder (FR-054, AC-034) with these requests, all using `{{baseUrl}}`, `{{bearerToken}}` and `{{invalidBearerToken}}`, with tests for status, structure and key values:
  - search with defaults; page 2; page 3 (the last); page 4 (past the last);
  - a filtered search (`updated_since={{peopleMidDate}}`) and a search with no matches;
  - `per_page=120`; flag `true`; flag `false`;
  - single office holder (`{{personId}}`); single office holder with the flag;
  - unknown `id`; malformed `id`;
  - missing, malformed and date-time `updated_since`; bad `page`; bad `per_page`; bad flag; unknown parameter; repeated parameter;
  - no token; wrong token.
  
  The environment gains `personId` and `peopleMidDate`. Tests that need a person read it from `{{personId}}` or from the search response, never a hard-coded value beyond the environment.
- **README**: a People section (routes, parameters, flag semantics and reference date, configuration, dataset rules, errors), and the list of closed deferred items.
- **CLAUDE.md**: a 013 entry in "Current repo state".
- **Earlier specs**: a closing note on 004 DF-2, 005 DF-3, 006 DF-4, 007 DF-6 and 010 DF-9 pointing at 013, as 011 did for 010 DF-8.
- **002's contract**: a pointer to `contracts/people-api.md`.

**Rationale**: FR-053 to FR-055 and Principles XVII and XVIII.
