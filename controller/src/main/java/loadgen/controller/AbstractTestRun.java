package loadgen.controller;

import loadgen.TestRunnerConstants;

import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;


/** A specification for a set of tests to be performed on a set of nodes (VMs).
	Once fully specified, a test run can be executed by calling its start method.
	Abstract base class for all test runs. */
abstract class AbstractTestRun
{
	private LoadGenerator lg;
	private String thisName;
	private String thisHostname;
	private String thisTimestamp;
	private String thisFeaturesDirectory;
	private String thisStepsJarURL;
	private String thisNodeFeaturesDirectory;
	private String thisNodeStepsDirectory;
	private List<String> thisFeatures = new Vector<String>();
	private Set<String> thisStepClassNames = new HashSet<String>();
	private List<String> thisTestRunProfiles = new Vector<String>();
	private String thisProjectGemServerURL;
	private Map<String, String> thisEnvVars = new HashMap<String, String>();
	private String thisProjectName;
	private String thisProjectRepoName;
	private String thisProjectRepoURL;
	private String thisGitConfigString;
	private String thisGitFQDN;
	private String thisGitUserid;
	private String thisGitPassword;
	private List<String[]> thisRequireRubyDirs;
	private String thisProviderConfigName;
	private Integer thisNoOfNodes;
	private List<String> thisBinaryResources = new Vector<String>();
	private boolean thisNodesAreHeaded = false;
	private boolean thisReuseNodes = false;
	private boolean thisKeepNodes = false;
	private long thisRandomSeed = 0x1234;
	private List<Node> thisNodes = new Vector<Node>();
	private ReqLog thisReqLog;
	private DetailLog thisDetailLog;
	private AggLog thisAggLog;


	AbstractTestRun(LoadGenerator lg, String name)
	{
		this.lg = lg;
		lg.validateName(name, "Test run");
		try { thisHostname = InetAddress.getLocalHost().getHostName(); }
		catch (UnknownHostException ex) { throw new RuntimeException(ex); }
		thisTimestamp = (new Date()).toString();
	}

	abstract boolean isFunctional();

	abstract boolean isPerformance();

	abstract String getTestRunType();

	String name()
	{
		return thisName;
	}

	/** Return the name of the test controller host. */
	String hostname()
	{
		return thisHostname;
	}

	/** Return the time at which this test run was defined. */
	String timestamp()
	{
		return thisTimestamp;
	}

	/** Return the names of the Profiles that are to be used for this AbstractTestRun. */
	List<String> testRunProfiles()
	{
		return thisTestRunProfiles;
	}

	ReqLog reqLog() { return this.thisReqLog; }

	DetailLog detailLog() { return this.thisDetailLog; }

	AggLog aggLog() { return this.thisAggLog; }

	List<ReqTimeLogEntry> reqTimeLogData()
	{
		return reqLog().timeLogData();
	}

	List<DetailTimeLogEntry> detailTimeLogData()
	{
		return detailLog().timeLogData();
	}

	List<AbstractTimeLogEntry> aggTimeLogData()
	{
		return aggLog().timeLogData();
	}

	/** Define an environment variable that will be set and exported to the process
		in which the tests will be run. */
	public void defineEnvVariable(String name, String value)
	{
		thisEnvVars.put(name, value);
	}

	String getEnvVariable(String name)
	{
		return thisEnvVars.get(name);
	}

	Map<String, String> envVars()
	{
		return thisEnvVars;
	}


	/** Specify where the tests are. This is normally a directory named "features". */
	public void setFeaturesDirectory(String path)
	{
		if (path.endsWith("/"))
			thisFeaturesDirectory = path.substring(0, path.length()-1);
		else
			thisFeaturesDirectory = path;
	}

	String featuresDirectory()
	{
		return thisFeaturesDirectory;
	}


	public void setStepsJarURL(String url)
	{
		thisStepsJarURL = url;
	}

	String stepsJarURL()
	{
		return thisStepsJarURL;
	}


	/** Return the directory on the test nodes where the features exist. */
	String nodeFeaturesDirectory()
	{
		if (thisNodeFeaturesDirectory == null)
		{
			String pn = "";
			String prn = "";
			thisNodeFeaturesDirectory =
				TestRunnerConstants.NodeProjectRoot + "/" + TestRunnerConstants.NodeFeatureDirName;
		}
		return thisNodeFeaturesDirectory;
	}

