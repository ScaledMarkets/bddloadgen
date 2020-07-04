package loadgen.controller;

import loadgen.TestRunnerConstants;
import loadgen.Version;

import java.io.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.awt.Color;


/** A test run that manages a large numbre of tests, such that the tests are performed
many times aross the set of available nodes, according to a load profile.
Once the run has completed, its methods may be called to obtain the results,
graph the results, etc.
*/
public class PerformanceRun extends AbstractTestRun
{
	private boolean thisGraphAllEvents = false;
	private List<String> thisGraphEvents = new Vector<String>();
	private Map<String, String> thisEventColors = new HashMap<String, String>();
	private boolean thisAbortIfTestsCannotAchieveProfile = false;
	private Map<String, Map<String, Double>> thisStats = new HashMap<String, Map<String, Double>>();
	private Map<String, String> thisGraphs = new HashMap<String, String>(); // eventTypeName, graphFileName
	private List<String> thisTables = new Vector<String>();


	PerformanceRun(LoadGenerator lg, String name, Consumer<PerformanceRun> block)
	{
		super(lg, name);
		if (block != null) block.accept(this);
	}


	boolean isFunctional()
	{
		return false;
	}

	boolean isPerformance()
	{
		return true;
	}

	String getTestRunType()
	{
		return "Performance";
	}

	/** Specify that a graph (in SVG format) of the results should be created for
	the specified event type. Event types are defined in the cucumber tests by calling the
	'timelogBegin' and 'timelogEnd' methods in the utility class 'TestRunnerConstants'.
	Note that the 'EndToEnd' and 'WholeTest' events are always graphed (see
	TestRunnerConstants for explanations of these event types). */
	public void graphEvent(String eventTypeName, String color)
	{
		graphEvents().add(eventTypeName);
		if (color != null) eventColors().put(eventTypeName, color);
	}

	public void graphEvent(String eventTypeName)
	{
		graphEvent(eventTypeName, null);
	}

	public void graphAllEvents()
	{
		thisGraphAllEvents = true;
	}

	boolean mustGraphAllEvents()
	{
		return thisGraphAllEvents;
	}

	List<String> graphEvents()
	{
		return thisGraphEvents;
	}

	Map<String, String> eventColors()
	{
		return thisEventColors;
	}

	/** Return color to use when graphing the specified event type. Colors
		are the standard SVG colors: see http://www.december.com/html/spec/colorsvg.html */
	String eventColor(String eventTypeName)
	{
		return eventColors().get(eventTypeName);
	}

	/** Specify that if the tests cannot precisely keep up with the profiles that
		have been specified for this AbstractTestRun, then the AbstractTestRun should be aborted.
		It is inadvisable to do this because small lags will then cause a run
		to be terminated, even though the overall results for the run are valid. */
	void abortIfTestsCannotAchieveProfile()
	{
		thisAbortIfTestsCannotAchieveProfile = true;
	}

	boolean willAbortIfTestsCannotAchieveProfile()
	{
		return thisAbortIfTestsCannotAchieveProfile;
	}

	/** Return the average response time that was measured between the specified
		two times, relative to the run starting time. This must be called after
		parseTimeLog has been called.
		If reqTypeNames are specified, then the result is only for those request types. */
	public double avgResponseTimeBetween(double fromTime, double toTime, String eventTypeName,
		String... reqTypeNames)
	throws
		Exception // if there were no requests of the specified type
	{
		return computeMean(earliestTime(eventTypeName) + fromTime * 60.0,
			earliestTime(eventTypeName) + toTime * 60.0, eventTypeName, reqTypeNames);
	}

	/** Return the average response time that was measured from the specified
		time, relative to the run start time, through the end of this AbstractTestRun. */
	public double avgResponseTimeAfter(double fromTime, String eventTypeName,
		String... reqTypeNames)
	throws
		Exception  // if there are no successful tests after fromTime.
	{
		double et = earliestTime(eventTypeName, reqTypeNames);
		double lt = latestTime(eventTypeName, reqTypeNames);
		return avgResponseTimeBetween(fromTime, lt - et, eventTypeName, reqTypeNames);
	}


	/** Return the statistics for the data represented by the aggregated time log.
		Returns a hashtable of these statistics:
			"pct_passed", "mean", "min", "max", "sd" (standard deviation).
		Note: the statistics are only for those tests that passed. */
	public Map<String, Double> getStats(String eventTypeName, String... reqTypeNames)
	{
		String reqTypeStr = "";
		boolean firstTime = true;
		for (String reqTypeName : reqTypeNames)
		{
			if (firstTime) firstTime = false;
			else reqTypeStr = reqTypeStr + ",";
			reqTypeStr = reqTypeStr + reqTypeName;
		}

		String et;
		if (eventTypeName == null) et = "";
		else et = eventTypeName;

		String key = et + ":" + reqTypeStr;
		Map<String, Double> stats = getPrecalculatedStats().get(key);
		if (stats == null) stats = computeStats(null, null, eventTypeName, reqTypeNames);
		getPrecalculatedStats().put(key, stats);
		return stats;
	}


	Map<String, Map<String, Double>> getPrecalculatedStats()
	{
		return thisStats;
	}


