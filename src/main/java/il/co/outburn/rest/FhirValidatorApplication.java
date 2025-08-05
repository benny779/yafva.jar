package il.co.outburn.rest;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;
import org.hl7.fhir.r5.utils.validation.constants.ReferenceValidationPolicy;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.VersionUtilities;
import org.hl7.fhir.utilities.settings.FhirSettings;
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
            configureFhirSettings(); // must be called before creating the ValidationEngine
            var loggingService = new FhirLoggingService();
            var validationEngine = createValidationEngine(configuration.getSv(), loggingService);
            loadIgs(validationEngine);
            validationEngine.prepare();
            log.info("Default ValidationEngine is initialized.");
            FhirValidationEngineCache.setDefaultValidationEngine(validationEngine);
        } catch (Exception ex) {
            log.error("Failed to initialize default ValidationEngine", ex);
            throw ex;
        }
    }

    private void configureFhirSettings() throws FileNotFoundException  {
        var settingsFilePath = configuration.getSettingsFilePath();
        if (settingsFilePath == null) return;

        var path = Paths.get(settingsFilePath);
        if (!Files.exists(path)) {
            log.error("FHIR settings file does not exist: {}", settingsFilePath);
            throw new FileNotFoundException("FHIR settings file does not exist: " + settingsFilePath);
        }

        settingsFilePath = path.toAbsolutePath().toString();
        log.info("FHIR settings file path: {}", settingsFilePath);
        FhirSettings.setExplicitFilePath(settingsFilePath);
        
        // Log configured servers for debugging
        var servers = FhirSettings.getServers();
        log.info("FHIR Settings servers: {}", servers);
    }

    private ValidationEngine createValidationEngine(String fhirVersion, FhirLoggingService loggingService) throws Exception {
        boolean canRunWithoutTerminologyServer = (configuration.getTxServer() == null);
        var builder = new ValidationEngine.ValidationEngineBuilder()
                .withVersion(fhirVersion)
                .withTxServer(configuration.getTxServer(), configuration.getTxLog(), null, true)
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

        return validationEngine;
    }

    private static BestPracticeWarningLevel readBestPractice(String s) {
        if (Utilities.noString(s)) {
            return BestPracticeWarningLevel.Warning;
        }
        s = s.toLowerCase();
        if (Utilities.existsInList(s, "h", "hint", "hints")) {
            return BestPracticeWarningLevel.Hint;
        }
        if (Utilities.existsInList(s, "w", "warning", "warnings")) {
            return BestPracticeWarningLevel.Warning;
        }
        if (Utilities.existsInList(s, "e", "error", "errors")) {
            return BestPracticeWarningLevel.Error;
        }
        if (Utilities.existsInList(s, "i", "ignore")) {
            return BestPracticeWarningLevel.Ignore;
        }
        return BestPracticeWarningLevel.Warning;
    }

    public void loadIgs(ValidationEngine validationEngine) throws Exception {
        IgLoader igLoader = validationEngine.getIgLoader();
        if (configuration.ig != null) {
            for (String ig : configuration.ig) {
                if (!Utilities.noString(ig)) {
                    igLoader.loadIg(validationEngine.getIgs(), validationEngine.getBinaries(), ig, true);
                }
            }
        }
    }
}
