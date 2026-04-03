package se.lnu.apis.graphql;

import se.lnu.data.Node;

/**
 * View of a Node for GraphQL resolvers.
 * Field access is generic — individual k-field DataFetchers are registered
 * programmatically in FieldWiringConfigurer, keyed by field name.
 */
public class NodeView {

    private final Node node;

    public NodeView(Node node) {
        this.node = node;
    }

    public String getId() {
        return node.getId();
    }

    public String getField(String name) {
        return node.getFields().get(name);
    }
}
