package loadgen;


import loadgen.TestRunnerConstants;
import loadgen.EnvVars;
import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


/** Represents a provisioned node (virtual machine or container) for testing.
	Nodes are created automatically - the methods below are not called by user
	written code. */
class Node
{
	private LoadGenerator lg;
	private String thisName;
	private AbstractTestRun thisTestRun;
	private String thisSshConfigFilename;
	private AbstractProvider thisProvider;
	private String thisProjectGemServerURL;
	private String thisProjectRepoName;
	private String thisProjectRepoURL;
	private String thisProjectGitConfigString;
	private String thisProjectRepoFQDN;
	private String thisGitUserid;
	private String thisGitPassword;
	private String thisCookbookName;
	private String thisNodeStandardDir;
	private String thisNodeProjectDir;
	private String thisNodeLoadgenLibDir;
	private String thisNodeResultsRoot;
	private String thisScriptDir;
	private String thisCookbookDir;
	private String thisNodeFeaturesDir;
	private String thisNodeIpAddress;
	private long thisRandomSeed;


	Node(LoadGenerator lg, String name, AbstractTestRun testRun)
	{
		this.lg = lg;
		thisName = name;
		thisTestRun = testRun;
	}

	String name()
	{
		return thisName;
	}

	AbstractTestRun testRun()
	{
		return thisTestRun;
	}

	String sshConfigFilename()
	{
		return thisSshConfigFilename;
	}

	AbstractProvider getProvider()
	{
		return thisTestRun.getProvider();
	}

	boolean providerIsDynamic()
	{
		return getProvider().isDynamic();
	}

	void performShellCommandOnNode(String command)
	{
		if (lg.isTestMode()) return;

		String userid = getProvider().getVagrantUserid();

		String machine;
		if (providerIsDynamic()) machine = "default";
		else machine = name();

		String script = "cd " + thisScriptDir + "; ssh -F " + sshConfigFilename() +
			" " + userid + "@" + machine + " " + command;

		System.out.println(">>>Using script to perform command on node " + name() + ":");
		System.out.println("\t" + script);

		boolean wasSuccessful = false;
		try {
			wasSuccessful = (Runtime.getRuntime().exec(script).waitFor() == 0);
			System.out.println("...success: " + Boolean.toString(wasSuccessful));
		}
		catch (Exception ex) { throw new RuntimeException(ex); }
		if (! wasSuccessful) throw new RuntimeException("Terminating");
	}


