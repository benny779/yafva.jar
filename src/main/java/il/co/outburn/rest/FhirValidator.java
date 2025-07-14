package il.co.outburn.rest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class FhirValidator {

    public static FhirValidationResult validateBundle(byte[] requestBundle, FhirValidatorConfiguration configuration) throws IOException {
        if (!isFhirVersionSupported(configuration)) {
            throw new UnsupportedOperationException("Unsupported FHIR version: " + configuration.getSv());
        }
        
        var gson = new Gson();
        var jsonStr = new String(requestBundle, StandardCharsets.UTF_8);
        var json = gson.fromJson(jsonStr, JsonObject.class);
        return validateBundle(json, configuration);
    }

    public static FhirValidationResult validateBundle(JsonObject requestBundle, FhirValidatorConfiguration configuration)
        throws IOException
    {
        // Checks
        if (!isFhirVersionSupported(configuration)) {
            throw new UnsupportedOperationException("Unsupported FHIR version: " + configuration.getSv());
        }
        ensureValidBundle(requestBundle);
        
        // Engine
        var validationEngine = FhirValidationEngineCache.getValidationEngine();

        // Create parser
        var gson = new Gson();
        var fhirJsonParser = createFhirJsonParser();

        // Result
        var responseBundle = new org.hl7.fhir.r5.model.Bundle();
        responseBundle.setType(org.hl7.fhir.r5.model.Bundle.BundleType.COLLECTION);

        // Validates every entry individually
        for (var entryElement: requestBundle.getAsJsonArray("entry")) {
            var entry = entryElement.getAsJsonObject();
            var resource = entry.get("resource");

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
        result.resourceBytes = fhirJsonParser.composeBytes(responseBundle);
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
        var fhirJsonParser = createFhirJsonParser();
        try {
            var messages = new ArrayList<ValidationMessage>();
            var operationOutcome = validationEngine.validate(resourceBytes, Manager.FhirFormat.JSON, internalProfileList, messages);
            if (configuration.getRemoveText() != null && configuration.getRemoveText()) {
                operationOutcome.setText(null);
            }

            validationResult.messages = messages;
            validationResult.resourceBytes = fhirJsonParser.composeBytes(operationOutcome);
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
            validationResult.resourceBytes = fhirJsonParser.composeBytes(operationOutcome2);
            validationResult.messages.add(message);
        }
        return validationResult;
    }

    private static Boolean isFhirVersionSupported(FhirValidatorConfiguration configuration) {
        return configuration.isR4Ver() || configuration.isR5Ver();
    }

    private static JsonParser createFhirJsonParser() {
        var fhirJsonParser = new JsonParser();
        fhirJsonParser.setOutputStyle(IParser.OutputStyle.NORMAL);
        return fhirJsonParser;
    }

    private static void ensureValidBundle(JsonObject bundle) {
        if (bundle == null || !bundle.isJsonObject()) {
            throw new IllegalArgumentException("Request bundle must be a JSON object");
        }

        var resouceTypeJsonElement = bundle.get("resourceType");
        if (resouceTypeJsonElement == null || !resouceTypeJsonElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Bundle.resourceType must be a JSON primitive");
        }
        var resourceType = resouceTypeJsonElement.getAsString();
        if (!resourceType.equals("Bundle")) {
            throw new IllegalArgumentException("Bundle resourceType must be of type BUNDLE");
        }

        var bundleTypeJsonElement = bundle.get("type");
        if (bundleTypeJsonElement == null || !bundleTypeJsonElement.isJsonPrimitive()) {
            throw new IllegalArgumentException("Bundle.type must be a JSON primitive");
        }
        var bundleType = bundleTypeJsonElement.getAsString();
        if (!bundleType.equals("batch")) {
            throw new IllegalArgumentException("Bundle type must be of type BATCH");
        }

        var bundleEntriesJsonElement = bundle.get("entry");
        if (bundleEntriesJsonElement != null && !bundleEntriesJsonElement.isJsonArray()) {
            throw new IllegalArgumentException("Bundle.entry must be a JSON array");
        }
        if (bundleEntriesJsonElement == null) {
            bundle.add("entry", new JsonArray());
        }
        
        Set<String> fullUrls = new HashSet<>();
        var entries = bundle.getAsJsonArray("entry");
        for (int i = 0; i < entries.size(); i++) {
            var entryElement = entries.get(i);
            if (!entryElement.isJsonObject()) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "] must be a JSON object");
            }

            var entry = entryElement.getAsJsonObject();
            
            var resource = entry.get("resource");
            if (resource == null || !resource.isJsonObject()) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "].resource must be a JSON object");
            }

            var fullUrl = entry.get("fullUrl");
            if (fullUrl == null) {
                continue;
            }
            if (!fullUrl.isJsonPrimitive()) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "].fullUrl must be a JSON primitive");
            }
            if (!fullUrls.add(fullUrl.getAsString())) {
                throw new IllegalArgumentException("Bundle.entry[" + i + "].fullUrl must be unique");
            }
        }
    }
}
