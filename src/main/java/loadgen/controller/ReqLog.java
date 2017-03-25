package loadgen.controller;


import loadgen.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


class ReqLog extends AbstractEntryLog
{
	private int thisNumberOfTests = 0;
	private BufferedReader br;
	private List<ReqTimeLogEntry> thisTimeLogData = new Vector<ReqTimeLogEntry>();
	
	ReqLog(BufferedReader br) { this.br = br; }
	
	void parse() throws IOException
	{
		for (String line = br.readLine(); line != null; line = br.readLine())
		{
			thisNumberOfTests = thisNumberOfTests + 1;  // each line represents a test
			ReqTimeLogEntry entry = ReqTimeLogEntry.parseLine(line);
			if (entry != null) insertTimeLogEntry(entry);
		}
	}

	/** Return the entries of time log data, consolidated for all nodes. */
	List<ReqTimeLogEntry> timeLogData()
	{
		return thisTimeLogData;
	}

	/** Entries are inserted such that they are in asscending order on the second
		field, which is the reqestStarTime field.
		To do: Improve this with a sorted tree. */
	protected void insertTimeLogEntry(ReqTimeLogEntry newEntry)
	{
		insertAnyTimeLogEntry(timeLogData(), newEntry);
	}
}

