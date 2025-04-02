package il.co.outburn.rest;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("validator")
public class FhirValidatorConfiguration {

    @Getter
    String sv;
    @Getter
    List<String> ig = new ArrayList<>();
    @Getter
    String txServer;
    @Getter
    String txLog;

    public void setSv(String value) {
        sv = value;
    }

    public void setIg(List<String> value) {
        ig = value;
    }

    public void setTxServer(String value) {
        txServer = value;
    }

    public void setTxLog(String value) {
        txLog = value;
    }
}
