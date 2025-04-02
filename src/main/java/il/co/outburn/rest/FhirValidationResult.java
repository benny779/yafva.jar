package il.co.outburn.rest;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import java.util.ArrayList;
import java.util.List;

@Getter
public class FhirValidationResult {

    @Getter
    @Setter
    byte[] resourceBytes;

    @Getter
    @Setter
    List<ValidationMessage> messages = new ArrayList<>();

    public FhirValidationResult() {
    }
}