	/** Return the response time below which 'percent' percent of the respones
		times fall. */
	public double getPercentile(double percent, String eventTypeName, String... reqTypeNames)
	{
		double fraction = percent / 100.0;
		List<Double> sortedTimes = getSortedResponseTimes(eventTypeName, reqTypeNames);
		int noOfEntries = sortedTimes.size();
		int percentileCount = (int)(fraction * (double)(noOfEntries-1));
		return sortedTimes.get(percentileCount);
	}

	/** Return an array of the response times, sorted by increasing response time,
		for the specified request types and the specified event type. (See TestRunnerConstants
		for explanation of event type.) */
	public List<Double> getSortedResponseTimes(String eventTypeName, String... reqTypeNames)
	{
		return sortResponseTimes(eventTypeName, reqTypeNames);
	}

	/** Return the start time of the first request. */
	public double earliestTime(String eventTypeName, String... reqTypeNames)
	throws
		Exception // if there were no requests of the specified types.
	{
		for (AbstractTimeLogEntry entry : aggTimeLogData())
		{
			if ((reqTypeNames.length != 0) &&
				(! Util.arrayContains(reqTypeNames, entry.reqType))) continue;
			if ((reqTypeNames.length != 0) &&
				(! Util.arrayContains(reqTypeNames, entry.reqType))) continue;
			if (! entry.name.equals(eventTypeName)) continue;
			return entry.startTime;
		}
		throw new Exception("there were no requests of that type");
	}

	/** Return the start time of the last request. */
	public double latestTime(String eventTypeName, String... reqTypeNames)
	{
		double lastEntryTime = 0.0;
		for (AbstractTimeLogEntry entry : aggTimeLogData())
		{
			if ((reqTypeNames.length != 0) &&
				(! Util.arrayContains(reqTypeNames, entry.reqType)))
				continue;
			if (! entry.name.equals(eventTypeName)) continue;
			lastEntryTime = entry.startTime;
		}
		return lastEntryTime;
	}

	/** Return the actual average request rate for the specified event type and the
		specified request types. If no request types are specified, then include
		all request types. If fromTime is nil then use the test start time.
		If toTime is nil then use the end of the test. */
	public double getAvgRequestRate(Double fromTime, Double toTime, String eventTypeName,
		String... reqTypeNames)
	{
		Double ft;
		if (fromTime == null) ft = null;
		else ft = fromTime;

		Double tt;
		if (toTime == null) tt = null;
		else tt = toTime;

		return computeAvgRequestRate(ft, tt, eventTypeName, reqTypeNames);
	}


	// Implementation methods================================================


	/** Execute this test run in the current proces. This method is invoked
	by the start method. */
	void perform()
	{
		System.out.println("Performing Performance Run " + name());

		provisionNodes();

		// Start each Node using a separate thread, so that they are
		// asynchronous and this process can wait for them all to complete.
		if (nodes() == null) throw new RuntimeException(
			"No nodes are defined for the static provider configuration " +
			getProvider().getConfigurationName());

		List<Thread> threads = new Vector<Thread>();
		for (Node node : nodes())  // this loop must execute rapidly
		{
			Thread t = new Thread(
				() -> node.start()  // ssh into node and start tests.
			);
			threads.add(t);
			t.start();
		}

		try {
			for (Thread t : threads) t.join();  // wait for all of the above threads to exit.
		} catch (InterruptedException ex) {
			System.err.println("Thread join interrupted");
			return;
		}

		// Now we can retrieve the time logs and generate statistics.
		retrieveLogs();

		// Generate statistics, aggregated across all nodes.
		if (reqLog() != null)
		{
			System.out.println("Statistics for test run " + name() +
				", end-to-end: " + getStats(TestRunnerConstants.EndToEnd));
			System.out.println("Statistics for test run " + name() +
				", all events: " + getStats(null));

			if (mustGraphAllEvents())
			{
				for (String eventTypeName : detailLog().eventTypeCount().keySet())
				{
					if (eventTypeName.equals(TestRunnerConstants.WholeTest)) continue;
					genGraph(eventTypeName);
				}
			}
			else
			{
				for (String eventTypeName : graphEvents())
				{
					if (eventTypeName.equals(TestRunnerConstants.WholeTest)) continue;
					if (eventTypeName.equals(TestRunnerConstants.EndToEnd)) continue;
					genGraph(eventTypeName);
				}
			}
			genGraph(TestRunnerConstants.WholeTest);  // always graph this
			genGraph(TestRunnerConstants.EndToEnd);  // always graph this

			writeResults();  // Note: the graphs must be generated before this can be called.

			// Insert data into database (Elastic Search).
			if (lg.getResultsDatabaseConfig().getURL() != null)
				pushResultsToDatabase();
		}
		else
			System.err.println("Absent time log: unable to generate statistics");

		// Destroy the VMs, if required.
		cleanup();
	}


