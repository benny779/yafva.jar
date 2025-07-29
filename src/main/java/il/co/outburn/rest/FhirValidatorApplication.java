package il.co.outburn.rest;
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;
import org.hl7.fhir.r5.utils.validation.constants.ReferenceValidationPolicy;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.validation.IgLoader;
import org.hl7.fhir.validation.ValidationEngine;
import org.hl7.fhir.validation.instance.advisor.BasePolicyAdvisorForFullValidation;
import org.hl7.fhir.validation.service.utils.ValidationLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;

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
        readAndExecuteParams(args);
        FhirValidatorApplication.redirectStdStreamsToLog(true);
        SpringApplication.run(FhirValidatorApplication.class, args);
    }

    private static void readAndExecuteParams(String[] args) {
        if (args.length == 0) return;

        if (args[0].equals("-v") || args[0].equals("-version")) {
            printVersions();
            System.exit(0);
        }
    }

    private static void printVersions() {
        var appVersion = "Application version:\t" + ApplicationProperties.getAppVersion();
        var hapiValidatorVersion = "HAPI Validator version:\t" + ApplicationProperties.getHapiValidatorVersion();

        System.out.println(appVersion);
        System.out.println(hapiValidatorVersion);
    }

    private void initializeDefaultValidationEngine() throws Throwable {
        log.info("Start initializing default ValidationEngine");
        try {
            if (configuration.txServer != null && configuration.txServer.isEmpty())
                configuration.txServer = null;
            if (configuration.txLog != null && configuration.txLog.isEmpty())
                configuration.txLog = null;

            boolean canRunWithoutTerminologyServer = (configuration.txServer == null);
            var loggingService = new FhirLoggingService();

            var newValidationEngine = createValidationEngine(configuration.getSv(), canRunWithoutTerminologyServer, loggingService);
            newValidationEngine.prepare();
            log.info("Default ValidationEngine is initialized.");
            FhirValidationEngineCache.setDefaultValidationEngine(newValidationEngine);
        } catch (Exception ex) {
            log.error("Failed to initialize default ValidationEngine", ex);
            throw ex;
        }
    }

    private ValidationEngine createValidationEngine(String fhirVersion, boolean canRunWithoutTerminologyServer, FhirLoggingService loggingService) throws Exception {
        var builder = new ValidationEngine.ValidationEngineBuilder()
                .withVersion(fhirVersion)
                .withTxServer(configuration.txServer, configuration.txLog, null, true)
                .withCanRunWithoutTerminologyServer(canRunWithoutTerminologyServer)
                .withLoggingService(loggingService);
        var corePackage = VersionUtilities.packageForVersion(fhirVersion) + "#" + VersionUtilities.getCurrentVersion(fhirVersion);

        log.info("Core package: {}", corePackage);
        configuration.getAllProperties().forEach(log::info);

        var validationEngine = builder.fromSource(corePackage);
        validationEngine.setDebug(true);
        validationEngine.setPolicyAdvisor(new BasePolicyAdvisorForFullValidation(ReferenceValidationPolicy.IGNORE));

        validationEngine.setAnyExtensionsAllowed(configuration.anyExtensionsAllowed);
        validationEngine.setUnknownCodeSystemsCauseErrors(configuration.unknownCodeSystemsCauseErrors);
        if (configuration.extensionDomains != null && !configuration.extensionDomains.isEmpty()) {
            validationEngine.getExtensionDomains().addAll(configuration.extensionDomains);
        }

        validationEngine.setAllowExampleUrls(configuration.allowExampleUrls);
        validationEngine.setDisplayWarnings(configuration.displayWarnings);
        validationEngine.setWantInvariantInMessage(configuration.wantInvariantInMessage);
        validationEngine.setLevel(ValidationLevel.fromCode(configuration.level));
        validationEngine.setBestPracticeLevel(readBestPractice(configuration.bestPracticeLevel));
        validationEngine.setCrumbTrails(configuration.verbose);
        validationEngine.setShowTimes(configuration.showTimes);

        IgLoader igLoader = validationEngine.getIgLoader();
        if (configuration.ig != null) {
            for (String ig : configuration.ig) {
                if (ig != null) {
                    igLoader.loadIg(validationEngine.getIgs(), validationEngine.getBinaries(), ig, true);
                }
            }
        }

        return validationEngine;
    }

    private static BestPracticeWarningLevel readBestPractice(String s) {
        if (s == null) return BestPracticeWarningLevel.Warning;
        switch (s.toLowerCase()) {
            case "warning" : return BestPracticeWarningLevel.Warning;
            case "error" : return BestPracticeWarningLevel.Error;
            case "hint" : return BestPracticeWarningLevel.Hint;
            case "ignore" : return BestPracticeWarningLevel.Ignore;
            case "w" : return BestPracticeWarningLevel.Warning;
            case "e" : return BestPracticeWarningLevel.Error;
            case "h" : return BestPracticeWarningLevel.Hint;
            case "i" : return BestPracticeWarningLevel.Ignore;
        }
        throw new Error("The best-practice level '" + s + "' is not valid");
    }
}
