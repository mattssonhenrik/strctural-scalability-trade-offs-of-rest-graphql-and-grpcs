package se.lnu.model;

/**
 * Stores the independent variables for one experiment run.
 *
 * series = swept variable (D, F, K, or S)
 * paradigm = API under test (REST, GraphQL or gRPC)
 * d, f, k, s = test-case values
 * run = repetition number
 */
public class TestCase {

    private String series;
    private String paradigm;
    private int d;
    private int f;
    private int k;
    private int s;
    private int run;

    public TestCase(String series, String paradigm, int d, int f, int k, int s, int run) {
        this.series = series;
        this.paradigm = paradigm;
        this.d = d;
        this.f = f;
        this.k = k;
        this.s = s;
        this.run = run;
    }

    public String getSeries() {
        return series;
    }

    public String getParadigm() {
        return paradigm;
    }

    public int getD() {
        return d;
    }

    public int getF() {
        return f;
    }

    public int getK() {
        return k;
    }

    public int getS() {
        return s;
    }

    public int getRun() {
        return run;
    }
}
