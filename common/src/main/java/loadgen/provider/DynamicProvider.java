package loadgen.provider;

public interface DymamicProvider extends Provider {
	void createNodes() throws Exception;
	void destroyNodes() throws Exception;
}
