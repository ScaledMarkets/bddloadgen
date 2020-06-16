package loadgen;



import loadgen.controller.templates.SupportedProviders;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


/** Defines the configuration of the database into which test results are written. */
public class ResultsJSONDatabaseConfig
{
	private String thisUrl;
	private String thisUserid;
	private String thisPassword;
	private String thisIndexName;
	private String thisJsonDataType;

	ResultsJSONDatabaseConfig(Consumer<ResultsJSONDatabaseConfig> block)
	{
		if (block != null) block.accept(this);
	}

	public void setUrl(String url)
	{
		thisUrl = url;
	}

	public void setUserid(String userid)
	{
		thisUserid = userid;
	}

	public void setPassword(String pswd)
	{
		thisPassword = pswd;
	}

	String getURL()
	{
		return thisUrl;
	}

	String getUserId()
	{
		return thisUserid;
	}

	String getPassword()
	{
		return thisPassword;
	}

	public void setIndexName(String name)
	{
		thisIndexName = name;
	}

	public void setJsonDataType(String type)
	{
		thisJsonDataType = type;
	}

	String indexName()
	{
		return thisIndexName;
	}

	String jsonDataType()
	{
		return thisJsonDataType;
	}
}