	/** Compute statistics for the specified request types. Only passed tests are included.
		If no request types are specified, then include all request types. */
	Map<String, Double> computeStats(Double fromTime, Double toTime, String eventTypeName,
		String... reqTypeNames)
	{
		// Initialize a hash of hashes with each request type.
		Map<String, Double> stats = new HashMap<String, Double>() {
			public String toString()  // Make the string representation useful.
			{
				String s = "";
				boolean firstTime = true;
				for (String key : keySet())
				{
					if (firstTime) firstTime = false;
					else s += ", ";
					s += (key + ": " + get(key));
				}
				return s;
			}
		};

		stats.put("pct_passed", ((double)numberPassed() / (double)numberOfTests()) * 100.0);
		double mean = computeMean(fromTime, toTime, eventTypeName, reqTypeNames);
		stats.put("mean", new Double(mean));
		double sumOfDif = 0.0;
		double n = 0.0;
		boolean firstTime = true;
		double min = 0.0;
		double max = 0.0;
		int noOfReqs = 0;

		for (AbstractTimeLogEntry logDataEntry : aggTimeLogData()) try
		{
			if (! logDataEntry.result.equals("true")) continue;
			if ((reqTypeNames.length != 0) &&
				(! Util.arrayContains(reqTypeNames, logDataEntry.reqType))) continue;
			if ((eventTypeName != null) &&
				(! logDataEntry.name.equals(eventTypeName))) continue;

			// Exclude whole test and end-to-end events from the request count.
			if ((!eventTypeName.equals(TestRunnerConstants.EndToEnd)) &&
				(! eventTypeName.equals(TestRunnerConstants.WholeTest)))
				noOfReqs = noOfReqs + 1;

			double time = logDataEntry.duration;  // select the duration
			if ((fromTime != null) && (time < fromTime)) continue;
			if ((toTime != null) && (time > toTime)) continue;
			if (firstTime)
			{
				firstTime = false;
				min = time;
				max = time;
			}
			else
			{
				if (time < min) min = time;
				if (time > max) max = time;
			}
			n = n + 1.0;
			double d = (time - mean);
			sumOfDif = sumOfDif + (d * d);
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}

		stats.put("sd", Math.sqrt(sumOfDif / n));
		stats.put("min", min);
		stats.put("max", max);

		double ft;
		if (fromTime == null) ft = 0.0;
		else ft = fromTime;

		double tt;
		if (toTime == null) tt = getOverallLoadDuration();
		else tt = toTime;

		if (noOfReqs > 0) stats.put("reqrate", noOfReqs / (tt - ft));
		return stats;
	}


	/** Compute the actual average request rate for the specified event type and the
		specified request types. If no request types are specified, then include
		all request types. If fromTime is null then use the test start time.
		If toTime is nil then use the end of the test. */
	double computeAvgRequestRate(Double fromTime, Double toTime, String eventTypeName,
		String... reqTypeNames)
	{
		int noOfReqs = 0;
		for (AbstractTimeLogEntry logDataEntry : aggTimeLogData())
		{
			if (! logDataEntry.result.equals("true")) continue;
			if ((reqTypeNames.length != 0) &&
				(! Util.arrayContains(reqTypeNames, logDataEntry.reqType))) continue;
			if ((eventTypeName != null) &&
				(! logDataEntry.name.equals(eventTypeName))) continue;

			// Exclude whole test and end-to-end events from the request count.
			if ((! eventTypeName.equals(TestRunnerConstants.EndToEnd)) &&
				(! eventTypeName.equals(TestRunnerConstants.WholeTest)))
				noOfReqs = noOfReqs + 1;
		}

		double ft;
		if (fromTime == null) ft = 0.0;
		else ft = fromTime;

		double tt;
		if (toTime == null) tt = getOverallLoadDuration();
		else tt = toTime;

		return (double)noOfReqs / (tt - ft);
	}


	/** Find the latest time spanned by all profiles. */
	double getOverallLoadDuration()
	{
		double latestTime = 0.0;
		for (String profileName : testRunProfiles())
		{
			PerformanceProfile profile = lg.getPerformanceProfile(profileName);
			Distribution distribution = lg.getDistribution(profile.distribution());
			double distLatestTime = 0.0;
			for (double[] level : distribution.levels())
				distLatestTime = distLatestTime + (level[1] * 60.0);
			if (distLatestTime > latestTime) latestTime = distLatestTime;
		}
		return latestTime;
	}


	/** Compute the mean of the values in the duration ('detla') column of the log data,
		for the specified time range, event type name, and request types. */
	double computeMean(Double fromTime, Double toTime, String eventTypeName, String... reqTypeNames)
	{
		double seconds = 0.0;
		double n = 0.0;
		for (AbstractTimeLogEntry logDataEntry : aggTimeLogData())
		{
			if (! logDataEntry.result.equals("true")) continue;
			if ((reqTypeNames.length != 0) &&
				(! Util.arrayContains(reqTypeNames, logDataEntry.reqType)))
				continue;
			if ((eventTypeName != null) ||
				(! logDataEntry.name.equals(eventTypeName))) continue;

			double time = logDataEntry.duration;
			seconds = seconds + time;

			n = n + 1.0;
			if ((fromTime != null) && (time < fromTime)) continue;
			if ((toTime != null) && (time > toTime)) continue;
		}
		return seconds / n;
	}


	/** Return an array of the response times, sorted by increasing response time,
		for the specified request types and the specified event type. (See TestRunnerConstants
		for explanation of event type.) */
	List<Double> sortResponseTimes(String eventTypeName, String... reqTypeNames)
	{
		List<Double> responseTimes = new Vector<Double>();
		for (AbstractTimeLogEntry tlentry : aggTimeLogData())
		{
			if (tlentry.result.equals("true")) continue;
			if ((reqTypeNames.length != 0) ||
				(! Util.arrayContains(reqTypeNames, tlentry.reqType)))
				continue;
			if (! tlentry.name.equals(eventTypeName)) continue;

			// Place the log entry's response time in the responseTimes list.
			double responseTime = tlentry.duration;
			int pos = -1;
			boolean inserted = false;
			for (double rtentry : responseTimes)
			{
				pos = pos + 1;
				if (responseTime < responseTimes.get(pos))
				{
					responseTimes.add(pos, responseTime);  // insert at pos
					inserted = true;
					break;
				}
			}
			if (! inserted) responseTimes.add(responseTime);
		}
		return responseTimes;
	}


