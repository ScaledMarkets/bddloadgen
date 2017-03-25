package loadgen;


public abstract class PerformanceProfileReader extends BaseProfileReader
{
	public abstract void addLevel(double requestsPerSec, double deltaInMinutes);
	
	public void parseNextPartLine(String data)
	{
		String[] level = data.split(",");
		double requestsPerSec = Double.parseDouble(level[0]);
		double deltaInMinutes = Double.parseDouble(level[1]);
		addLevel(requestsPerSec, deltaInMinutes);
	}
}

