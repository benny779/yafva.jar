package il.co.outburn.rest;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.validation.ValidationEngine;

import java.io.IOException;

@Slf4j
public class FhirValidationEngineCache {
    @Getter
    @Setter
    static ValidationEngine defaultValidationEngine;

    static ThreadLocal<ValidationEngine> validationEngine = new ThreadLocal<>();

    public static ValidationEngine getValidationEngine() throws IOException {
        ValidationEngine result = validationEngine.get();
        if (result == null) {
            log.warn("For current thread, ValidationEngine is not initialized. Cloning...");
            result = new ValidationEngine(defaultValidationEngine);
            validationEngine.set(result);
            log.info("An instance of ValidationEngine has been initialized for the current thread.");
        } else {
            log.info("An instance of ValidationEngine for the current thread found in the cache.");
        }
        return result;
    }
}
