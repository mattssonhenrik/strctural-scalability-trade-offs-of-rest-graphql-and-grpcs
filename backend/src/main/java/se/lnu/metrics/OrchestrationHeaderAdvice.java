package se.lnu.metrics;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Writes X-Orchestration-Count header before the response body is serialized.
 *
 * ResponseBodyAdvice is the only hook that fires after the controller has
 * finished all DataStore calls but before the response is committed.
 * Interceptor postHandle and Servlet filters are both too late for @RestController.
 */
@ControllerAdvice
public class OrchestrationHeaderAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().set("X-Orchestration-Count",
                String.valueOf(OrchestrationCounter.get()));
        OrchestrationCounter.reset();
        return body;
    }
}
