package loadgen.controller;


import loadgen.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


public class PerformanceProfile extends AbstractProfile
{
	private String thisDistributionName;
	
	
	PerformanceProfile(String name, Consumer<PerformanceProfile> block)
	{
		super(name);
		if (block != null) block.accept(this);
	}
	
	/** Specify the distribution that this profile will perform. See the distribution
		method in LoadGenerator. */
	public void setDistribution(String distName)
	{
		thisDistributionName = distName;
	}

	public String distribution()
	{
		return thisDistributionName;
	}

	
	String getProfileDefinition(final AbstractTestRun testRun)
	{
		PerformanceProfileWriter profileWriter = new PerformanceProfileWriter()
		{
			public String getTestRunType() { return testRun.getTestRunType(); }
			
			public String getRequestTypeName() { return requestType(); }
			
			public List<String> getTags() { return LoadGenerator.getRequestType(getRequestTypeName()).tags(); }
			
			public int getNoOfNodes()
			{
				AbstractProvider prov = testRun.getProvider();
				if (prov.isDynamic()) return testRun.noOfNodes();  // use value configured for the AbstractTestRun
				else return ((StaticProvider)prov).noOfNodes();  // use value configured for the Provider
			}

			public List<double[]> getLevels()
			{
				String distributionName = PerformanceProfile.this.distribution();
				Distribution distribution = LoadGenerator.getDistribution(distributionName);
				return distribution.levels();
			}
		};
	
		String profileDef = profileWriter.getProfileDefinition();

		for (double[] level : profileWriter.getLevels())
		{
			profileDef = profileDef + (level[0] / profileWriter.getNoOfNodes()) +
				", " + level[1] + "\n";
		}
		
		return profileDef;
	}
	

	void writeProfileAsJSON(int indentLevel, PrintWriter file)
	{
		String indstr = LoadGenerator.getIndentStrForLevel(indentLevel);
		
		file.println(indstr + "\"name\": \"" + name() + "\",");
		file.println(indstr + "\"hostname\": \"" + hostname() + "\",");
		file.println(indstr + "\"timestamp\": \"" + timestamp() + "\",");
		file.println(indstr + "\"requestType\": \"" + requestType() + "\",");
		file.println(indstr + "\"distribution\": \"" + distribution() + "\"");
	}
}

