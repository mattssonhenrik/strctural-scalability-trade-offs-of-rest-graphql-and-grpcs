package se.lnu;

public class Main {
	public static void main(String[] args) throws Exception {
		String rq = args.length > 0 ? args[0].toUpperCase() : "RQ1";
		se.lnu.runner.TestRunner runner = new se.lnu.runner.TestRunner();
		switch (rq) {
			case "RQ1" -> runner.runRQ1();
			case "RQ2" -> runner.runRQ2();
			default -> {
				System.err.println("Unknown argument: " + args[0]);
				System.err.println("Usage: runner [RQ1|RQ2]");
				System.exit(1);
			}
		}
	}
}
