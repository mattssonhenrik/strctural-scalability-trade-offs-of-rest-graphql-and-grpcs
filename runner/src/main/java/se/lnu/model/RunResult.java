package se.lnu.model;

/**
 * Stores the measured results for one experiment run.
 *
 * CM1 = client-to-server requests
 * CM2 = server-side orchestration operations
 * CM3 = total response body bytes
 * overfetchFields = fields returned beyond target K
 * underfetchExtraCalls = extra requests beyond the first
 * status = ok, capped, or error
 */
public class RunResult {

    private TestCase testCase;
    private int cm1;
    private int cm2;
    private int cm3;
    private int overfetchFields;
    private int underfetchExtraCalls;
    private String status;

    public RunResult(TestCase testCase, int cm1, int cm2, int cm3,
            int overfetchFields, int underfetchExtraCalls, String status) {
        this.testCase = testCase;
        this.cm1 = cm1;
        this.cm2 = cm2;
        this.cm3 = cm3;
        this.overfetchFields = overfetchFields;
        this.underfetchExtraCalls = underfetchExtraCalls;
        this.status = status;
    }

    public TestCase getTestCase() {
        return testCase;
    }

    public int getCm1() {
        return cm1;
    }

    public int getCm2() {
        return cm2;
    }

    public int getCm3() {
        return cm3;
    }

    public int getOverfetchFields() {
        return overfetchFields;
    }

    public int getUnderfetchExtraCalls() {
        return underfetchExtraCalls;
    }

    public String getStatus() {
        return status;
    }
}
