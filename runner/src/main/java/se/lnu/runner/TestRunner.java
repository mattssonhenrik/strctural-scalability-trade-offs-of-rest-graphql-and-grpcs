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
 * Results are written to CSV files in results/ (project root).
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

    /** RQ1: runs the three OVAT series (D, F, K) and writes results to CSV. */
    public void runRQ1() throws IOException {
        runSeries("D", TestConfig.D_BASELINE, TestConfig.F_BASELINE, TestConfig.K_BASELINE, TestConfig.S_BASELINE);
        runSeries("F", TestConfig.D_BASELINE, TestConfig.F_BASELINE, TestConfig.K_BASELINE, TestConfig.S_BASELINE);
        runSeries("K", TestConfig.D_BASELINE, TestConfig.F_BASELINE, TestConfig.K_BASELINE, TestConfig.S_BASELINE);
    }

    /** RQ2: sweeps S (string length) with K as inner loop to find the protobuf/GraphQL crossover point. */
    public void runRQ2() throws IOException {
        runSeriesS(TestConfig.D_BASELINE, TestConfig.F_BASELINE, TestConfig.K_MAX);
    }

    // ── private ───────────────────────────────────────────────────────────────

    /**
     * Sweeps one variable (D, F, or K) from SWEEP_MIN to SWEEP_MAX, writes one CSV
     * file.
     */
    private void runSeries(String series, int dBase, int fBase, int kBase, int sBase) throws IOException {
        CsvWriter csv = new CsvWriter("../results/rq1_" + series + "_series_" + buildTag(series) + ".csv", series);

        for (int value = TestConfig.SWEEP_MIN; value <= TestConfig.SWEEP_MAX; value++) {
            int d = series.equals("D") ? value : dBase;
            int f = series.equals("F") ? value : fBase;
            int k = series.equals("K") ? value : kBase;

            // F=0 is meaningless (no children), skip
            if (f == 0) continue;
            // K=0 means no fields, skip
            if (k == 0) continue;

            // Dataset always generated with K_MAX fields — target k varies per test case
            reload(d, f, TestConfig.K_MAX, sBase);

            for (String paradigm : TestConfig.PARADIGMS) {
                for (int run = 1; run <= TestConfig.N_RUNS; run++) {
                    TestCase testcase = new TestCase(series, paradigm, d, f, k, sBase, run);
                    RunResult result = runSingle(paradigm, testcase);
                    csv.appendRow(result);
                    System.out.printf("%-8s %-7s D=%2d F=%2d K=%2d S=%3d run=%d → %s%n",
                            paradigm, series, d, f, k, sBase, run, result.getStatus());
                }
            }
        }

        csv.close();
    }

    /**
     * RQ2: sweeps S (string length) from S_SWEEP_MIN to S_SWEEP_MAX.
     * D, F, K are fixed at baselines. K_target sweeps 1..K_MAX per S value
     * to expose the crossover point where protobuf compactness beats GraphQL
     * field selection.
     */
    private void runSeriesS(int dBase, int fBase, int kMax) throws IOException {
        int sMin = TestConfig.S_SWEEP_VALUES[0];
        int sMax = TestConfig.S_SWEEP_VALUES[TestConfig.S_SWEEP_VALUES.length - 1];
        String tag = "D" + dBase + "_F" + fBase + "_K" + kMax + "_S" + sMin + "-" + sMax;
        CsvWriter csv = new CsvWriter("../results/rq2_S_series_" + tag + ".csv", "S");

        for (int s : TestConfig.S_SWEEP_VALUES) {
            reload(dBase, fBase, kMax, s);

            for (int k = 1; k <= kMax; k++) {
                for (String paradigm : TestConfig.PARADIGMS) {
                    for (int run = 1; run <= TestConfig.N_RUNS; run++) {
                        TestCase testcase = new TestCase("S", paradigm, dBase, fBase, k, s, run);
                        RunResult result = runSingle(paradigm, testcase);
                        csv.appendRow(result);
                        System.out.printf("%-8s S-series D=%2d F=%2d K=%2d S=%3d run=%d → %s%n",
                                paradigm, dBase, fBase, k, s, run, result.getStatus());
                    }
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
        int cm1 = 0;
        int cm2 = 0;
        int cm3 = 0;
        int cm5 = 0;
        int cm4 = 0;

        // queue entries: [nodeId, currentDepth]
        Deque<String[]> queue = new ArrayDeque<>();

        try {
            HttpResponse<String> rootResp = restClient.fetchRoot();
            cm1++;
            cm2 += MetricsAccumulator.orchestrationCount(rootResp);
            cm3 += MetricsAccumulator.contentLength(rootResp);
            

            if (rootResp.statusCode() != 200) return error(testcase, cm1, cm2, cm3, cm5, cm4);

            RunnerNode root = mapper.readValue(rootResp.body(), RunnerNode.class);
            if (testcase.getD() > 0) queue.add(new String[]{ root.getId(), "0" });

            while (!queue.isEmpty()) {
                String[] entry = queue.poll();
                String nodeId   = entry[0];
                int nodeDepth   = Integer.parseInt(entry[1]);

                HttpResponse<String> childResp = restClient.fetchChildren(nodeId);
                cm1++;
                cm2 += MetricsAccumulator.orchestrationCount(childResp);
                cm3 += MetricsAccumulator.contentLength(childResp);

                if (childResp.statusCode() != 200) return error(testcase, cm1, cm2, cm3, cm5, cm4);

                RunnerNode[] children = mapper.readValue(childResp.body(), RunnerNode[].class);
                if (children.length != testcase.getF()) return error(testcase, cm1, cm2, cm3, cm5, cm4);

                if (nodeDepth + 1 < testcase.getD()) {
                    for (RunnerNode child : children) {
                        queue.add(new String[]{ child.getId(), String.valueOf(nodeDepth + 1) });
                    }
                }
            }

            cm5 = MetricsAccumulator.overfetch(TestConfig.K_MAX, testcase.getK(), testcase.getD(), testcase.getF());
            cm4 = MetricsAccumulator.underfetch(cm1);

            return new RunResult(testcase, cm1, cm2, cm3, cm5, cm4, "ok");

        } catch (Exception e) {
            return error(testcase, cm1, cm2, cm3, cm5, cm4);
        }
    }

    /**
     * GraphQL run: single fetch, then target-shape verification via overfetch check.
     * Non-zero overfetch means the query was built incorrectly → status=error.
     */
    private RunResult runGraphQL(TestCase tescase) {
        int cm1 = 0;
        int cm2 = 0;
        int cm3 = 0;
        int cm4 = 0;
        int cm5 = 0;

        try {
            HttpResponse<String> resp = graphQLClient.fetch(tescase.getD(), tescase.getK());
            cm1++;
            cm2 += MetricsAccumulator.orchestrationCount(resp);
            cm3 += MetricsAccumulator.contentLength(resp);
            cm4 = MetricsAccumulator.underfetch(cm1);

            if (resp.statusCode() != 200) return error(tescase, cm1, cm2, cm3, cm5, cm4);

            cm5 = MetricsAccumulator.overfetchGraphQL(resp.body(), tescase.getK());
            if (cm5 != 0) return new RunResult(tescase, cm1, cm2, cm3, cm5, cm4, "error");

            if (!MetricsAccumulator.verifyTreeShape(resp.body(), tescase.getD(), tescase.getF()))
                return error(tescase, cm1, cm2, cm3, cm5, cm4);

            return new RunResult(tescase, cm1, cm2, cm3, cm5, cm4, "ok");

        } catch (Exception e) {
            return error(tescase, cm1, cm2, cm3, cm5, cm4);
        }
    }

    /**
     * gRPC run: single RPC call returns the full tree.
     * CM1=1 always. CM2 and CM3 come from GrpcClient.GrpcResult.
     * gRPC has no field selection → overfetch = (K_MAX - K) × nodeCount.
     * gRPC fetches full tree in one call → underfetch = 0.
     */
    private RunResult runGrpc(TestCase tc) {
        int cm1 = 0;
        int cm2 = 0;
        int cm3 = 0;
        int cm4 = 0;
        int cm5 = 0;

        try {
            GrpcClient.GrpcResult result = grpcClient.fetch(tc.getD());
            cm1++;
            cm2 = MetricsAccumulator.orchestrationCount(result);
            cm3 = MetricsAccumulator.contentLength(result);
            if (!MetricsAccumulator.verifyTreeShape(result.response(), tc.getD(), tc.getF()))
                return error(tc, cm1, cm2, cm3, cm5, cm4);

            cm5 = MetricsAccumulator.overfetch(TestConfig.K_MAX, tc.getK(), tc.getD(), tc.getF());
            cm4 = MetricsAccumulator.underfetch(cm1);
            return new RunResult(tc, cm1, cm2, cm3, cm5, cm4, "ok");
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
        reload(d, f, k, TestConfig.S_BASELINE);
    }

    private void reload(int d, int f, int k, int s) {
        String url = String.format("%s/api/admin/reload?D=%d&F=%d&K=%d&S=%d&seed=%d",
                TestConfig.BASE_URL, d, f, k, s, TestConfig.SEED);
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

    private RunResult error(TestCase testcase, int cm1, int cm2, int cm3, int cm5, int cm4) {
        return new RunResult(testcase, cm1, cm2, cm3, cm5, cm4, "error");
    }
}
