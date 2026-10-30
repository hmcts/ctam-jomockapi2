# ctam-jomockapi2

A mock implementation of the **E-Links API** (Ministry of Justice / judiciary.uk) — a
Java/Spring Boot service that stands in for the real E-Links API for integration testing
and development, serving synthetic data only.

## Purpose

The real E-Links API's Swagger/OpenAPI specification is the source of truth for this mock.
Every endpoint the mock exposes preserves the real API's paths, methods, parameters,
response shapes, status codes, filtering, and pagination behaviour, so consumers can
develop and test against it exactly as they would against the real service — without
touching production data.

Reference extracts from the real service are local-only reference material: they are
gitignored, never committed, and the build doesn't depend on them. The checked-in fixtures
are synthetic.

## Governance

This project is built following the principles in
[`.specify/memory/constitution.md`](.specify/memory/constitution.md) — contract-first
development, a component-centric Spring Boot architecture, synthetic data only,
deterministic behaviour, and centralised, reusable handling of pagination, filtering,
validation, and reference data. Any change to the mock's observable HTTP behaviour must
trace back to the source specification.

## Development process

This repository is developed feature-by-feature using the
[Spec Kit](https://github.com/github/spec-kit) workflow. Each feature lives under
`specs/<feature-name>/` with its specification, implementation plan, and task
breakdown.

## Status

Implemented and covered by automated tests:

- `GET /api/v1/healthcheck` (see `specs/001-healthcheck-api/`)
- the Reference Data API for ten types, from appointment titles to ticket categories (see
  [Reference data](#reference-data) and `specs/002-reference-data-api/` to
  `specs/011-ticket-categories-reference-data/`)

### Quality gates

```bash
./gradlew check                 # compile (-Werror), Checkstyle, the four test suites, JaCoCo + coverage gate, OWASP check
./gradlew check -PskipOwasp     # quicker local run without the NVD download (CI never skips it)
./gradlew dependencyUpdates     # dependency freshness report
```

The coverage report, merged from all four suites, is at `build/reports/jacoco/test/html/index.html`.
`check` fails if line coverage drops below 90% or branch coverage below 80%, and CI shows the figures
on each run's summary page. Run one suite at a time with
`./gradlew test`, `integrationTest`, `functionalTest` or `smokeTest`. The OWASP check reads an NVD
API key from `NVD_API_KEY`; without one it runs slowly, and in CI it fails rather than run keyless.

## Running locally

Prerequisites: none beyond the Gradle wrapper committed in this repo — it will
provision the required Java 25 toolchain automatically on first run.

```bash
./start.sh
```

This builds and runs the application in the foreground on `http://localhost:8080`
(stop it with `Ctrl+C`). It's a plain process — no IDE or Claude session required —
so it can be started independently and exercised by any HTTP client.

Once it's running, verify it directly:

```bash
curl -i http://localhost:8080/api/v1/healthcheck
```

Or exercise it with the checked-in Postman collection (`postman/`), which has a request
with assertions for each success and error case:

```bash
npx newman run postman/ctam-jomockapi.postman_collection.json \
  -e postman/ctam-jomockapi.postman_environment.json
```

Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`, and the
raw OpenAPI document at `http://localhost:8080/v3/api-docs`.

## Reference data

Two routes serve every configured reference-data type through the same code:

| Route | Returns |
|-------|---------|
| `GET /api/v1/reference_data/{attribute_name}` | `{"results": [...]}`: every record, in ascending `id` order |
| `GET /api/v1/reference_data/{attribute_name}/{reference_id}` | One record, as a single object |

Ten types are configured:

| Type | Records | Deprecated alias served |
|------|---------|-------------------------|
| `appointment_titles` | 194 | `appointment_title`, which behaves identically |
| `base_locations` | 1,462, one per source row (1,276 distinct names) | none: `base_location` returns `400` |
| `contract_types` | 14 | none: `contract_type` returns `400` |
| `genders` | 4 | none: `gender` returns `400` |
| `judiciary_roles` | 164 | none: `judiciary_role` returns `400` |
| `locations` | 2,000, one per source row (1,523 distinct names) | none: `location` returns `400` |
| `location_types` | 16, one per source row (13 distinct names) | none: `location_type` returns `400` |
| `jurisdictions` | 5, one per source entry | none: `jurisdiction` returns `400` |
| `tickets` | 159, one per source row (152 distinct names) | none: `ticket` returns `400` |
| `ticket_categories` | 54, one per source row (53 distinct names) | none: `ticket_category` returns `400` |

Base-location records also carry `type_id`, `parent_id` and `jurisdiction_id`, after `name`:
the E-Links ids of each location's type, parent location and jurisdiction (30 Courts, 34 Tribunals,
84 Magistrates, 86 Coroners), copied from the source data. Every `parent_id` is the id of a served
location, and every `type_id` the id of a served location type in the same jurisdiction. Every
`jurisdiction_id` is the id of a served jurisdiction. Appointment-title records don't have these
fields.

Location records carry the same three fields, after `name`. Unlike every other type, locations
keep the E-Links ids (1 to 2433, with gaps), so that parent references resolve: every non-null
`parent_id` is the id of another served location. The 142 top-level locations have
`"parent_id": null`; the field is never left out. Every `type_id` is the id of a served location
type with the same `jurisdiction_id`, and every `jurisdiction_id` the id of a served
jurisdiction. Base-location ids are not location ids: the two types are not linked by id.

Location-type records also keep the E-Links ids (1 to 16), so that every location's and base
location's `type_id` resolves. Every `jurisdiction_id` is the id of a served jurisdiction. Each
carries `jurisdiction_id` and then `integration_code` (E-Links' code for the type, e.g.
`MAGS_AC_LJA`), after `name`. Names repeat: "Region" three times and "AC"
twice, told apart by jurisdiction and integration code. Ids 7 and 14 are ended by the synthetic
date rule, although open locations still use them.

Jurisdiction records keep the E-Links ids too (30 Courts, 34 Tribunals, 84 Magistrates,
86 Coroners, 87 Skills), so that every location's, base location's and location type's
`jurisdiction_id` resolves. They carry no reference, boolean or text field, and none is ended.
Skills (87) is referenced only by the ticket category Languages.

Ticket records also keep the E-Links ids (289 to 1709, with gaps), so that People authorisations'
`ticket_id` values can refer to them later. Each carries `ticket_category_id`, after `name`: the
E-Links id of the ticket's category. Every `ticket_category_id` is the id of a served ticket
category. Seven names repeat, each in two categories.

Ticket-category records keep the E-Links ids too (31 to 88, with gaps). Each carries `type_id`,
`jurisdiction_id` and `parent_category_id`, after `name`. Categories form two levels: the 8
top-level categories have `"parent_category_id": null`, and every other category's parent is
top-level, in the same jurisdiction. `type_id` is the id of a ticket category type; those aren't
served yet, so it is a forward reference that the feature serving them must make resolve
(constitution, Principle IV). "Immigration and Asylum Chamber" is in both tiers.

Contract-type records also carry `salaried`, after `name`: a JSON boolean, `true` for "Salaried"
and the ten salaried part-time working ("SPTW-…%") types, `false` for "Fee-paid", "Unknown" and
"Voluntary". Only contract-type records have it.

Each type's feature decides which of the E-Links deprecated aliases it serves (constitution,
Principle VII). Names are carried over exactly from the source, including non-ASCII characters
such as the en dash in "Royal Courts of Justice – Office of the Judge Advocate General", and
different real locations can share a name, so a name may appear on more than one record. Any
other attribute name returns `400`, an unknown id returns `404`, and any query parameter returns
`400`. Every error uses one shape:
`{"error": ..., "timestamp": ..., "traceId": ...}`.