	String nodeStepsJarPath()
	{
		if (thisNodeStepsDirectory == null)
		{
			String pn = "";
			String prn = "";
			thisNodeStepsDirectory = TestRunnerConstants.NodeProjectRoot + "/" +
				TestRunnerConstants.NodeStepsJarName;
		}
		return thisNodeStepsDirectory;
	}


	/** Specify a particular feature to test. The featuresDirectory is prepended
		to this string. The string may contain a full feature spec, including
		sub-directories and line number. What is passed to JBehave is merely
		{featuresDirectory}/{feature} for each feature that is specified.
		If no features are specified, then only the directory is passed to
		cucumber. */
	public void feature(String name)
	{
		features().add(name);
	}

	List<String> features()
	{
		return thisFeatures;
	}


	public void stepClass(String name)
	{
		thisStepClassNames.add(name);
	}

	Set<String> stepClassNames()
	{
		return thisStepClassNames;
	}


	/** Specify the name of a load profile (AbstractProfile) that must be used for running
		the tests. This method can be called multiple times to specify multiple
		concurrent profiles. */
	public void useProfile(String pr)
	{
		testRunProfiles().add(pr);
	}

	public void setProjectRepoName(String name)
	{
		thisProjectRepoName = name;
	}

	String projectRepoName()
	{
		return thisProjectRepoName;
	}

	public void setProjectRepoURL(String url)
	{
		thisProjectRepoURL = url;
	}

	String projectRepoURL()
	{
		return thisProjectRepoURL;
	}

	/** E.g., "http.sslVerify false" */
	public void setGitConfigString(String str)
	{
		thisGitConfigString = str;
	}

	String gitConfigString()
	{
		return thisGitConfigString;
	}

	public void setProjectRepoFQDN(String fqdn)
	{
		thisGitFQDN = fqdn;
	}

	String projectRepoFQDN()
	{
		return thisGitFQDN;
	}

	public void setGitUserid(String userid)
	{
		thisGitUserid = userid;
	}

	String gitUserid()
	{
		return thisGitUserid;
	}

	public void setGitPassword(String pswd)
	{
		thisGitPassword = pswd;
	}

	String gitPassword()
	{
		return thisGitPassword;
	}

	/** Specify a directory that is required by the cucumber tests. Optional.
		The directory is relative to the project repo root.
		The specified env variable will be set to the actual path on the node,
		so that test programs can access it if they desire.
		There must be a Gemfile in each of these directories. */
	public void requireRubyDir(String envvar, String path)
	{
		requireRubyDirs().add(new String[] {envvar, path});
	}

	List<String[]> requireRubyDirs()
	{
		return thisRequireRubyDirs;
	}

	/** E.g., "virtualbox", or "vmware_fusion". (Right now, only virtualbox is supported.) */
	public void setProviderConfig(String name)
	{
		thisProviderConfigName = name;
		if (getProvider() == null) throw new RuntimeException(
			"Unrecognized provider config name: " + name);
		if ((! getProvider().isDynamic()) && (thisNoOfNodes != null)) throw new RuntimeException(
			"Cannot specify a static provider if one has specified the number of nodes: " +
				"these are mutually exclusive settings.");
	}

	String providerConfig()
	{
		return thisProviderConfigName;
	}

	AbstractProvider getProvider()
	{
		return lg.getProviderConfigs().get(providerConfig());
	}

	String providerName()
	{
		return getProvider().providerName();
	}

	/** Specify the number of test client nodes that are needed to run the tests.
		For dynamic providers, the nodes will be brought up as needed. */
	public void setNoOfNodes(int n)
	{
		AbstractProvider p = getProvider();
		if ((p != null) && (! p.isDynamic())) throw new RuntimeException(
			"Cannot specify no of nodes if provider is static");

		System.out.println("Setting no of nodes for " + name() + " to " + n);
		thisNoOfNodes = new Integer(n);
	}

	int noOfNodes()
	{
		return thisNoOfNodes.intValue();
	}

	/** Specify a binary resource that will be installed, using yum, on each node. */
	public void binaryResource(String binResName)
	{
		thisBinaryResources.add(binResName);
	}

	List<String> binaryResources()
	{
		return thisBinaryResources;
	}

	/** Specify that test client nodes must be headed. */
	public void nodesAreHeaded()
	{
		thisNodesAreHeaded = true;
	}

	boolean areNodesHeaded()
	{
		return thisNodesAreHeaded;
	}

	/** If this is set to true, then existing nodes will not be destroyed, but
		instead will be reprovisioned if they exist. */
	boolean willReuseNodes()
	{
		return thisReuseNodes;
	}

