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


/** For a provider that provisions boxes ahead of time. */
public class StaticProvider extends AbstractProvider
{
	private Map<String, String> thisNodeIps = new HashMap<String, String>();
	private String thisUserid;
	private String thisVagrantfileDir;
	
	StaticProvider(String confname, Consumer<StaticProvider> block)
	{
		super(confname);
		if (block != null) block.accept(this);
	}
	
	
	boolean isDynamic()
	{
		return false;
	}
	
	public void node(String name, String ip)
	{
		thisNodeIps.put(name, ip);
	}

	Map<String, String> nodeIps()
	{
		return thisNodeIps;
	}
	
	public void setUserid(String id)
	{
		thisUserid = id;
	}
	
	String getVagrantUserid()
	{
		return thisUserid;
	}
	
	String userid()
	{
		return getVagrantUserid();
	}
	
	int noOfNodes()
	{
		return thisNodeIps.size();
	}
	
	// Specify the path of the directory containing the Vagrantfile that can be
	// used to connect to the managed nodes.
	public void setVagrantfileDir(String path)
	{
		String expPath = (new File(path)).getAbsolutePath();
		if (expPath.endsWith("/"))
			thisVagrantfileDir = expPath.substring(0, expPath.length()-1);
		else
			thisVagrantfileDir = expPath;
	}
	
	String getVagrantfileDir()
	{
		return thisVagrantfileDir;
	}
	
	void writeProviderAsJSON(int indentLevel, PrintWriter file)
	{
		String indstr = LoadGenerator.getIndentStrForLevel(indentLevel);
		
		super.writeProviderAsJSON(indentLevel, file);
		file.println(indstr + "\"isDynamic\": " + isDynamic());
	}
}



