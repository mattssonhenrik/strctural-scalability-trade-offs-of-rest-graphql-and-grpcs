package se.lnu.apis.graphql;

import graphql.schema.idl.RuntimeWiring;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;

/**
 * Registers a DataFetcher for each k-field (k00..k99) programmatically.
 *
 * Replaces the hardcoded getK00()..getK09() methods in NodeView, making
 * the schema support any K_MAX up to 100 without boilerplate.
 *
 * Each DataFetcher resolves the field by name via NodeView.getField(name),
 * which delegates to Node.getFields().get(name).
 */
@Component
public class FieldWiringConfigurer implements RuntimeWiringConfigurer {

    static final int K_MAX = 100;

    @Override
    public void configure(RuntimeWiring.Builder builder) {
        for (int i = 0; i < K_MAX; i++) {
            final String field = String.format("k%02d", i);
            builder.type("Node", b -> b.dataFetcher(field,
                    env -> ((NodeView) env.getSource()).getField(field)));
        }
    }
}
