# Data Model: People API — Search and Single Office Holder

**Feature**: `013-people-api` | **Date**: 2026-11-02 | **Spec**: [spec.md](./spec.md) | **Research**: [research.md](./research.md)

The response models are the contract (spec § Records); this document adds the fixture format, the derivations, the generation rules for the default dataset and the validation rules enforced at start-up.

## 1. Response models (API DTOs, `domain/`)

Exactly the spec's Records section, in its field order. The table records only what the plan adds.

| DTO | Java type notes |
|---|---|
| `PeopleApiResponse` | `List<CurrentOfficeHolderResponse> results`, `PaginationResponse pagination` |
| `PaginationResponse` | `long results`, `long pages`, `long current_page`, `long results_per_page`, `boolean more_pages`; all required (R8) |
| `CurrentOfficeHolderResponse` | `String id` (`format: uuid`), `LocalDate last_updated`, `long per_id`, `boolean disability`, `LocalDate retirement_date`/`leaving_on` (nullable), lists never null |
| `AppointmentsResponse` | `long` ids, `boolean is_principal`, `LocalDate start_date`, nullable `LocalDate end_date` |
| `JudiciaryRoleResponse` | `long judiciary_role_id`, `long judiciary_role_name_id`, nullable `end_date` |
| `AuthorisationSummaryResponse` | `String jurisdiction`, `List<String> tickets` |
| `AuthorisationsResponse` | `long` ids, nullable `end_date` |

- **Left out when unset** (`@JsonInclude(NON_NULL)`): `gender`, `email_personal`, `circuit`, `bench`, `advisory_committee_area`.
- **Always written, possibly `null`**: `post_nominals`, `work_phone`, `retirement_date`, `leaving_on`, every `end_date`.

## 2. Fixture records (`entity/`)

`src/main/resources/people/office_holders.json` is a JSON array of office holders. Every key listed here is **required** (a `null` value is allowed only where marked); unknown keys fail start-up.

**`OfficeHolderRecord`**

| Key | JSON type | Null? | Notes |
|---|---|---|---|
| `id` | string | no | Lower-case canonical UUID |
| `last_updated` | string date | no | |
| `per_id` | integer | no | |
| `personal_code` | string | no | Digits |
| `title`, `known_as`, `surname`, `fullname`, `initials` | string | no | |
| `post_nominals` | string | yes | |
| `email` | string | no | |
| `email_personal` | string | yes | `null` ⇒ left out of the response |
| `gender_id` | integer | no | → `jo.people.reference-types.gender-id` |
| `show_gender` | boolean | no | `true` ⇒ response writes `gender` (R2) |
| `work_phone` | string | yes | |
| `disability` | boolean | no | |
| `retirement_date`, `leaving_on` | string date | yes | |
| `appointments` | array of `AppointmentRecord` | no | Non-empty |
| `judiciary_roles` | array of `JudiciaryRoleRecord` | no | May be empty |
| `authorisations` | array of `AuthorisationRecord` | no | May be empty; the dated authorisations |

**`AppointmentRecord`**: `appointment_id` (integer), `role_name_id` (integer), `base_location_id` (integer), `contract_type_id` (integer), `is_principal` (boolean), `start_date` (date), `end_date` (date, nullable), `circuit`, `bench`, `advisory_committee_area` (string, nullable ⇒ left out).

**`JudiciaryRoleRecord`**: `judiciary_role_id`, `judiciary_role_name_id` (integers), `start_date` (date), `end_date` (date, nullable).

**`AuthorisationRecord`**: `authorisation_id`, `appointment_id`, `ticket_id` (integers), `start_date` (date), `end_date` (date, nullable).

Names, jurisdictions and summaries are not in the fixture (§ 3).

**`OfficeHolder`** (resolved form, built at start-up): the validated `OfficeHolderRecord` plus its derived names (§ 3), with nested `ResolvedAppointment` (adds `role_name`, `contract_type`, `base_location`/`court_name`, `court_type`, `location`, `type`), `ResolvedJudiciaryRole` (adds `name`) and `ResolvedAuthorisation` (adds `ticket`, `jurisdiction_id`, `jurisdiction`). Each list is in FR-025 order. Responses are mapped from this per request (§ 3, "Computed per request").