	/** Write a Vagrantfile and a chef file, and run vagrant against the Vagrantfile,
		to create and provision the required set of test nodes (VMs).
		This method will create (in the results directory) a directory with a name
		that is derived from the name of this test run, and will create subdirectories 'vagrant'
		and 'chef' within that new directory. A Vagrantfile and chef cookbook will be
		written in those subdirectories. If these artifacts with those names already exist,
		they will be over-written. The IP address may be null. */
	void provision(long randomSeed, String ipAddress) throws IOException
	{
		System.out.println("Provisioning node " + name());

		thisCookbookName = name();

		thisNodeStandardDir = TestRunnerConstants.NodeStandardRoot;

		thisNodeProjectDir = TestRunnerConstants.NodeProjectRoot;

		thisNodeResultsRoot = "/var/results";

		thisSshConfigFilename = "sshconfig_" + name();

		// Create a scratch directory for writing the vagrant and chef files.
		thisScriptDir = thisTestRun.resultsDirectory() + "/scripts/" + thisTestRun.name() + "/" + thisName;
		File file = new File(thisScriptDir);
		if (! file.exists()) file.mkdirs();

		// Define where vagrant will look for the cookbook that we generate.
		// This will be in the
		thisCookbookDir = thisScriptDir + "/cookbooks";
		file = new File(thisCookbookDir);
		if (! file.exists()) file.mkdirs();

		// Directory cucumber "features" directory on each node where the test
		// case feature files can be found.
		thisNodeFeaturesDir = thisTestRun.nodeFeaturesDirectory();

		// Define the IP address to use for this Node in the virtual network.
		thisNodeIpAddress = ipAddress;

		thisRandomSeed = randomSeed;

		// Create/provision the node (VM), as appropriate.
		writeChefRecipe();
		String script;
		if (thisTestRun.getProvider().isDynamic())
		{
			VagrantProvider dynProvider = (VagrantProvider)(thisTestRun.getProvider());

			// Destroy existing nodes if necessary.
			if ((! thisTestRun.willReuseNodes()) && (! lg.isTestMode()))
			{
				script = "cd " + thisScriptDir + "; /usr/bin/vagrant destroy -f";
				System.out.println(">>>Using script to destroy nodes:");
				System.out.println("\t" + script);

				try {
					int status = Runtime.getRuntime().exec(script).waitFor();
					System.out.println("\tReturn status: " + status);
				}
				catch (Exception ex) { throw new RuntimeException(ex); }
			}

			System.out.println("Using provider " + thisTestRun.providerName());
			System.out.println("Using provider box " + dynProvider.providerBoxName());
			writeVagrantfile();
			script = "cd " + thisScriptDir + "; /usr/bin/vagrant up --provision --provider " +
				thisTestRun.providerName();
		}
		else
		{
			String vagrantfileDir = ((StaticProvider)(thisTestRun.getProvider())).getVagrantfileDir();
			script = "cd " + vagrantfileDir + "; export " + EnvVars.Suppl_cookbook_dir + "=" + thisCookbookDir + "; " +
				"/usr/bin/vagrant up --provider managed " + name() + "; /usr/bin/vagrant provision " + name();
		}

		// Run the script, to create the nodes if necessary, start them, and transfer the
		// tests to each node.

		if (lg.isTestMode())
		{
			System.out.println("Test mode: skipping vagrant up");
			return;
		}

		System.out.println(">>>Running vagrant in " + thisScriptDir + " for node " + name() + " with this command:");
		System.out.println("\t" + script);
		boolean wasSuccessful;
		try {
			wasSuccessful = (Runtime.getRuntime().exec(script).waitFor() == 0);
			System.out.println("\tsuccess: " + wasSuccessful);
			if (! wasSuccessful) throw new RuntimeException("Terminating");
		} catch (InterruptedException ex) { throw new RuntimeException(ex); }

		// Grab the SSH config info. This will be needed later to open an SSH to the node.
		if (thisTestRun.getProvider().isDynamic())
			script = "cd " + thisScriptDir + "; /usr/bin/vagrant ssh-config > " + thisSshConfigFilename;
		else
		{
			String vagrantfileDir = ((StaticProvider)(thisTestRun.getProvider())).getVagrantfileDir();
			script = "cd " + vagrantfileDir + "; /usr/bin/vagrant ssh-config " +
				name() + " > " + thisScriptDir + "/" + thisSshConfigFilename;
		}
		System.out.println(">>>Obtaining ssh config with this command:");
		System.out.println("\t" + script);
		try {
			wasSuccessful = (Runtime.getRuntime().exec(script).waitFor() == 0);
			System.out.println("Obtained SSH config: success: " + wasSuccessful);
			if (! wasSuccessful) throw new RuntimeException("Terminating");
		} catch (InterruptedException ex) { throw new RuntimeException(ex); }

		// Workaround for the problem that a FOG warning gets written to
		// the sshconfig file, corrupting it. Simply remove the warning line.
		// Also working around the fact that VagrantfileUtil contains a puts
		// statement that causes vagrant ssh-config to output the setting for
		// ProjectPaths.BERKSHELF_PATCH.
		String contents = "";
		int nlines = 0;
		String filepath = thisScriptDir + "/" + thisSshConfigFilename;
		BufferedReader br;
		try { br = new BufferedReader(new FileReader(filepath)); }
		catch (FileNotFoundException ex) { throw new RuntimeException(ex); }
		for (String line = br.readLine(); line != null; line = br.readLine())
		{
			if (line.contains("[fog][WARNING]")) continue;  // workaround.
			if (line.contains("ProjectPaths.BERKSHELF_PATCH")) continue;  // workaround.
			if (line.contains("INFO:")) continue;  // workaround put statements in ManagedServers.rb.
			nlines = nlines + 1;
			contents = contents + line;
		}
		br.close();
		System.out.println("sshconfig (" + nlines + " lines, " + contents.length() + " chars):");
		System.out.println(contents);
		System.out.println("---end of sshconfig---");
		FileWriter f = new FileWriter(filepath);
		f.write(contents);
		f.close();
	}

