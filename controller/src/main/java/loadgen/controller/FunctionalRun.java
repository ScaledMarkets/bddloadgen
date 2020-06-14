package loadgen.controller;


import loadgen.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.File;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


/** A test run that runs functional tests, such that each test is only run on
	one node and only run once. In this context, "test" means the unit of processing
	that is defined by a RequestType. */
public class FunctionalRun extends AbstractTestRun
{
	private List<String> thisRequestTypes = new Vector<String>();
	private Integer thisMaxRequestsPerNode;
	
	
	FunctionalRun(String name, Consumer<FunctionalRun> block)
	{
		super(name);
		if (block != null) block.accept(this);
	}
	
	boolean isFunctional()
	{
		return true;
	}
	
	boolean isPerformance()
	{
		return false;
	}

	String getTestRunType()
	{
		return "Functional";
	}

	public List<String> requestTypes(String... requestTypeNames)
	{
		for (String reqtp : requestTypeNames) requestTypes().add(reqtp);
		return requestTypes();
	}
	
	List<String> requestTypes()
	{
		return thisRequestTypes;
	}
	
	public void setMaxRequestsPerNode(int max)
	{
		thisMaxRequestsPerNode = new Integer(max);
	}
	
	Integer maxRequestsPerNode()
	{
		return thisMaxRequestsPerNode;
	}
	
	
	// Implementation methods================================================
	
	
	/** Execute the test run in the current proces. This method is invoked
		by the start method. */
	void perform()
	{
		System.out.println("Performing Functional Run " + name());
		
		// Create a FunctionalProfile for each request type.
		for (String reqTypeName : requestTypes())
		{
			LoadGenerator.functionalProfile(reqTypeName, p ->
			{
				for (String rtName: requestTypes())
					p.addRequestType(rtName);
			});
		}
		
		provisionNodes();
		
		if (nodes() == null) throw new RuntimeException(
			"No nodes are defined for the static provider configuration " +
				getProvider().getConfigurationName());
		
		// Iterate through the request types, running each on a node.
		List<Thread> allThreads = new Vector<Thread>();
		for (String requestTypeName : requestTypes())
		{
			RequestType requestType = LoadGenerator.getRequestType(requestTypeName);
			if (requestType == null) throw new RuntimeException(
				"Internal error: Request type " + requestTypeName + " not found");
				
			allThreads.add(new Thread(new Runnable() {
				public void run() {
					synchronized (thisSemaphore) {
						// Get a node that is in nodes but not in busyNodes, and put it
						// into busyNodes; then return it. This blocks until a node
						// is available, and releases the lock when it blocks.
						Node node = getNode();
							
						// Tell the node to start, and wait for it. I.e., this blocks until
						// the remote node execution has completed.
						node.start(requestTypeName);  // ssh into node and start the specified tests.
							
						releaseNode(node);
					}
				}
			}));
		}
		
		// At this point, there are many threads running - up to the number of nodes.
		// Each of those threads has invoked a remote process and waits unitl that process
		// completes. We want to wait until all of those process have completed.
		
		for (Thread t : allThreads) try
		{
			t.join();  // wait for all threads to complete, meaning
			// that all remote executions have completed.
		}
		catch (InterruptedException ex) { throw new RuntimeException(ex); }
		
		// Now we can retrieve the time logs and generate statistics.
		retrieveLogs();
		
		// Generate statistics, aggregated across all nodes.
		if (reqLog() != null)
		{
			double pctPassed = (double)numberPassed() / (double)numberOfTests();
			System.out.println("% passed for test run " + name() + ": " + pctPassed);
		
			writeResults();
		}
		else
			System.out.println("Absent time log: unable to generate statistics");
		
		// Destroy the VMs, if required.
		cleanup();
	}
	
	
	// Intention: Define a pool of available nodes. Create a thread for each
	// request type. Define a mechanism for the threads to obtain a node. Start each
	// thread. A thread waits until it can obtain a node. When a thread obtains
	// a node, the thread executes the request on the node and waits for it
	// to complete. A node is permitted to execute multiple concurrent
	// requests, specified by the maxRequestsPerNode parameter.
	
	// The following three objects define the state that is needed to manage the
	// concurrent activity of the nodes.
	
	private Object thisSemaphore = new Object();
	
	/** how many requests each node is currently performing */
	private Map<Node, Integer> thisBusyNodes = new HashMap<Node, Integer>();
	
	/** nodes that are available to perform requests */
	private List<Node> thisFreeNodes = new Vector<Node>(nodes());


	/** Return true if all nodes are occupied performing requests. Thread-safe. */
	boolean nodesAllBusy()
	{
		synchronized (thisSemaphore) {
			return ( thisFreeNodes.size() == 0 );
		}
	}

	
	/** Obtain a node from the set of free nodes. Thread-safe. */
	Node getNode()
	{
		synchronized (thisSemaphore) {
			// Wait for @semaphoreCondition to be free. This releases the lock.
			while (nodesAllBusy()) try
			{
				thisSemaphore.wait();
			}
			catch (InterruptedException ex) { throw new RuntimeException(ex); }
			// We now have the lock again, and we can be sure that a
			// node is available.
			
			// Identify the least busy node.
			int minBusiness = 0;
			Node leastBusy = null;
			for (Node node : thisFreeNodes)
			{
				Integer howBusy = thisBusyNodes.get(node);
				if ((leastBusy == null) || (howBusy.intValue() < minBusiness))
				{
					leastBusy = node;
					minBusiness = howBusy;
				}
			}
			
			// Increment the business of the chosen node, and if it is now
			// maxed out, remove it from the freeNodes set.
			thisBusyNodes.put(leastBusy, thisBusyNodes.get(leastBusy).intValue() + 1);
			if ((maxRequestsPerNode() != null) &&
					(thisBusyNodes.get(leastBusy) == maxRequestsPerNode().intValue()))
				thisFreeNodes.remove(leastBusy);
			return leastBusy;
		}
	}
	
	
	/** Reduce a node's business count and return it to the set of free nodes
		if appropriate. Thread-safe. */
	void releaseNode(Node node)
	{
		synchronized (thisSemaphore) {
			if ((maxRequestsPerNode() != null) &&
				(thisBusyNodes.get(node).intValue() == maxRequestsPerNode()))
					// node was unavail.
				thisFreeNodes.add(node);  // make node available again
			thisBusyNodes.put(node, thisBusyNodes.get(node).intValue() - 1);
		}
		thisSemaphore.notify();  // let a waiting thread know that
			// a node _might_ be available
	}
		

	/** Top level entry point for writing all results to files.
		Write consolidated results (for all nodes) to CSV files. */
	void writeResults()
	{
		writeResultsForTestRunAsCSV();
		for (String rt : LoadGenerator.getRequestTypes().keySet())
		{
			writeResultsForRequestTypeAsCSV(rt);
		}
	}
	
	
	void writeResultsForTestRunAsCSV()
	{
	}
	
	
	void writeResultsForRequestTypeAsCSV(String requestTypeName)
	{
	}
}

