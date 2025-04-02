package il.co.outburn.rest;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import java.util.List;

public class FhirValidatorResponse {
    @Getter
    @Setter
    List<ValidationMessage> messages;
}
