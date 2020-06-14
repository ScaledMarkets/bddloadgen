/** Run a set of performance tests at a rate defined by one or more load profiles.
	This script can be run on one or more VMs. Test suites should be designed so that
	they do not interfere when run in parallel.

	Arguments: See the "main" method (below).
	*/
package testrunner;


import loadgen.profile.*;
import loadgen.TestRunnerUtil;
import loadgen.EnvVars;
import java.util.*;
import java.io.*;
import java.net.URLClassLoader;
import org.jbehave.core.embedder.*;
import org.jbehave.core.steps.*;


/** Define a class that can perform a set of cucumber tests, asychronously, according to
	a distribution consisting of a series of contiguous linear load ramps. */
public class TestRunner
{
	// Define some "governors": program will issue warnings or terminate if any of these are reached.
	private static int MaxRequestRate = 100;  // terminate if exceeded
	private static int MaxNoOfProcesses = 1000;  // issues warning only
	private static int TestTimeoutTime = 1000;  // Max time (in seconds) that a test can execute before it is killed.

	private static long thisRandomSeed = Long.parseLong(System.getenv("RANDOM_SEED"));
	private static Random thisRandom = new Random(thisRandomSeed);
	private static long thisUniqueId = 0;
	private static long thisStartTimeOfRun;
	private static int thisResultsStatus = 0;
	private static Boolean thisAbortIfTestsCannotAchieveProfile = null;;
	private static String thisFeatureSpec;
	private static String thisFeatureString;
	private static List<String> thisFeatures = new Vector<String>();
	private static List<String> thisStepClassNames;
	private static List<String> thisStepClasspathElementURLs;
	private static List<String> thisProfileFiles = new Vector<String>();
	private static String thisResultsDir;
	private static String thisLockfile;
	private static String thisLogfile;
	private static List<Profile> thisProfiles = new Vector<Profile>();


	/** Main entry point. (See end of this file, where this method is invoked.)
	 Arguments:
		arg 1: Features dir: A path (may be relative to cur dir) on the test node
			for the directory containing the cucumber features to be tested. Usually
			this directory is called "features". Or, may be a sequence of features,
			specified in the manner that JBehave requires; however, each feature
			is separated from the others by a semicolon.
		arg 2: Step classes: A comma-separated (no spaces) list of class names. These
			classes must extend org.jbehave.core.steps.StepCandidate.
		arg 3: Step classpath: A classpath (may be relative to cur dir) on the test node
			for the Java class files containing the step definitions.
		arg 4: Results dir: a path (may be relative) on the test node, into which the
			results of this test run should be written.
		arg 5...: Profiles: One or more paths (may be relative to cur dir) on the
			test node for files containing request profiles. These file are generated
			by the LoadGenerator and then moved to each test node. */
	public static void main(String[] args)
	{
		if (args.length < 4) throw new RuntimeException("Expected 4 arguments.");
		System.out.println("All tests started...");
		System.out.println("System Time (ms): " + System.currentTimeMillis());

		String fspec = args[0];
		String rdir = args[1];
		String stepClasses = args[2];
		String stepcp = args[3];

		setFeatureSpec(fspec);
		setStepClasses(stepClasses);
		setStepClasspath(stepcp);
		System.out.println("featureSpec = " + fspec);
		System.out.println("Using abortIfTestsCannotAchieveProfile=" +
			abortIfTestsCannotAchieveProfile());

		setResultsDir(canonicalizePath(rdir));
		if (! (new File(resultsDir())).isDirectory()) throw new RuntimeException(
			"Directory not found: " + resultsDir());
		System.out.println("results directory = " + resultsDir());
		TestRunnerUtil.removeOldLogfile();

		// Read and validate arguments.
		int argno = 0;
		for (String arg : args)
		{
			argno = argno + 1;
			if (argno <= 4) continue;
			String f = arg;
			System.out.println("Profile file: " + f);
			String file = canonicalizePath(f);
			if (! (new File(file)).exists()) throw new RuntimeException(
				"File not found: " + file);
			addProfileFile(file);
		}

		removeOldLogfile();
		buildProfiles();
		performProfiles();

		System.out.println("Tests complete: results are valid: " + resultsAreValid());
	}


