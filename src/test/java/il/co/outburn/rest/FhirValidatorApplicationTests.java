package il.co.outburn.rest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@SpringBootTest(classes = il.co.outburn.rest.FhirValidatorApplication.class)
@TestPropertySource(locations = "classpath:application.yaml")
class FhirValidatorApplicationTests {
    
    List<String> profiles = List.of();

    @Autowired
    FhirValidatorConfiguration configuration;

    // validateBytes tests
    @Test
    void validateBytes_nullInput_shouldReturnErrorOutcome() throws Throwable {
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBytes(null, profiles, configuration)
        );
        assertNotNull(result);
        OperationOutcome outcome = (OperationOutcome)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBytes_emptyJson_shouldReturnErrorOutcome() throws Throwable {
        byte[] emptyJson = "{}".getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBytes(emptyJson, profiles, configuration)
        );
        assertNotNull(result);
        OperationOutcome outcome = (OperationOutcome)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBytes_invalidPatient_shouldReturnErrorOutcome() throws Throwable {
        String patientStr = """
            { "resourceType": "Patient", "gender": "invalid" }
        """;
        byte[] patientBytes = patientStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBytes(patientBytes, profiles, configuration)
        );
        assertNotNull(result);
        OperationOutcome outcome = (OperationOutcome)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBytes_batchBundleWithInvalidPatient_shouldReturnErrorOutcome() throws Throwable {
        String bundleStr = """
            { "resourceType": "Bundle", "type": "batch", "entry": [
                { "request": { "method": "POST", "url": "Patient" },
                  "resource": { "resourceType": "Patient", "gender": "invalid" } }
            ] }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBytes(bundleBytes, profiles, configuration)
        );
        assertNotNull(result);
        OperationOutcome outcome = (OperationOutcome)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBytes_transactionBundleWithInvalidPatient_shouldReturnErrorOutcome() throws Throwable {
        String bundleStr = """
            { "resourceType": "Bundle", "type": "transaction", "entry": [
                { "request": { "method": "POST", "url": "Patient" },
                  "resource": { "resourceType": "Patient", "gender": "invalid" } }
            ] }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBytes(bundleBytes, profiles, configuration)
        );
        assertNotNull(result);
        OperationOutcome outcome = (OperationOutcome)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBytes_validPatient_shouldSuccess() throws Throwable {
        String patientStr = """
                {
                    "resourceType": "Patient",
                    "id": "9add8b2c-f91f-44ba-81e9-84b0c891108",
                    "gender": "male"
                }
                """;
        byte[] patientBytes = patientStr.getBytes();
        FhirValidationResult result =  assertDoesNotThrow(() ->
            FhirValidator.validateBytes(patientBytes, profiles, configuration)
        );
        assertNotNull(result);
        assertNotNull(result.resourceBytes);
        assertTrue(result.resourceBytes.length > 0);
        OperationOutcome operationOutcome = (OperationOutcome)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertNotNull(operationOutcome);
        assertFalse(FhirUtil.operationOutcomeHasErrorIssue(operationOutcome));
    }

