package se.lnu.metrics;

/**
 * Counts DataStore read operations (CM2) per HTTP request.
 *
 */
public class OrchestrationCounter {

    private static int counter = 0;

    /**
     * Reset the counter.
     */
    public static void reset() {
        counter = 0;
    } // called before each request

    /**
     * Increments the counter
     */
    public static void increment() {
        counter++;
        
    } // called by DataStore.getNode/getChildren

    /**
     * 
     * @return The number counted.
     */
    public static int get() {
        return counter;
    } // called after request to read final value
}
