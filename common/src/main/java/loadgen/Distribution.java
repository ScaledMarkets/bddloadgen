package loadgen;



import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;



/** The number of tests to perform over time. Can be specified using one of a set of
	distribution types. At present, only a "ramp" distribution type is supported. A ramp
	consists of a set of points: each point is a load level reached after a period of time.
	Load is ramped up (or dowen) linearly between each point. Load is assumed to begin
	at 0. */
public class Distribution
{
	private AbstractLoadGenerator lg;
	private String thisname;
	private String thistype;
	private List<double[]> thislevels = new Vector<double[]>();

	/** The allowed value for 'type' is 'ramp'. */
	public Distribution(AbstractLoadGenerator lg, String name, Consumer<Distribution> block)
	{
		this.lg = lg;
		lg.validateName(name, "Distribution");
		thisname = name;
		thistype = "ramp";
		if (block != null) block.accept(this);
	}

	public String name()
	{
		return thisname;
	}

	public String type()
	{
		return thistype;
	}

	/** Define a level for the ramp. Each level is a (value, time) pair: the ramp
		reaches "value" tests per second after "time" minutes from the prior level.
		A ramp implicitly begins at a level of zero (0) at test time zero (0). */
	public void level(double requestsPerSec, double deltaInMinutes)
	{
		if (deltaInMinutes <= 0.0) throw new RuntimeException(
			"invalid level time value: " + deltaInMinutes);

		thislevels.add(new double[] {requestsPerSec, deltaInMinutes});
	}

	public List<double[]> levels()
	{
		return thislevels;
	}


	void writeDistributionAsJSON(int indentLevel, PrintWriter file)
	{
		String indstr = lg.getIndentStrForLevel(indentLevel);

		file.println(indstr + "\"name\": \"" + name() + "\",");
		file.println(indstr + "\"type\": \"" + type() + "\",");
		file.println(indstr + "\"levels\": [");
		boolean firstTime = true;
		for (double[] level : levels())
		{
			if (firstTime) firstTime = false;
			else
				file.println(indstr + "\t,");
			file.println(indstr + "\t{ \"dt\": " + level[1] +
				", \"reqPerSec\": " + level[0] + " }");
		}
		file.println(indstr + "]");
	}
}
