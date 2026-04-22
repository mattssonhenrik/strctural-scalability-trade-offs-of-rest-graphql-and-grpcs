package se.lnu.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.lnu.apis.grpc.Node;
import se.lnu.apis.grpc.TreeResponse;
import se.lnu.clients.GrpcClient;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


/**
 * Single source of truth for all DP1–DP5 measurement logic.
 *
 * Shared methods are paradigm-agnostic.
 * Paradigm-specific methods are grouped by API type.
 */
public class MetricsAccumulator {

    private static final ObjectMapper mapper = new ObjectMapper();

    // ── Shared ────────────────────────────────────────────────────────────────

    /** DP3: UTF-8 byte count of response body. */
    public static int contentLength(HttpResponse<String> resp) {
        return resp.body().getBytes(StandardCharsets.UTF_8).length;
    }

    /** DP2: X-Orchestration-Count header, error if absent. */
    public static int orchestrationCount(HttpResponse<String> resp) {
        return resp.headers().firstValue("X-Orchestration-Count")
                .map(Integer::parseInt)
                .orElseThrow(() -> new IllegalStateException(
                    "X-Orchestration-Count header missing — backend instrumentation did not run"));
    }

    

    // ── gRPC ─────────────────────────────────────────────────────────────────

    /** DP2: x-orchestration-count trailing metadata. */
    public static int orchestrationCount(GrpcClient.GrpcResult result) {
        return result.dp2();
    }

    /** DP3: serialized protobuf message bytes. */
    public static int contentLength(GrpcClient.GrpcResult result) {
        return result.dp3();
    }

    // ── REST ──────────────────────────────────────────────────────────────────

    /** DP5 for REST: REST always returns all kMax fields, overfetch = surplus × nodes. */
    public static int overfetch(int kMax, int k, int d, int f) {
        return (kMax - k) * totalNodes(d, f);
    }

    /** DP4: extra calls beyond the first — 0 for paradigms that resolve in one call. */
    public static int underfetch(int dp1) {
        return dp1 - 1;
    }

    // ── GraphQL ───────────────────────────────────────────────────────────────

    /**
     * DP5 for GraphQL: parses the response body and counts actual k-fields per node.
     * Returns total surplus fields across all nodes.
     * Should be 0 if the query was built correctly — non-zero indicates a bug.
     */
    @SuppressWarnings("unchecked")
    public static int overfetchGraphQL(String body, int expectedK) throws Exception {
        Map<String, Object> root = (Map<String, Object>)
                ((Map<String, Object>) mapper.readValue(body, Map.class).get("data")).get("root");
        return countOverfetchNode(root, expectedK);
    }

    /** Verifies that the GraphQL response tree matches expected depth D and fan-out F. */
    @SuppressWarnings("unchecked")
    public static boolean verifyTreeShape(String body, int expectedD, int expectedF) throws Exception {
        Map<String, Object> root = (Map<String, Object>)
                ((Map<String, Object>) mapper.readValue(body, Map.class).get("data")).get("root");
        return verifyJsonNode(root, expectedD, expectedF, 0);
    }

    /** Verifies that the gRPC response tree matches expected depth D and fan-out F. */
    public static boolean verifyTreeShape(TreeResponse response, int expectedD, int expectedF) {
        return verifyGrpcNode(response.getRoot(), expectedD, expectedF, 0);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static int countOverfetchNode(Map<String, Object> node, int expectedK) {
        int kCount = 0;
        for (String key : node.keySet()) {
            if (key.matches("k\\d+")) kCount++;
        }
        int overfetch = Math.max(0, kCount - expectedK);
        Object children = node.get("children");
        if (children instanceof List) {
            for (Map<String, Object> child : (List<Map<String, Object>>) children) {
                overfetch += countOverfetchNode(child, expectedK);
            }
        }
        return overfetch;
    }

    @SuppressWarnings("unchecked")
    private static boolean verifyJsonNode(Map<String, Object> node, int expectedD, int expectedF, int depth) {
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (depth == expectedD) {
            return children == null || children.isEmpty();
        }
        if (children == null || children.size() != expectedF) return false;
        for (Map<String, Object> child : children) {
            if (!verifyJsonNode(child, expectedD, expectedF, depth + 1)) return false;
        }
        return true;
    }

    private static boolean verifyGrpcNode(Node node, int expectedD, int expectedF, int depth) {
        if (depth == expectedD) {
            return node.getChildrenCount() == 0;
        }
        if (node.getChildrenCount() != expectedF) return false;
        for (Node child : node.getChildrenList()) {
            if (!verifyGrpcNode(child, expectedD, expectedF, depth + 1)) return false;
        }
        return true;
    }

    /** Total node count for a complete F-ary tree of depth D. */
    public static int totalNodes(int d, int f) {
        if (f == 1) return d + 1;
        return (int) ((Math.pow(f, d + 1) - 1) / (f - 1));
    }
}
