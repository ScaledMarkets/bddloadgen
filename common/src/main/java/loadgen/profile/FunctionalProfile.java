package loadgen.profile;



import java.io.PrintWriter;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;


public class FunctionalProfile extends AbstractProfile
{
	private List<String> thisReqTypeNames = new Vector<String>();

	FunctionalProfile(AbstractLoadGenerator lg, String name, Consumer<FunctionalProfile> block)
	{
		super(lg, name);
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
		return lg.getRequestType(name);
	}

	String getProfileDefinition(AbstractTestRun testRun)
	{
		throw new RuntimeException("Not implememnted yet");
	}

	void writeProfileAsJSON(int indentLevel, PrintWriter file)
	{
		String indstr = lg.getIndentStrForLevel(indentLevel);

		file.println(indstr + "\"name\": \"" + name() + "\",");
	}
}
