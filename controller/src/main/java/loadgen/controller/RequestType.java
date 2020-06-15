package loadgen.controller;



import loadgen.controller.templates.SupportedProviders;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;
import java.awt.Color;


/** Specifies a type of test to perform. This is passed to each testing node, where
	a cucumber script will recognize the test type name and apply the appropriate
	test accordingly - including verification of the response. */
public class RequestType
{
	protected LoadGenerator lg;
	private String thisname;
	private List<String> taglist = new Vector<String>();
	private String thiscolor;

	RequestType(LoadGenerator lg, String name, Consumer<RequestType> block)
	{
		this.lg = lg;
		lg.validateName(name, "Request type");
		thisname = name;
		if (block != null) block.accept(this);
	}

	public String name()
	{
		return thisname;
	}

	/** A string that is passed directly to cucumber as a --tags option value. */
	public void tag(String t)
	{
		taglist.add(t);
	}

	public List<String> tags()
	{
		return taglist;
	}

	/** Set the color in which requests of this type should be drawn when graphed. */
	public void setColor(String c)
	{
		thiscolor = c;
	}

	public String color()
	{
		return thiscolor;
	}
}