    @Test
    void validateBytes_ilCorePatientWithValidNationalId_shouldSuccess() throws Throwable {
        List<String> ilCoreProfiles = List.of("http://fhir.health.gov.il/StructureDefinition/il-core-patient");
        String patientStr = """
            {
                "resourceType": "Patient",
                "meta": { "profile": ["http://fhir.health.gov.il/StructureDefinition/il-core-patient"] },
                "identifier": [
                    {
                        "system": "http://fhir.health.gov.il/identifier/il-national-id",
                        "value": "000000018"
                    }
                ],
                "name": [ { "given": [ "Israel" ], "family": "Israeli" } ],
                "gender": "male",
                "birthDate": "2025-01-01"
            }
        """;
        byte[] patientBytes = patientStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBytes(patientBytes, ilCoreProfiles, configuration)
        );
        assertNotNull(result);
        OperationOutcome outcome = (OperationOutcome)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertFalse(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBytes_ilCorePatientWithInvalidNationalId_shouldFail() throws Throwable {
        List<String> ilCoreProfiles = List.of("http://fhir.health.gov.il/StructureDefinition/il-core-patient");
        String patientStr = """
            {
                "resourceType": "Patient",
                "meta": { "profile": ["http://fhir.health.gov.il/StructureDefinition/il-core-patient"] },
                "identifier": [
                    {
                        "system": "http://fhir.health.gov.il/identifier/il-national-id",
                        "value": "000000019"
                    }
                ],
                "name": [ { "given": [ "Israel" ], "family": "Israeli" } ],
                "gender": "male",
                "birthDate": "2025-01-01"
            }
        """;
        byte[] patientBytes = patientStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBytes(patientBytes, ilCoreProfiles, configuration)
        );
        assertNotNull(result);
        OperationOutcome outcome = (OperationOutcome)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    // validateBundle tests
    @Test
    void validateBundle_nullInput_shouldThrowException() {
        assertThrows(Exception.class, () ->
            FhirValidator.validateBundle((byte[])null, configuration)
        );
    }

    @Test
    void validateBundle_emptyJson_shouldThrowException() throws Exception {
        byte[] emptyJson = "{}".getBytes();
        assertThrows(Exception.class, () ->
            FhirValidator.validateBundle(emptyJson, configuration)
        );
    }

    @Test
    void validateBundle_batchBundleWithInvalidPatient_shouldReturnErrorOutcome() throws Exception {
        String bundleStr = """
            { "resourceType": "Bundle", "type": "batch", "entry": [
                { "request": { "method": "POST", "url": "Patient" },
                  "resource": { "resourceType": "Patient", "gender": "invalid" } }
            ] }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = FhirValidator.validateBundle(bundleBytes, configuration);
        assertNotNull(result);
        Bundle bundle = (Bundle)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertEquals(1, bundle.getEntry().size());
        OperationOutcome outcome = (OperationOutcome)bundle.getEntryFirstRep().getResponse().getOutcome();
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBundle_batchBundleWithoutEntries_shouldReturnEmptyBundle() throws Exception {
        String bundleStr = """
            { "resourceType": "Bundle", "type": "batch" }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = FhirValidator.validateBundle(bundleBytes, configuration);
        assertNotNull(result);
        Bundle bundle = (Bundle)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertNotNull(bundle);
        assertEquals(0, bundle.getEntry().size());
    }

    @Test
    void validateBundle_batchBundleWithEmptyEntryArray_shouldReturnEmptyBundle() throws Exception {
        String bundleStr = """
            { "resourceType": "Bundle", "type": "batch", "entry": [] }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = FhirValidator.validateBundle(bundleBytes, configuration);
        assertNotNull(result);
        Bundle bundle = (Bundle)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertNotNull(bundle);
        assertEquals(0, bundle.getEntry().size());
    }

    @Test
    void validateBundle_batchBundleWithEntryWithoutResource_shouldThrowException() throws Exception {
        String bundleStr = """
            { "resourceType": "Bundle", "type": "batch", "entry": [
                { "request": { "method": "POST", "url": "Patient" } }
            ] }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        assertThrows(Exception.class, () ->
            FhirValidator.validateBundle(bundleBytes, configuration)
        );
    }

    @Test
    void validateBundle_batchBundleWithValidPatient_shouldSuccess() throws Exception {
        String bundleStr = """
                {
                    "resourceType": "Bundle",
                    "id": "9add8b2c-f91f-44ba-81e9-84b0c891108d",
                    "type": "batch",
                    "entry": [
                        {
                            "request": { "method": "POST", "url": "Patient" },
                            "fullUrl": "urn:uuid:9fe81a66-77ca-599a-bacd-77f590d2d35d",
                            "resource": {
                                "resourceType": "Patient",
                                "gender": "male"
                            }
                        }
                    ]
                }
                """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBundle(bundleBytes, configuration)
        );
        assertNotNull(result);
        assertNotNull(result.resourceBytes);
        assertTrue(result.resourceBytes.length > 0);
        Bundle responseBundle = (Bundle)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertNotNull(responseBundle);
        OperationOutcome operationOutcome = (OperationOutcome)responseBundle.getEntryFirstRep().getResponse().getOutcome();
        assertFalse(FhirUtil.operationOutcomeHasErrorIssue(operationOutcome));
    }

