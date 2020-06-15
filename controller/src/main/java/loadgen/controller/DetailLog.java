package loadgen.controller;



import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


class DetailLog extends AbstractEntryLog
{
	private int thisNumberOfDetailLogEntries;
	private List<DetailTimeLogEntry> thisDetailTimeLogData = new Vector<DetailTimeLogEntry>();
	private BufferedReader br;
	private Map<String, Double> thisUnmatchedEvents = new HashMap<String, Double>();
	private Map<String, Integer> thisEventTypeCount = new HashMap<String, Integer>();
	
	DetailLog(BufferedReader br) { this.br = br; }
	
	void parse() throws IOException
	{
		thisNumberOfDetailLogEntries = 0;
		for (String line = br.readLine(); line != null; line = br.readLine())
		{
			thisNumberOfDetailLogEntries = thisNumberOfDetailLogEntries + 1;
			DetailTimeLogEntry entry = parseLine(line);
			if (entry != null) insertDetailTimeLogEntry(entry);
		}
	}
	
	/** Return the detail (event level) time log data, consolidated for all nodes.
		See the 'TestRunnerUtil' class for an explanation of events. */
	List<DetailTimeLogEntry> timeLogData()
	{
		return thisDetailTimeLogData;
	}
	
	protected DetailTimeLogEntry parseLine(String line)
	{
		String data = line.trim();
		String[] fields = data.split(",");
		for (int i = 0; i < fields.length; i++) fields[i] = fields[i].trim();
		
		String reqType = fields[DetailTimeLogEntry.sequence.ReqTypeField.ordinal()];
		String id = fields[DetailTimeLogEntry.sequence.IdField.ordinal()];
		String name = fields[DetailTimeLogEntry.sequence.NameField.ordinal()];
		double reqRate = Double.parseDouble(fields[DetailTimeLogEntry.sequence.ReqRateField.ordinal()]);
		String beginOrEnd = fields[DetailTimeLogEntry.sequence.BeginOrEndField.ordinal()];
		double time = Double.parseDouble(fields[DetailTimeLogEntry.sequence.TimeField.ordinal()]);
		
		if (beginOrEnd.equals("Begin"))
			pushBeginEvent(reqType, id, name, time);
		else if (beginOrEnd.equals("End"))
		{
			Double beginTime = popMatchingBeginEvent(reqType, id, name);
			if (beginTime != null)
			{
				double delta = time - beginTime.doubleValue();
				return
					new DetailTimeLogEntry(reqType, id, name, reqRate, beginTime, time, delta);
			}
		}
		return null;
	}

	/** Store the specified event in an unmatched event hash. */
	protected void pushBeginEvent(String reqType, String pid, String name, Double beginTime)
	{
		unmatchedEvents().put(reqType + "." + pid + "." + name, beginTime);
		
		// Increment count of this type of event.
		Integer priorCount = eventTypeCount().get(reqType);
		if (priorCount == null) priorCount = new Integer(0);
		eventTypeCount().put(reqType, new Integer(priorCount.intValue() + 1));
	}
	
	/** Find a stored event that matches the specified event, in the unmatched
		event hash, remove it from the list, and return it. */
	protected Double popMatchingBeginEvent(String reqType, String pid, String name)
	{
		String key = reqType + "." + pid + "." + name;
		Double time = unmatchedEvents().get(key);
		if (time != null) unmatchedEvents().remove(key);
		return time;
	}
	
	protected void insertDetailTimeLogEntry(DetailTimeLogEntry newEntry)
	{
		insertAnyTimeLogEntry(thisDetailTimeLogData, newEntry);
	}
	
	/** Return the events that have beginnings (calls to TestRunnerUtil.timelogBegin),
		but no matching call to TestRunnerUtil.timelogEnd. */
	protected Map<String, Double> unmatchedEvents()
	{
		return thisUnmatchedEvents;
	}
	
	
	Map<String, Integer> eventTypeCount()
	{
		return thisEventTypeCount;
	}
}

