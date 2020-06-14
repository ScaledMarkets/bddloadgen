package loadgen.controller.templates;


import loadgen.Version;
import loadgen.TestRunnerUtil;
import loadgen.EnvVars;
import loadgen.controller.templates.SupportedProviders;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


public class SupportedProviders
{
	private static List<String> supportedProviders = Arrays.asList(new String[]
	{
		"managed",
		//"vmware_fusion",
		"aws",  // ec2
		"virtualbox",

		"docker",
		"aws_ecs"
	});

	private static Map<String, String> providerUserIds = new HashMap<String, String>();

	{
		//providerUserIds.put("vmware_fusion" => ????);
		providerUserIds.put("aws", "ec2-user");
		providerUserIds.put("virtualbox", "vagrant");
	}

    public static List<String> getSupportedProviders()
    {
    	return supportedProviders;
    }

    public static Map<String, String> getProviderUserIds()
    {
    	return providerUserIds;
    }
}
