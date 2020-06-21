package loadgen.demo.controller;


import loadgen.controller.*;
import loadgen.providers.DynamicVagrantProvider;


/** Demonstration JBehave script for performing a set of performance tests using
	the performance testing framework. This demo runs locally using VirtualBox. */
public class Demo
{
	private static final String ProviderBoxURL = "http://sourceforge.net/projects/virtualboximage/files/CentOS/6.0/CentOS-6-i386.7z/download";
		// See http://virtualboxes.org/images/centos/
	private static final FeatureDir = "/Users/cliffordberg/Documents/loadgen-java/demo/tests/features";
	private static final String LoadgenJarURL = "http://localhost:8080/repository/snapshots/loadgen/loadgen-java/1.0-SNAPSHOT/loadgen-java-1.0-SNAPSHOT.jar";
		// Set up a maven repository manager.
		// See https://maven.apache.org/repository-management.html
		// See https://archiva.apache.org/docs/2.1.1/quick-start.html
		// Archiva admin user: admin/admin1
		// Archive deployment user: Cliff/cliff1
		// To start archiva: ~/Library/apache-archiva-2.1.1/bin/archiva console
	private static final StepsJarURL = LoadgenJarURL; // for tests
	private static final String JBehaveJarURL = "http://central.maven.org/maven2/org/jbehave/jbehave-core/3.9.5/jbehave-core-3.9.5.jar";

	private String thisScenarioName;
	private String thisProjectGemServerURL;
	private String thisTargetIpAddr;
	private double thisTypDuration;
	private double thisSpikeDuration;
	private TestRun thisTestRun;

	private LoadGenerator loadGenerator = new LoadGenerator(....);


	@Given("^for scenario \"([^\"]*)\" that the system has ramped up to a load of \"([^\"]*)\" requests per second after \"([^\"]*)\" minutes and continues for \"([^\"]*)\" minutes$")
	public void do_given(String scenarioName, double typReqRate, double typRampTime, double typDuration)
	{
		loadGenerator.setTestMode();

		thisScenarioName = scenarioName;
		thisTypDuration = typDuration;

		// The server where agilex-loadgen and Jbehave are hosted.
		this.setJBehaveJarURL(JBehaveJarURL);
		this.setLoadgenJarURL(LoadgenJarURL);

		// The URL that the tests should access. This is passed to the tests by
		// storing it in the profile files.
		thisTargetIpAddr = "http://cliffberg.com";

		// Define parameters for ElasticSearch.
		//LoadGenerator.resultsDatabaseConfig.setUrl "http://localhost:9200"
		//LoadGenerator.resultsDatabaseConfig.setUserid ""
		//LoadGenerator.resultsDatabaseConfig.setPassword ""
		//LoadGenerator.resultsDatabaseConfig.setIndexName "temp1";
		//LoadGenerator.resultsDatabaseConfig.setJsonDataType "temp1";

		loadGenerator.requestType("TypicalRequest", (rt) -> {
			rt.tag("+Performance");
			rt.tag("+Tag2");
			rt.setColor("darkblue");  // See http://www.december.com/html/spec/colorsvg.html
		});

		loadGenerator.distribution("TypicalDistribution", (d) -> {
			d.level(typReqRate, typRampTime);  // ramp up to the request rate.
			d.level(typReqRate, thisTypDuration);  // keep the base load going until the end of the test.
		});

		loadGenerator.profile("TypicalProfile", (p) -> {
			p.setRequestType("TypicalRequest");
			p.setDistribution("TypicalDistribution");
		});
	}


	@When("^the load is increased by \"([^\"]*)\" requests per second after \"([^\"]*)\" minutes for \"([^\"]*)\" minutes$")
	public void do_when(double spikeReqRate, double spikeDelay, double spikeDuration)
	{
		thisSpikeDuration = spikeDuration;

		loadGenerator.requestType("SpikeRequest", (rt) -> {
			rt.tag("@Performance");
			rt.setColo("goldenrod");
		});

		loadGenerator.distribution("SpikeDistribution", (d) -> {
			d.level(0.0, spikeDelay);
			d.level(spikeReqRate, 0.01);
			d.level(spikeReqRate, thisSpikeDuration);
			d.level(0.0, 0.01);
		});

		loadGenerator.profile("SpikeProfile", (p) -> {
			p.setRequestType("SpikeRequest");
			p.setDistribution("SpikeDistribution");
		}

		loadGenerator.ipPool("10.3.3.30", "10.3.3.31");

		loadGenerator.setResultsDirectory("~/temp/results");

		loadGenerator.dynamicVagrantProvider("vbconf", (p) -> {
			p.setProviderName("virtualbox");
			p.setProviderBoxName("opscode-centos-6.3");
			p.setProviderBoxURL(ProviderBoxURL);
			//setVagrantfileTemplatePath ...
			//setChefRecipeTemplatePath ...
		}

		thisTestRun = loadGenerator.performanceRun(thisScenarioName, (pr) -> {

			pr.defineEnvVariable("TARGET_URL", thisTargetIpAddr);

			pr.setProviderConfig("vbconf");
			pr.setNoOfNodes(1);

			// For this demo, copy the files in demo/tests/features to your project,
			// and point this parameter at the features directory.
			pr.setFeaturesDirectory(FeatureDir);
			pr.setStepsJarURL(StepsJarURL);
			pr.feature("Experiment.feature");
			pr.stepClass("steps.Experiment");

			pr.graphAllEvents();

			pr.useProfile("TypicalProfile");
			pr.useProfile("SpikeProfile");
			pr.keepNodes();
			pr.setRandomSeed(0.1);
		}

		System.err.printnln("Starting");
		thisTestRun.start();
	}


	@Then("^at \"([^\"]*)\" minutes after the spike ends, the system response time still averages less than \"([^\"]*)\" seconds$")
	public void do_then(double delay, double avgRespTime)
	{
		double afterTime = thisTypDuration + thisSpikeDuration + delay;
		System.out.println("Expected that average after " + afterTime +
			" seconds is less than " + avgRespTime + ".");

		double actualAvg = thisTestRun.avgResponseTimeAfter(afterTime, TestRunnerUtil::EndToEnd, "TypicalRequest");
		if (actualAvg == null) throw new RuntimeException("No tests passed after " + afterTime + " seconds.");
		System.out.println("Average response time after " + afterTime + " is " + actualAvg + ".");

		if (actualAvg > avgRespTime) throw new RuntimeException(
			"Failed: actual average (" + actualAvg + ") > SLA (" + avgRespTime + ")");
	}
}
