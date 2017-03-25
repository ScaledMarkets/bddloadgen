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


public class FunctionalProfile extends AbstractProfile
{
	private List<String> thisReqTypeNames = new Vector<String>();
	
	FunctionalProfile(String name, Consumer<FunctionalProfile> block)
	{
		super(name);
		if (block != null) block.accept(this);
	}
	
	public void addRequestType(String reqTypeName)
	{
		thisReqTypeNames.add(reqTypeName);
	}
	
	public List<String> requestTypes()
	{
		return thisReqTypeNames;
	}
	
	public RequestType getRequestType(String name)
	{
		return LoadGenerator.getRequestType(name);
	}
	
	String getProfileDefinition(AbstractTestRun testRun)
	{
		throw new RuntimeException("Not implememnted yet");
	}
	
	void writeProfileAsJSON(int indentLevel, PrintWriter file)
	{
		String indstr = LoadGenerator.getIndentStrForLevel(indentLevel);
		
		file.println(indstr + "\"name\": \"" + name() + "\",");
	}
}

