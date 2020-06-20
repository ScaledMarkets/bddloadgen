package loadgen.providers;

import loadgen.provider.DynamicProvider;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
/**
A provider instance provides a runtime facade for creating and managing load
generation nodes.
*/
public class DockerProvider implements DymamicProvider {

	private String thisName;
	private int thisNoOfNodes;
	private Set<String> thisNodeIps = new TreeSet<String>();
	private Set<Integer> thisPorts;  // the ports that each node should map
	private static String thisNodeImagePath = .... // the full container registry path of
	private static ipAddrRange = ....192.168.0.0/16
		// the image to run on each load generation node.
	private static String netName = "loadgennet";

	public DockerProvider(String name, int noOfNodes, Set<int> ports, String nodeImagePath) {
		thisName = name;
		thisNoOfNodes = noOfNodes;
		thisPorts = ports;
		thisNodeImagePath = nodeImagePath;
	}


	public String getName() { return thisName; }

	public int noOfNodes() {
		return thisNoOfNodes;
	}

	public Set<String> nodeIps() {
		return thisNodeIps;
	}

	public Set<Integer> nodePorts() {
		return thisPorts;
	}

	/**
	Call the Docker client (which must have been installed on the current system)
	to create the required load generation nodes, which for this provider are
	containers.
	*/
	public void createNodes() throws Exception {
		createNetwork();
		for (int n : thisNoOfNodes) {
			String ip = createNode();
			thisNodeIps.add(ip);
		}
	}

	public void destroyNodes() throws Exception {
		for (String ip : thisNodeIps) {
			destroyNode(ip);
		}
		destroyNetwork();
	}


	/*
	Implementation only methods.
	*/

	private void createNetwork() throws Exception {
		String cmd = "sudo docker network create --subnet=${SUBNET_ADDR_RANGE} mynet"
	}

	private void destroyNetwork() throws Exception {
		String cmd = "sudo docker network rm mynet"
	}

	private String createNode() throws Exception {
		String cmd = "sudo docker run -d ";
		for (int p : thisPorts) {
			cmd += "--net " + netName + " --ip " + ....ip + " ";
			cmd += "-p " + p + " ";
			cmd += "--name " + ....nodename
		}
		cmd += nodeImagePath;

		try { perfProcessSync(cmd); }
		catch (TimeoutException tex) {
			throw new Exception("Unable to create container - timeout", tex);
		}
		catch (ExitValueException evex) {
			throw new Exception("Container creation exited with error", evex);
		}

	}

	private void destroyNode(String ip) throws Exception {


		sudo docker stop hello
		sudo docker rm hello

	}

	private void perfProcessSync(String cmd) throws TimeoutException, ExitValueException {
		Process p = Runtime.exec(cmd);
		boolean completed = p.waitFor(100, TimeUnit.MILLISECONDS);
		if (! completed) {
			throw new TimeoutException();
		}
		if (p.exitValue() != 0) {
			throw new ExitValueException();
		}
	}

	class TimeoutException extends Exception {
	}

	class ExitValueException extends Exception {
	}
}
