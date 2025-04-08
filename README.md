# YAFJAVAR
## (Yet Another) FHIR JAva VAlidator wRapper
This is a wrapper for the HL7/HAPI Java validator that exposes the validation engine as an HTTP server.

Unlike the official FHIR Validator Wrapper, this project is tailored towards production use, where the "context" (The loaded FHIR packages) is fixed, so the memory cache can be cloned in runtime for separate threads, instead of loaded from disk every time.
The client does not need to worry about user session initialization times, sesssion ID's or session TTL.

### ⚙️ Configuration Options
Configurations can be set using application.yaml or by passing command line arguments when calling the JAR:  
`java -jar validator.jar --server.port=3500 --validator.tx-server= --validator.sv=4.0.1 --server.tomcat.threads.min-spare=9 --server.tomcat.threads.max=15 --validator.ig[0]=hl7.fhir.r4.examples#4.0.1`  
The above command will start the server using port 3500, FHIR version R4, with no terminology server, 9-15 concurrent threads and the package `hl7.fhir.r4.examples#4.0.1` in context.

This same configuration in `application.yaml` will look like this:
```
server:
  port: 3500
  tomcat:
    threads:
      min-spare: 9
      max: 15

validator:
  sv: '4.0.1'
  ig:
    - 'hl7.fhir.r4.examples#4.0.1'
  tx-server:
```
### ⚙️ Recommended server.tomcat.threads configuration
As a baseline it is recommended to start with:  
`min-spare`: ~0.75 * CPU's  
`max`: ~1.25 * CPU's  
For example, the configurations supplied above of 9-15 is for a 12-core machine.

## HTTP Endpoints
### GET / (Root)
Home page (HTML) with information about the deployed validator server.

### GET /swagger-ui/index.html
API documentation page (Swagger)

### /validate
Validate a single resource.  
*optional URL parameters*: 
- profiles
- format

### /validateBundle
Validate a bath Bundle and recieve the results as a Bundle of OperationOutcomes.