	/** Exception class indicating the the node cannot maintain the precise request
	 rate specified by the profile. This can occur if the node does not have enough
	 resources, or if the latency in forking is large compared to the average time
	 between tests. */
	public static class NodeCannotKeepUp extends Exception
	{
		public NodeCannotKeepUp(String msg) { super(msg); }
	}


  // Implementation methods and classes ------------------------------------------


	private static boolean abortIfTestsCannotAchieveProfile()
	{
		if (thisAbortIfTestsCannotAchieveProfile == null)
		{
			String s = System.getenv("ABORT_IF_TESTS_CANNOT_ACHIEVE_PROFILE_VALUE");
			thisAbortIfTestsCannotAchieveProfile = new Boolean(s.equals("true"));
		}

		return thisAbortIfTestsCannotAchieveProfile.booleanValue();
	}


	private static int ProcessReturnCodeForWarning = 1;
	private static int ProcessReturnCodeForError = 2;


	/** Log the start and end time. */
	private static void writeTimeLogEntry(String reqType, long startTime, long endTime, String result)
	{
		String id = System.getenv("ID");
		String reqRate = System.getenv("REQ_RATE");
		int timeout = 10;  // flock will timeout after this number of seconds.
		double delta = (double)(endTime - startTime) / 1000.0;
		String command = "echo reqType:" + reqType + ", id:" + id + ", name:EndToEnd, reqRate:" +
			reqRate + ", start:" + startTime + ", end:" + endTime + ", delta:" +
			delta + ", result:" + result + " >> " + logfile();
		command = "flock -w " + timeout + " " + lockfile() + " -c \"" + command + "\"";
		System.out.println("flock command: " + command);
		Process p;
		try
		{
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
		}
		catch (Exception ex) { throw new RuntimeException(ex); }

		if (p.exitValue() != 0) System.out.println("ERROR: Unable to write to log; process status was " + p.exitValue());
	}


	private static void setFeatureSpec(String spec)
	{
		thisFeatureSpec = spec;
		thisFeatureString = spec.replace(";", " ");

		String[] parts = spec.split(" ");
		for (String part : parts)
		{
			expandFiles(thisFeatures, part, new FilenameFilter()
			{
				public boolean accept(File dir, String name)
				{
					File f = new File(dir, name);
					if (f.isDirectory()) return true;
					if (name.endsWith(".feature")) return true;
					if (name.endsWith(".story")) return true;
					return false;
				}
			});
		}
	}


	private static void setStepClasses(String classes)
	{
		String[] parts = classes.split(",");
		thisStepClassNames = new Vector<String>();
		for (String part : parts) thisStepClassNames.add(part);
	}


	private static void setStepClasspath(String cp)
	{
		String[] parts = cp.split(":");
		thisStepClasspathElementURLs = new Vector<String>();
		for (String part : parts) thisStepClasspathElementURLs.add("file://" + part);
	}


	/** Recursively expand the path into the files that are contained in it, and
		add them to the specified List. */
	private static List<String> expandFiles(List<String> files, String path, FilenameFilter filter)
	{
		File file = new File(path);
		if (file.isDirectory())
		{
			for (String s : file.list(filter)) expandFiles(files, path + "/" + s, filter);
		}
		else
			files.add(path);
		return files;
	}


	private static String featureSpec()
	{
		return thisFeatureSpec;
	}


	private static List<String> stepClasspathElementURLs()
	{
		return thisStepClasspathElementURLs;
	}


	private static List<String> stepClasseNames()
	{
		return thisStepClassNames;
	}


	/** Return a space-separated sequence of features. This string is passed
		directly to cucumber. */
	private static String featureString()
	{
		return thisFeatureString;
	}


	/** Return a list of story/feature file paths. Directories are expaned to
		file paths. */
	private static List<String> features()
	{
		return thisFeatures;
	}


	private static void addProfileFile(String file)
	{
		profileFiles().add(file);
		System.out.println("Added profile file " + file);
	}


	private static List<String> profileFiles()
	{
		return thisProfileFiles;
	}


	private static void setResultsDir(String dir)
	{
		thisResultsDir = dir;
		thisLockfile = thisResultsDir + "/lockfile";
		thisLogfile = thisResultsDir + "/timelog.csv";
	}


	private static String resultsDir()
	{
		return thisResultsDir;
	}


	private static String lockfile()
	{
		return thisLockfile;
	}


	private static String logfile()
	{
		return thisLogfile;
	}


