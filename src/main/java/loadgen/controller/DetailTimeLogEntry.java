package loadgen.controller;


import loadgen.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


class DetailTimeLogEntry extends AbstractTimeLogEntry
{
	/** These are used to index into columns (fields) of the detail time log entries. */
	enum sequence {
		ReqTypeField,
		IdField,
		NameField,
		ReqRateField,
		BeginOrEndField,
		TimeField
	}
	
	DetailTimeLogEntry(String reqType, String id, String name, double reqRate,
		double startTime, double endTime, double duration)
	{
		super(reqType, id, name, reqRate, startTime, endTime, duration);
	}
}

