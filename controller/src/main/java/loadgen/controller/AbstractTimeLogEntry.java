package loadgen.controller;


import java.io.*;
import java.net.*;
import java.util.jar.*;
import java.util.function.*;
import java.util.*;


abstract class AbstractTimeLogEntry
{
	String reqType;
	String id;
	String name;
	double reqRate;
	double startTime;
	double endTime;
	double duration;
	String result;

	AbstractTimeLogEntry(String reqType, String id, String name, double reqRate,
		double startTime, double endTime, double duration)
	{
		this.reqType = reqType; this.id = id; this.name = name; this.reqRate = reqRate;
		this.startTime = startTime; this.endTime = endTime;
		this.duration = duration;
	}

	double getSortableTime() { return startTime; }

	void setResult(String result) { this.result = result; }
}
