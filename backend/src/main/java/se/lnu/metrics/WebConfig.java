package se.lnu.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers OrchestrationInterceptor with Spring MVC.
 * 
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private OrchestrationInterceptor orchestrationInterceptor;

    /**
     * Registers orchestrationInterceptor for all HTTP endpoints
     * 
     * @param InterceptorRegistry springs interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(orchestrationInterceptor);
    }
}
