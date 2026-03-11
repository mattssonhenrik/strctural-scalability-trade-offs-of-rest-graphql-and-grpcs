package se.lnu.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Random;

/**
 * Generates controlled, nested JSON structures for API experiment data.
 *
 * ── Parameters ───────────────────────────────────────────────────────────────
 *   D (depth)       – how many levels deep the tree goes
 *   F (fan-out)     – number of child nodes per node
 *   K (field-count) – number of scalar fields per node
 *   seed            – random seed for reproducibility (-1 = random each run)
 *
 * ── Structure ────────────────────────────────────────────────────────────────
 *   The output is a tree of JSON objects. Each node contains:
 *     - K fields with fixed-length keys ("f00".."f99") and 16-char random values
 *     - A "children" array with F child nodes (omitted at leaf level)
 *
 *   Example with D=2, F=2, K=2:
 *     { "f00": "...", "f01": "...",
 *       "children": [
 *         { "f00": "...", "f01": "...",
 *           "children": [ {leaf}, {leaf} ] },
 *         { "f00": "...", "f01": "...",
 *           "children": [ {leaf}, {leaf} ] }
 *       ]
 *     }
 *
 * ── Payload predictability ───────────────────────────────────────────────────
 *   Keys are zero-padded ("f00", "f01", ...) so key length is always 3 bytes.
 *   Values are always STRING_LENGTH (16) bytes.
 *   → Each node contributes exactly K × (3 + 16) = K × 19 bytes in field data,
 *     regardless of depth. This ensures fair payload comparison across D values.
 *
 * ── Size formulas ────────────────────────────────────────────────────────────
 *   Total nodes  ≈ (F^(D+1) - 1) / (F - 1)
 *   Total fields ≈ nodes × K
 */
public class Datagenerator {

    // ── tuneable constants ────────────────────────────────────────────────────
    private static final int STRING_LENGTH = 16;   // fixed length for all strings
    private static final String ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    // ─────────────────────────────────────────────────────────────────────────

    private final int depth;       // D
    private final int fanOut;      // F
    private final int fieldCount;  // K

    private final ObjectMapper mapper = new ObjectMapper();
    private final Random random;

    /**
     * @param depth      D – max nesting depth  (0 = only root fields, no children)
     * @param fanOut     F – children per node
     * @param fieldCount K – scalar fields per node
     * @param seed       random seed for reproducible output (use -1 for random)
     */
    public Datagenerator(int depth, int fanOut, int fieldCount, long seed) {
        if (depth      < 0) throw new IllegalArgumentException("depth must be >= 0");
        if (fanOut     < 1) throw new IllegalArgumentException("fanOut must be >= 1");
        if (fieldCount < 1) throw new IllegalArgumentException("fieldCount must be >= 1");

        this.depth      = depth;
        this.fanOut     = fanOut;
        this.fieldCount = fieldCount;
        this.random     = (seed < 0) ? new Random() : new Random(seed);
    }

    // ── public methods ────────────────────────────────────────────────────────────

    /** Returns the generated structure as a Jackson ObjectNode. */
    public ObjectNode generate() {
        return buildNode(0);
    }

    /** Returns the generated structure as a pretty-printed JSON string or raw string */
    public String generateJson() throws Exception {

        //TODO: övre metod returnerar pretty print, (LÄSBART), undre metod returnerar json i löpande, bättre för experiment??? Viktigt för att hålla DP3/payload size minimal
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(generate());
        // return mapper.writeValueAsString(generate());
    }

    // ── internal builder ─────────────────────────────────────────────────────

    /**
     * Recursively builds one node at the given depth level.
     * All K fields use fixed-length keys ("f00".."f99") and fixed-length values (STRING_LENGTH chars).
     * → Each node contributes exactly K × (3 + STRING_LENGTH) bytes in field data.
     *
     * @param currentDepth  0 = root, depth = leaf level
     */
    private ObjectNode buildNode(int currentDepth) {
        ObjectNode node = mapper.createObjectNode();

        for (int k = 0; k < fieldCount; k++) {
            node.put(String.format("f%02d", k), randomString(STRING_LENGTH));
        }

        if (currentDepth < depth) {
            ArrayNode children = mapper.createArrayNode();
            for (int f = 0; f < fanOut; f++) {
                children.add(buildNode(currentDepth + 1));
            }
            node.set("children", children);
        }

        return node;
    }

    // ── random helpers ────────────────────────────────────────────────────────

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    // TODO: ifall vi vill ha int, bool, double i field values???
    // private int    randomInt()     { return random.nextInt(100_000); }
    // private double randomDouble()  { return Math.round(random.nextDouble() * 10_000.0) / 100.0; }
    // private boolean randomBoolean(){ return random.nextBoolean(); }

    // ── stats helper ─────────────────────────────────────────────────────────

    /** Prints a quick summary of expected node/field counts. */
    public void printStats() {
        long nodes  = totalNodes(depth, fanOut);
        long fields = nodes * fieldCount;
        System.out.printf("D=%d, F=%d, K=%d  →  ~%,d nodes, ~%,d scalar fields%n",
                depth, fanOut, fieldCount, nodes, fields);
    }

    private static long totalNodes(int d, int f) {
        if (f == 1) return d + 1;
        return (long)((Math.pow(f, d + 1) - 1) / (f - 1));
    }

    // ── main / demo ───────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {

        // ── example 1: small, human-readable ─────────────────────────────────
        System.out.println("=== Example 1: D=2, F=2, K=3, seed=42 ===");
        Datagenerator gen1 = new Datagenerator(2, 2, 3, 42);
        gen1.printStats();
        System.out.println(gen1.generateJson());

        // ── example 2: wide & flat ────────────────────────────────────────────
        System.out.println("\n=== Example 2: D=1, F=5, K=10, seed=42 ===");
        Datagenerator gen2 = new Datagenerator(1, 5, 10, 42);
        gen2.printStats();
        System.out.println(gen2.generateJson());

        // ── example 3: deep & narrow ─────────────────────────────────────────
        System.out.println("\n=== Example 3: D=5, F=1, K=4, seed=42 ===");
        Datagenerator gen3 = new Datagenerator(5, 1, 4, 42);
        gen3.printStats();
        System.out.println(gen3.generateJson());

        // ── example 4: large stress-test payload (no JSON print) ─────────────
        System.out.println("\n=== Example 4: D=4, F=4, K=8 (stress test) ===");
        Datagenerator gen4 = new Datagenerator(4, 4, 8, -1);
        gen4.printStats();
        long t0 = System.currentTimeMillis();
        String json = gen4.generateJson();
        long t1 = System.currentTimeMillis();
        System.out.printf("Generated %,d bytes in %d ms%n", json.length(), t1 - t0);
    }
}