package se.lnu.metrics;

import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Resets the CM2 counter before GraphQL execution and writes the final count
 * as a response header after all resolvers have completed.
 *
 * ResponseBodyAdvice does not fire for Spring for GraphQL responses — it uses
 * a separate response-writing pipeline. WebGraphQlInterceptor is the correct hook.
 */
@Component
public class OrchestrationGraphQlInterceptor implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
        OrchestrationCounter.reset();
        return chain.next(request).doOnNext(response ->
                response.getResponseHeaders().set("X-Orchestration-Count",
                        String.valueOf(OrchestrationCounter.get())));
    }
}