	/** Top level entry point for writing all results to files (but not database).
		Write consolidated results (for all nodes) to JSON and CSV files. */
	void writeResults()
	{
		writeResultsAsJSON();
		writeResultsForTestRunAsCSV();
		for (String rt : lg.getRequestTypes().keySet())
			writeResultsForRequestTypeAsCSV(rt);
		writeStatisticalSummary();
	}


	/** Note: the graphs must be generated before this can be called. */
	void writeStatisticalSummary()
	{
		String filepath = resultsDirectory() + "/" + name() + "_summary.html";
		PrintWriter file;
		try { file = new PrintWriter(filepath); } catch (FileNotFoundException ex) {
			throw new RuntimeException(ex); }
		file.println("<html>");
		file.println("<head>");
		file.println("<style>");
		file.println("body {text-indent: 50px;}");
		file.println("p {color:blue;}");
		file.println("h1 {font-family:verdana;}");
		file.println("h2 {font-family:verdana;}");
		file.println("h3 {font-family:verdana;}");
		file.println("p {font-family:verdana;}");
		file.println("</style>");
		file.println("</head>");
		file.println("<body>");
		file.println("<h1>Summary results for test run " + name() + ", " + timestamp() + "</h1>");
		file.println("<h2>Statistics for end-to-end test completion times</h2>");
		file.println("<p>" + getStats(TestRunnerConstants.EndToEnd) + "</p>");
		file.println("<h2>Statistics for all other event times, aggregated</h2>");
		file.println("<p>" + getStats(null) + "</p>");
		file.println("<h1>Graphs (" + getGraphs().size() + ")</h1>");
		for (String graph : getGraphs().keySet())
			file.println("<p><a href=\"" + getGraphs().get(graph) + "\">" + graph + "</a></p>");

		file.println("<h1>Tables (" + getTables().size() + ")</h1>");
		for (String table : getTables())
			file.println("<p><a href=\"" + table + "\">" + table + "</a></p>");

		file.println("<h1>JSON output</h1>");
		file.println("<p><a href=\"" + name() + ".json\">" + name() + ".json</a></p>");

		file.println("</body>");
		file.println("</html>");
		file.close();
	}


	/** Write end to end test times. These times include the startup time of each test.
		Test passage (or failure) is also written. */
	void writeResultsForTestRunAsCSV()
	{
		String fileName = name() + ".csv";
		String filepath = resultsDirectory() + "/" + fileName;
		getTables().add(fileName);
		PrintWriter file;
		try { file = new PrintWriter(filepath);}
		catch (FileNotFoundException ex) { throw new RuntimeException(ex); }
		file.println("req type,\tid,\tevent name,\tstart time,\tend time,\telapsed,\trate,\tresult");
		for (ReqTimeLogEntry entry : reqTimeLogData())
		{
			file.println(entry.reqType +
				",\t" + entry.id +
				",\t" + entry.name +
				",\t" + entry.startTime +
				",\t" + entry.endTime +
				",\t" + entry.duration +
				",\t" + entry.reqRate +
				",\t" + entry.result);
		}
	}


	/** Write test results for the specified request type. */
	void writeResultsForRequestTypeAsCSV(String requestTypeName)
	{
		String fileName = name() + "." + requestTypeName + ".csv";
		String filepath = resultsDirectory() + "/" + fileName;
		getTables().add(fileName);
		PrintWriter file;
		try { file = new PrintWriter(filepath); }
		catch (FileNotFoundException ex) { throw new RuntimeException(ex); }
		file.println("req type,\tid,\tevent name,\tstart time,\tend time,\telapsed,\trate,\tresult");
		for (ReqTimeLogEntry entry : reqTimeLogData())
		{
			if (entry.reqType.equals(requestTypeName))
			{
				file.println(entry.reqType +
					",\t" + entry.id +
					",\t" + entry.name +
					",\t" + entry.startTime +
					",\t" + entry.endTime +
					",\t" + entry.duration +
					",\t" + entry.reqRate +
					",\t" + entry.result);
			}
		}
	}


	/** Top level entry point for writing results as JSON. */
	void writeResultsAsJSON()
	{
		String filepath = getJSONResultsFilePath();
		PrintWriter file;
		try { file = new PrintWriter(filepath); }
		catch (FileNotFoundException ex) { throw new RuntimeException(ex); }
		file.println("{");
		file.println("\t\"agilex_loadgen_test_run\": {");
		file.println("\t\t\"agilex_loadgen_version\": \"" + Version.VERSION + "\",");

		writeTestRunAsJSON(2, file);

		file.println("\t}");
		file.println("}");
	}


	String getJSONResultsFilePath()
	{
		return (resultsDirectory() + "/" + name() + ".json");
	}


	String getElasticSearchURL()
	{
		return lg.getResultsDatabaseConfig().getURL();
	}


	String getElasticSearchUserId()
	{
		return lg.getResultsDatabaseConfig().getUserId();
	}


	String getElasticSearchPassword()
	{
		return lg.getResultsDatabaseConfig().getPassword();
	}


