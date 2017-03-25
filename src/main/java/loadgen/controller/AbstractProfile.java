package loadgen.controller;


import loadgen.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


/** A specification for a set of tests to be run, including the types of tests,
	and the distrubtion with which to apply those tests (requests per second over time). */
abstract class AbstractProfile
{
	private String thisname;
	private String thishostname;
	private String thistimestamp;
	private String thisRequestTypeName;
	
	AbstractProfile(String name)
	{
		LoadGenerator.validateName(name, "AbstractProfile");
		thisname = name;
		if (name.equals("Vagrantfile") || name.equals("cookbooks") || name.equals("sshconfig") ||
			name.equals("Berksfile") || name.equals("Berksfile.default.lock") ||
			name.equals(".vagrant"))
			throw new RuntimeException("Disallowed profile name");
		
		try { thishostname = InetAddress.getLocalHost().getHostName(); }
		catch (Exception ex) { throw new RuntimeException(ex); }
		thistimestamp = (new Date()).toString();
	}

	public String name()
	{
		return thisname;
	}
	
	/** Return the name of the test controller host. */
	public String hostname()
	{
		return thishostname;
	}
	
	/** Return the time at which this profile was defined. */
	public String timestamp()
	{
		return thistimestamp;
	}

	abstract void writeProfileAsJSON(int indentLevel, PrintWriter file);

	abstract String getProfileDefinition(AbstractTestRun testRun);
	
	/** Specify the request type that this profile will perform. See the requestType
		method in LoadGenerator. */
	public void setRequestType(String reqTypeName)
	{
		thisRequestTypeName = reqTypeName;
	}

	public String requestType()
	{
		return thisRequestTypeName;
	}
}

