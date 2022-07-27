package ljeda.medicover.log;

public class Logger {
	
	private static boolean debugMode = true;
	
	private Logger() {}

	public static void log(String message) {
		System.out.println(message);
	}

	public static void warn(String message) {
		System.out.println("WARNING: " + message);
	}

	public static void error(String message) {
		System.err.println(message);
	}

	public static void debug(String message) {
		if (debugMode) {
			System.out.println(message);
		}
	}
	
}
