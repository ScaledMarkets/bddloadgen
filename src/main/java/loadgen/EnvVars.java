package loadgen;


/** Define the names of environment variables that are set by LoadGenerator. */

public interface EnvVars
{
	/** Where the test client cookbook is written to. Normally called "cookbooks".
		This is set before calling vagrant. */
    String Suppl_cookbook_dir = "SUPPLEMENTAL_COOKBOOK_DIR";

	String JBehaveJarPathOnNode = "JBEHAVEJARPATHONNODE";
	String LoadgenJarPathOnNode = "LOADGENJARPATHONNODE";
}
