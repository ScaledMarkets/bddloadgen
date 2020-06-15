package loadgen.controller;


public class DetailTimeLogEntry extends AbstractTimeLogEntry
{
	/** These are used to index into columns (fields) of the detail time log entries. */
	public enum sequence {
		ReqTypeField,
		IdField,
		NameField,
		ReqRateField,
		BeginOrEndField,
		TimeField
	}

	public DetailTimeLogEntry(String reqType, String id, String name, double reqRate,
		double startTime, double endTime, double duration)
	{
		super(reqType, id, name, reqRate, startTime, endTime, duration);
	}
}