	/** Open an ssh to the node and invoke the tests. */
	void start(String reqType)
	{
		System.out.println("Starting test run on Node " + name());
		String command = thisNodeStandardDir + "/testrunner.sh";
		if (reqType != null) command = command + " " + reqType;
		performShellCommandOnNode(command);
	}

	void start()
	{
		start(null);
	}

	String resultsRoot()
	{
		return thisNodeResultsRoot;
	}

	/** Destroy the VM or whatever resource this Node represents. */
	void destroy()
	{
		if (! providerIsDynamic())
		{
			System.out.println("Attempt to destroy a node for a non-dynamic provider");
			return;
		}

		String script = "cd " + thisScriptDir + "; /usr/bin/vagrant destroy -f";
		System.out.println(">>>Using this script to destroy node:");
		System.out.println("\t" + script);
		try {
			if (Runtime.getRuntime().exec(script).waitFor() == 0)
				System.out.println("Node " + name() + " (for test run " + thisTestRun.name() + ") removed.");
			else
				System.out.println("Unable to remove Node " + name() + " (for test run " + thisTestRun.name());
		}
		catch (InterruptedException ex) { throw new RuntimeException(ex); }
		catch (IOException ex2) { throw new RuntimeException(ex2); }
	}

	// Retrieve this Node's time log and store it in the specified file.
	void fetchTimeLogInto(String targetLocalFilePath)
	{
		String command = "cat " + resultsRoot() + "/timelog.csv > " + targetLocalFilePath;
		performShellCommandOnNode(command);
	}

	void fetchDetailTimeLogInto(String targetLocalFilePath)
	{
		String command = "cat " + resultsRoot() + "/detailtimelog.csv > " + targetLocalFilePath;
		performShellCommandOnNode(command);
	}

	// Retrieve this Node's stdout log and store it in the specified file.
	void fetchStdoutInto(String localnodestdoutpath)
	{
		String command = "cat " + resultsRoot() + "/" + thisTestRun.name() + " > " + localnodestdoutpath;
		performShellCommandOnNode(command);
	}


	/* ----------------------------------------------------------------------
	Implementation methods.

	Notes:
	The directory "controller/templates" contains a vagrantfile template and a chef recipe
	template for each supported provider. These templates embed replaceable
	variables (always in all-caps) that are substituted for actual values
	by the methods below. In addition, the templates directory contains a
	"SupportedProviders" class, which must be updated if additional built-in
	providers are added.
	*/


	void writeVagrantfile() throws IOException
	{
		System.out.println("Writing Vagrantfile for node " + name());
		String providerName = lg.getProviderConfigs().get(
			thisTestRun.providerConfig()).providerName();
		String content = getProvider().getVagrantfileTemplate();
		content = content.replace("GEN_COOKBOOKDIR", thisCookbookDir);
		if (thisTestRun.getProvider().isDynamic())
		{
			content = content.replace("PROVIDER_BOX_NAME",
				((VagrantProvider)(thisTestRun.getProvider())).providerBoxName());
			content = content.replace("PROVIDER_BOX_URL",
				((VagrantProvider)(thisTestRun.getProvider())).providerBoxURL());
		}
		if (thisNodeIpAddress != null)
			content = content.replace("GEN_HOSTONLY_ADDRESS", thisNodeIpAddress);
		content = content.replace("GEN_HOST_RESULTS_ROOT", thisTestRun.resultsDirectory());
		content = content.replace("GEN_FEATURES_DIR", thisTestRun.featuresDirectory());
		content = content.replace("GEN_NODE_RESOURCES_DIR", thisNodeStandardDir);
		content = content.replace("COOKBOOK_NAME", thisCookbookName);

		String headedconfig = "";
		if (thisTestRun.areNodesHeaded())
			headedconfig =
				"config.vm.provider \"" + thisTestRun.getProvider() + "\" do |v|\n" +
				"    v.gui = true\n" +
				"  end\n";

		content = content.replace("GUICONFIG", headedconfig);

		String vagrantfilePath = thisScriptDir + "/Vagrantfile";
		(new FileWriter(vagrantfilePath)).write(content);
		System.out.println("wrote Vagrantfile to " + vagrantfilePath);
		String berksPath = thisScriptDir + "/Berksfile";
		(new FileWriter(berksPath)).write("");
		System.out.println("write Berksfile to " + berksPath);
	}