	String getElasticSearchIndexName()
	{
		return lg.getResultsDatabaseConfig().indexName();
	}


	String getElasticSearchDataType()
	{
		return lg.getResultsDatabaseConfig().jsonDataType();
	}


	/** Write results to the database that is specified by the resultsDatabaseConfig. */
	void pushResultsToDatabase()
	{
		String userid = getElasticSearchUserId();
		String password = getElasticSearchPassword();
		String indexName = getElasticSearchIndexName();
		String jsonDataType = getElasticSearchDataType();
		String loginString = "";
		if ((userid != null) && (userid != ""))
			loginString = "-u " + userid + ":" + password + " ";

		String cmd = "curl -v -H \"Content-Type: application/json\" -X POST " +
			"--data @" + getJSONResultsFilePath() + " " + loginString +
			getElasticSearchURL() + "/" + indexName + "/" + jsonDataType;
		try
		{
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			if (p.exitValue() != 0)
			{
				System.err.println("Error pushing results to database: return status was " + p.exitValue());
				printDBFailureInfo();
			}
		}
		catch (Exception ex)
		{
			System.err.println("Error pushing results to database: " + ex.getMessage());
			printDBFailureInfo();
		}
	}


	void printDBFailureInfo()
	{
		System.err.println("Database URL: " + getElasticSearchURL());
		System.err.println("Database user id: " + getElasticSearchUserId());
		System.err.println("JSON results file path: " + getJSONResultsFilePath());
	}


	void writeTestRunAsJSON(int indentLevel, PrintWriter file)
	{
		String indstr = "";
		for (int i = 1; i <= indentLevel; i++) indstr = indstr + "\t";

		file.println(indstr + "\"test_run_name\": \"" + name() + "\",");
		//file.println(indstr + "\"performance_feature_desc\": \"" + lg.getFeatureDesc() + "\",");
		//file.println(indstr + "\"performance_scenario_desc\": \"" + lg.getScenarioDesc() + "\",");
		//file.println(indstr + "\"performance_scenario_tags\": [");
		boolean firstTime = true;
		//for (String tag : lg.getScenarioTags())
		//{
		//	if (firstTime) firstTime = false;
		//	else file.println(indstr + "\t,");
		//	file.println(indstr + "\t\"" + tag + "\"");
		//}
		//file.println(indstr + "],");
		file.println(indstr + "\"hostname\": \"" + hostname() + "\",");
		file.println(indstr + "\"timestamp\": \"" + timestamp() + "\",");
		file.println(indstr + "\"projectRepoName\": \"" + projectRepoName() + "\",");
		file.println(indstr + "\"sut_featuresDirectory\": \"" + featuresDirectory() + "\",");

		file.println(indstr + "\"sut_features\": [");
		firstTime = true;
		for (String featureName : features())
		{
			if (firstTime) firstTime = false;
			else file.println(indstr + "\t,");
			file.println(indstr + "\t{ \"sut_feature\": \"" + featureName + "\" }");
		}
		file.println(indstr + "],");

		file.println(indstr + "\"requireRubyDirs\": [");
		firstTime = true;
		for (String[] entry : requireRubyDirs())
		{
			if (firstTime) firstTime = false;
			else file.println(indstr + "\t,");
			file.println(indstr + "\t{ \"requireRubyDir\": [\"" + entry[0] + "\", \"" + entry[1] + "\"] }");
		}
		file.println(indstr + "],");

		file.println(indstr + "\"provider\": {");
		getProvider().writeProviderAsJSON(indentLevel + 1, file);
		file.println(indstr + "},");
		file.println(indstr + "\"nodes\": " + noOfNodes() + ",");
		file.println(indstr + "\"nodesAreHeaded\": \"" + areNodesHeaded() + "\",");

		file.println(indstr + "\"profiles\": [");
		firstTime = true;
		for (String profileName : testRunProfiles())
		{
			if (firstTime) firstTime = false;
			else file.println(indstr + "\t,");

			AbstractProfile profile = lg.getProfile(profileName);
			file.println(indstr + "\t{");

			// Write the profile as it currently exists at the end of the
			// test run. (Note that the master test file could modify
			// a profile after the test run.)
			profile.writeProfileAsJSON(indentLevel + 1, file);

			file.println(indstr + "\t}");
		}
		file.println(indstr + "],");

		file.println(indstr + "\"distributions\": [");
		firstTime = true;
		for (Distribution dist : lg.getDistributions().values())
		{
			if (firstTime) firstTime = false;
			else file.println(indstr + "\t,");

			file.println(indstr + "\t{");
			dist.writeDistributionAsJSON(indentLevel + 1, file);
			file.println(indstr + "\t}");
		}
		file.println(indstr + "],");

		file.println(indstr + "\"reuseNodes\": " + willReuseNodes() + ",");
		file.println(indstr + "\"keepNodes\": " + willKeepNodes() + ",");
		file.println(indstr + "\"events\": [");
		firstTime = true;
		for (ReqTimeLogEntry entry : reqTimeLogData())
		{
			if (firstTime) firstTime = false;
			else file.println(indstr + "\t\t,");
			file.println(indstr + "\t{");
			file.println(indstr + "\t\t\"req_type\": \"" + entry.reqType + "\",");
			file.println(indstr + "\t\t\"id\": \"" + entry.id + "\",");
			file.println(indstr + "\t\t\"name\": \"" + entry.name + "\",");
			file.println(indstr + "\t\t\"start_time\": " + entry.startTime + ",");
			file.println(indstr + "\t\t\"end_time\": " + entry.endTime + ",");
			file.println(indstr + "\t\t\"elapsed_time\": " + entry.duration + ",");
			file.println(indstr + "\t\t\"rate\": " + entry.reqRate + ",");
			file.println(indstr + "\t\t\"result\": \"" + entry.result + "\"");
			file.println(indstr + "\t}");
		}
		file.println(indstr + "]");
	}


