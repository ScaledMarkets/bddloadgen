package loadgen;

/**
 * Define the constants that are needed by TestRunner, but that must also be
 * accessed by LoadGenerator.
 */
public class TestRunnerConstants
{
	// Predefined event types.


	/** Includes time to invoke JBehave. This event is generated automatically
		by the TestRunner. */
	public static String EndToEnd = "EndToEnd";

	/** From timelogBeginTest to timelogEndTest */
	public static String WholeTest = "WholeTest";



	public static final String NodeProjectRoot = "/var/Project";
	public static final String NodeFeatureDirName = "features";
	public static final String NodeStepsJarName = "steps.jar";
	public static final String NodeStandardRoot = "var/Standard";
}