```bash
curl -i http://localhost:8080/api/v1/reference_data/appointment_titles/70 \
  -H 'Authorization: Bearer local-dev-token'
```

### Authentication

Every route under `/api/` needs an `Authorization: Bearer <token>` header, except the healthcheck.
A missing or wrong token gets `401` with `WWW-Authenticate: Bearer`.

- The default token, `local-dev-token`, is **not a secret**. It exists for local development and
  Postman.
- Set `JO_SECURITY_BEARER_TOKENS` to a comma-separated list to replace it, e.g.
  `JO_SECURITY_BEARER_TOKENS=token-a,token-b ./start.sh`.
- Authentication can't be switched off: the application refuses to start if the token list is
  empty or contains a blank entry.

An optional `X-Correlation-Id` request header (`^[A-Za-z0-9._-]{1,64}$`) is echoed on the response
and in `traceId`; otherwise a UUID is generated.

### Adding a reference-data type

No Java changes are needed, as long as the type's fields are among those below. Add two things:

1. **A fixture**: `src/main/resources/reference-data/<attribute_name>.json`, a JSON array of records
   with the fields `id`, `name`, `created_at`, `updated_at`, `start_date` and `end_date`, plus any
   reference fields, then any text fields and then any boolean fields the type declares (see below),
   after `name`. The rules
   (unique positive ids, unique trimmed names unless the type allows repeats, whole-second UTC timestamps,
   dates in order) are in [data-model.md §2](specs/002-reference-data-api/data-model.md), as amended
   by [003 FR-017](specs/003-base-locations-reference-data/spec.md). The application refuses to
   start if a fixture breaks one.