	/** Expand the specified path, and remove any trailing slash. */
	private static String canonicalizePath(String path)
	{
		String expPath = (new File(path)).getAbsolutePath();
		if (expPath.endsWith("/"))
			return expPath.substring(0, expPath.length()-1);
		else
			return expPath;
	}


	private static void removeOldLogfile()
	{
		if (logfile() == null) return;
		File f = new File(logfile());
		if (f.exists()) f.delete();
	}


	private static void buildProfiles()
	{
		for (String profileFile : profileFiles())
		{
			buildProfile(profileFile);
		}
	}


	/** Begin the execution of each profile. Each is done as a separate thread
		so that they each occur in parallel. */
	private static void performProfiles()
	{
		setStartTimeOfRun();
		List<Thread> profileThreads = new Vector<Thread>();
		for (Profile profile : profiles())
		{
			Thread t = new Thread(new Runnable()
			{
				public void run()
				{
					profile.perform(); // can terminate with NodeCannotKeepUp
				}
			});
			profileThreads.add(t);
			t.start();
			System.out.println("Initiated profile for requestType " + profile.requestType());
		}

		try {
			for (Thread t : profileThreads) t.join();  // wait for all of the above  to exit.
		} catch (InterruptedException ex) {
			throw new RuntimeException("Thread join interrupted");
		}

		for (Profile profile : profiles())
		{
			if (profile instanceof PerformanceProfile)
			{
				if (! ((PerformanceProfile)profile).resultsAreValid())
					setResultsInvalid("Execution of profile " + profile.profileType() + " is invalid");

				if (((PerformanceProfile)profile).resultsAreSkewed())
					setResultsSkewed("Results for profile " + profile.profileType() + " are skewed");
			}
		}
	}


	private static List<Profile> profiles()
	{
		return thisProfiles;
	}


	/** Read the distribution file and build the Distribution object.
		The counterpart to this method in LoadGenerator.rb is the method
		getProfileDefinition(profileName).
		The format of a profile is specified in the heading of that method.
		These two methods should ideally be factored into a shared module but it
		is too much trouble for the benefit. */
	private static void buildProfile(final String profileFile)
	{
		PerformanceProfileReader reader = new PerformanceProfileReader()
		{
			private PerformanceProfile profile;

			public String getProfileFile() { return profileFile; }

			public void createProfile()
			{
				this.profile = new PerformanceProfile("ramp", random().nextLong());
				profiles().add(profile);
			}

			public void setRequestType(String rt)
			{
				this.profile.setRequestType(rt);
			}

			public void setTagString(String ts)
			{
				this.profile.setTagString(ts);
			}

			public void addLevel(double requestsPerSec, double deltaInMinutes)
			{
				profile.distribution().level(requestsPerSec, deltaInMinutes);
			}
		};

		reader.readProfile();
	}


	private static void setResultsInvalid(String reason)
	{
		setResultsStatus(ProcessReturnCodeForError);
		System.err.println("===========ERROR: RESULTS ARE INVALID: " + reason);
	}


	private static boolean resultsAreInvalid()
	{
		return (getResultsStatus() == ProcessReturnCodeForError);
	}


	private static boolean resultsAreValid()
	{
		return ! resultsAreInvalid();
	}


	private static void setResultsSkewed(String reason)
	{
		setResultsStatus(ProcessReturnCodeForWarning);
		System.err.println("WARNING: results are skewed: " + reason);
	}


	private static boolean resultsAreSkewed()
	{
		return (getResultsStatus() == ProcessReturnCodeForWarning);
	}


	private static void setResultsStatus(int status)
	{
		thisResultsStatus = status;
	}

	private static int getResultsStatus()
	{
		return thisResultsStatus;
	}


	private static void setStartTimeOfRun()
	{
		thisStartTimeOfRun = System.currentTimeMillis();
	}


	private static long startTimeOfRun()
	{
		return thisStartTimeOfRun;
	}


	private static long getCurRelativeTimeInMs()
	{
		return System.currentTimeMillis() - startTimeOfRun();
	}


	private static Random random()
	{
		return thisRandom;
	}

	private static long getRandomSeed()
	{
		return thisRandomSeed;
	}

	private static void setRandomSeed(long seed)
	{
		thisRandomSeed = seed;
		thisRandom = new Random(seed);
		System.out.println("Main thread using random seed " + seed);
	}


