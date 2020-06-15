package loadgen.providers;



import loadgen.controller.templates.SupportedProviders;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


/** For providers that can perform vagrant up. */
public class VagrantProvider extends AbstractProvider
{
	private String thisProviderBoxName;
	private String thisProviderBoxURL;
	private String thisVagrantfileTemplatePath;
	private String thisVagrantUserid;

	VagrantProvider(LoadGenerator lg, String confname, Consumer<VagrantProvider> block)
	{
		super(lg, confname);

		if (block != null) block.accept(this);
	}

	boolean isDynamic()
	{
		return true;
	}

	Map<String, String> nodeIps() { throw new RuntimeException("Should not be called"); }

	/** The name of the provider box definition. This is passed to Vagrant
		as the "config.vm.box" value. Do not specify this for the aws provider. */
	public void setProviderBoxName(String name)
	{
		thisProviderBoxName = name;
	}

	String providerBoxName()
	{
		return thisProviderBoxName;
	}

	/** The URL where the vagrant box definition can be found, for the desired provider.
		Do not specify this for the aws provider. */
	public void setProviderBoxURL(String url)
	{
		thisProviderBoxURL = url;
	}

	String providerBoxURL()
	{
		return thisProviderBoxURL;
	}

	/** Optional. Only use if the provider is not builtin. The path is absolute,
		on the test controller's file system. */
	public void setVagrantfileTemplatePath(String path)
	{
		thisVagrantfileTemplatePath = (new File(path)).getAbsolutePath();
	}

	String getVagrantfileTemplate()
	{
		if (thisVagrantfileTemplatePath == null)  // fetch the builtin one
			return lg.vagrantBuiltinTemplate(providerName());
		else try
		{
			System.out.println("Using user supplied Vagrantfile template: " + thisVagrantfileTemplatePath);
			if (! (new File(thisVagrantfileTemplatePath)).canRead()) throw new RuntimeException(
				"Cannot find vagrantfile template " + thisVagrantfileTemplatePath);
			return new String(Files.readAllBytes((new File(thisVagrantfileTemplatePath)).toPath()));
		}
		catch (IOException ex) { throw new RuntimeException(ex); }
	}

	/** Optional. Only use if the provider is not builtin. */
	public void setVagrantUserid(String id)
	{
		thisVagrantUserid = id;
	}

	String getVagrantUserid()
	{
		if (thisVagrantUserid == null)
			return lg.getBuiltinProviderUserIds().get(providerName());
		else
			return thisVagrantUserid;
	}

	void writeProviderAsJSON(int indentLevel, PrintWriter file)
	{
		String indstr = lg.getIndentStrForLevel(indentLevel);

		super.writeProviderAsJSON(indentLevel, file);
		file.println(indstr + "\"isDynamic\": " + isDynamic());
		file.println(indstr + "\"providerBoxName\": \"" + providerBoxName() + "\",");
		file.println(indstr + "\"providerBoxURL\": \"" + providerBoxURL() + "\",");
	}
}