	void writeChefRecipe() throws IOException
	{
		System.out.println("Writing chef recipe for test run " + name());
		String controlScriptContent = getShellScript();
		controlScriptContent = " <<-SCRIPT\n" + controlScriptContent + "\nSCRIPT\n";
		String profilesChefCode = "";
		List<String> profiles = thisTestRun.testRunProfiles();
		for (String profileName : profiles)
		{
			AbstractProfile profile = lg.getProfile(profileName);
			String profileDef = profile.getProfileDefinition(thisTestRun);
			profileDef = profileDef.replace("/", "\"");
			System.out.println("AbstractProfile " + profileName + ":");
			System.out.println(profileDef);
			System.out.println();
			String filepath = thisNodeProjectDir + "/" + profileName;
			profilesChefCode = profilesChefCode +
				"file \"" + filepath + "\" do\n" +
				"    owner \"root\"\n" +
				"    group \"root\"\n" +
				"    mode \"0755\"\n" +
				"    action :create\n" +
				"    content \"" + profileDef + "\"\n" +
				"end\n\n";
		}

		String recipeDir = thisCookbookDir + "/" + thisCookbookName + "/recipes";
		File recDirFile = new File(recipeDir);
		if (! recDirFile.exists()) recDirFile.mkdirs();
		String providerName = lg.getProviderConfigs().get(thisTestRun.providerConfig()).providerName();
		String content = getProvider().getChefRecipeTemplate();
		if (thisProjectRepoName != null) content = content.replace("PROJECT_REPO_NAME", thisProjectRepoName);
		if (thisProjectRepoURL != null) content = content.replace("PROJECT_REPO_URL", thisProjectRepoURL);
		if (thisProjectGitConfigString != null) content = content.replace("GIT_CONFIG",
			"git config --local " + thisProjectGitConfigString + "; ");
		else
			content = content.replace("GIT_CONFIG", "");
		if (thisProjectRepoFQDN != null) content = content.replace("PROJECT_REPO_FQND", thisProjectRepoFQDN);
		if (thisGitUserid != null) content = content.replace("GIT_USERID", thisGitUserid);
		if (thisGitPassword != null) content = content.replace("GIT_PASSWORD", thisGitPassword);

		content = content.replace("PROJECT_GEM_SERVER_URL", thisProjectGemServerURL);
		content = content.replace("HASHSIGN", "#");
		content = content.replace("BACKSLASH", "\\");
		content = content.replace("GEN_NODE_RESULTS_DIR", thisNodeResultsRoot);
		content = content.replace("PROFILES", profilesChefCode);
		content = content.replace("CONTROL_SCRIPT_CONTENT", controlScriptContent);
		content = content.replace("GEN_NODE_RESOURCES_DIR", thisNodeStandardDir);
		content = content.replace("STEPS_JAR_URL", thisTestRun.stepsJarURL());
		content = content.replace("LOADGEN_JAR_URL", lg.loadgenJarURL());

		String binrsrcs = "";
		for (String binrsrc : thisTestRun.binaryResources())
		{
			binrsrcs = binrsrcs +
				"yum_package \"" + binrsrc + "\" do\n" +
				"    action :install\n" +
				"end\n";
		}
		content = content.replace("BIN_RESOURCES", binrsrcs);

		String bundles = "";
		for (String[] entry : thisTestRun.requireRubyDirs())
		{
			bundles = bundles +
				"execute \"get_gems\" do\n" +
				"    command \"cd /var/Tests" + "/" + thisProjectRepoName +
				"/" + entry[1] + "; /opt/chef/embedded/bin/bundle install\"\n" +
				"    action :run\n" +
				"end\n";
		}
		content = content.replace("ADDL_BUNDLES", bundles);

		String recipePath = recipeDir + "/default.rb";
		(new FileWriter(recipePath)).write(content);
		System.out.println("wrote recipe to " + recipePath);
	}