	private static long getUniqueId()
	{
		return thisUniqueId;
	}

	private static void seedUniqueId(long seed)
	{
		thisUniqueId = seed;
	}

	private static long createUniqueId()
	{
		return thisUniqueId = (thisUniqueId + 1);
	}


	/** Defines a request rate distribution to be run. A distribution consists of
		a series of levels, each representing a target request rate. A distribution
		implicitly begins at the level 0, at elapsed time 0, and so a level for the
		starting point should not be specified. */
	private static class Distribution
	{
		private String thisType;
		private List<double[]> levels = new Vector<double[]>();

		Distribution(String type)
		{
			thisType = type;
		}

		String type()
		{
			return thisType;
		}

		/** A level is a point at the end of a linear ramp. Thus, a level is a new
			target point, to which the request rate will be ramped. Tests implicitly
			begin at the level (0.0,0.0). Arguemnts must be float. */
		void level(double requestsPerSec, double deltaInMinutes)
		{
			if (deltaInMinutes == 0.0) throw new RuntimeException(
				"Cannot specify an elapsed time of 0");
			levels.add(new double[] { requestsPerSec, deltaInMinutes });
		}

		List<double[]> levels()
		{
			return levels;
		}
	}


	abstract private static class Profile
	{
		private int thisResultsStatus = 0;
		private String thisProfileType;
		private String thisRequestTypeName;
		private String thisTagString;
		private long thisUniqueId = 1;
		private List<Thread> thisRequestThreads = new Vector<Thread>();

		Profile()
		{
		}


		abstract void perform();


		List<Thread> requestThreads() { return thisRequestThreads; }


		void setProfileType(String profileType)
		{
			thisProfileType = profileType;
		}

		String profileType()
		{
			return thisProfileType;
		}

		void setRequestType(String reqTypeName)
		{
			thisRequestTypeName = reqTypeName;
		}

		String requestType()
		{
			return thisRequestTypeName;
		}

		void setTagString(String data)
		{
			thisTagString = data;
		}

		String tagString()
		{
			return thisTagString;
		}

		long getCurRelativeTimeInMs()
		{
			return TestRunner.getCurRelativeTimeInMs();
		}

		long getUniqueId()
		{
			return thisUniqueId;
		}

		void seedUniqueId(long seed)
		{
			thisUniqueId = seed;
		}

		long createUniqueId()
		{
			return thisUniqueId = (thisUniqueId + 1);
		}

		// Multi-threaded and multi-process structure:
		//	TestRunner main thread
		//		One thread for each Profile, to cycle through each ramp.
		//			One process for each request of each ramp

		// Random numbre generation:
		//	Each profile gets its own random generator, seeded by the main thread.


		/** Perform one test run. This method creates an asynchronous
			thread and a new class loader in order to ensure isolation from other tests.
			Non-blocking.
			For tutorial on JBehave:
			https://blog.codecentric.de/en/2012/06/jbehave-configuration-tutorial/ */
		void performOneRequest(final String reqType, String tagStr)
		{
			final long startTime = getCurRelativeTimeInMs();

			try
			{
				final Thread t = new Thread()
				{
					public void run()
					{
						requestThreads().add(this);

						// Execute JBehave in a new Class Loader to provide isolation.
						// The class loader must be able to find the loadgen jar.
						List<String> cp = new Vector<String>();
						cp.add("file://" + System.getenv(EnvVars.LoadgenJarPathOnNode));
						cp.addAll(stepClasspathElementURLs());
						EmbedderClassLoader loader = new EmbedderClassLoader(
							cp, ClassLoader.getSystemClassLoader());

						Embedder embedder = new Embedder();
						embedder.useClassLoader(loader);
						List<String> storyPaths = features();

						embedder.useMetaFilters(Arrays.asList(new String[] { tagString() } ));
						// See http://jbehave.org/reference/stable/javadoc/core/index.html

						for (String className : stepClasseNames()) try
						{
							Class c = loader.loadClass(className);
							if (! (org.jbehave.core.steps.StepCandidate.class.isAssignableFrom(c)))
								throw new RuntimeException("Class " +
									className + " is not a StepCandidate");

							StepCandidate step = (StepCandidate)(loader.newInstance(c, className));
							final List<StepCandidate> stepList = new Vector<StepCandidate>();
							stepList.add(step);
							List<CandidateSteps> stepsList = new Vector<CandidateSteps>();
							Steps steps = new Steps() {
								public List<StepCandidate> listCandidates() { return stepList; }
							};
							stepsList.add(steps);
							embedder.useCandidateSteps(stepsList);
						}
						catch (Exception ex) { throw new RuntimeException(ex); }

						boolean success = false;
						try {
							System.out.println("Running stories...");
							embedder.runStoriesAsPaths(features());
							success = true;
						}
						catch (Exception ex) {
							ex.printStackTrace(System.err);
						}

						requestThreads().remove(this);
						writeTimeLogEntry(reqType, startTime, getCurRelativeTimeInMs(), Boolean.toString(success));
					}
				};
				t.start();

			}
			catch (Throwable t)
			{
				System.out.println("JBehave run failed. Stack trace follows.");
				t.printStackTrace(System.err);
			}
		}
	}


