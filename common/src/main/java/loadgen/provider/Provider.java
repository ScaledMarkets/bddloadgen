package loadgen.provider;

public interface Provider {
	String getName();
	int noOfNodes();
	Set<String> nodeIps();
	Set<Integer> nodePorts();
}
