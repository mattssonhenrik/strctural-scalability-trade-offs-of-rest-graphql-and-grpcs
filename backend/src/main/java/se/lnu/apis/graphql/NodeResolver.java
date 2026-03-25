package se.lnu.apis.graphql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import se.lnu.data.DataStore;
import se.lnu.data.Node;

import java.util.ArrayList;
import java.util.List;


/**
 * GraphQL resolver for the root node.
 * Loads children lazily and fields are stated by the query.
 */
@Controller
public class NodeResolver {

    @Autowired
    private DataStore dataStore;

    /**
     * Entry point for all GraphQL queries.
     *
     * @return root NodeView, or null if dataset not loaded
     */
    @QueryMapping
    public NodeView root() {
        var root = dataStore.getRoot();
        if (root == null)
            return null;
        return new NodeView(root);
    }


    /**
     * Resolves the children of a node from the data store.
     * @param parent the parent node being resolved
     * @return list of child NodeViews, empty list if leaf
     */
    @SchemaMapping(typeName = "Node", field = "children")
    public List<NodeView> children(NodeView parent) {
        List<NodeView> result = new ArrayList<>();
        for (Node child : dataStore.getChildren(parent.getId())) {
            result.add(new NodeView(child));
        }
        return result;
    }
}
