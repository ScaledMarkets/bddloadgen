package loadgen.controller;


import loadgen.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


class AggLog extends AbstractEntryLog
{
	private List<AbstractTimeLogEntry> thisAggTimeLogData = new Vector<AbstractTimeLogEntry>();

	AggLog()
	{
	}
	
	protected void insertAggTimeLogEntry(AbstractTimeLogEntry newEntry)
	{
		insertAnyTimeLogEntry(thisAggTimeLogData, newEntry);
	}

	/** Return the aggregated time log data, consolidated for all nodes. Entries
		can either be ReqTimeLogEntry or DetailTimeLogEntry. */
	List<AbstractTimeLogEntry> timeLogData()
	{
		return thisAggTimeLogData;
	}
}

