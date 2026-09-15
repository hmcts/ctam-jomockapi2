# Quickstart: Reference Data Lookup by Type and ID

**Feature**: `002-reference-data-lookup` | **Contract**: [reference-data-by-id.md](./contracts/reference-data-by-id.md)

This guide validates the feature end-to-end once implemented. It assumes the base
Spring Boot application skeleton from `001-healthcheck-endpoint` is in place and this
feature's `referencedata` and `security` packages have been implemented per `plan.md`.

## Prerequisites

- Java 25 and Maven installed
- Repository checked out with `joh-elinks-api/ReferenceData/` present (source extracts
  used to build the bundled classpath resources — see `research.md`)

## Setup

```bash
mvn -q clean package
```

## Run

```bash
mvn -q spring-boot:run
```

The application starts on its configured port (default Spring Boot: `8080`) and serves
this feature at `/elinks/api/v5/reference_data/{attribute_name}/{reference_id}`.

## Validation scenarios

Each scenario maps to an acceptance scenario in `spec.md`. Replace `<TOKEN>` with any
non-blank string — simulated authentication accepts any well-formed bearer token (see
`research.md`).

1. **Successful lookup by canonical type name** (User Story 1, Scenario 1)

   ```bash
   curl -i -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/elinks/api/v5/reference_data/jurisdictions/30
   ```

   Expected: `200 OK`, JSON body with `"id": 30` and `"name": "Courts"` (per
   `joh-elinks-api/ReferenceData/eLinks_Pivotl_Production_all-data_2026-06-01_REF_Jurisdiction.json`),
   and an `X-Correlation-Id` response header (Constitution Principle XIV, added
   v1.1.0) — present on every response in every scenario below, error or success.

2. **Unknown ID within a valid type** (User Story 1, Scenario 2)

   ```bash
   curl -i -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/elinks/api/v5/reference_data/jurisdictions/999999
   ```

   Expected: `404 Not Found`, with a JSON body in the shared error shape
   (`{"message": "...", "timestamp": "...", "traceId": "..."}`, Constitution Principle
   IX, amended v1.1.0), and an `X-Correlation-Id` response header whose value matches
   `traceId`.

3. **Missing authentication** (User Story 1, Scenario 4)

   ```bash
   curl -i http://localhost:8080/elinks/api/v5/reference_data/jurisdictions/30
   ```

   Expected: `401 Unauthorized`, no record data in the body — instead the shared
   `ErrorResponse` shape, per Constitution Principle IX (amended v1.1.0).

4. **Deprecated alias returns the identical record** (User Story 2, Scenario 1)

   ```bash
   curl -s -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/elinks/api/v5/reference_data/jurisdictions/30 -o /tmp/canonical.json
   curl -s -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/elinks/api/v5/reference_data/jurisdiction/30 -o /tmp/deprecated.json
   diff /tmp/canonical.json /tmp/deprecated.json
   ```

   Expected: no diff output (identical bodies).

5. **Unsupported type name** (User Story 3, Scenario 1)

   ```bash
   curl -i -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/elinks/api/v5/reference_data/not_a_real_type/1
   ```

   Expected: `404 Not Found`, with the shared `ErrorResponse` body shape (Constitution
   Principle IX, amended v1.1.0).

6. **Malformed (non-numeric) ID** (User Story 3, Scenario 2)

   ```bash
   curl -i -H "Authorization: Bearer <TOKEN>" \
     http://localhost:8080/elinks/api/v5/reference_data/jurisdictions/not-a-number
   ```

   Expected: `400 Bad Request`, with the shared `ErrorResponse` body shape
   (Constitution Principle IX, amended v1.1.0).

7. **Record outside its start/end date window is still returned** (Clarifications, FR-012)

   Pick any record whose `start_date`/`end_date` places it outside today's date in the
   bundled dataset (see `data-model.md` for field meanings) and repeat scenario 1 against
   it.

   Expected: `200 OK` with the full record — no date-based filtering applied.

## Automated coverage

The scenarios above are also expressed as automated tests per `plan.md`'s Project
Structure: `ReferenceDataControllerTest` (controller/API level) and
`ReferenceDataContractTest` (pins the contract in
`contracts/reference-data-by-id.md`). Run:

```bash
mvn -q test
```
