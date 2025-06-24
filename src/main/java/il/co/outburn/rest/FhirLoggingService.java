package il.co.outburn.rest;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.context.ILoggingService;

// https://hapifhir.io/hapi-fhir/docs/appendix/logging.html
@Slf4j
public class FhirLoggingService implements ILoggingService {
    @Override
    public void logMessage(String s) {
        log.info(s);
    }

    @Override
    public void logDebugMessage(LogCategory logCategory, String s) {
        log.debug(s);
    }
}
