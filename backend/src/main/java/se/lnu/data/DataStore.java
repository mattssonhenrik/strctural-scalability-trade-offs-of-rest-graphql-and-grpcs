package se.lnu.data;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import se.lnu.metrics.OrchestrationCounter;

import java.util.*;

/**
 * Holds the in-memory dataset shared by all three API implementations.
 *
 * Test runner calls reload(D, F, K, seed) to regenerate the dataset before each
 * test case.
 *
 */
@Component
public class DataStore {

    private Node root;
    private Map<String, Node> index = new HashMap<>();

    // TODO: RM before experiment. DEV ONLY
    @PostConstruct
    public void init() {
        reload(2, 2, 4, 42);
    }

    /**
     * Regenerates the dataset. Called before each test case via POST
     * /api/admin/reload.
     *
     * @param d    nesting depth
     * @param f    fan-out (children per node)
     * @param k    field count per node
     * @param seed random seed — use 42 for reproducible experiment runs
     */
    public void reload(int d, int f, int k, int seed) {
        index = new HashMap<>();
        root = new Datagenerator(d, f, k, seed).generate();
        buildIndex(root);
    }

    /** @return root node of the current dataset */
    public Node getRoot() {
        return root;
    }

    /**
     * @param id node identifier
     * @return node with the given id, or null if not found
     */
    public Node getNode(String id) {
        OrchestrationCounter.increment();
        return index.get(id);
    }

    /**
     * @param id node identifier
     * @return direct children of the node, empty list if leaf or not found
     */
    public List<Node> getChildren(String id) {
        OrchestrationCounter.increment();
        Node node = index.get(id);
        return node != null ? node.getChildren() : Collections.emptyList();
    }

    // ── private ───────────────────────────────────────────────────────────────

    /**
     * Recursively registers all nodes in the index for O(1) lookup by id.
     *
     * @param node current node to register
     */
    private void buildIndex(Node node) {
        index.put(node.getId(), node);
        for (Node child : node.getChildren()) {
            buildIndex(child);
        }
    }
}