	private static class FunctionalProfile extends Profile
	{
		void perform()
		{
			// Simply perform the request type, in this process.
			performOneRequest(requestType(), tagString());
		}


		void performOneRequest(String reqType, String tagStr)
		{
			TestRunnerUtil.setUniqueId(createUniqueId());
			TestRunnerUtil.setRequestType(reqType);
			TestRunnerUtil.setTimeOffset(TestRunner.startTimeOfRun());
			super.performOneRequest(reqType, tagStr);
		}
	}


	/** A performance profile is simply the concatenation of a distribution and a request type.
		Each profile is performed in parallel, via the performProfiles method, which
		calls a performProfile method for each profile.
		To do: move the TestRunner.performProfile method into the Profile class. */
	private static class PerformanceProfile extends Profile
	{
		private long thisRandomSeed;
		private Random thisRandom;
		private Distribution thisDistribution;


		PerformanceProfile(String type, long seed)
		{
			setDistribution(new Distribution("ramp"));
			setRandomSeed(seed);
		}


		void performOneRequest(String reqType, String tagStr, double reqRate)
		{
			TestRunnerUtil.setUniqueId(createUniqueId());
			TestRunnerUtil.setRequestType(reqType);
			TestRunnerUtil.setRequestRate(reqRate);
			TestRunnerUtil.setTimeOffset(TestRunner.startTimeOfRun());
			super.performOneRequest(reqType, tagStr);
		}


		void setDistribution(Distribution dist)
		{
			thisDistribution = dist;
		}

		Distribution distribution()
		{
			return thisDistribution;
		}


		Random random()
		{
			return thisRandom;
		}

		long getRandomSeed()
		{
			return thisRandomSeed;
		}

		void setRandomSeed(long seed)
		{
			thisRandomSeed = seed;
			thisRandom = new Random(seed);
		}


		void setResultsInvalid(String reason)
		{
			setResultsStatus(ProcessReturnCodeForError);
			System.err.println("===========ERROR: RESULTS ARE INVALID: " + reason);
		}


		boolean resultsAreInvalid()
		{
			return (getResultsStatus() == ProcessReturnCodeForError);
		}


		boolean resultsAreValid()
		{
			return ! resultsAreInvalid();
		}


		void setResultsSkewed(String reason)
		{
			setResultsStatus(ProcessReturnCodeForWarning);
			System.err.println("WARNING: results are skewed: " + reason);
		}


		boolean resultsAreSkewed()
		{
			return (getResultsStatus() == ProcessReturnCodeForWarning);
		}


		void setResultsStatus(int status)
		{
			thisResultsStatus = status;
		}

		int getResultsStatus()
		{
			return thisResultsStatus;
		}