	Map<String, String> getGraphs()
	{
		return thisGraphs;
	}


	List<String> getTables()
	{
		return thisTables;
	}


	private int thisXOffset = 100;
	private int thisYOffset = 100;
	private int thisImageWidth = 1000;
	private int thisImageHeight = 600;
	private int thisCharHeight = 20; // in pixels
	private int statisticBoxHeight = (lg.getRequestTypes().size() + 2) * thisCharHeight;
	private int thisGraphHeight = thisImageHeight - statisticBoxHeight;


	/** Convert y from diagram space to display space. */
	int translateDiagramY(int y)
	{
		return thisImageHeight - y;
	}


	/** Convert a delta-y from diagram space to display space. */
	int translateDiagramDy(int dy)
	{
		return -dy;
	}


	int translateGraphY(int y)
	{
		return thisGraphHeight - y + thisYOffset;
	}


	/** Generate an SVG graph of the results, for test events with the specified name.
		The x axis is the elapsed time, and the y axis is also the elapsed time.
		Requests are drawn a vertical bars, beginning at their start time and
		ending at their ending time: these are placed on the x axis according
		to their start time. */
	void genGraph(String eventTypeName)
	{
		String header = "<?xml version=\"1.0\" standalone=\"yes\"?>\n" +
			"<svg version=\"1.1\" fill=\"none\" stroke=\"none\" stroke-linecap=\"square\" " +
			"stroke-miterlimit=\"10\" xmlns=\"http://www.w3.org/2000/svg\" " +
			"xmlns:xlink=\"http://www.w3.org/1999/xlink\">";
		String trailer = "</svg>";

		int xOffset = thisYOffset;  // distance from left at which to begin the graph.
		int yOffset = thisYOffset;  // distance from baseline at which to begin the graph.
		int imageWidth = thisImageWidth;
		int imageHeight = thisImageHeight;

		// Find the extent (domain, range) of the data.
		double minStartTime = 0;
		double maxStartTime = 0;
		double minEndTime = 0;
		double maxEndTime = 0;
		double maxDuration = 0;
		boolean firstTime = true;
		for (ReqTimeLogEntry logDataEntry : reqTimeLogData())
		{
			if (! logDataEntry.name.equals(eventTypeName)) continue;
			if (firstTime)
			{
				firstTime = false;
				minStartTime = logDataEntry.startTime;
				maxStartTime = logDataEntry.startTime;
				minEndTime = logDataEntry.endTime;
				maxEndTime = logDataEntry.endTime;
				maxDuration = maxEndTime - maxStartTime;
			}

			if (minStartTime >= logDataEntry.startTime) minStartTime = logDataEntry.startTime;
			if (maxStartTime <= logDataEntry.startTime) maxStartTime = logDataEntry.startTime;
			if (minEndTime >= logDataEntry.endTime) minEndTime = logDataEntry.endTime;
			if (maxEndTime <= logDataEntry.endTime) maxEndTime = logDataEntry.endTime;

			double duration = logDataEntry.endTime - logDataEntry.startTime;
			if (maxDuration <= duration) maxDuration = duration;
		}

		double latestProfileTime = 0.0;
		double highestLevel = 0.0;
		for (String profileName : testRunProfiles())
		{
			PerformanceProfile profile = lg.getPerformanceProfile(profileName);
			Distribution distribution = lg.getDistribution(profile.distribution());
			latestProfileTime = minStartTime;
			double priorLevel = 0.0;
			for (double[] level : distribution.levels())
			{
				double dSecs = level[1] * 60.0;
				latestProfileTime = latestProfileTime + dSecs;
				double thisLevel = level[0] + priorLevel;
				if (thisLevel > highestLevel) highestLevel = thisLevel;
			}
		}

		double t = latestProfileTime;
		if (maxStartTime > latestProfileTime) t = maxStartTime;

		double xScaleFactor = (double)(imageWidth - xOffset*2) / (t - minStartTime);
		//yScaleFactor = (imageHeight - yOffset*2) / (maxEndTime - minStartTime)
		double yRateScaleFactor = (double)(imageHeight - statisticBoxHeight - yOffset*2) / highestLevel;
		double yDurationScaleFactor = (double)(imageHeight - statisticBoxHeight - yOffset*2) / maxDuration;  // pixels/sec.
		//yDurationScaleFactor = (imageHeight - yOffset*2) / maxEndTime

		String graphFileName = name() + '-' + eventTypeName + ".svg";
		String filepath = resultsDirectory() + "/" + graphFileName;
		getGraphs().put(eventTypeName, graphFileName);

		PrintWriter file;
		try { file = new PrintWriter(filepath); }
		catch (FileNotFoundException ex) { throw new RuntimeException(ex); }

		file.println(header);

		file.println("<g class=\"labels\">");

		// Label X axis.
		int n = 0;
		double tRange = maxStartTime;
		double fraction = tRange;
		while (true)
		{
			n = n + 1;
			fraction = fraction / 10.0;
			if (fraction < 1.0) break;
		}

		// Draw vertical grid.
		double tGridSize = 1.0;
		for (int i = 1; i <= (n-1); i++) tGridSize = tGridSize * 10.0;

		t = 0.0;
		tRange = tRange + tGridSize;
		n = 0;
		while (t < tRange)
		{
			n = n + 1;
			double x = t * xScaleFactor + xOffset;
			file.println("<text x=\"" + x + "\" y=\"" + translateGraphY(yOffset - thisCharHeight) +
				"\" style=\"fill:black;font-family:Arial;font-size:14px;kerning:1;\">" + (int)t + "</text>");
			file.println("<path id=\"vgrid" + n + "\" d=\"M " + x + " " + translateGraphY(yOffset) +
				" l 0 " + translateDiagramDy(thisGraphHeight-yOffset) + "\" stroke=\"gray\" stroke-width=\"1\"  fill=\"none\" />");
			t = t + tGridSize;
		}

		// Draw x axis label.
		int x = xOffset + imageWidth/2;
		int y = yOffset - 40;
		file.println("<text x=\"" + x + "\" y=\"" + translateGraphY(y) +
			"\" text-anchor=\"middle\" style=\"fill:black;font-family:Arial;font-size:14px;kerning:1;\">Time (seconds)</text>");

		// Draw y axis labels.
		x = xOffset - 50;
		y = yOffset + (thisGraphHeight-yOffset) / 2;
		file.println("<text x=\"" + x + "\" y=\"" + translateGraphY(y) +
			"\" text-anchor=\"middle\" transform=\"rotate(-90, " + x + ", " + translateGraphY(y) +
			")\" style=\"fill:green;font-family:Arial;font-size:14px;kerning:1;\">Test Rate (tests per sec)</text>");

		x = xOffset + imageWidth + 60;
		y = yOffset + (thisGraphHeight-yOffset) / 2;
		file.println("<text x=\"" + x + "\" y=\"" + translateGraphY(y) +
			"\" text-anchor=\"middle\" transform=\"rotate(-90, " + x + ", " + translateGraphY(y) +
			")\" style=\"fill:darkblue;font-family:Arial;font-size:14px;kerning:1;\">Event Duration (seconds)</text>");

		// Draw horizontal grids (2).
		n = 0;
		double lRange = highestLevel;
		fraction = lRange;
		while (true)
		{
			n = n + 1;
			fraction = fraction / 10;
			if (fraction < 1.0) break;
		}

		double lGridSize = 1.0;  // requests/sec.
		for (int i = 1; i <= (n-1); i++) lGridSize = lGridSize * 10.0;

		n = 0;
		double eRange = maxDuration;
		//eRange = maxEndTime;
		fraction = eRange;
		while (true)
		{
			n = n + 1;
			fraction = fraction / 10;
			if (fraction < 1.0) break;
		}

		double eGridSize = 1.0;   // seconds
		for (int i = 1; i <= (n-1); i++) eGridSize = eGridSize * 10.0;

		double l = 0;
		lRange = lRange + lGridSize;
		n = 0;
		while (l < lRange)
		{
			n = n + 1;
			y = (int)(l * yRateScaleFactor) + yOffset;
			file.println("<text x=\"" + (xOffset - 30) + "\" y=\"" + translateGraphY(y) +
				"\" style=\"fill:green;font-family:Arial;font-size:14px;kerning:1;\">" + (int)l + "</text>");
			file.println("<path id=\"lgrid" + n + "\" d=\"M " + xOffset + " " +
				translateGraphY(y) + " l " + imageWidth + " " + translateDiagramDy(0) +
				"\" stroke=\"green\" stroke-width=\"1\"  fill=\"none\" />");
			l = l + lGridSize;
		}

		double e = 0;
		eRange = eRange + eGridSize;
		n = 0;
		double lowerEndOfeRange = 0;
		while (e < eRange)
		{
			n = n + 1;
			y = (int)(e * yDurationScaleFactor) + yOffset;
			file.println("<text x=\"" + xOffset + imageWidth + thisCharHeight +
				"\" y=\"" + translateGraphY(y) +
				"\" style=\"fill:darkblue;font-family:Arial;font-size:14px;kerning:1;\">" +
				(int)e + "</text>");
			file.println("<path id=\"egrid" + n + "\" d=\"M " + xOffset + " " +
				translateGraphY(y) + " l " + imageWidth + " " + translateDiagramDy(0) +
				"\" stroke=\"darkblue\" stroke-width=\"1\"  fill=\"none\" />");
			lowerEndOfeRange = e;
			e = e + eGridSize;
		}

		// Display statistical results.
		x = xOffset + imageWidth/2;
		y = -thisYOffset;
		//y = lowerEndOfeRange * yDurationScaleFactor + yOffset + 100;
		int i = 1;
		file.println("<text x=\"" + x + "\" y=\"" + translateDiagramY(y+thisCharHeight*i) +
			"\" text-anchor=\"middle\" style=\"fill:black;font-family:Arial;font-size:14px;kerning:1;\">all: " +
			getStats(eventTypeName) + "</text>");
		for (String rt : lg.getRequestTypes().keySet())
		{
			i = i + 1;
			file.println("<text x=\"" + x + "\" y=\"" + translateDiagramY(y+thisCharHeight*i) +
				"\" text-anchor=\"middle\" style=\"fill:black;font-family:Arial;font-size:14px;kerning:1;\">" +
				rt + ": " + getStats(eventTypeName, rt) + "</text>");
		}
		i = i + 1;
		file.println("<text x=\"" + x + "\" y=\"" + translateDiagramY(y+thisCharHeight*i) +
			"\" text-anchor=\"middle\" style=\"fill:black;font-family:Arial;font-size:14px;kerning:1;\">Duration Statistics</text>");

		// Draw X axis
		x = (int)((maxStartTime - minStartTime) * xScaleFactor) + xOffset;
		file.println("<path id=\"xaxis\" d=\"M " + xOffset + " " +
			translateGraphY(yOffset) + " l " + imageWidth + " " + translateDiagramDy(0) +
			"\" stroke=\"black\" stroke-width=\"3\"  fill=\"none\" />");

		// Draw level (request rate) axis.
		file.println("<path id=\"laxis\" d=\"M " + xOffset + " " + translateGraphY(yOffset) +
			"l 0 " + translateDiagramDy(thisGraphHeight-yOffset) + "\" stroke=\"green\" stroke-width=\"3\"  fill=\"none\" />");

		// Draw duration axis.
		file.println("<path id=\"eaxis\" d=\"M " + xOffset + imageWidth + " " +
			translateGraphY(yOffset) + " l 0 " + translateDiagramDy(thisGraphHeight-yOffset) +
			"\" stroke=\"darkblue\" stroke-width=\"3\"  fill=\"none\" />");
		//file.println("<path id=\"eaxis\" d=\"M #{xOffset + imageWidth} #{translateGraphY(yOffset)} l 0 #{translateDiagramDy(eGridSize * yDurationScaleFactor * (n-1))}\" stroke=\"darkblue\" stroke-width=\"3\"  fill=\"none\" />"

		// Draw actual request rate.
		//file.write "<path id=\"act_req_rate\" d=\"M #{xOffset} #{yOffset}"
		//priorTime = 0.0
		//priorRate = 0.0
		//timeLogData.each do |logDataEntry|
		//    dx = (logDataEntry[1] - priorTime) * xScaleFactor
		//    dy = (logDataEntry[0] - priorRate) * yRateScaleFactor
		//    priorTime = logDataEntry[1]
		//    priorRate = logDataEntry[0]
		//    file.write " l #{dx.to_i} #{dy.to_i}"
		//end
		//file.println("\" stroke=\"gray\" stroke-width=\"3\"  fill=\"none\" />"


		// Draw each profile of the test run.
		String[] colors = {"green", "blue", "orange", "brown"};
		int colorIndex = 0;
		for (String profileName : testRunProfiles())
		{
			PerformanceProfile profile = lg.getPerformanceProfile(profileName);
			Distribution distribution = lg.getDistribution(profile.distribution());

			RequestType requestType = lg.getRequestType(profile.requestType());
			String color = requestType.color();
			//color = colors[colorIndex]
			colorIndex = (colorIndex + 1) % (colors.length);
			int px = xOffset;
			int py = translateGraphY(yOffset);
			file.print("<path id=\"profile_" + profile.name() + "\" d=\"M " + px + " " + py);
			double priorYLevel = 0.0;
			for (double[] level : distribution.levels())
			{
				int dx = (int)(level[1] * 60.0 * xScaleFactor);
				int dy = translateDiagramDy((int)((level[0] - priorYLevel) * yRateScaleFactor));
				priorYLevel = level[0];
				file.print(" l " + dx + " " + dy);
			}
			file.println("\" stroke=\"" + color + "\" stroke-width=\"3\"  fill=\"none\" />");
		}


		// Draw each request.
		i = 0;
		for (ReqTimeLogEntry logDataEntry : reqTimeLogData())
		{
			if (! logDataEntry.name.equals(eventTypeName)) continue;

			i = i + 1;
			int rx = (int)((logDataEntry.startTime) * xScaleFactor) + xOffset;
			int ry = translateGraphY(yOffset);
			//y = ((logDataEntry[TimeLog_StartTimeField]) * yScaleFactor) + yOffset
			int dy = translateDiagramDy((int)(logDataEntry.duration * yDurationScaleFactor));

			// Determine color for the request line.
			String reqTypeName = logDataEntry.reqType;
			RequestType rt = lg.getRequestType(reqTypeName);
			String color;
			if (rt.color() == null)
				color = "darkblue";  // default
			else
				color = rt.color();

			String c = eventColor(eventTypeName);
			if (c != null) color = c; // override other color settings

			// Always draw failed requests in a reddish color.
			String result = logDataEntry.result;
			if (result.equals("false")) color = "red";
			else if (result.equals("incomplete")) color = "orange";
			else if (! result.equals("true")) color = "deeppink";

			//file.println("<circle cx=\"#{x.to_i}\" cy=\"#{(y+dy).to_i}\" r=\"3\" stroke=\"#{color}\" stroke-width=\"1\" fill=\"#{color}\" />"
			file.println("<path id=\"line" + i + "\" d=\"M " + rx + " " + ry +
				" l 0 " + dy + "\" stroke=\"" + color + "\" stroke-width=\"1\"  fill=\"none\" />");
		}

		file.println("</g>");

		file.println(trailer);
	}
}
