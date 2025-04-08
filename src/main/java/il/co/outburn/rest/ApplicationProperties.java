package il.co.outburn.rest;

public class ApplicationProperties {
    public static String getAppVersion() {
        Package pkg = ApplicationProperties.class.getPackage();
        String version = (pkg != null) ? pkg.getImplementationVersion() : "Unknown";
        return (version != null) ? version : "Unknown";
    }

    public static String getHapiValidatorVersion() {
        return org.hl7.fhir.utilities.VersionUtil.getVersion();
    }

    public static class VersionInfo {
        public String appVersion = getAppVersion();
        public String hapiValidatorVersion = getHapiValidatorVersion();
    }
}
