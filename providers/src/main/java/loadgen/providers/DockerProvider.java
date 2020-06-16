package loadgen.providers;

import loadgen.provider.DynamicProvider;

public class DockerProvider implements DymamicProvider {

	private String thisName;

	public DockerProvider(String name) {
		thisName = name;
	}


	public String getName() { return thisName; }

	public Set<String> nodeIps() {

	}

	public Set<Integer> nodePorts() {

	}

	public void createNodes() throws Exception {
		
	}

}
