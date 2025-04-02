package il.co.outburn.rest;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.IParser;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FhirValidator {
    public static FhirValidationResult validateBytes(byte[] resourceBytes, List<String> profileList) throws Throwable {
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
            operationOutcome.setText(null);

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
