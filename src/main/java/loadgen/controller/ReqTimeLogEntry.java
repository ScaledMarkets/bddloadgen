package loadgen.controller;


import loadgen.*;
import loadgen.controller.templates.SupportedProviders;
import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;
import java.nio.file.Files;


class ReqTimeLogEntry extends AbstractTimeLogEntry
{
	/** These are used to index into columns (fields) of the time log entries. */
	enum sequence {
		ReqTypeField,
		IdField,
		NameField,
		ReqRateField,
		StartTimeField,
		EndTimeField,
		DurationField,
		ResultField
	}
		
	ReqTimeLogEntry(String reqType, String id, String name, double reqRate,
		double startTime, double endTime, double duration, String result)
	{
		super(reqType, id, name, reqRate, startTime, endTime, duration);
		setResult(result);
	}

	static ReqTimeLogEntry parseLine(String line)
	{
		String data = line.trim();
		String[] fields = data.split(",");
		for (int i = 0; i < fields.length; i++) fields[i] = fields[i].trim();
		String reqType = fields[ReqTimeLogEntry.sequence.ReqTypeField.ordinal()].split(".")[1];
		String id = fields[ReqTimeLogEntry.sequence.IdField.ordinal()].split(".")[1];
		String name = fields[ReqTimeLogEntry.sequence.NameField.ordinal()].split(".")[1];
		double reqRate = Double.parseDouble(fields[ReqTimeLogEntry.sequence.ReqRateField.ordinal()].split(".")[1]);
		double startTime = Double.parseDouble(fields[ReqTimeLogEntry.sequence.StartTimeField.ordinal()].split(".")[1]);
		double endTime = Double.parseDouble(fields[ReqTimeLogEntry.sequence.EndTimeField.ordinal()].split(".")[1]);
		double duration = Double.parseDouble(fields[ReqTimeLogEntry.sequence.DurationField.ordinal()].split(".")[1]);
		String result = fields[ReqTimeLogEntry.sequence.ResultField.ordinal()].split(".")[1];
		return new ReqTimeLogEntry(
			reqType, id, name, reqRate, startTime, endTime, duration, result);
	}
}

