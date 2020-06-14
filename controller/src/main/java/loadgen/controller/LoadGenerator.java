package loadgen.controller;


import loadgen.profile.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


/** Main entry point for the test controller of the LoadGeneration framework.
	User-written performance test scenarios should make calls to the methods of
	this class. */
public class LoadGenerator
{
	private static String thisJBehaveJarURL;
	private static String thisLoadgenJarURL;
	private static Map<String, RequestType> requestTypes = new HashMap<String, RequestType>();
	private static Map<String, Distribution> distributions = new HashMap<String, Distribution>();
	private static Map<String, AbstractProfile> profiles = new HashMap<String, AbstractProfile>();
	private static String[] ips;
	private static Map<String, AbstractProvider> providerConfigs = new HashMap<String, AbstractProvider>();

	/** The location that has been set for where to place the test results
		on the test controller file system. */
	private static String resultsDirectory;

	private static Map<String, AbstractTestRun> testRuns = new HashMap<String, AbstractTestRun>();
	private static boolean testMode = false;

	/** Defines the configuration of the results database (normally, Elastic Search). */
	private static ResultsJSONDatabaseConfig resultsDatabaseConfig;


	public static void setJBehaveJarURL(String url) { thisJBehaveJarURL = url; }

	public static void setLoadgenJarURL(String url) { thisLoadgenJarURL = url; }


	/** Define a new request type. A request type is a type of test or test suite.
		When a load profile is defined, it is associated with a request type. */
	public static RequestType requestType(String name, Consumer<RequestType> block)
	{
		RequestType rt = new RequestType(name, block);
		requestTypes.put(name, rt);
		return rt;
	}


	/** Define a load distribution. Once defined, one can add "levels" to the
		distribution. Thus, a load distribution is a sequence of levels, where
		each level is a (rate, after_time) value pair. */
	public static Distribution distribution(String name, Consumer<Distribution> block)
	{
		Distribution d = new Distribution(name, block);
		distributions.put(name, d);
		return d;
	}


	/** Define a profile. A profile is simply a combination of a request type
		and a load distribution. */
	public static PerformanceProfile performanceProfile(String name, Consumer<PerformanceProfile> block)
	{
		PerformanceProfile p = new PerformanceProfile(name, block);
		profiles.put(name, p);
		return p;
	}


	/** Define a profile. A profile is simply a combination of a request type
		and a load distribution. */
	public static FunctionalProfile functionalProfile(String name, Consumer<FunctionalProfile> block)
	{
		FunctionalProfile p = new FunctionalProfile(name, block);
		profiles.put(name, p);
		return p;
	}


	/** Define the available IPs to use for the test client (Node) VMs. */
	public static void ipPool(String... ipAddresses)
	{
		ips = ipAddresses;
	}


	static Map<String, AbstractProvider> getProviderConfigs()
	{
		return providerConfigs;
	}


	static ResultsJSONDatabaseConfig getResultsDatabaseConfig()
	{
		return resultsDatabaseConfig;
	}


	static String getResultsDirectory()
	{
		return resultsDirectory;
	}


	/** Define a configuration for a dynamic provider. Dynamic providers support the dynamic
		creation of test client nodes. */
	public static DynamicProvider dynamicProvider(String name, Consumer<DynamicProvider> block)
	{
		DynamicProvider p = new DynamicProvider(name, block);
		providerConfigs.put(name, p);
		return p;
	}


	/** Define a configuration for a static provider. Static providers do NOT support the dynamic
		creation of test client nodes: the test client nodes must be manually
		provisioned before tests are conducted. */
	public static StaticProvider staticProvider(String name, Consumer<StaticProvider> block)
	{
		StaticProvider p = new StaticProvider(name, block);
		providerConfigs.put(name, p);
		return p;
	}


	/** Test results will be placed in here. A subdirectory will be created
		for each AbstractTestRun, and within that there will be an output file for
		each Node of the run. */
	public static void setResultsDirectory(String path)
	{
		String expPath = (new File(path)).getAbsolutePath();
		if (expPath.endsWith("/"))
			resultsDirectory = expPath.substring(0, expPath.length()-1);
		else
			resultsDirectory = expPath;
	}


	/** Define a performance run. A performance run executes a load profile. One can execute
		multiple test runs from a single cucumber controller script. */
	public static PerformanceRun performanceRun(String name, Consumer<PerformanceRun> block)
	{
		PerformanceRun p = new PerformanceRun(name, block);
		testRuns.put(name, p);
		return p;
	}


	/** Perform a test run in which each test (specifically, each requestType) is
		executed only once. This is a functional test run - not a performance run. */
	public static FunctionalRun functionalRun(String name, Consumer<FunctionalRun> block)
	{
		FunctionalRun f = new FunctionalRun(name, block);
		testRuns.put(name, f);
		return f;
	}


	/** Start each test run that has been defined. */
	public static void start()
	{
		for (String trName : testRuns.keySet())
		{
			AbstractTestRun tr = testRuns.get(trName);
			System.out.println("About to run " + trName + ":");
			tr.start();
		}
	}


	/** Return true if there is builtin support for the specified provider within
		this framework, and so there is a Vagrantfile and chef recipe template for
		the provider in the Templates directory of the gem. */
	public static boolean providerHasBuiltinSupport(String providerName)
	{
		return SupportedProviders.getSupportedProviders().contains(providerName);
	}