## 3. Derived values

Resolved once at start-up through `ReferenceDataRepository.findById`, using the types named in `jo.people.reference-types` (research R4):

| Response field | Rule |
|---|---|
| `sex` | name of `gender_id` (gender-id type) |
| `gender` | the same name, written only when `show_gender` is `true` |
| `role_name` | name of `role_name_id` (role-name-id type) |
| `contract_type` | name of `contract_type_id` (contract-type-id type) |
| `base_location`, `court_name` | name of `base_location_id` (base-location-id type) |
| `court_type` | name of that record's `type_id` (base-location-type-id type) |
| `location` | name of that record's `parent_id` (base-location-parent-id type) |
| `type` | name of that record's `jurisdiction_id` (jurisdiction-id type) |
| judiciary role `name` | name of `judiciary_role_name_id` (judiciary-role-name-id type) |
| `ticket` | name of `ticket_id` (ticket-id type) |
| authorisation `jurisdiction_id` | `jurisdiction_id` of that ticket's `ticket_category_id` record (ticket-category-id type) |
| authorisation `jurisdiction` | name of that `jurisdiction_id` (jurisdiction-id type) |

Computed per request (research R7), with `current` meaning `end_date` is `null` or ≥ `jo.people.reference-date`:

| Response field | Rule |
|---|---|
| `appointments` | flag `true`: all; otherwise the current ones |
| `authorisations_with_dates` | flag `true`: all; otherwise those whose `appointment_id` is a current appointment |
| `authorisations` | summary of the `authorisations_with_dates` in this response |
| `authorisations_current` | summary of the current dated authorisations (flag-independent) |
| `judiciary_roles` | all (flag-independent) |

Summary: group by `jurisdiction`; one `AuthorisationSummaryResponse` per jurisdiction in ascending name; `tickets` the distinct ticket names in ascending order (`String.compareTo`).

Ordering (FR-012, FR-025): office holders by (`last_updated`, `id`); appointments by (`start_date`, `appointment_id`); judiciary roles by (`start_date`, `judiciary_role_id`); dated authorisations by (`start_date`, `authorisation_id`). Applied at load, so the fixture's own order doesn't matter.

## 4. Default dataset generation rules

The fixture is generated once by a throwaway script and checked in (research R3). `PeopleFixtureRulesTest` asserts every rule below against the checked-in file.

**Size and identity**
- G-1: exactly 120 office holders (FR-031).
- G-2: `id`: UUID version 4 form, lower case, generated from a fixed seed; unique.
- G-3: `per_id`: 900001 to 900120, unique; `personal_code`: seven digits, 9100001 upward in steps of 7, unique.
- G-4: `appointment_id` from 700001, `authorisation_id` from 800001, `judiciary_role_id` from 600001, each consecutive across the dataset in generation order and unique dataset-wide (FR-032).

**People**
- G-5: `known_as` from a fixed list of 40 everyday forenames and `surname` from a fixed list of 60 everyday surnames (in the script; the test checks only format: letters, spaces, hyphens and apostrophes). No (`known_as`, `surname`) pair repeats.
- G-6: `title` from {"District Judge", "Deputy District Judge", "Circuit Judge", "Recorder", "Tribunal Judge", "Employment Judge", "Magistrate"}; `fullname` = `title` + " " + `known_as` + " " + `surname`; `initials` = first letter of `known_as` + first letter of `surname`, upper case.
- G-7: `email` = lower-case `known_as.surname` (spaces and apostrophes removed, then `.` plus `per_id` if needed to be unique) + `@judiciary.example`; `email_personal`, where set, the same local part + `@personal.example` (FR-033).
- G-8: `work_phone`, where set: `020 7946 0` + three digits, unique (FR-033).
- G-9: `post_nominals` `null` or one of {"KC", "OBE", "MBE"}; `retirement_date` `null` or a date after the person's `last_updated`; `leaving_on` always `null` (spec Records).
- G-10: `gender_id` cycles through the four served genders; `show_gender` `false` for every fifth person.

