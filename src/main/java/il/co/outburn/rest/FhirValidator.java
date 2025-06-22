package il.co.outburn.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FhirValidator {

    public static FhirValidationResult validateBundle(byte[] requestBundle, FhirValidatorConfiguration configuration) throws IOException {
        if (!isFhirVersionSupported(configuration)) {
            throw new UnsupportedOperationException("Unsupported FHIR version: " + configuration.getSv());
        }
        
        var gson = new Gson();
        var jsonStr = new String(requestBundle, StandardCharsets.UTF_8);
        var json = gson.fromJson(jsonStr, JsonObject.class);

        if (configuration.isR4Ver()) {
            return validateBundleR4(json, configuration);
        }

        return validateBundleR5(json, configuration);
    }

    public static FhirValidationResult validateBundleR4(JsonObject requestBundle, FhirValidatorConfiguration configuration)
        throws IOException
    {
        // Checks
        if (requestBundle == null) {
            throw new IllegalArgumentException("Bundle cannot be null");
        }
        var bundleTypeJsonElement = requestBundle.get("type");
        if (bundleTypeJsonElement == null || !bundleTypeJsonElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Bundle.type must be a JSON primitive");
        }
        var bundleType = bundleTypeJsonElement.getAsString();
        if (!bundleType.equals(Bundle.BundleType.BATCH.toCode())) {
            throw new IllegalArgumentException("Bundle type must be of type BATCH");
        }

        if (!requestBundle.has("entry")) {
            requestBundle.add("entry", new JsonArray());
        }

        // Engine
        var validationEngine = FhirValidationEngineCache.getValidationEngine();

        // Create parser
        var gson = new Gson();
        var parserR4 = new org.hl7.fhir.r4.formats.JsonParser();
        var parserR5 = new org.hl7.fhir.r5.formats.JsonParser();

        // Result
        var responseBundle = new org.hl7.fhir.r4.model.Bundle();
        responseBundle.setType(Bundle.BundleType.COLLECTION);

        // Validates every entry individually
        int i = 0;
        for (var entryElement: requestBundle.getAsJsonArray("entry")) {
            if (!entryElement.isJsonObject()) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "] must be a JSON object");
            }
            var entry = entryElement.getAsJsonObject();
            var resource = entry.getAsJsonObject().get("resource");
            if (resource == null) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "].resource cannot be null");
            }
            
            var resourceBytes = gson.toJson(resource).getBytes();
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
            if (entry.has("fullUrl")) {
                responseEntry.setFullUrl(entry.get("fullUrl").getAsString());
            }
            responseEntry.setResponse(new Bundle.BundleEntryResponseComponent());
            responseEntry.getResponse().setOutcome(ooR4);
        }

        var result = new FhirValidationResult();
        result.resourceBytes = parserR4.composeBytes(responseBundle);
        return  result;
    }

    public static FhirValidationResult validateBundleR5(JsonObject requestBundle, FhirValidatorConfiguration configuration)
        throws IOException
    {
        // Checks
        if (requestBundle == null) {
            throw new IllegalArgumentException("Bundle cannot be null");
        }

        var bundleTypeJsonElement = requestBundle.get("type");
        if (bundleTypeJsonElement == null || !bundleTypeJsonElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Bundle.type must be a JSON primitive");
        }
        var bundleType = bundleTypeJsonElement.getAsString();
        if (!bundleType.equals(org.hl7.fhir.r5.model.Bundle.BundleType.BATCH.toCode())) {
            throw new IllegalArgumentException("Bundle type must be of type BATCH");
        }

        if (!requestBundle.has("entry")) {
            requestBundle.add("entry", new JsonArray());
        }

        // Engine
        var validationEngine = FhirValidationEngineCache.getValidationEngine();

        // Create parser
        var gson = new Gson();
        var parserR5 = new org.hl7.fhir.r5.formats.JsonParser();

        // Result
        var responseBundle = new org.hl7.fhir.r5.model.Bundle();
        responseBundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.COLLECTION);

        // Validates every entry individually
        int i = 0;
        for (var entryElement: requestBundle.getAsJsonArray("entry")) {
            if (!entryElement.isJsonObject()) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "] must be a JSON object");
            }
            var entry = entryElement.getAsJsonObject();
            var resource = entry.getAsJsonObject().get("resource");
            if (resource == null) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "].resource cannot be null");
            }

            var resourceBytes = gson.toJson(resource).getBytes();
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
            if (entry.has("fullUrl")) {
                responseEntry.setFullUrl(entry.get("fullUrl").getAsString());
            }
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

    private static Boolean isFhirVersionSupported(FhirValidatorConfiguration configuration) {
        return configuration.isR4Ver() || configuration.isR5Ver();
    }
}
