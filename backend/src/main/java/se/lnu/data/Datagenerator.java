package se.lnu.data;

import java.util.*;

/**
 * Generates controlled, nested tree structures for API experiment data.
 *
 * ── Parameters ───────────────────────────────────────────────────────────────
 * D (depth) – how many levels deep the tree goes
 * F (fan-out) – number of child nodes per node
 * K (field-count) – number of scalar fields per node
 * seed – random seed for reproducibility (-1 = random each run)
 *
 * ── Structure ────────────────────────────────────────────────────────────────
 * Each node contains:
 * - K fields with fixed-length keys ("k00".."k99") and 16-char random values
 * - F child nodes (none at leaf level)
 *
 * ── Payload predictability ───────────────────────────────────────────────────
 * Keys are zero-padded ("k00", "k01", ...) so key length is always 3 bytes.
 * Values are always STRING_LENGTH (16) bytes.
 * Node IDs are always 6 bytes ("000000", "000001", ...).
 * → Each node contributes exactly K × 19 bytes in field data, regardless of
 * depth.
 *
 * ── Size formulas ────────────────────────────────────────────────────────────
 * Total nodes ≈ (F^(D+1) - 1) / (F - 1)
 * Total fields ≈ nodes × K
 */


// NY KOMMENTAR?
/**
 * Generates tree-shaped test data for the API experiment.
 * The tree is based on depth, fan-out, field count, and seed.
 */
public class Datagenerator {

    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private int depth;
    private int fanOut;
    private int fieldCount;
    private int stringLength;
    private Random random;

    public Datagenerator(int depth, int fanOut, int fieldCount, int seed) {
        this(depth, fanOut, fieldCount, 16, seed);
    }

    public Datagenerator(int depth, int fanOut, int fieldCount, int stringLength, int seed) {
        if (depth < 0)
            throw new IllegalArgumentException("depth must be >= 0");
        if (fanOut < 1)
            throw new IllegalArgumentException("fanOut must be >= 1");
        if (fieldCount < 1)
            throw new IllegalArgumentException("fieldCount must be >= 1");
        if (stringLength < 1)
            throw new IllegalArgumentException("stringLength must be >= 1");

        this.depth = depth;
        this.fanOut = fanOut;
        this.fieldCount = fieldCount;
        this.stringLength = stringLength;
        this.random = new Random(seed);
    }

    /**
     * Generates the full tree and returns the root node.
     * Node IDs are assigned sequentially in depth-first order: "000000", "000001",
     * 
     * @param int[] instead of Int to access an object (through pointer) for shared state.
     * @return root Node of the generated tree
     */
    public Node generate() {
        return buildNode(0, new int[] { 0 });
    }

    /**
     * Prints a short summary of the generated dataset.
     * Used for development only.
     */
    public void printStats() {
        int nodes = totalNodes(depth, fanOut);
        int fields = nodes * fieldCount;
        System.out.printf("D=%d, F=%d, K=%d  →  ~%,d nodes, ~%,d scalar fields%n",
                depth, fanOut, fieldCount, nodes, fields);
    }

    // ── private ───────────────────────────────────────────────────────────────

    /**
     * Builds one node and its children.
     *
     * @param int[] instead of Int to access an object (through pointer) for shared state.
     * @param currentDepth current level in the tree
     * @param counter shared counter for node ids
     * @return generated node
     */
    private Node buildNode(int currentDepth, int[] counter) {
        String id = String.format("%06d", counter[0]++);

        Map<String, String> fields = new LinkedHashMap<>();
        for (int k = 0; k < fieldCount; k++) {
            fields.put(String.format("k%02d", k), randomString(stringLength));
        }

        List<Node> children = new ArrayList<>();
        if (currentDepth < depth) {
            for (int f = 0; f < fanOut; f++) {
                children.add(buildNode(currentDepth + 1, counter));
            }
        }

        return new Node(id, fields, children);
    }

    /**
     * @param length number of characters to generate
     * @return random string of the given length using characters from ALPHABET + 0-9
     */
    private String randomString(int length) {
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return stringBuilder.toString();
    }

    /**
     * Calculates the total number of nodes.
     * Used for development only.
     */
    private int totalNodes(int d, int f) {
        if (f == 1)
            return d + 1;
        return (int) ((Math.pow(f, d + 1) - 1) / (f - 1));
    }
}