2. **A configuration entry** under `jo.reference-data.types` in `application.yml`:

   ```yaml
   jo:
     reference-data:
       types:
         - name: <attribute_name>            # lower-case letters and underscores
           aliases: [<deprecated_name>]      # optional
           fixture: classpath:reference-data/<attribute_name>.json
           reference-fields: [type_id, parent_id, jurisdiction_id, ticket_category_id, parent_category_id]   # optional: any of the five
           boolean-fields: [salaried]        # optional
           text-fields: [integration_code]   # optional
           unique-names: false               # optional: allow repeated names (default true)
           nullable-reference-fields: [parent_id]   # optional: declared reference fields that may be null
   ```

   `reference-fields` lists the ids of other types' records that the type's records carry. Every
   record must then have each declared field as a positive integer, and no record may have an
   undeclared one. Responses include a reference field only when the record has it. Names must be
   unique within the type unless it declares `unique-names: false`; ids must always be unique.

   `nullable-reference-fields` lists declared reference fields that may be null. Every record must
   still have each such field, as a positive integer or `null`, and responses write it as `null`
   when the record has no value. Start-up fails if a listed field isn't also in `reference-fields`.

   `boolean-fields` works the same way for yes/no fields: every record must have each declared
   field as a JSON `true` or `false` (not `null`, `0`/`1` or a string), and no record may have an
   undeclared one.

   `text-fields` works the same way for text: every record must have each declared field as a
   non-empty JSON string with no leading or trailing whitespace (not `null`, a number or a
   boolean), and no record may have an undeclared one. Responses write text fields after the
   reference fields.

   `reference-fields`, `boolean-fields` and `text-fields` choose from fixed sets (the
   `ReferenceField`, `BooleanField` and `TextField` enums). A type that needs a field outside them
   needs a small, type-agnostic code change (a new enum constant and a record and response
   property), as 003, 004 and 008 made.

The new type is then served by both routes, with the same authentication, validation and errors,
and appears in the OpenAPI `attribute_name` enum. `ReferenceDataExtensionIntegrationTest` shows
this working for a type declared only in test properties.

Tests to update for a new type:

- `NoTypeSpecificCodeTest`: add the type's forbidden fragment, unless an existing one already covers its
  name (as `location` covers `location_types`); if a generic field name contains the singular, use the
  plural instead (as `jurisdictions`, `tickets` and `ticket_categories` do, because of `jurisdiction_id` and
  `ticket_category_id`).
- `ReferenceDataFixtureRulesTest`: add the fixture with its record count, distinct-name count,
  whether it has reference, text or boolean fields, and reference points.
- `ReferenceDataOpenApiContractTest`: update the `attribute_name` enum and description.
- `ReferenceDataFunctionalTest`: remove the names the type serves from the unsupported list.
- Add a functional test class, a smoke-test path and a Postman sub-folder for the type. The design is in
[`specs/002-reference-data-api/`](specs/002-reference-data-api/).
