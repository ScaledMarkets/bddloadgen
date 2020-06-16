package loadgen.provider;

public interface Provider {
	String getName();
	Set<String> nodeIps();
	Set<Integer> nodePorts();
}
