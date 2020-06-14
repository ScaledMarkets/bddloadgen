package loadgen.controller;


import loadgen.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


/** Describe an infrastructure provider (e.g., aws, virtualbox, etc.). */
abstract class AbstractProvider
{
	private String thisname;
	private String thisProviderName;
	private String thisRecipeTemplatePath;
	
	
	AbstractProvider(String confname)
	{
		LoadGenerator.validateName(confname, "Provider configuration");
		thisname = confname;  // the name to use when referencing this configuration.
	}
	
	abstract boolean isDynamic();

	abstract Map<String, String> nodeIps();
	
	
	String getConfigurationName()
	{
		return thisname;
	}
	
	public void setProviderName(String name)
	{
		thisProviderName = name;
	}

	String providerName()
	{
		return thisProviderName;
	}
	
	String getVagrantfileTemplate()
	{
		return LoadGenerator.vagrantBuiltinTemplate(providerName());  // Fetch the builtin one
	}

	String getVagrantUserid()
	{
		return LoadGenerator.getBuiltinProviderUserIds().get(providerName());  // Fetch the builtin one
	}
	
	/** Optional. Only use if the provider is not builtin.
		The path should be absolute, on the test controller's file system. */
	public void setChefRecipeTemplatePath(String path)
	{
		if (providerName() == null) throw new RuntimeException(
			"Provider name has not been specified");
		thisRecipeTemplatePath = (new File(path)).getAbsolutePath();
	}

	String getChefRecipeTemplate()
	{
		if (thisRecipeTemplatePath == null)
			return LoadGenerator.chefBuiltinTemplate(providerName());
		else try
		{
			System.out.println("Using user supplied recipe template: " + thisRecipeTemplatePath);
			if (! (new File(thisRecipeTemplatePath)).canRead()) throw new RuntimeException(
				"Cannot find chef recipe template " + thisRecipeTemplatePath);
			return new String(Files.readAllBytes((new File(thisRecipeTemplatePath)).toPath()));
		}
		catch (Exception ex) { throw new RuntimeException(ex); }
	}
	
	void writeProviderAsJSON(int indentLevel, PrintWriter file)
	{
		String indstr = LoadGenerator.getIndentStrForLevel(indentLevel);

		file.println(indstr + "\"name\": \"" + thisname + "\",");
		file.println(indstr + "\"providerName\": \"" + providerName() + "\",");
	}
}