		/** Perform a ramp for each level of the Distribution. */
		void perform()
		{
			long t = System.currentTimeMillis();

			double initialReqRatePerSec = 0.0;
			int lno = 0;  // level number
			for (double[] level : distribution().levels()) try
			{
				long startTime = getCurRelativeTimeInMs();
				lno = lno + 1;
				double finalReqRatePerSec = level[0];
				double rampDurationInSec = level[1] * 60.0;  // convert from min to sec.

				System.out.println();
				System.out.println("Calling perform for level " + lno +
					" for req type=" + requestType() + " at t=" + getCurRelativeTimeInMs() +
					" seconds from start of run...");
				System.out.println();

				try
				{
					performLevel(requestType(), tagString(),
						initialReqRatePerSec, finalReqRatePerSec, rampDurationInSec);
					System.out.println("...completed level " + lno +
						" for req type " + requestType() + " at t=" + getCurRelativeTimeInMs() +
						" seconds from start of run.");
				}
				catch (NodeCannotKeepUp ex)
				{
					setResultsSkewed(ex.getMessage());
					System.out.println("...level " + lno +
						" completed with warning for req type " + requestType() +
						" at t=" + getCurRelativeTimeInMs() + " seconds from start of run.");
					throw new RuntimeException(ex);
				}

				long endTime = getCurRelativeTimeInMs();
				long elapsedTime = endTime - startTime;
				long sleepTime = (long)(rampDurationInSec * 1000.0) - elapsedTime;  // in ms.
				if (sleepTime >= 0.0) Thread.sleep(sleepTime);   // sleep until the end of the ramp
				System.out.println("Level " + lno + " finished sending requests for req type " +
					requestType() + " at t=" + getCurRelativeTimeInMs() +
					" seconds from start of run.");

				initialReqRatePerSec = finalReqRatePerSec;
			}
			catch (InterruptedException ex) { throw new RuntimeException(ex); }

			// Wait until all child threads are done.
			while (true) try
			{
				System.out.println("There are " + requestThreads().size() + " workers...");
				if (requestThreads().size() == 0) break;
				Thread.sleep(1000);  // 1000 ms
			}
			catch (InterruptedException ex) { throw new RuntimeException(ex); }
		}


		/** Generate the load defined by a Distribution Level. Load is generated
			beginning at the current ("initial") load level, escalating (or decreasing) to
			the request rate defined by the Level parameters. */
		void performLevel(String reqType, String tagStr, double initialReqRatePerSec,
			double finalReqRatePerSec, double rampDurationInSec)
		throws
			NodeCannotKeepUp
		{
			// At present, only ramps are implemented. Later, other functions
			// will be supported.
			ramp(reqType, tagStr, initialReqRatePerSec, finalReqRatePerSec, rampDurationInSec);
		}


