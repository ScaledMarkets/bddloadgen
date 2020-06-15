package loadgen.controller;



import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


abstract class AbstractEntryLog
{
	void insertAnyTimeLogEntry(List entryList, AbstractTimeLogEntry newEntry)
	{
		if (newEntry == null) throw new RuntimeException("Internal error");
		int entryPos = 0;
		for (Object o : entryList)
		//for (AbstractTimeLogEntry entry : entryList)
		{
			AbstractTimeLogEntry entry = (AbstractTimeLogEntry)o;
			if (newEntry.getSortableTime() < entry.getSortableTime())
			{
				entryList.add(entryPos, newEntry);  // insert at pos of entry
				return;
			}
			entryPos = entryPos + 1;
		}
		entryList.add(newEntry);  // insert at the end
	}
}

