package se.lnu.runner;

import se.lnu.model.RunResult;
import se.lnu.model.TestCase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Writes experiment results to a CSV file.
 *
 * One file per series (D, F, K). Header is written once on construction.
 * Each appendRow() call writes one line.
 */
public class CsvWriter {

    private static String HEADER = "paradigm,series,D-Target,F-Target,K-Target,S-Target,run,cm1_request_count,cm2_orchestration_ops,cm3_payload_bytes,overfetch_fields,underfetch_extra_calls,status";

    private PrintWriter writer;

    /**
     * Opens the file for writing and writes the CSV header row. Creates parent dirs
     * if needed.
     */
    public CsvWriter(String filePath, String series) throws IOException {
        new File(filePath).getParentFile().mkdirs();
        this.writer = new PrintWriter(new FileWriter(filePath, false));
        writeMetadata(series);
        writer.println(HEADER);
    }

    /** Appends one result row to the CSV. */
    public void appendRow(RunResult result) {
        TestCase testCase = result.getTestCase();
        writer.printf("%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s%n",
                testCase.getParadigm(),
                testCase.getSeries(),
                testCase.getD(),
                testCase.getF(),
                testCase.getK(),
                testCase.getS(),
                testCase.getRun(),
                result.getCm1(),
                result.getCm2(),
                result.getCm3(),
                result.getOverfetchFields(),
                result.getUnderfetchExtraCalls(),
                result.getStatus());
    }

    /** Flushes and closes the file. Must be called after all rows are written. */
    public void close() {
        writer.flush();
        writer.close();
    }

    /**
     * Write meta data to the csv.
     * 
     * @param series Current variabel in experiment.
     */
    private void writeMetadata(String series) {
        writer.println("# RQ1 experiment - " + series + "-series");

        String minMaxString = TestConfig.SWEEP_MIN + "-" + TestConfig.SWEEP_MAX;
        String d = series.equals("D") ? "D=" + minMaxString : "D=" + TestConfig.D_BASELINE;
        String f = series.equals("F") ? "F=" + minMaxString : "F=" + TestConfig.F_BASELINE;
        String k = series.equals("K") ? "K=" + minMaxString : "K=" + TestConfig.K_BASELINE;

        writer.println("Dataset includes: " + d + " " + f + " " + k);
    }
}
