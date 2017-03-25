package loadgen;


import java.util.*;


/**
 * The LoadGenerator places a set of Profile files on each node, in the
 * directory defined by TestRunnerConstants.NodeProjectRoot. These Profiles are
 * read by TestRunner (using a ProfileReader).
 *
 * A performance profile has this syntax (in BNF); tokens separated by whitespace
 * that may not include newline:
 *
 * profile ::=
 *   profile_type request_type_name '\n' tag_line levels
 *   profile_type ::= 'PerformanceProfile' | 'FunctionalProfile'
 *   request_type_name ::= StringToken
 *   tag_line ::= tag_entry* '\n'
 *   tag_entry ::= '--tags' tag
 *   tag ::= StringToken
 *   levels ::= level_line*
 *   level_line ::= req_per_sec ',' after_minutes '\n'
 *   req_per_sec ::= FloatNumberToken
 *   after_minutes ::= FloatNumberToken
 *
 * If profile_type is 'FunctionalProfile', then there may not be any levels.
 *
 * Example of a performance profile:
 *   PerformanceProfile BaselineLoad
 *   --tags @Performance --tags @Future
 *   1.0, 10.0
 *   5.0, 1.0
 *
 * Example of a functional profile:
 *   FunctionalProfile LogonTests
 *   --tags @Logon --tags @Release1
 * 
 * The counterpart to this method in TestRunner.rb is the method buildProfile(profileFile).
 * These two methods should ideally be factored into a shared module but it
 * is too much trouble for the benefit.
 */
public abstract class BaseProfileWriter
{
	public abstract String getTestRunType();
	public abstract String getRequestTypeName();
	public abstract List<String> getTags();
	public abstract int getNoOfNodes();


	public String getProfileDefinition()
	{
		String profileDef = getTestRunType() + "Profile ";
		profileDef = profileDef + getRequestTypeName() + "\n";
		boolean first_time = true;
		for (String tag : getTags())
		{
			if (first_time) first_time = false;
			else profileDef = profileDef + " ";  // separator between tags
			profileDef = profileDef + "--tags " + tag;
		}

		int nnodes = getNoOfNodes();

		profileDef = profileDef + "\n";
		return profileDef;
	}
}