		/** Generate a linearly ramped load as specified by the arguments.
			The ramp is defined by two endpoints: the initial request rate, beginning
			at the current time, the final request rate, and the duration of the
			time interval.
			All arguments must be float.
			See https://drive.google.com/open?id=1fumERgNlwgWLeLsRaaWNCssZpIyo2Eu-D7AfDeXMGVY&authuser=0
			*/
		void ramp(String reqType, String tagStr, double initialReqPerSec,
			double finalReqPerSec, double rampDurationInSec)
		throws
			NodeCannotKeepUp
		{
			System.out.println("ramp(" + initialReqPerSec + ", " + finalReqPerSec +
				", " + rampDurationInSec + ")");
			long initialRampTimeInMs = getCurRelativeTimeInMs();
			long finalRampTimeInMs = initialRampTimeInMs + (long)rampDurationInSec * 1000;
			double rampSlope = (finalReqPerSec - initialReqPerSec) / rampDurationInSec;

			// Loop until we reach the final time for the ramp interval.
			boolean pastFinalTimeForRamp = false;
			double secThroughLoop = 0.0;
			double computeTimeInSec = 0.0;
			double secToNextRequest;
			int i = 0;
			while (! pastFinalTimeForRamp)
			{
				long loopStartTimeInMs = getCurRelativeTimeInMs();

				double randomDouble = random().nextDouble();
				double reqRate = rampSlope * (double)(loopStartTimeInMs - initialRampTimeInMs)/1000.0 + initialReqPerSec;
				double secToEndOfRamp = (double)(finalRampTimeInMs - getCurRelativeTimeInMs())/1000.0;
				if (secToEndOfRamp < 0.0) break;
				double tEnd = secToEndOfRamp;
				if (rampSlope < 0.0) // Deal with discontinuity in ramp function.
				{
					// Compute F for t=tEnd:
					double f0 = 1.0 - Math.exp(-rampSlope * tEnd*tEnd / 2.0 - reqRate*tEnd);
					// If ramp slope is negative, then F is undefined in <f0, 1>.
					if (randomDouble > f0) // CDF is imaginary.
						// The request occurs after the end of the ramp, so exit the ramp.
						//System.out.println("CDF is imaginary:  f0=" + f0 +
						//", randomDouble=" + randomDouble);
						//System.out.println("\tT=" + tEnd + ", rampSlope=" + rampSlope +
						//", reqRate=" + reqRate);
						secToNextRequest = secToEndOfRamp;
					else
						secToNextRequest = icdf(rampSlope, reqRate, randomDouble);
				}
				else
					secToNextRequest = icdf(rampSlope, reqRate, randomDouble);

				// Install a governor.
				if (reqRate > MaxRequestRate) throw new RuntimeException("Aborting: reqRate = " + reqRate);

				// Check if the requests are falling behind the required request rate.
				if (secToNextRequest < computeTimeInSec)
				{
					String msg = "Node cannot keep up with request rate of " + reqRate + "; " +
							"secThroughLoop=" + secThroughLoop + "; compute time=" + computeTimeInSec;
					setResultsSkewed(msg);
					System.err.println("Warning - can't keep up");
					if (TestRunner.abortIfTestsCannotAchieveProfile()) throw new NodeCannotKeepUp(msg);
					System.err.println("Node was not able to maintain request rate at t=" +
						getCurRelativeTimeInMs());
				}

				// Check if we will go past the end of the ramp.
				if (secToNextRequest >= secToEndOfRamp) try
				{
					pastFinalTimeForRamp = true;
					Thread.sleep((long)secToEndOfRamp * 1000);
					continue;
				}
				catch (InterruptedException ex) { throw new RuntimeException(ex); }

				// Sleep until the next test must be initiated. Assuming that tests
				// can be initiated as quickly as required, we achieve the required
				// request rate.
				i = i + 1;
				System.out.println("For " +reqType + ", iter " + i +
					", about to sleep " + secToNextRequest + " seconds:");
				secToNextRequest = secToNextRequest - (getCurRelativeTimeInMs() - loopStartTimeInMs);
				if (secToNextRequest <= 0.0)
				{
					String msg = "Node cannot keep up with request rate of " + reqRate + "; " +
						"secThroughLoop=" + secThroughLoop + "; compute time=" + computeTimeInSec;
					setResultsSkewed(msg);
					if (TestRunner.abortIfTestsCannotAchieveProfile()) throw new NodeCannotKeepUp(msg);
				}
				else try
				{
					Thread.sleep((long)secToNextRequest*1000);
					System.out.println("\tfor " + reqType + ", iter " + i +
						", slept " + secToNextRequest + " seconds.");
				}
				catch (InterruptedException ex) { throw new RuntimeException(ex); }

				// Invoke a test. This must be done as a non-blocking action.
				reqRate = rampSlope * (double)(getCurRelativeTimeInMs() - initialRampTimeInMs)/1000.0 + initialReqPerSec;

				// Perform in a manner that ensures isolation of the test
				// from other tests. This does not block.
				performOneRequest(reqType, tagStr, reqRate);

				//sleep 2.0  //////// To test the "governor", uncomment this line.
				secThroughLoop = (getCurRelativeTimeInMs() - loopStartTimeInMs)/1000.0;
				computeTimeInSec = secThroughLoop - secToNextRequest;
			}
		}


		/** Inverse cumulative distribution function, for a "hazard function" of
				lambda = a t + C
			where a is the rate at which requests increase (i.e., the slope of the
			ramp function), and C is the request rate at t=0. The hazard function
			is simply the request rate. cp is the cumulative probability that a request
			will have occurred before time t: the value of cp should be chosen using
			a random variable with uniform distribution between 0 and 1.
			See https://drive.google.com/open?id=1X3S8DdAt4GHZkcIgyxDM8re2-Op9ZHXPoxaczOdVQjk&authuser=0
			See also this article: http://data.princeton.edu/wws509/notes/c7.pdf
			*/
		double icdf(double a, double c, double cp)
		{
			try
			{
				if (a == 0.0)
					return (- Math.log(1.0 - cp) / c);
				else
					return (-c + Math.sqrt((c*c) - (2.0 * a * Math.log(1.0 - cp)))) / a ;
			}
			catch (Exception ex)
			{
				System.err.println("icdf: ta=" + a + ", c=" + c + ", cp=" + cp +
					", log(1-cp)=" + Math.log(1.0-cp));
				throw new RuntimeException(ex);
			}
		}
	}
}
