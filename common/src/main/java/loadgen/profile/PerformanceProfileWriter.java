package loadgen.profile;


import java.util.List;


public abstract class PerformanceProfileWriter extends BaseProfileWriter
{
	public abstract List<double[]> getLevels();

	public String getProfileDefinition()
	{
		String profileDef = super.getProfileDefinition();
		List<double[]> levels = getLevels();
		for (double[] level : levels)
			profileDef = profileDef + (level[0] / getNoOfNodes()) + ", " + level[1] + "\n";
		return profileDef;
	}
}
