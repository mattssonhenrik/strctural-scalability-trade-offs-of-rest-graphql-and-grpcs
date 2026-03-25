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
public class Datagenerator {

    private static int STRING_LENGTH = 16;
    private static String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private int depth;
    private int fanOut;
    private int fieldCount;
    private Random random;

    public Datagenerator(int depth, int fanOut, int fieldCount, int seed) {
        if (depth < 0)
            throw new IllegalArgumentException("depth must be >= 0");
        if (fanOut < 1)
            throw new IllegalArgumentException("fanOut must be >= 1");
        if (fieldCount < 1)
            throw new IllegalArgumentException("fieldCount must be >= 1");

        this.depth = depth;
        this.fanOut = fanOut;
        this.fieldCount = fieldCount;
        this.random = (seed < 0) ? new Random() : new Random(seed);
    }

    /**
     * Generates the full tree and returns the root node.
     * Node IDs are assigned sequentially in depth-first order: "000000", "000001",
     * 
     *
     * @return root Node of the generated tree
     */
    public Node generate() {
        return buildNode(0, new int[] { 0 });
    }

    /** Prints a quick summary of expected node/field counts.
     * For DEV
     */
    public void printStats() {
        int nodes = totalNodes(depth, fanOut);
        int fields = nodes * fieldCount;
        System.out.printf("D=%d, F=%d, K=%d  →  ~%,d nodes, ~%,d scalar fields%n",
                depth, fanOut, fieldCount, nodes, fields);
    }

    // ── private ───────────────────────────────────────────────────────────────

    /**
     * Recursively builds one node.
     *
     * @param currentDepth current level in the tree (0 = root)
     * @param counter      single-element array used as a shared mutable ID counter
     */
    private Node buildNode(int currentDepth, int[] counter) {
        String id = String.format("%06d", counter[0]++);

        Map<String, String> fields = new LinkedHashMap<>();
        for (int k = 0; k < fieldCount; k++) {
            fields.put(String.format("k%02d", k), randomString(STRING_LENGTH));
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
     * @return random string of the given length using characters from ALPHABET
     */
    private String randomString(int length) {
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            stringBuilder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return stringBuilder.toString();
    }

    /**
     * Helper for printing
     * For DEV
     */
    private int totalNodes(int d, int f) {
        if (f == 1)
            return d + 1;
        return (int) ((Math.pow(f, d + 1) - 1) / (f - 1));
    }
}