    @Test
    void validateBundle_batchBundleWithInvalidPatient_shouldFail() throws Exception {
        String bundleStr = """
                {
                    "resourceType": "Bundle",
                    "id": "9add8b2c-f91f-44ba-81e9-84b0c891108d",
                    "type": "batch",
                    "entry": [
                        {
                            "request": { "method": "POST", "url": "Patient" },
                            "fullUrl": "urn:uuid:9fe81a66-77ca-599a-bacd-77f590d2d35d",
                            "resource": {
                                "resourceType": "Patient",
                                "gender": "invalid"
                            }
                        }
                    ]
                }
                """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBundle(bundleBytes, configuration)
        );
        assertNotNull(result);
        assertNotNull(result.resourceBytes);
        assertTrue(result.resourceBytes.length > 0);
        Bundle responseBundle = (Bundle)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertNotNull(responseBundle);
        OperationOutcome operationOutcome = (OperationOutcome)responseBundle.getEntryFirstRep().getResponse().getOutcome();
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(operationOutcome));
    }

    @Test
    void validateBundle_batchBundleWithIlCorePatientValidNationalId_shouldSuccess() throws Exception {
        String bundleStr = """
            {
                "resourceType": "Bundle",
                "type": "batch",
                "entry": [
                    {
                        "request": { "method": "POST", "url": "Patient" },
                        "resource": {
                            "resourceType": "Patient",
                            "meta": { "profile": ["http://fhir.health.gov.il/StructureDefinition/il-core-patient"] },
                            "identifier": [
                                {
                                    "system": "http://fhir.health.gov.il/identifier/il-national-id",
                                    "value": "000000018"
                                }
                            ],
                            "name": [ { "given": [ "Israel" ], "family": "Israeli" } ],
                            "gender": "male",
                            "birthDate": "2025-01-01"
                        }
                    }
                ]
            }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBundle(bundleBytes, configuration)
        );
        assertNotNull(result);
        Bundle bundle = (Bundle)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertEquals(1, bundle.getEntry().size());
        OperationOutcome outcome = (OperationOutcome)bundle.getEntryFirstRep().getResponse().getOutcome();
        assertFalse(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBundle_batchBundleWithIlCorePatientInvalidNationalId_shouldFail() throws Exception {
        String bundleStr = """
            {
                "resourceType": "Bundle",
                "type": "batch",
                "entry": [
                    {
                        "request": { "method": "POST", "url": "Patient" },
                        "resource": {
                            "resourceType": "Patient",
                            "meta": { "profile": ["http://fhir.health.gov.il/StructureDefinition/il-core-patient"] },
                            "identifier": [
                                {
                                    "system": "http://fhir.health.gov.il/identifier/il-national-id",
                                    "value": "000000019"
                                }
                            ],
                            "name": [ { "given": [ "Israel" ], "family": "Israeli" } ],
                            "gender": "male",
                            "birthDate": "2025-01-01"
                        }
                    }
                ]
            }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBundle(bundleBytes, configuration)
        );
        assertNotNull(result);
        Bundle bundle = (Bundle)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertEquals(1, bundle.getEntry().size());
        OperationOutcome outcome = (OperationOutcome)bundle.getEntryFirstRep().getResponse().getOutcome();
        assertTrue(FhirUtil.operationOutcomeHasErrorIssue(outcome));
    }

    @Test
    void validateBundle_batchBundleWithTwoEntries_fullUrlPropagation() throws Exception {
        String bundleStr = """
            {
                "resourceType": "Bundle",
                "type": "batch",
                "entry": [
                    {
                        "fullUrl": "urn:uuid:entry1",
                        "request": { "method": "POST", "url": "Patient" },
                        "resource": { "resourceType": "Patient", "gender": "male" }
                    },
                    {
                        "request": { "method": "POST", "url": "Patient" },
                        "resource": { "resourceType": "Patient", "gender": "male" }
                    }
                ]
            }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        FhirValidationResult result = assertDoesNotThrow(() ->
            FhirValidator.validateBundle(bundleBytes, configuration)
        );
        assertNotNull(result);
        Bundle bundle = (Bundle)FhirUtil.bytesToResource(result.resourceBytes, configuration);
        assertEquals(2, bundle.getEntry().size());
        // First entry should have fullUrl in response
        assertEquals("urn:uuid:entry1", bundle.getEntry().get(0).getFullUrl());
        // Second entry should not have fullUrl
        assertNull(bundle.getEntry().get(1).getFullUrl());
    }

    @Test
    void validateBundle_batchBundleWithSecondEntryWithoutResource_shouldThrowException() throws Exception {
        String bundleStr = """
            { "resourceType": "Bundle", "type": "batch", "entry": [
                {
                    "request": { "method": "POST", "url": "Patient" },
                    "resource": { "resourceType": "Patient", "gender": "male" }
                },
                {
                    "request": { "method": "POST", "url": "Patient" }
                }
            ] }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        Exception exception = assertThrows(Exception.class, () ->
            FhirValidator.validateBundle(bundleBytes, configuration)
        );
        assertTrue(exception.getMessage().contains("Bundle.entry[1].resource must be a JSON object"));
    }

    @Test
    void validateBundle_batchBundleWithDuplicateFullUrl_shouldHandleFullUrls() throws Exception {
        String bundleStr = """
            { "resourceType": "Bundle", "type": "batch", "entry": [
                {
                    "request": { "method": "POST", "url": "Patient" },
                    "resource": { "resourceType": "Patient", "gender": "male" }
                },
                {
                    "fullUrl": "urn:uuid:not-duplicate",
                    "request": { "method": "POST", "url": "Patient" },
                    "resource": { "resourceType": "Patient", "gender": "male" }
                },
                {
                    "fullUrl": "urn:uuid:duplicate",
                    "request": { "method": "POST", "url": "Patient" },
                    "resource": { "resourceType": "Patient", "gender": "male" }
                },
                {
                    "fullUrl": "urn:uuid:duplicate",
                    "request": { "method": "POST", "url": "Patient" },
                    "resource": { "resourceType": "Patient", "gender": "male" }
                }
            ] }
        """;
        byte[] bundleBytes = bundleStr.getBytes();
        Exception exception = assertThrows(Exception.class, () ->
            FhirValidator.validateBundle(bundleBytes, configuration)
        );
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Bundle.entry[3].fullUrl must be unique"));
    }
}
