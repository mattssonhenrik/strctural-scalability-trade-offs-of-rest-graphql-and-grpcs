package se.lnu.runner;

/**
 * Experiment constants, single source of truth for the test matrix.
 *
 * K_MAX is always the dataset field count passed to /api/admin/reload.
 * K in test cases is the target K (subset of K_MAX) used for overfetch
 * measurement.
 */
public class TestConfig {

    public static int D_BASELINE = 3;
    public static int F_BASELINE = 3;
    public static int K_BASELINE = 3;

    public static int SWEEP_MIN = 0;
    // Careful rest over 10 can be exausting
    public static int SWEEP_MAX = 10;

    // Dataset is always generated with K_MAX fields, target K varies per test case
    public static int K_MAX = 10;

    public static int N_RUNS = 2;
    public static int SEED = 42;

    public static String[] PARADIGMS = { "REST", "GraphQL", "gRPC" };

    public static String BASE_URL = "http://localhost:8080";
    public static int GRPC_PORT = 9090;
}