	public void reuseNodes()
	{
		thisReuseNodes = true;
	}

	/** Specify that nodes shall not be destroyed after the test runs are complete. */
	public void keepNodes()
	{
		thisKeepNodes = true;
	}

	/** If this is set to true, then the nodes will not be destroyed after the
		test runs are complete. */
	boolean willKeepNodes()
	{
		return thisKeepNodes;
	}

	/** Create a new Node, add it to this AbstractTestRun's list of Nodes, and set the
		new Node's IP address. */
	Node createNode(String nodeName, long randomSeed, String ip)
	{
		Node node = new Node(nodeName, this);
		nodes().add(node);
		try {
			node.provision(randomSeed, ip);  // For a dynamic provider, the VM is also created.
		} catch (IOException ex) { throw new RuntimeException(ex); }
		return node;
	}

	String resultsDirectory()
	{
		return lg.getResultsDirectory() + "/" + name();
	}

	/** Specify a random seed for the test run. */
	void setRandomSeed(long seed)
	{
		thisRandomSeed = seed;
	}

	long randomSeed()
	{
		return thisRandomSeed;
	}

	/** Initiate this AbstractTestRun.
		This will determine all derived configuration parameters, set up the testing nodes,
		and start the tests.
		If the 'async' option is specified, perform as a sub-process, so that this
		method does not block and allows other test runs to be defined and started. */
	public void start (String... options)
	{
		try
		{
			// Create the results directory.
			File f = new File(resultsDirectory());
			if (! f.exists()) f.mkdirs();

			if ((options != null) && (options[0] != null) && (options[0].equals("async")))
			{
				System.out.println("async");
				new Thread( new Runnable() { public void run() { perform(); } } );
			}
			else
			{
				System.out.println("not async");
				perform();
			}
		}
		catch (Exception exception)
		{
			exception.printStackTrace(System.err);
			throw new RuntimeException(exception);
		}
	}


	abstract void perform();


	/** Create and provisions the nodes (VMs). The shell script that gets
		put on each node (testrunner.sh) takes a parameter that specifies
		the cucumber tags to use. */
	void provisionNodes()
	{
		AbstractProvider prov = getProvider();
		Random random = new java.util.Random(thisRandomSeed);
		if (prov.isDynamic())
		{
			// Create and provision the number of nodes specified by noOfNodes.
			int n = noOfNodes();
			if (n == 0) throw new RuntimeException(
				"noOfNodes is zero: no nodes to be created!");
			String[] ips = lg.getIps();  // may be null
			if ((ips != null) && (n > ips.length)) throw new RuntimeException(
				"Insufficient IP addresses in the ipPool");
			for (int i = 1; i <= n; i++)
			{
				String ip = null;
				if (ips != null) ip = ips[i-1];
				String nodeName = Integer.toString(i);
				long nodeRandomSeed = random.nextLong();
				createNode(nodeName, nodeRandomSeed, ip);  // ip may be null
			}
		}
		else
		{
			// Provision nodes as specified by the StaticProvider instance.
			if (prov.nodeIps().size() == 0) throw new RuntimeException(
				"No nodes specified for provider!");
			//	Map<String, String> nodeIps()

			Map<String, String> ips = prov.nodeIps();
			for (String nodeName : ips.keySet())
			{
				String ip = ips.get(nodeName);
				long nodeRandomSeed = random.nextLong();
				createNode(nodeName, nodeRandomSeed, ip);
			}
		}
	}

	/** Return the number of tests that were actually run of the specified request type. */
	int numberOfTests(String... reqTypeNames)
	{
		int count = 0;
		for (ReqTimeLogEntry entry : reqTimeLogData())
		{
			if ((reqTypeNames.length != 0) &&
				(! Util.arrayContains(reqTypeNames, entry.reqType)))
				continue;
			if (! entry.name.equals(TestRunnerConstants.EndToEnd))
				continue;

			count = count + 1;
		}
		return count;
	}

	/** Return the number of tests that passed, for the specified request type. */
	int numberPassed(String... reqTypeNames)
	{
		int nPassed = 0;
		for (ReqTimeLogEntry entry : reqTimeLogData())
		{
			if (! entry.result.equals("true")) continue;
			if ((reqTypeNames.length != 0) &&
				(! Util.arrayContains(reqTypeNames, entry.reqType))) continue;
			if (! entry.name.equals(TestRunnerConstants.EndToEnd)) continue;
			nPassed = nPassed + 1;
		}
		return nPassed;
	}


