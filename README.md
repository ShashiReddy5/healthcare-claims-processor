# healthcare-claims-processor
HIPAA-compliant claims adjudication microservice with HL7/FHIR integration, PHI encryption, and Spring Batch EDI processing
# Healthcare Claims Processor

[![Java](https://img.shields.io/badge/Java-8-ED8B00?style=flat&logo=java)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-6DB33F?style=flat&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![HIPAA](https://img.shields.io/badge/HIPAA-Compliant-009900?style=flat)](https://www.hhs.gov/hipaa)
[![AWS](https://img.shields.io/badge/AWS-ECS%20|%20RDS%20|%20S3-FF9900?style=flat&logo=amazon-aws)](https://aws.amazon.com)
[![React](https://img.shields.io/badge/React-17-61DAFB?style=flat&logo=react)](https://reactjs.org)

HIPAA-compliant claims adjudication microservice with HL7/FHIR integration, PHI field-level encryption, AWS cloud-native deployment, and Spring Batch processing for EDI transactions.

Inspired by healthcare insurance platform architecture built at Wipro for a major insurance payer client.

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────┐
│         React Healthcare Portal (Claims Examiner)     │
│         (real-time status · prior auth · eligibility) │
└────────────────────────┬─────────────────────────────┘
                         │ REST API (HTTPS + JWT)
          ┌──────────────▼───────────────┐
          │   Claims Processor Service    │
          │   (Spring Boot / Java 8)      │
          │   - HL7/FHIR parsing          │
          │   - Business rule engine      │
          │   - PHI encryption            │
          └──────┬───────────────┬────────┘
                 │               │
    ┌────────────▼───┐  ┌────────▼──────────────┐
    │  MySQL (RDS)   │  │  Spring Batch Jobs     │
    │  PHI encrypted │  │  EDI · settlement      │
    └────────────────┘  └───────────────────────┘
                 │
    ┌────────────▼────────────┐
    │  AWS S3 (PHI storage)   │
    │  server-side encryption │
    │  + audit logging        │
    └─────────────────────────┘
```

---

## Key Features

- **HIPAA compliance** — field-level PHI encryption, comprehensive audit logging, role-based access to patient data
- **HL7/FHIR integration** — parses and validates HL7 v2 messages and FHIR R4 resources for payer/provider interoperability
- **Claims adjudication** — Java business rule engine automates routine approvals, routes complex cases for manual review
- **Spring Batch processing** — EDI 837/835 transaction processing, claims settlement reconciliation, compliance reporting
- **React portal** — real-time claims status, prior authorisation tracking, eligibility verification for claims examiners
- **PHI-safe S3 storage** — server-side encryption (SSE-KMS) for all patient document storage with access audit trails

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 8 |
| Framework | Spring Boot 2.7, Spring MVC, Spring Security, Spring Batch |
| Persistence | Spring Data JPA, Hibernate, MySQL (AWS RDS) |
| Security | OAuth2, JWT, Spring Security, field-level AES-256 encryption |
| Healthcare | HL7 v2, FHIR R4 (HAPI FHIR library) |
| Frontend | React 17, TypeScript, JavaScript |
| Cloud | AWS (EC2, ECS, RDS, S3 + SSE-KMS, Route53, CloudWatch, IAM) |
| Batch | Spring Batch — EDI 837/835 processing |
| Testing | JUnit 4/5, Mockito, WireMock |

---

## Core Code Samples

### PHI Field-Level Encryption

```java
@Component
public class PhiEncryptionService {

    @Value("${encryption.key}")
    private String encryptionKey;

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    /**
     * Encrypts PHI fields before persisting to database.
     * Required for HIPAA Technical Safeguard compliance.
     */
    public String encrypt(String plainText) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                Base64.getDecoder().decode(encryptionKey), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            // prepend IV for decryption
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new PhiEncryptionException("Failed to encrypt PHI field", e);
        }
    }
}
```

### Claims Adjudication Service

```java
@Service
@Slf4j
public class ClaimsAdjudicationService {

    private final ClaimsRepository claimsRepository;
    private final EligibilityService eligibilityService;
    private final PhiEncryptionService encryptionService;
    private final AuditLogger auditLogger;

    @Transactional
    public AdjudicationResult adjudicateClaim(ClaimSubmission submission) {
        auditLogger.log("CLAIM_RECEIVED", submission.getClaimId(), getCurrentUser());

        // validate patient eligibility
        EligibilityResult eligibility = eligibilityService
            .checkEligibility(submission.getMemberId(), submission.getServiceDate());

        if (!eligibility.isActive()) {
            return AdjudicationResult.denied(submission.getClaimId(),
                DenialReason.MEMBER_INELIGIBLE);
        }

        // apply payer-specific business rules
        RuleEvaluationResult ruleResult = evaluateBusinessRules(submission, eligibility);

        if (ruleResult.isAutoApprove()) {
            return approveClaim(submission, ruleResult.getAllowedAmount());
        } else if (ruleResult.isDeny()) {
            return denyClaim(submission, ruleResult.getDenialReason());
        } else {
            // route for manual review
            return pendClaim(submission, ruleResult.getPendReason());
        }
    }

    private AdjudicationResult approveClaim(ClaimSubmission submission,
                                             BigDecimal allowedAmount) {
        Claim claim = Claim.builder()
            .claimId(submission.getClaimId())
            // encrypt PHI before persisting
            .memberIdEncrypted(encryptionService.encrypt(submission.getMemberId()))
            .diagnosisCodeEncrypted(encryptionService.encrypt(submission.getDiagnosisCode()))
            .allowedAmount(allowedAmount)
            .status(ClaimStatus.APPROVED)
            .adjudicatedAt(Instant.now())
            .build();

        claimsRepository.save(claim);
        auditLogger.log("CLAIM_APPROVED", submission.getClaimId(), getCurrentUser());
        return AdjudicationResult.approved(submission.getClaimId(), allowedAmount);
    }
}
```

### Spring Batch EDI 837 Job

```java
@Configuration
public class Edi837BatchConfig {

    @Bean
    public Job edi837ProcessingJob(JobRepository jobRepository,
                                    Step parseStep,
                                    Step adjudicateStep,
                                    Step settlementStep) {
        return new JobBuilder("edi837ProcessingJob", jobRepository)
            .start(parseStep)
            .next(adjudicateStep)
            .next(settlementStep)
            .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<EdiTransaction> edi837Reader(
            @Value("#{jobParameters['input.file']}") String inputFile) {
        return new FlatFileItemReaderBuilder<EdiTransaction>()
            .name("edi837Reader")
            .resource(new FileSystemResource(inputFile))
            .lineMapper(new Edi837LineMapper())
            .build();
    }

    @Bean
    public ItemProcessor<EdiTransaction, ClaimSubmission> edi837Processor() {
        return transaction -> {
            // parse EDI 837 segments into domain model
            ClaimSubmission submission = Edi837Parser.parse(transaction);
            // validate required fields
            Edi837Validator.validate(submission);
            return submission;
        };
    }
}
```

### FHIR Patient Resource Parsing

```java
@Service
public class FhirPatientService {

    private final FhirContext fhirContext = FhirContext.forR4();

    public PatientDto parsePatientResource(String fhirJson) {
        IParser parser = fhirContext.newJsonParser();
        Patient patient = parser.parseResource(Patient.class, fhirJson);

        return PatientDto.builder()
            .resourceId(patient.getIdElement().getIdPart())
            .familyName(patient.getNameFirstRep().getFamily())
            .givenName(patient.getNameFirstRep().getGivenAsSingleString())
            .dateOfBirth(patient.getBirthDate())
            .gender(patient.getGender() != null
                ? patient.getGender().getDisplay() : null)
            .build();
    }
}
```

---

## Running Locally

```bash
# start dependencies
docker-compose up -d mysql

# run the service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# run batch EDI job manually
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.batch.job.names=edi837ProcessingJob \
  --input.file=src/test/resources/sample-837.edi"

# run tests
./mvnw test
```

> **Note:** PHI encryption key is managed via AWS KMS in production. Locally, set `ENCRYPTION_KEY` environment variable to a Base64-encoded 32-byte key for testing only.

