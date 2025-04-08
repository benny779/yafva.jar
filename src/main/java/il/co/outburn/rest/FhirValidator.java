package il.co.outburn.rest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FhirValidator {

    public static FhirValidationResult validateBundle(byte[] requestBundle, FhirValidatorConfiguration configuration) throws IOException {
        var sv = configuration.getSv();

        if (VersionUtilities.isR4Ver(sv)) {
            var parserR4 = new org.hl7.fhir.r4.formats.JsonParser();
            var requestBundleR4 = (org.hl7.fhir.r4.model.Bundle)parserR4.parse(requestBundle);
            return validateBundleR4(requestBundleR4, configuration);

        } else if (VersionUtilities.isR5Ver(sv)) {
            var parserR5 = new org.hl7.fhir.r5.formats.JsonParser();
            var requestBundleR5 = (org.hl7.fhir.r5.model.Bundle)parserR5.parse(requestBundle);
            return validateBundleR5(requestBundleR5, configuration);

        } else {
            throw new IllegalArgumentException("Unsupported FHIR version: " + sv);
        }
    }

    public static FhirValidationResult validateBundleR4(org.hl7.fhir.r4.model.Bundle requestBundle, FhirValidatorConfiguration configuration)
        throws IOException
    {
        // Checks
        if (requestBundle == null) {
            throw new IllegalArgumentException("Bundle cannot be null");
        }
        if (requestBundle.getType() != Bundle.BundleType.BATCH) {
            throw new IllegalArgumentException("Bundle type must be of type BATCH");
        }

        // Engine
        var validationEngine = FhirValidationEngineCache.getValidationEngine();

        // Create parser
        var parserR4 = new org.hl7.fhir.r4.formats.JsonParser();
        var parserR5 = new org.hl7.fhir.r5.formats.JsonParser();

        // Result
        var responseBundle = new org.hl7.fhir.r4.model.Bundle();
        responseBundle.setType(Bundle.BundleType.COLLECTION);

        // Validates every entry individually
        int i = 0;
        for (var entry: requestBundle.getEntry()) {
            var resource = entry.getResource();
            if (resource == null) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "].resource cannot be null");
            }
            var resourceBytes = parserR4.composeBytes(resource);
            org.hl7.fhir.r5.model.OperationOutcome ooR5;
            var messages = new ArrayList<ValidationMessage>();
            try {
                ooR5 = validationEngine.validate(resourceBytes, Manager.FhirFormat.JSON, new ArrayList<String>(), messages);
            }
            catch (org.hl7.fhir.r5.utils.EOperationOutcome e) {
                ooR5 = e.getOutcome();
            }
            var ooR5Bytes = parserR5.composeBytes(ooR5);
            var ooR4 = (OperationOutcome)parserR4.parse(ooR5Bytes);
            if (configuration.removeText != null && configuration.removeText) {
                ooR4.setText(null);
            }
            var responseEntry = responseBundle.addEntry();
            responseEntry.setFullUrl(entry.getFullUrl());
            responseEntry.setResponse(new Bundle.BundleEntryResponseComponent());
            responseEntry.getResponse().setOutcome(ooR4);
        }

        var result = new FhirValidationResult();
        result.resourceBytes = parserR4.composeBytes(responseBundle);
        return  result;
    }

    public static FhirValidationResult validateBundleR5(org.hl7.fhir.r5.model.Bundle requestBundle, FhirValidatorConfiguration configuration) throws IOException
    {
        // Checks
        if (requestBundle == null) {
            throw new IllegalArgumentException("Bundle cannot be null");
        }
        if (requestBundle.getType() != org.hl7.fhir.r5.model.Bundle.BundleType.BATCH &&
            requestBundle.getType() != org.hl7.fhir.r5.model.Bundle.BundleType.TRANSACTION) {
            throw new IllegalArgumentException("Bundle type must be of type BATCH or TRANSACTION");
        }

        // Engine
        var validationEngine = FhirValidationEngineCache.getValidationEngine();

        // Create parser
        var parserR5 = new org.hl7.fhir.r5.formats.JsonParser();

        // Result
        var responseBundle = new org.hl7.fhir.r5.model.Bundle();
        responseBundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.COLLECTION);

        // Validates every entry individually
        int i = 0;
        for (var requestEntry: requestBundle.getEntry()) {
            var resource = requestEntry.getResource();
            if (resource == null) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "].resource cannot be null");
            }
            var resourceBytes = parserR5.composeBytes(resource);
            org.hl7.fhir.r5.model.OperationOutcome ooR5;
            var messages = new ArrayList<ValidationMessage>();
            try {
                ooR5 = validationEngine.validate(resourceBytes, Manager.FhirFormat.JSON, new ArrayList<String>(), messages);
            }
            catch (org.hl7.fhir.r5.utils.EOperationOutcome e) {
                ooR5 = e.getOutcome();
            }
            if (configuration.getRemoveText() != null && configuration.getRemoveText()) {
                ooR5.setText(null);
            }
            var responseEntry = responseBundle.addEntry();
            responseEntry.setFullUrl(requestEntry.getFullUrl());
            responseEntry.setResponse(new org.hl7.fhir.r5.model.Bundle.BundleEntryResponseComponent());
            responseEntry.getResponse().setOutcome(ooR5);
        }
        var result = new FhirValidationResult();
        result.resourceBytes = parserR5.composeBytes(responseBundle);
        return result;
    }

    public static FhirValidationResult validateBytes(byte[] resourceBytes, List<String> profileList, FhirValidatorConfiguration configuration) throws Throwable {
        long start = System.currentTimeMillis();

        // Sanitizing the list of profiles
        if (profileList == null) profileList = new ArrayList<>();
        List<String> internalProfileList = new ArrayList<>();

        for (var profile : profileList) {
            if (profile != null && !profile.trim().isBlank()) {
                internalProfileList.add(profile);
            }
        }

        var validationEngine = FhirValidationEngineCache.getValidationEngine();
        var validationResult = new FhirValidationResult();
        var parserR5 = new JsonParser();
        parserR5.setOutputStyle(IParser.OutputStyle.NORMAL);
        try {
            var messages = new ArrayList<ValidationMessage>();
            var operationOutcome = validationEngine.validate(resourceBytes, Manager.FhirFormat.JSON, internalProfileList, messages);
            if (configuration.getRemoveText() != null && configuration.getRemoveText()) {
                operationOutcome.setText(null);
            }

            validationResult.messages = messages;
            validationResult.resourceBytes = parserR5.composeBytes(operationOutcome);
            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            log.info("FhirValidator::validateBytes - OK ({} bytes for {} ms)", resourceBytes.length, timeElapsed);
        } catch (Throwable e) {
            var stackTrace = ExceptionUtils.getStackTrace(e);

            log.error("FhirValidator::validateBytes - fatal error occurred: {}", String.valueOf(e));
            log.error("  Stack trace: {}", stackTrace);

            var operationOutcome2 = FhirUtil.exceptionToOutcome(e);
            var message = new ValidationMessage();
            message.setMessage(e.getMessage());
            message.setType(ValidationMessage.IssueType.EXCEPTION);
            message.setLevel(ValidationMessage.IssueSeverity.FATAL);
            message.setLocation(stackTrace);
            validationResult.resourceBytes = parserR5.composeBytes(operationOutcome2);
            validationResult.messages.add(message);
        }
        return validationResult;
    }
}
