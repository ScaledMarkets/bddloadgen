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

	private static String nodeImagePath = "cliffberg/loadgen"; // the container registry path of
		// the image to run on each load generation node.
	private static netBaseAddr = "192.168.0.0";
	private static String thisNetName = "loadgennet";

	private String thisName;
	private int thisNoOfNodes;
	private int thisNetMaskLength;
	private Set<String> thisNodeIps = new TreeSet<String>();
	private int thisBaseAddrInt = 0;
	private Set<Integer> thisPorts;  // the ports that each node should map
	private String thisCidr;
	private int thisNetMaskLength;
	private int thisNoOfIPsAllocated = 0;

	public DockerProvider(String name, int noOfNodes, Set<int> ports)
		throws Exception {

		thisName = name;
		thisNoOfNodes = noOfNodes;
		thisPorts = ports;

		/* Compute configuration: */
		if (noOfNodes <= 0) throw new Exception("No of nodes must be > 0");
		thisNetMaskLength = Integer.numberOfLeadingZeros(thisNoOfNodes);
		if (thisNetMaskLength < 16) throw new Exception("No of nodes is too large: mask must be at least 16 bits");
		thisCidr = netBaseAddr + "/" + Integer.toString(thisNetMaskLength);

		/* Separate out the portions of the CIDR base address: */
		thisBaseAddrInt = ipDotSepFormToInt(netBaseAddr);

		/* Verify that Docker is available: */
		String cmd = "sudo docker ps -a";
		perfProcessSync(cmd);
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

	public static class TimeoutException extends Exception {}

	public static class ExitValueException extends Exception {}


	/*
	Implementation only methods.
	*/

	private void createNetwork() throws Exception {
		destroyNetwork();
		String cmd = "sudo docker network create --subnet=" + thisCidr + " " + thisNetName;
		performProcess(cmd);
	}

	private void destroyNetwork() {
		String cmd = "sudo docker network rm " + thisNetName;
		try { perfProcessSync(cmd); } catch (Exception ex) {
			// ignore
		}
	}

	/** Return the IP address of the node that was created. */
	private String createNode() throws Exception {
		String ip = getNextIPFromCidr();
		destroyNode(ip);
		String nodeName = makeNodeNameFromIP(ip);
		String cmd = "sudo docker run -d ";
		cmd += "--net " + thisNetName + " --ip " + ip + " ";
		cmd += "--name " + nodeName + " ";
		for (int p : thisPorts) {
			cmd += "-p " + p + " ";
		}
		cmd += nodeImagePath;

		try { perfProcessSync(cmd); }
		catch (TimeoutException tex) {
			throw new Exception("Unable to create container - timeout", tex);
		}
		catch (ExitValueException evex) {
			throw new Exception("Container creation exited with error", evex);
		}
		return ip;
	}

	private void destroyNode(String ip) {

		String nodeName = makeNodeNameFromIP(ip);
		String cmd1 = "sudo docker stop " + nodeName;
		String cmd2 = "sudo docker rm " + nodeName;

		try { perfProcessSync(cmd1); } catch (Exception ex) {
			// ignore
		}
		try { perfProcessSync(cmd2); } catch (Exception ex) {
			// ignore
		}
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

	private String makeNodeNameFromIP(String ip) {
		return bddloadgen + ip;
	}

	private String getNextIPFromCidr() throws Exception {
		if (thisNoOfIPsAllocated >= thisNoOfNodes) throw new Exception(
			"Attempt to allocate more than " + thisNoOfNodes " nodes"
		);
		int ip = thisBaseAddrInt + thisNoOfIPsAllocated++;
		return ipIntToFourByteForm(ip);
	}

	/** Convert the specified IP address string, expressed using the four-number dot-separated
		form, to an integer value. */
	private int ipDotSepFormToInt(String ip) throws Exception {
		int intValue = 0;
		String[] parts = ip.split("\.");
		int i = 0;
		for (String part : parts) {
			i++;
			if (i > 4) throw new Exception("Ill-formatted IP address");
			int ipPart = Integer.parseInt(part);
			if (ipPart < 0) throw new Exception("Negative IP address portion");
			if (ipPart > Byte.MAX_VALUE) throw new Exception("Invalid IP address portion");
			intValue += ( ipPart << (32 - 8*i) );
		}
		return intValue;
	}

	/** Convert the specified integral IP address to the four-byte dot-separated form. */
	private String ipIntToFourByteForm(int ip) {
		String ipStr = "";
		int mask = 0x000F;
		int temp = ip;
		for (int i = 1;; i++) {
			int part = temp & mask;
			byte b = (byte)part;
			ipStr += Byte.toString(b);
			if (i == 4) break;
			ipStr += ".";
			temp >> 4;  // shift to the next byte
		}
		return ipStr;
	}
}
