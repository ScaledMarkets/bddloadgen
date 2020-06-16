package loadgen.providers.templates;


import java.io.File;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;


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
