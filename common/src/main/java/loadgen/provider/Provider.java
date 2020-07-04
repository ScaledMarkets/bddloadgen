package loadgen.provider;

/**
A Provider is a source of test client runtimes.
The Controller creates a pool of Provider instances.
*/
public interface Provider {
	String getName();
	int noOfNodes();
	Set<String> nodeIps();
	Set<Integer> nodePorts();
	boolean isDynamic();
}
