package il.co.outburn.rest;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web layer configuration.
 * <p>
 * Enables a simple, permissive CORS policy that allows requests from any origin
 * to every endpoint. This lets browser-based clients call the validator directly
 * without a proxy. No credentials are allowed, so wildcard origins are safe here.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