	String getShellScript()
	{
		String content = testrunnerTemplate();

		String exports = "";
		for (String[] entry : thisTestRun.requireRubyDirs())
			exports = exports + "export " + entry[0] + "=/var/Tests" +
				"/" + thisProjectRepoName + "/" + entry[1] + "\n";
		for (String varname : thisTestRun.envVars().keySet())
			exports = exports + "export " + varname + "=" +
				thisTestRun.envVars().get(varname) + "\n";
		content = content.replace("ADDL_EXPORTS", exports);

		/* Assemble a feature parameter for JBehave. It is passed to TestRunner
		as a single parameter, consisting of a sequence of features separated
		by a semicolon. The feature directory is prepended to each feature. If the
		user only specified a feature directory, then only that is passed. */
		String featureSpecs = "";
		List<String> features = thisTestRun.features();
		if ((features == null) || (features.size() == 0))
			featureSpecs = thisNodeFeaturesDir;
		else
		{
			boolean firstTime = true;
			for (String featureSpec : features)
			{
				if (firstTime)
					firstTime = false;
				else
					featureSpecs = featureSpecs + ";";  // append a separator
				// Append another feature spec.
				featureSpecs = featureSpecs + thisNodeFeaturesDir + "/" + featureSpec;
			}
		}

		String stepClassnames = "";
		boolean first = true;
		for (String stepClassname : testRun().stepClassNames())
		{
			if (first) first = false;
			else stepClassnames += ",";
			stepClassnames += stepClassname;
		}

		String nodeStepsJarPath = testRun().nodeStepsJarPath();

		content = content.replace("GEN_NODE_FEATURES_SPEC", featureSpecs);
		content = content.replace("GEN_NODE_STEP_CLASSES", stepClassnames);
		content = content.replace("GEN_NODE_STEP_JARPATH", nodeStepsJarPath);
		content = content.replace("GEN_TEST_OUTPUT_FILE_NAME", thisNodeResultsRoot + "/" + thisTestRun.name());
		content = content.replace("GEN_NODE_RESULTS_DIR", thisNodeResultsRoot);
		if (thisTestRun.isPerformance())
			content = content.replace("ABORT_IF_TESTS_CANNOT_ACHIEVE_PROFILE_VALUE",
				Boolean.toString(((PerformanceRun)thisTestRun).willAbortIfTestsCannotAchieveProfile())
				);
		content = content.replace("RANDOM_SEED_VALUE", Long.toString(thisRandomSeed));

		String profileListString = "";
		List<String> profiles = thisTestRun.testRunProfiles();
		for (String profileName : profiles)
		{
			AbstractProfile profile = lg.getProfile(profileName);
			profileListString = profileListString + thisNodeProjectDir + "/" + profileName + " ";
		}

		content = content.replace("GEN_PROFILE_FILE", profileListString);
		return content;
	}


	/** Template for a script that is deployed to each VM. The test controller then
		ssh-es into each VM and runs this script, which starts the tests. */
	static String testrunnerTemplate()
	{
		return
		"#!/bin/bash\n" +
		"export PATH=$PATH:/opt/chef/embedded/bin:/opt/chef/embedded/lib/ruby/gems/1.9.1/gems\n" +
		"export " + EnvVars.JBehaveJarPathOnNode + "=" + lg.jBehaveJarPathOnNode() + "\n" +
		"export " + EnvVars.LoadgenJarPathOnNode + "=" + lg.loadgenJarPathOnNode() + "\n" +
		"export RESULTS_DIR=GEN_NODE_RESULTS_DIR\n" +
		"export ABORT_IF_TESTS_CANNOT_ACHIEVE_PROFILE=ABORT_IF_TESTS_CANNOT_ACHIEVE_PROFILE_VALUE\n" +
		"export RANDOM_SEED=RANDOM_SEED_VALUE\n" +
		"ADDL_EXPORTS\n" +
		"java -cp \"" +
			lg.loadgenJarPathOnNode() + ":" + lg.jBehaveJarPathOnNode() +
			"\" testrunner.TestRunner\n" +
			"\"GEN_NODE_FEATURES_SPEC\" " +
			"\"GEN_NODE_STEP_CLASSES\" " + // (comma-separated list)
			"\"GEN_NODE_STEP_JARPATH\" " +
			"\"GEN_NODE_RESULTS_DIR\" \"GEN_PROFILE_FILE\" " +
			"&> \"GEN_TEST_OUTPUT_FILE_NAME\"\n" +
		"SHELLSCRIPTTEMPLATE\n";
	}
}
