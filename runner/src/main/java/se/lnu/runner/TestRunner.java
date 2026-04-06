package se.lnu.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.lnu.clients.GraphQLClient;
import se.lnu.clients.GrpcClient;
import se.lnu.clients.RestClient;
import se.lnu.model.RunnerNode;
import se.lnu.model.RunResult;
import se.lnu.model.TestCase;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Main experiment loop — runs the full OVAT sweep for REST and GraphQL.
 *
 * Three series: D-sweep (F=F_BASELINE, K=K_BASELINE),
 * F-sweep (D=D_BASELINE, K=K_BASELINE),
 * K-sweep (D=D_BASELINE, F=F_BASELINE).
 *
 * Before each test case the DataStore is reloaded via POST /api/admin/reload.
 * Results are written to CSV files in runner/results/.
 *
 * TestRunner owns all orchestration: traversal loop, target-shape check,
 * metric accumulation, and RunResult assembly. Clients are atomic HTTP executors.
 */
public class TestRunner {

    private final RestClient restClient = new RestClient();
    private final GraphQLClient graphQLClient = new GraphQLClient();
    private final GrpcClient grpcClient = new GrpcClient();
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    /** Runs all three OVAT series (D, F, K) and writes results to CSV! */
    public void run() throws IOException {
        runSeries("D", TestConfig.D_BASELINE, TestConfig.F_BASELINE, TestConfig.K_BASELINE);
        runSeries("F", TestConfig.D_BASELINE, TestConfig.F_BASELINE, TestConfig.K_BASELINE);
        runSeries("K", TestConfig.D_BASELINE, TestConfig.F_BASELINE, TestConfig.K_BASELINE);
    }

    // ── private ───────────────────────────────────────────────────────────────

    /**
     * Sweeps one variable (D, F, or K) from SWEEP_MIN to SWEEP_MAX, writes one CSV
     * file.
     */
    private void runSeries(String series, int dBase, int fBase, int kBase) throws IOException {
        CsvWriter csv = new CsvWriter("results/csv/rq1_" + series + "_series_" + buildTag(series) + ".csv", series);

        for (int value = TestConfig.SWEEP_MIN; value <= TestConfig.SWEEP_MAX; value++) {
            int d = series.equals("D") ? value : dBase;
            int f = series.equals("F") ? value : fBase;
            int k = series.equals("K") ? value : kBase;

            // F=0 is meaningless (no children), skip
            if (f == 0) continue;
            // K=0 means no fields, skip
            if (k == 0) continue;

            // Dataset always generated with K_MAX fields — target k varies per test case
            reload(d, f, TestConfig.K_MAX);

            for (String paradigm : TestConfig.PARADIGMS) {
                for (int run = 1; run <= TestConfig.N_RUNS; run++) {
                    TestCase testcase = new TestCase(series, paradigm, d, f, k, run);
                    RunResult result = runSingle(paradigm, testcase);
                    csv.appendRow(result);
                    System.out.printf("%-8s %-7s D=%2d F=%2d K=%2d run=%d → %s%n",
                            paradigm, series, d, f, k, run, result.getStatus());
                }
            }
        }

        csv.close();
    }

    /** Dispatches one run to the correct paradigm method. */
    private RunResult runSingle(String paradigm, TestCase testcase) {
        if (paradigm.equals("REST"))    return runRest(testcase);
        if (paradigm.equals("GraphQL")) return runGraphQL(testcase);
        if (paradigm.equals("gRPC"))    return runGrpc(testcase);
        throw new IllegalArgumentException("Unknown paradigm: " + paradigm);
    }

