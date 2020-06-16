package loadgen;

import loadgen.profile.PerformanceProfile;
import loadgen.profile.FunctionalProfile;
import loadgen.profile.RequestType;

import java.util.function.Consumer;


/** Main entry point for the test controller of the LoadGeneration framework.
	User-written performance test scenarios should make calls to the methods of
	this class. */
public interface AbstractLoadGenerator
{
	/** Define a new request type. A request type is a type of test or test suite.
		When a load profile is defined, it is associated with a request type. */
	public RequestType requestType(String name, Consumer<RequestType> block);


	/** Define a load distribution. Once defined, one can add "levels" to the
		distribution. Thus, a load distribution is a sequence of levels, where
		each level is a (rate, after_time) value pair. */
	public Distribution distribution(String name, Consumer<Distribution> block);


	/** Define a profile. A profile is simply a combination of a request type
		and a load distribution. */
	public PerformanceProfile performanceProfile(String name, Consumer<PerformanceProfile> block);


	/** Define a profile. A profile is simply a combination of a request type
		and a load distribution. */
	public FunctionalProfile functionalProfile(String name, Consumer<FunctionalProfile> block);


	/** Define the available IPs to use for the test client (Node) VMs. */
	public void ipPool(String... ipAddresses);


	Map<String, AbstractProvider> getProviderConfigs();


	ResultsJSONDatabaseConfig getResultsDatabaseConfig();


	String getResultsDirectory();


	/** Define a configuration for a specified provider. */
	public AbstractProvider provider(String name, String providerClassName,
		Consumer<AbstractProvider> block);

	/** Define a configuration for a dynamic provider. Dynamic providers support the dynamic
		creation of test client nodes. */
	//public VagrantProvider vagrantProvider(String name, Consumer<VagrantProvider> block);


	/** Define a configuration for a provider. Static providers do NOT support the dynamic
		creation of test client nodes: the test client nodes must be manually
		provisioned before tests are conducted. */
	//public StaticProvider staticProvider(String name, Consumer<StaticProvider> block);


	/** Test results will be placed in here. A subdirectory will be created
		for each AbstractTestRun, and within that there will be an output file for
		each Node of the run. */
	public void setResultsDirectory(String path);


	/** Define a performance run. A performance run executes a load profile. One can execute
		multiple test runs from a single cucumber controller script. */
	public PerformanceRun performanceRun(String name, Consumer<PerformanceRun> block);


	/** Perform a test run in which each test (specifically, each requestType) is
		executed only once. This is a functional test run - not a performance run. */
	public FunctionalRun functionalRun(String name, Consumer<FunctionalRun> block);


	/** Start each test run that has been defined. */
	public void start();


	/** Return true if there is builtin support for the specified provider within
		this framework, and so there is a Vagrantfile and chef recipe template for
		the provider in the Templates directory of the gem. */
	public boolean providerHasBuiltinSupport(String providerName);


	/** Return the names of the providers that are supported. */
	public Set<String> getProvidersWithBuiltinSupport();


	// Meta data methods=====================================================


	/** Return the description of the feature that appears in the cucumber .feature
		file for the performance scenario that is being run. */
	//public String getFeatureDesc();


	/** Return the description of the scenario that appears in the cucumber .feature
		file for the performance scenario that is being run. */
	//public String getScenarioDesc();


	/** Return all of the tags of the feature of the performance scenario that is being run. */
	//public String getScenarioTags();


	// Diagnostic methods====================================================


	/** If set, this program will not actually provision any nodes or run any tests. */
	public void setTestMode();

	boolean isTestMode();
}
