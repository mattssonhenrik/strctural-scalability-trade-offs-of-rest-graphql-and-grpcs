package se.lnu.apis.graphql;

import se.lnu.data.Node;


/**
 * Wrapper for Node.
 * Exposes k00-k09 as getters, beyond k09 will return null.
 */
public class NodeView {

    private final Node node;

    public NodeView(Node node) {
        this.node = node;
    }

    public String getId() {
        return node.getId();
    }

    public String getK00() {
        return node.getFields().get("k00");
    }

    public String getK01() {
        return node.getFields().get("k01");
    }

    public String getK02() {
        return node.getFields().get("k02");
    }

    public String getK03() {
        return node.getFields().get("k03");
    }

    public String getK04() {
        return node.getFields().get("k04");
    }

    public String getK05() {
        return node.getFields().get("k05");
    }

    public String getK06() {
        return node.getFields().get("k06");
    }

    public String getK07() {
        return node.getFields().get("k07");
    }

    public String getK08() {
        return node.getFields().get("k08");
    }

    public String getK09() {
        return node.getFields().get("k09");
    }
}