    /**
     * REST run: BFS traversal via atomic fetchRoot/fetchChildren calls.
     * TestRunner drives the loop, accumulates metrics, verifies target shape,
     * and assembles the RunResult.
     */
    private RunResult runRest(TestCase testcase) {
        int dp1 = 0;
        int dp2 = 0;
        int dp3 = 0;
        int dp5 = 0;
        int dp4 = 0;

        // queue entries: [nodeId, currentDepth]
        Deque<String[]> queue = new ArrayDeque<>();

        try {
            HttpResponse<String> rootResp = restClient.fetchRoot();
            dp1++;
            dp2 += MetricsAccumulator.orchestrationCount(rootResp);
            dp3 += MetricsAccumulator.contentLength(rootResp);
            

            if (rootResp.statusCode() != 200) return error(testcase, dp1, dp2, dp3, dp5, dp4);

            RunnerNode root = mapper.readValue(rootResp.body(), RunnerNode.class);
            if (testcase.getD() > 0) queue.add(new String[]{ root.getId(), "0" });

            while (!queue.isEmpty()) {
                String[] entry = queue.poll();
                String nodeId   = entry[0];
                int nodeDepth   = Integer.parseInt(entry[1]);

                HttpResponse<String> childResp = restClient.fetchChildren(nodeId);
                dp1++;
                dp2 += MetricsAccumulator.orchestrationCount(childResp);
                dp3 += MetricsAccumulator.contentLength(childResp);

                if (childResp.statusCode() != 200) return error(testcase, dp1, dp2, dp3, dp5, dp4);

                if (nodeDepth + 1 < testcase.getD()) {
                    RunnerNode[] children = mapper.readValue(childResp.body(), RunnerNode[].class);
                    for (RunnerNode child : children) {
                        queue.add(new String[]{ child.getId(), String.valueOf(nodeDepth + 1) });
                    }
                }
            }

            dp5 = MetricsAccumulator.overfetchRest(TestConfig.K_MAX, testcase.getK(), testcase.getD(), testcase.getF());
            dp4 = MetricsAccumulator.underfetchRest(dp1);

            return new RunResult(testcase, dp1, dp2, dp3, dp5, dp4, "ok");

        } catch (Exception e) {
            return error(testcase, dp1, dp2, dp3, dp5, dp4);
        }
    }

    /**
     * GraphQL run: single fetch, then target-shape verification via overfetch check.
     * Non-zero overfetch means the query was built incorrectly → status=error.
     */
    private RunResult runGraphQL(TestCase tescase) {
        int dp1 = 0;
        int dp2 = 0;
        int dp3 = 0;
        int dp4 = 0;
        int dp5 = 0;

        try {
            HttpResponse<String> resp = graphQLClient.fetch(tescase.getD(), tescase.getK());
            dp1++;
            dp2 += MetricsAccumulator.orchestrationCount(resp);
            dp3 += MetricsAccumulator.contentLength(resp);
            dp4 = dp1 - 1;

            if (resp.statusCode() != 200) return error(tescase, dp1, dp2, dp3, dp5, dp4);

            dp5 = MetricsAccumulator.overfetchGraphQL(resp.body(), tescase.getK());
            if (dp5 != 0) return new RunResult(tescase, dp1, dp2, dp3, dp5, dp4, "error");

            return new RunResult(tescase, dp1, dp2, dp3, dp5, dp4, "ok");

        } catch (Exception e) {
            return error(tescase, dp1, dp2, dp3, dp5, dp4);
        }
    }

    /**
     * gRPC run: single RPC call returns the full tree.
     * DP1=1 always. DP2 and DP3 come from GrpcClient.GrpcResult.
     * gRPC has no field selection → overfetch = (K_MAX - K) × nodeCount.
     * gRPC fetches full tree in one call → underfetch = 0.
     */
    private RunResult runGrpc(TestCase tc) {
        try {
            GrpcClient.GrpcResult result = grpcClient.fetch(tc.getD());
            int dp1 = 1;
            int dp2 = result.dp2();
            int dp3 = result.dp3();
            int dp5 = MetricsAccumulator.overfetchRest(TestConfig.K_MAX, tc.getK(), tc.getD(), tc.getF());
            int dp4 = 0;
            return new RunResult(tc, dp1, dp2, dp3, dp5, dp4, "ok");
        } catch (Exception e) {
            return new RunResult(tc, 1, 0, 0, 0, 0, "error");
        }
    }

    /**
     * Builds a filename tag encoding the fixed and swept parameters, e.g.
     * "D2-5_F5_K8".
     */
    private String buildTag(String series) {
        String minMaxString = TestConfig.SWEEP_MIN + "-" + TestConfig.SWEEP_MAX;
        String d = series.equals("D") ? "D" + minMaxString : "D" + TestConfig.D_BASELINE;
        String f = series.equals("F") ? "F" + minMaxString : "F" + TestConfig.F_BASELINE;
        String k = series.equals("K") ? "K" + minMaxString : "K" + TestConfig.K_MAX;
        return d + "_" + f + "_" + k;
    }

    /**
     * Calls POST /api/admin/reload to regenerate the dataset before each test case.
     */
    private void reload(int d, int f, int k) {
        String url = String.format("%s/api/admin/reload?D=%d&F=%d&K=%d&seed=%d",
                TestConfig.BASE_URL, d, f, k, TestConfig.SEED);
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new RuntimeException("Reload failed for D=" + d + " F=" + f + " K=" + k, e);
        }
    }

    private RunResult error(TestCase testcase, int dp1, int dp2, int dp3, int dp5, int dp4) {
        return new RunResult(testcase, dp1, dp2, dp3, dp5, dp4, "error");
    }
}
