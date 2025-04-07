package il.co.outburn.rest;

import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r5.utils.validation.constants.ReferenceValidationPolicy;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.validation.IgLoader;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.instance.advisor.BasePolicyAdvisorForFullValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class FhirValidatorApplication {

    FhirValidatorConfiguration configuration;

    public FhirValidatorApplication(@Autowired FhirValidatorConfiguration configuration) throws Throwable {
        this.configuration = configuration;
        initializeDefaultValidationEngine();
    }

    public static void redirectStdStreamsToLog(boolean redirect) {
        if (redirect) {
            System.setOut(new FhirLogPrintStream(System.out, false));
            System.setErr(new FhirLogPrintStream(System.err, true));
        } else {
            System.setOut(System.out);
            System.setErr(System.err);
        }
    }

    public static void main(String[] args) {
        FhirValidatorApplication.redirectStdStreamsToLog(true);
        SpringApplication.run(FhirValidatorApplication.class, args);
    }

    private void initializeDefaultValidationEngine() throws Throwable {
        log.info("Start initializing default ValidationEngine");
        try {
            if (configuration.txServer != null && configuration.txServer.isEmpty())
                configuration.txServer = null;
            if (configuration.txLog != null && configuration.txLog.isEmpty())
                configuration.txLog = null;

            var fhirVersion = configuration.sv;
            if (fhirVersion == null)
                fhirVersion = "4.0.1";

            boolean canRunWithoutTerminologyServer = (configuration.txServer == null);

            var loggingService = new FhirLoggingService();

            var builder = new ValidationEngine.ValidationEngineBuilder()
                    .withVersion(fhirVersion)
                    .withTxServer(configuration.txServer, configuration.txLog, null, true)
                    .withCanRunWithoutTerminologyServer(canRunWithoutTerminologyServer)
                    .withLoggingService(loggingService);
            var corePackage = VersionUtilities.packageForVersion(fhirVersion) + "#" + VersionUtilities.getCurrentVersion(fhirVersion);

            log.info("FHIR version: {}", fhirVersion);
            log.info("Core package: {}", corePackage);
            log.info("Tx server: {}", configuration.txServer);
            log.info("Tx log: {}", configuration.txLog);

            var newValidationEngine = builder.fromSource(corePackage);
            newValidationEngine.setDebug(true);
            newValidationEngine.setDisplayWarnings(true);
            newValidationEngine.setPolicyAdvisor(new BasePolicyAdvisorForFullValidation(ReferenceValidationPolicy.IGNORE));
            IgLoader igLoader = newValidationEngine.getIgLoader();

            if (configuration.ig != null) {
                for (String ig : configuration.ig) {
                    if (ig != null) {
                        igLoader.loadIg(newValidationEngine.getIgs(), newValidationEngine.getBinaries(), ig, true);
                    }
                }
            }
            newValidationEngine.prepare();
            log.info("Default ValidationEngine is initialized.");
            FhirValidationEngineCache.setDefaultValidationEngine(newValidationEngine);
        } catch (Exception ex) {
            log.error("Failed to initialize default ValidationEngine", ex);
            throw ex;
        }
    }

}
