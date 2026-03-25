package se.lnu.data;

import java.util.List;
import java.util.Map;

/**
 * Represents one node in the generated tree.
 *
 * id – zero-padded sequential integer string, e.g. "000000", "000001" (always 6 chars)
 * fields – K key/value pairs, keys are "k00".."k99", values are 16-char strings
 * children – F child nodes (empty list at leaf level)
 */
public class Node {

    private String id;
    private Map<String, String> fields;
    private List<Node> children;

    /**
     * @param id       unique node identifier
     * @param fields   K fields (keys "k00".."k99", values 16-char strings)
     * @param children F child nodes, empty list at leaf level
     */
    public Node(String id, Map<String, String> fields, List<Node> children) {
        this.id = id;
        this.fields = fields;
        this.children = children;
    }

    /**
     * @return unique node identifier
     */
    public String getId() {
        return id;
    }

    /**
     * @return K fields for this node
     */
    public Map<String, String> getFields() {
        return fields;
    }

    /**
     * @return child nodes, empty list if this is a leaf
     */
    public List<Node> getChildren() {
        return children;
    }
}