**Appointments, roles and authorisations**
- G-11: each person has 1 to 3 appointments. All `start_date`s are before 2026-01-01; every current appointment has `end_date` `null` or 2026-03-31 or later; every previous appointment ends before 2026-01-01. At least 5 current appointments end between 2026-03-31 and 2029-12-31, so moving the reference date to 2030-01-01 makes them previous (T048, quickstart.md § 5).
- G-12: at least 20 people have both current and previous appointments; at least 3 have only previous appointments (EC-006); at least 30 have two or more appointments.
- G-13: exactly one current appointment per person with current appointments has `is_principal` `true`; for a person with only previous appointments, exactly one previous appointment has it (FR-034).
- G-14: `role_name_id`, `contract_type_id`, `judiciary_role_name_id` and `ticket_id` refer to records with `end_date` `null`, except tickets 294, 365 and 367 (FR-036); `base_location_id` refers to a base location with `end_date` `null`. At least two appointments use different base locations that share a name (EC-008).
- G-15: `circuit`, `bench` and `advisory_committee_area`, each present on roughly half the appointments, from small fixed lists (e.g. circuits "North Eastern", "Midland", "Western", "South Eastern", "Northern", "Wales").
- G-16: 0 to 2 judiciary roles per person; at least 10 ended and at least 10 open.
- G-17: 0 to 4 dated authorisations per person, each under one of that person's appointments and within its dates; at least 10 with an `end_date` and at least 10 without; an authorisation for a ticket with an `end_date` ends on or before it (ticket 367 ends 2025-03-31).
- G-18: at least one person holds tickets 294, 365 and 367 together (FR-036).

**Dates**
- G-19: `last_updated` ≥ every `start_date` and `end_date` in the record (FR-034), spread over at least 12 distinct months, with at least two people sharing a `last_updated` (EC-007).
- G-20: at least one optional field of each kind present and at least one absent (FR-035).

## 5. Start-up validation rules (FR-046)

Checked by `FixturePeopleRepository` and `PeopleDatasetValidator`, for **any** configured fixture; the first failure stops start-up with `Invalid People fixture <location>: <message>`, naming the office holder's `id` (or `record at position <n>` when the `id` is missing or malformed).

| # | Rule | Spec |
|---|---|---|
| V-1 | The fixture exists and is a JSON array (possibly empty). | FR-030, EC-013 |
| V-2 | Every § 2 key is present, of its JSON type, `null` only where marked; no unknown key; strings trimmed and non-empty. | FR-023 |
| V-3 | `id` is a lower-case canonical UUID; `per_id`, every `*_id` > 0; `personal_code` digits only. | FR-032 |
| V-4 | `id`, `per_id` and `personal_code` unique across office holders; `appointment_id`, `authorisation_id`, `judiciary_role_id` each unique across the dataset. | FR-032 |
| V-5 | `email` and `email_personal` end in `.example`; `work_phone` matches `^020 7946 0[0-9]{3}$`. | FR-033 |
| V-6 | At least one appointment; at most one current `is_principal`, and exactly one when the person has a current appointment. | FR-034 |
| V-7 | Every `start_date` ≤ its `end_date`; `last_updated` ≥ every `start_date` and `end_date` in the record. | FR-034 |
| V-8 | When the fixture is non-empty: every `jo.people.reference-types` value is a configured type; the base-location-id type declares `type_id`, `parent_id`, `jurisdiction_id`; the ticket-id type declares `ticket_category_id`; the ticket-category-id type declares `jurisdiction_id`. | FR-046, R4 |
| V-9 | `gender_id`, `role_name_id`, `contract_type_id`, `base_location_id`, `judiciary_role_name_id`, `ticket_id` each resolve; each base location has a non-null `parent_id`, and its `type_id`, `parent_id` and `jurisdiction_id` resolve; each ticket's category resolves, with a non-null `jurisdiction_id` that resolves. | FR-037 to FR-042 |
| V-10 | Every authorisation's `appointment_id` is one of the same person's appointments, with `start_date` ≥ the appointment's and, when the appointment has an `end_date`, a non-null `end_date` ≤ it. | FR-043 |

Rules about the *default* dataset only (FR-031, FR-035, FR-036, § 4) are not start-up rules.

## 6. Configuration

See research R4 for the `jo.people` block. `PeopleProperties` requires `fixture`, `reference-date` and all ten `reference-types` entries (`@NotBlank`/`@NotNull`), so a missing setting fails binding before V-1.
