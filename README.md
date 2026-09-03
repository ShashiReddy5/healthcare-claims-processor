# healthcare-claims-processor

A small, self-contained Spring Boot 3 microservice that models a simplified claims adjudication API: submit a claim, have it auto-approved, denied, or pended for manual review based on the billed amount, and fetch it back later — with the member identifier and diagnosis code encrypted at rest.

This is a reference/demo implementation, not a HIPAA-compliant production system. It's built to run with zero external setup (no database server, no message broker) while still showing a realistic layout: layered packages, JWT-secured endpoints, real field-level encryption, validation, pagination, and clear seams for the pieces that a real payer platform would need and this one doesn't implement.

## What it actually does

- `POST /api/claims` — submit a claim (`memberId`, `diagnosisCode`, `billedAmount`). Claims at or under $500 are auto-approved for the full billed amount; claims over $50,000 are denied as exceeding the policy limit; everything in between is pended as `PENDING_REVIEW`.
- `GET /api/claims/{claimId}` — fetch a single claim, with the member ID and diagnosis code decrypted back for display.
- `GET /api/claims?status=...` — paginated list, optionally filtered by status.
- `POST /api/auth/login` — exchanges a demo credential (`examiner` / `demo-password`) for a JWT used as a Bearer token on the endpoints above.
- `GET /actuator/health` — health check.

## Tech stack (what's really in the repo)

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.4 (Web, Data JPA, Security, Validation, Actuator) |
| Persistence | H2, in-memory |
| PHI protection | Real AES-256-GCM field-level encryption/decryption for `memberId` and `diagnosisCode` before they're persisted |
| Auth | Stateless JWT (HS256, `io.jsonwebtoken`/jjwt) via a custom filter |
| Audit | A logging-based `AuditLogger` seam |
| Tests | JUnit 5 + Mockito + AssertJ |
| Build | Maven |

## Project structure

```
healthcare-claims-processor/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/shashireddy/claims/
    │   │   ├── ClaimsProcessorApplication.java
    │   │   ├── model/Claim.java, ClaimStatus.java
    │   │   ├── repository/ClaimRepository.java
    │   │   ├── dto/ClaimDtos.java
    │   │   ├── crypto/PhiEncryptionService.java
    │   │   ├── service/ClaimsAdjudicationService.java, AuditLogger.java
    │   │   ├── security/JwtService.java, SecurityConfig.java
    │   │   └── controller/AuthController.java, ClaimsController.java
    │   └── resources/application.yml
    └── test/java/com/shashireddy/claims/service/ClaimsAdjudicationServiceTest.java
```

## Running it locally

```bash
./mvnw spring-boot:run
```

The service starts on port 8080 with an in-memory H2 database — no external services required.

```bash
# get a token
curl -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"examiner","password":"demo-password"}'

# submit a claim
curl -X POST localhost:8080/api/claims \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"memberId":"member-123","diagnosisCode":"E11.9","billedAmount":120.00}'
```

Run the tests with:

```bash
./mvnw test
```

## What's simplified, and how it maps to a real deployment

This repo intentionally trades regulatory and production concerns for something that's honest, runnable, and easy to read end-to-end. It is **not** HIPAA-compliant as-is — real PHI handling needs a lot more than one encrypted field.

- **PHI encryption**: `memberId` and `diagnosisCode` are genuinely AES-256-GCM encrypted before being written to the database — that part is real. What's missing for a real deployment: encryption of every PHI-adjacent field, KMS-backed key management and rotation instead of a config value, and encryption in transit enforced at the infra layer.
- **HL7/FHIR**: not implemented. A real payer/provider integration would add a parsing layer (e.g. HAPI FHIR) in front of `ClaimSubmissionRequest` to translate inbound HL7 v2 messages or FHIR resources into this model.
- **Spring Batch / EDI 837-835**: not implemented. Bulk claims intake and settlement reconciliation would be a separate batch module reading EDI files and calling into `ClaimsAdjudicationService` per claim.
- **Eligibility & business rules**: the $500 / $50,000 thresholds are a placeholder for a real rules engine that checks member eligibility, contracted rates, and prior authorizations.
- **Audit logging**: `AuditLogger` writes structured log lines. A regulated deployment needs an immutable, queryable audit store — the interface is the seam for that.
- **Auth**: a single hard-coded demo credential issues a real JWT, so the rest of the API is genuinely JWT-secured. A real deployment swaps `AuthController` for a call to an actual identity provider — `JwtService` and the filter chain stay the same either way.
- **Database**: H2 in-memory instead of a managed Postgres/MySQL instance. The JPA layer doesn't care.

## License

MIT