	/** Return the Node instances that have been created for this test run.
		These instances are created automatically, based on the performance test
		cucumber file. */
	List<Node> nodes()
	{
		return thisNodes;
	}

	/** Read the specified file, parse it, and add its data to timeLogData.
		The data is sorted (based on request start time) as it is added. */
	boolean parseTimeLog(String nodetimelogpath)
	{
		System.out.println("Parsing time log...");
		if (! (new File(nodetimelogpath)).exists())
		{
			System.err.println("Cannot find time log: " + nodetimelogpath);
			return false;
		}
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(nodetimelogpath));
			thisReqLog = new ReqLog(br);
			thisReqLog.parse();
		}
		catch (IOException ex) { throw new RuntimeException(ex); }

		System.out.println("...parsed time log: " + reqTimeLogData().size() + " entries.");
		return true;
	}


	/** Parse event level data.
		See the 'TestRunnerConstants' class for an explanation of events. */
	boolean parseDetailTimeLog(String logpath)
	{
		System.out.println("Parsing detail time log...");
		if (! (new File(logpath)).exists())
		{
			System.err.println("Cannot find time log: " + logpath);
			return false;
		}

		try
		{
			BufferedReader br = new BufferedReader(new FileReader(logpath));
			thisDetailLog = new DetailLog(br);
			thisDetailLog.parse();

			System.out.println("...parsed detail time log: " + detailTimeLogData().size() + " entries.");

			if (thisDetailLog.unmatchedEvents().size() > 0)
			{
				System.out.println("WARNING: There are " + thisDetailLog.unmatchedEvents().size() + " unmatched events:");
				for (String eventKey : thisDetailLog.unmatchedEvents().keySet())
				{
					System.out.println(eventKey + ": " + thisDetailLog.unmatchedEvents().get(eventKey));
				}
			}
		}
		catch (Exception ex) { throw new RuntimeException(ex); }

		return true;
	}


	/** Return the test result for the 'EndToEnd' entry with the specified id. */
	String getResultForId(String id)
	{
		for (ReqTimeLogEntry entry : reqTimeLogData())
		{
			if (entry.id.equals(id))
			{
				if (entry.name.equals(TestRunnerConstants.EndToEnd))
					return (String)entry.result;
			}
		}
		return null;
	}


	/** Retrieve all logs from the nodes, parse them and aggregate the data. */
	void retrieveLogs()
	{
		for (Node node : nodes())
		{
			// Retrieve the node's time logs.
			String localnodetimelogpath = resultsDirectory() + "/" + name() + "_" + node.name() + "_timelog.csv";
			node.fetchTimeLogInto(localnodetimelogpath);
			String localnodedetailtimelogpath = resultsDirectory() + '/' + name() + '_' + node.name() + "_detailtimelog.csv";
			node.fetchDetailTimeLogInto(localnodedetailtimelogpath);

			// Retrieve the stdout file from the tests.
			String localnodestdoutpath = resultsDirectory() + '/' + name() + '_' + node.name() + "_stdout.log";
			node.fetchStdoutInto(localnodestdoutpath);

			// Parse the log.
			if (! parseTimeLog(localnodetimelogpath))
			{
				System.err.println("There is no time log for node " + node.name());
				break;
			}

			// Parse the detailed log.
			if (! parseDetailTimeLog(localnodedetailtimelogpath))
			{
				System.err.println("There is no detail time log for node " + node.name());
				break;
			}

			createAggregateLog();
		}
	}


	/** Merge detail time log into an aggregate time log.
		The request rate and result fields need to be set. */
	void createAggregateLog()
	{
		thisAggLog = new AggLog();

		for (ReqTimeLogEntry entry : reqTimeLogData())
		{
			thisAggLog.insertAggTimeLogEntry(entry);
		}

		for (DetailTimeLogEntry detailEntry : detailTimeLogData())
		{
			String result = getResultForId(detailEntry.id);
			if (result == null) result = "incomplete";
			detailEntry.setResult(result);
			thisAggLog.insertAggTimeLogEntry(detailEntry);
		}
	}

	/** Destroy the VMs, if required. */
	void cleanup()
	{
		if (! willKeepNodes())
		{
			System.out.println("Removing each node for test run " + name() + "...");
			for (Node node : nodes()) node.destroy();
		}
		else
			System.out.println("Nodes not destroyed");
	}
}
