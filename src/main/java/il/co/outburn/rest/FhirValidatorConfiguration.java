package il.co.outburn.rest;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.utilities.VersionUtilities;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties("validator")
public class FhirValidatorConfiguration {

    /**
     * The FHIR version to use for validation. This is a mandatory field.
     */
    @Setter
    String sv;
    public String getSv() {
        if (sv == null || sv.isBlank() || sv.isEmpty())
            return "4.0.1";
        return sv;
    }

    public boolean isR4Ver() {
        return VersionUtilities.isR4Ver(sv);
    }
    public boolean isR5Ver() {
        return VersionUtilities.isR5Ver(sv);
    }

    /**
     * The IGs to load for validation. This is an optional field.
     */
    @Getter
    List<String> ig = new ArrayList<>();
    public void setIg(List<String> value) {
        ig = value;
    }

    /**
     * The terminology server URL to use for validation. This is an optional field.
     */
    @Setter
    String txServer;
    public String getTxServer() {
        if (txServer == null || txServer.isBlank() || txServer.isEmpty())
            return null;
        return txServer;
    }

    /**
     * The terminology server log file to use for validation. This is an optional field.
     */
    @Setter
    String txLog;
    public String getTxLog() {
        if (txLog == null || txLog.isBlank() || txLog.isEmpty())
            return null;
        return txLog;
    }

    /**
     * Removes OperationOutcome text from the validation result. This is an optional field.
     */
    @Getter
    @Setter
    Boolean removeText = false;

    /**
     * Allow all unknown extensions. By default, unknown extensions are prohibited.
     */
    @Getter
    @Setter
    Boolean anyExtensionsAllowed = false;

    /**
     * Extension domains to allow. This allows extensions from specified domains.
     * Extensions from specified domains are allowed by matching the URL for the extension.
     */
    @Getter
    @Setter
    List<String> extensionDomains = new ArrayList<>();

    /**
     * When an unknown code system is encountered, determines whether this causes an error or warning.
     * By default, unknown code systems cause warnings.
     */
    @Getter
    @Setter
    Boolean unknownCodeSystemsCauseErrors = false;

    /**
     * Allow URLs from example.org. By default, references to example.org are marked as errors.
     */
    @Getter
    @Setter
    Boolean allowExampleUrls = false;

    /**
     * When the validator encounters a coding or CodeableConcept where the display
     * value isn't consistent with the display(s) defined by the code systems, this controls
     * whether it's treated as an error or warning. By default, wrong display names are errors.
     */
    @Getter
    @Setter
    Boolean displayWarnings = false;

    /**
     * Control whether the FHIRPath for invariants is included in the message.
     * By default, FHIR Path is omitted for easier end-user consumption.
     */
    @Getter
    @Setter
    Boolean wantInvariantInMessage = false;

    /**
     * Set the minimum level for validation messages. Valid values: hints, warnings, errors.
     * Default is hints (report all hints, warnings and errors).
     */
    @Getter
    @Setter
    String level = "warnings";

    /**
     * When verbose is set, the validator will create hints against the resources
     * to explain which profiles it has validated the resource against, and why.
     */
    @Getter
    @Setter
    Boolean verbose = false;

    /**
     * When show-times is set, the validator will produce a line in the output
     * summarizing how long some internal processes took.
     */
    @Getter
    @Setter
    Boolean showTimes = false;

    /**
     * Controls how best practice rules are handled. Best practice rules are constraints
     * in the specification that are warnings but marked as 'best practice'. These are 
     * typically rules that the committees believe should be followed, but cannot be due 
     * to legacy data constraints.
     * Valid values: hint, warning, error, ignore. Default is warning.
     */
    @Getter
    @Setter
    String bestPracticeLevel = "warning";

    /**
     * Returns the core package for the specified FHIR version.
     */
    public String getCorePackage() {
        var fhirVersion = sv;
        if (fhirVersion == null || fhirVersion.isBlank())
            fhirVersion = "4.0.1";
        var corePackage = VersionUtilities.packageForVersion(fhirVersion) + "#" + VersionUtilities.getCurrentVersion(fhirVersion);

        return corePackage;
    }

    /**
     * Returns all properties of the configuration.
     */
    public List<String> getAllProperties() {
        return List.of(
            "FHIR Version: " + getSv(),
            "Implementation Guides: " + ig,
            "Terminology Server URL: " + getTxServer(),
            "Terminology Server Log: " + getTxLog(),
            "Remove OperationOutcome Text: " + removeText,
            "Allow Any Extensions: " + anyExtensionsAllowed,
            "Allowed Extension Domains: " + extensionDomains,
            "Unknown Code Systems Cause Errors: " + unknownCodeSystemsCauseErrors,
            "Allow Example URLs: " + allowExampleUrls,
            "Display Warnings Instead of Errors: " + displayWarnings,
            "Include FHIRPath in Invariant Messages: " + wantInvariantInMessage,
            "Validation Level: " + level,
            "Best Practice Level: " + bestPracticeLevel,
            "Verbose Output: " + verbose,
            "Show Processing Times: " + showTimes
        );
    }
}
