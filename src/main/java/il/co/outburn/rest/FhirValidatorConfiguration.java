package il.co.outburn.rest;

import lombok.Getter;
import org.hl7.fhir.utilities.VersionUtilities;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("validator")
public class FhirValidatorConfiguration {

    /**
     * The FHIR version to use for validation. This is a mandatory field.
     */
    String sv;
    public void setSv(String value) {
        sv = value;
    }
    public String getSv() {
        if (sv == null || sv.isBlank() || sv.isEmpty())
            return "4.0.1";
        return sv;
    }

    public Boolean isR4Ver() {
        return VersionUtilities.isR4Ver(sv);
    }
    public Boolean isR5Ver() {
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
    @Getter
    String txServer;
    public void setTxServer(String value) {
        txServer = value;
    }

    /**
     * The terminology server log file to use for validation. This is an optional field.
     */
    @Getter
    String txLog;

    public void setTxLog(String value) {
        txLog = value;
    }

    /**
     * Removes OperationOutcome text from the validation result. This is an optional field.
     */
    @Getter
    Boolean removeText = false;

    public void setRemoveText(Boolean value) {
        removeText = value;
    }

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
}
