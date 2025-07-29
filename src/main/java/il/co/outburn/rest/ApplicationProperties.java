package il.co.outburn.rest;

import java.io.IOException;
import java.util.List;

public class ApplicationProperties {
    public static String getAppVersion() {
        Package pkg = ApplicationProperties.class.getPackage();
        String version = (pkg != null) ? pkg.getImplementationVersion() : "Unknown";
        return (version != null) ? version : "Unknown";
    }

    public static String getHapiValidatorVersion() {
        return org.hl7.fhir.utilities.VersionUtil.getVersion();
    }

    public static class ApplicationInfo {
        public String appVersion;
        public String hapiValidatorVersion;
        public String fhirVersion;
        public List<String> implementationGuides;
        public List<String> loadedPackages;
        public String cacheFolder;
        public String terminologyServer;

        public ApplicationInfo(FhirValidatorConfiguration configuration) throws IOException {
            var validationEngine = FhirValidationEngineCache.getValidationEngine();

            appVersion = getAppVersion();
            hapiValidatorVersion = getHapiValidatorVersion();
            fhirVersion = configuration.getSv();
            implementationGuides = configuration.ig;
            loadedPackages = validationEngine.getContext().getLoadedPackages();
            cacheFolder = validationEngine.getPcm().getFolder();
            terminologyServer = configuration.txServer;
        }
    }
}