	/** Return the names of the providers that are supported. */
	public static Set<String> getProvidersWithBuiltinSupport()
	{
		return getBuiltinProviderUserIds().keySet();
	}


	// Meta data methods=====================================================


	/** Return the description of the feature that appears in the cucumber .feature
		file for the performance scenario that is being run. */
	//public static String getFeatureDesc()
	//{
	//	return Metadata.getFeatureDescription().replace('\n', ' ').replace("  ", " ");
	//}


	/** Return the description of the scenario that appears in the cucumber .feature
		file for the performance scenario that is being run. */
	//public static String getScenarioDesc()
	//{
	//	return Metadata.getScenarioDescription().replace('\n', ' ').replace("  ", " ");
	//}


	/** Return all of the tags of the feature of the performance scenario that is being run. */
	//public static String getScenarioTags()
	//{
	//	return Metadata.getScenarioTags();
	//}


	// Diagnostic methods====================================================


	/** If set, this program will not actually provision any nodes or run any tests. */
	public static void setTestMode()
	{
		testMode = true;
	}

	static boolean isTestMode() { return testMode; }


	// Implementation methods================================================


	static String loadgenJarURL() { return thisLoadgenJarURL; }

	static String jBehaveJarURL() { return thisJBehaveJarURL; }

	static String loadgenJarPathOnNode() { return TestRunnerConstants.NodeStandardRoot + "/loadgen.jar"; }

	static String jBehaveJarPathOnNode() { return TestRunnerConstants.NodeStandardRoot + "/jbehave.jar"; }

	static String[] getIps() { return ips; }

	static Map<String, RequestType> getRequestTypes() { return requestTypes; }

	static RequestType getRequestType(String name)
	{
		return requestTypes.get(name);
	}

	static Map<String, Distribution> getDistributions() { return distributions; }

	static Distribution getDistribution(String name) { return distributions.get(name); }

	static AbstractProfile getProfile(String name) { return profiles.get(name); }

	static FunctionalProfile getFunctionalProfile(String name)
	{
		AbstractProfile profile = getProfile(name);
		if (profile instanceof FunctionalProfile) return (FunctionalProfile)profile;
		return null;
	}

	static PerformanceProfile getPerformanceProfile(String name)
	{
		AbstractProfile profile = getProfile(name);
		if (profile instanceof PerformanceProfile) return (PerformanceProfile)profile;
		return null;
	}

	static AbstractProvider getProviderConfig(String name) { return providerConfigs.get(name); }


	/** Ensure that the specified string does not contain whitespace. */
	static void validateName(String str, String msg)
	{
		if (str.contains(" ")) throw new RuntimeException(msg + ": Cannot contain whitespace");
		if (str.contains("\t")) throw new RuntimeException(msg + ": Cannot contain whitespace");
	}


	static Map<String, String> getBuiltinProviderUserIds()
	{
		return SupportedProviders.getProviderUserIds();
	}


	/** Return the Vagrantfile template for the specified provider. */
	static String vagrantBuiltinTemplate(String providerName)
	{
		return getBuiltinTemplate(providerName, ".vagrantfile.template");
	}


	/** Return the chef recipe template for the specified provider. */
	static String chefBuiltinTemplate(String providerName)
	{
		return getBuiltinTemplate(providerName, ".recipe.template");
	}


	static String getBuiltinTemplate(String providerName, String suffix)
	{
		if (! providerHasBuiltinSupport(providerName)) throw new RuntimeException(
			"Unrecognized provider: " + providerName + ".");
		try
		{
			String entryName = "/loadgen/controller/templates/" + providerName + suffix;
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (! (cl instanceof URLClassLoader)) throw new RuntimeException(
				"Do not have a URL class loader to load template for " + providerName);
			URLClassLoader ucl = (URLClassLoader)cl;
			URL url = ucl.findResource(entryName);
			if (url == null) throw new RuntimeException("Could not find " + entryName);
			String entry;
			try {
				URLConnection urlConn = url.openConnection();
				urlConn.connect();
				InputStream is = urlConn.getInputStream();
				DataInputStream dis = new DataInputStream(is);
				entry = dis.readUTF();
			}
			catch (IOException ex) { throw new RuntimeException(ex); }
			//JarEntry entry = new JarFile(new File(getNodeLoadgenJarDir())).getJarEntry(entryName);
			if (entry == null) throw new RuntimeException("Jar file entry " + entryName + " not found");
			return entry;
		}
		catch (Exception ex) {
			System.err.println("Unable to obtain template for provider " + providerName);
			throw new RuntimeException(ex);
		}
	}


	private static String[] tabs = {"", "\t", "\t\t", "\t\t\t", "\t\t\t\t", "\t\t\t\t\t", "\t\t\t\t\t\t"};


	/** Return a string with the number of tabs specified by indentLevel. */
	static String getIndentStrForLevel(int indentLevel)
	{
		if (indentLevel < tabs.length) return tabs[indentLevel];
		String indstr = "";
		for (int i = 1; i <= indentLevel; i++)
		{
			indstr = indstr + "\t";
		}
		return indstr;
	}
}
